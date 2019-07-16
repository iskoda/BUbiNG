package it.unimi.di.law.bubing.frontier;

/*
 * Copyright (C) 2013-2017 Paolo Boldi, Massimo Santini, and Sebastiano Vigna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * FILE CHANGED BY KAREL ONDŘEJ
 * NOTICE: 04/2018 - Incremental crawling 
 *         07/2018 - Added stats
 */

import it.unimi.di.law.bubing.util.BURL;
import it.unimi.dsi.Util;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.stat.SummaryStats;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//RELEASE-STATUS: DIST

/** A thread that distributes {@linkplain Frontier#readyURLs ready URLs}
 *  (coming out of the {@linkplain Frontier#sieve sieve}) into
 *  the {@link Workbench} queues with the help of a {@link WorkbenchVirtualizer}.
 *  We invite the reader to consult the documentation of {@link WorkbenchVirtualizer} first.
 *
 *  <p>The behaviour of this distributor is controlled by two conditions:
 *  <ol>
 *  	<li>Whether the workbench is full.
 *  	<li>Whether the front is large enough, that is, whether the number of visit states
 *  		in the {@linkplain Frontier#todo todo list} (which are actually thought as being representative of their own IP address)
 *          plus {@link Frontier#workbench}.size()
 *  		is below the current required size (i.e., the front is counted in IP addresses).
 *  </ol>
 *
 *  <p>Then, this distributor iteratively:
 *  <ul>
 *  	<li>Checks that the workbench is not full and that the front is not large enough. If either condition
 *  	fails, there is no point in doing I/O.
 *  	<li>If the conditions are fulfilled, the distributor checks the {@link Frontier#refill} queue
 *		to see whether there are visit states requiring a refill from the {@link WorkbenchVirtualizer},
 *		in which case it performs a refill.
 *  	<li>Otherwise, if there are no ready URLs and it is too early to force a flush of the sieve, this thread is put
 *      to sleep with an exponential backoff.
 *      <li>Otherwise, (possibly after a flush) a ready URL is loaded from {@link Frontier#readyURLs} and either deleted
 *      (if we already have too many URLs for its scheme+authority),
 *      or enqueued to the workbench (if its visit state has no virtualized URLs and has not reached {@link VisitState#pathQueryLimit()}),
 *      or otherwise enqueued to the {@link WorkbenchVirtualizer}.
 *  </ul>
 *
 */
public final class Distributor extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(Distributor.class);
	/** We purge {@linkplain VisitState visit states} from {@link #schemeAuthority2VisitState} when
	 * this amount of time has passed (approximately) since the last fetch. */
	private static final long PURGE_DELAY = TimeUnit.DAYS.toMillis(365);
	/** We prints low-cost stats at this interval. */
	private static final long LOW_COST_STATS_INTERVAL = TimeUnit.SECONDS.toMillis(10);
	/** We prints high-cost stats at this interval. */
	private static final long HIGH_COST_STATS_INTERVAL = TimeUnit.MINUTES.toMillis(1);
	/** We check for visit states to be purged at this interval. */
	private static final long PURGE_CHECK_INTERVAL = TimeUnit.MINUTES.toMillis(15);
	/** We check for visit states with ready path+query to fetch. */
	private final long REVISIT_CHECK_INTERVAL;

	/** A reference to the frontier. */
	private final Frontier frontier;
	/** An <strong>unsynchronized</strong> map from scheme+authorities to the corresponding {@link VisitState}. */
	protected final VisitStateSet schemeAuthority2VisitState;
	/** The thread printing statistics. */
	protected final StatsThread statsThread;
	/** The last time we produced a high-cost statistics. */
	protected volatile long lastHighCostStat;
	/** The last time we checked for visit states to be purged. */
	protected volatile long lastPurgeCheck;
	/** */
	protected volatile long lastRevisitCheck;

	/** Creates a distributor for the given frontier.
	 *
	 * @param frontier the frontier instantiating this distribution.
	 */
	public Distributor(final Frontier frontier) {
		this.frontier = frontier;
		this.schemeAuthority2VisitState = new VisitStateSet();
		setName(this.getClass().getSimpleName());
		setPriority(Thread.MAX_PRIORITY);
		statsThread = new StatsThread(frontier, this);
                REVISIT_CHECK_INTERVAL=frontier.rc.revisitCheckPeriodicity;
	}

	@Override
	public void run() {
		try {
			long movedFromQueues = 0, deletedFromQueues = 0, lastLowCostStat = 0;
			long fullWorkbenchSleepTime = 0, largeFrontSleepTime = 0, noReadyURLsSleepTime = 0;
			long movedFromSieveToVirtualizer = 0, movedFromSieveToOverflow = 0, movedFromSieveToWorkbench = 0, deletedFromSieve = 0;
			/* During the following loop, you should set round to -1 every time something useful is done (e.g., a URL is read from the sieve, or from the virtual queues etc.) */
			ByteArrayList byteList = new ByteArrayList();
			for(int round = 0; ; round++) {
				frontier.rc.ensureNotPaused();
				if (frontier.rc.stopping) break;
				long now = System.currentTimeMillis();

				final boolean workbenchIsFull = frontier.workbenchIsFull();
				final boolean frontIsSmall = frontIsSmall();

				PathQueryState pathQueryToRevisit;
				for(int i = 100; i != 0 && (pathQueryToRevisit = frontier.revisit.poll()) != null; i--) {
					round = -1;
					VisitState visitState = pathQueryToRevisit.visitState;
					frontier.virtualizer.enqueuePathQueryState(visitState, pathQueryToRevisit);
				}

				/* Redistribute the URL from the virtualizer workbench */
				VisitState nonLocal = frontier.nonLocalVisitStates.poll();
				if (nonLocal != null) {
					round = -1;
					final int pathQueryLimit = (int)frontier.virtualizer.count(nonLocal);
					if (pathQueryLimit != 0) {
						final int dequeuedURLs = frontier.virtualizer.dequeuePathQueries(nonLocal, pathQueryLimit);
						movedFromQueues += dequeuedURLs;
					}
					frontier.submitVisitState(nonLocal);
				}
				/* The basic logic of workbench updates is that if the front is large enough, we don't do anything.
				 * In this way we both automatically batch disk reads and reduce core memory usage. The required
				 * front size is adaptively set by FetchingThread instances when they detect that the
				 * visit states in the todo list plus workbench.size() is below the current required size
				 * (i.e., we are counting IPs). */
				else if (! workbenchIsFull) {
					synchronized(frontier.sieve) {} // We stop here if we are flushing.

					VisitState visitState = frontier.refill.poll();
					if (visitState != null) { // The priority is given to already started visits
						round = -1;
						if (! visitState.isEmpty()) LOGGER.info("Visit state is not empty: "  + visitState);
						else if (frontier.virtualizer.count(visitState) == 0) LOGGER.info("No URLs on disk during refill: " + visitState);
						else if (! frontier.virtualizer.isReadyVisitState(visitState)) LOGGER.info("No ready URLs on disk during refill: " + visitState);
						else {
							// Note that this might make temporarily the workbench too big by a little bit.
							final int pathQueryLimit = visitState.pathQueryLimit();
							if (LOGGER.isDebugEnabled()) LOGGER.debug("Refilling {} with {} URLs", visitState, Integer.valueOf(pathQueryLimit));
							visitState.checkRobots(now);
							final int dequeuedURLs = frontier.virtualizer.dequeuePathQueriesState(visitState, pathQueryLimit);
							movedFromQueues += dequeuedURLs;
						}
                                                visitState.refill= false;
					}
					else if (frontIsSmall){
						// It is necessary to enrich the workbench picking up URLs from the sieve
						if (frontier.readyURLs.isEmpty() && now >= frontier.nextFlush) { // No URLs--time for a forced flush
							round = -1;
							frontier.sieve.flush();
							final long endOfFlush = System.currentTimeMillis();
							frontier.nextFlush = endOfFlush + Math.max(Frontier.MIN_FLUSH_INTERVAL, (endOfFlush - now) * 10);
							now = endOfFlush;
						}

						// Note that this might make temporarily the workbench too big by a little bit.
						for(int i = 100; i-- != 0 && ! frontier.readyURLs.isEmpty();) {
							round = -1;
							frontier.readyURLs.dequeue();
							final ByteArrayList url = frontier.readyURLs.buffer();
							final byte[] urlBuffer = url.elements();
							final int startOfpathAndQuery = BURL.startOfpathAndQuery(urlBuffer);
							final byte[] pathQuery = BURL.pathAndQueryAsByteArray(url);

							final int currentlyInStore = frontier.schemeAuthority2Count.get(urlBuffer, 0, startOfpathAndQuery);
							if (currentlyInStore < frontier.rc.maxUrlsPerSchemeAuthority) { // We have space for this scheme+authority

								visitState = schemeAuthority2VisitState.get(urlBuffer, 0, startOfpathAndQuery);

								if (visitState == null) {
									final byte[] schemeAuthority = BURL.schemeAndAuthorityAsByteArray(urlBuffer);
									if (LOGGER.isTraceEnabled()) LOGGER.trace("New scheme+authority {} with path+query {}", it.unimi.di.law.bubing.util.Util.toString(schemeAuthority), it.unimi.di.law.bubing.util.Util.toString(BURL.pathAndQueryAsByteArray(url)));
									visitState = new VisitState(frontier, schemeAuthority);
									visitState.lastRobotsFetch = Long.MAX_VALUE; // This inhibits further enqueueing until robots.txt is fetched.
									visitState.enqueueRobots();
									visitState.enqueuePathQuery(new PathQueryState(visitState, pathQuery));
									schemeAuthority2VisitState.add(visitState);
									// Send the visit state to the DNS threads
									frontier.newVisitStates.add(visitState);
									movedFromSieveToWorkbench++;
								}
								else {
									if (! frontier.enqueueURLWithIP(url, visitState)) {
										if (frontier.virtualizer.isReadyVisitState(visitState)) {
											// Safe: there are URLs on disk, and this fact cannot change concurrently.
											movedFromSieveToVirtualizer++;
											frontier.virtualizer.enqueuePathQueryState(visitState, new PathQueryState(visitState, pathQuery));
										}
										else if (visitState.size() < visitState.pathQueryLimit() && visitState.workbenchEntry != null && visitState.lastExceptionClass == null) {
											/* Safe: we are enqueueing to a sane (modulo race conditions)
											 * visit state, which will be necessarily go through the DoneThread later. */
											visitState.checkRobots(now);
											visitState.enqueuePathQuery(new PathQueryState(visitState, pathQuery));
											movedFromSieveToWorkbench++;
										}
										else { // visitState.urlsOnDisk == 0
											movedFromSieveToVirtualizer++;
											frontier.virtualizer.enqueuePathQueryState(visitState, new PathQueryState(visitState, pathQuery));
										}
									}
								}
							}
							else deletedFromSieve++;
						}
					}
				}

				if (now - LOW_COST_STATS_INTERVAL > lastLowCostStat) {
					final long overallSieve = movedFromSieveToVirtualizer + movedFromSieveToWorkbench + movedFromSieveToOverflow + deletedFromSieve;
					final long overallQueues = movedFromQueues + deletedFromQueues;
					if (overallSieve != 0) LOGGER.info("Moved " + overallSieve  + " URLs from sieve (" + Util.format(100.0 * deletedFromSieve / overallSieve) + "% deleted, " + Util.format(100.0 * movedFromSieveToWorkbench / overallSieve) + "% to workbench, " + Util.format(100.0 * movedFromSieveToVirtualizer / overallSieve) + "% to virtual queues, " + Util.format(100.0 * movedFromSieveToOverflow / overallSieve) + "% to overflow)");
					if (overallQueues != 0) LOGGER.info("Moved " + overallQueues + " URLs from queues (" + Util.format(100.0 * deletedFromQueues / overallQueues) + "% deleted)");
					movedFromSieveToVirtualizer = movedFromSieveToWorkbench = movedFromSieveToOverflow = movedFromQueues = deletedFromSieve = deletedFromQueues = 0;

					LOGGER.info("Sleeping: large front " + largeFrontSleepTime + ", full workbench " + fullWorkbenchSleepTime + ", no ready URLs " + noReadyURLsSleepTime);
					largeFrontSleepTime = 0;
					fullWorkbenchSleepTime = 0;
					noReadyURLsSleepTime = 0;
					statsThread.emit();
					lastLowCostStat = now;

					SummaryStats entrySummaryStats = frontier.getStatsThread().entrySummaryStats;
					long time = System.currentTimeMillis();
					LOGGER.info("Agent stats: "+
						time+";"+
						frontier.workbench.approximatedSize()+";"+                  // IPOnWorkbench
						frontier.pathQueriesInQueues.get()+";"+                     // URLsInQueues
						(100.0 * frontier.weightOfpathQueriesInQueues.get() / frontier.rc.workbenchMaxByteSize)+";"+ // URLsInQueuesPercentage
						frontier.getStatsThread().brokenPathQueryCount+";"+         // broken
						frontier.brokenVisitStates.get()+";"+                       // brokenVisitStates
						frontier.getStatsThread().brokenVisitStatesOnWorkbench+";"+ // broeknVisitStatesOnWorkbench
						frontier.transferredBytes.get()+";"+                        // bytes
						(100.0 * frontier.duplicates.get() / (1 + frontier.archetypes()))+";"+ // duplicatesPercentage
						frontier.duplicates.get()+";"+                              // duplicates
						(entrySummaryStats != null ? entrySummaryStats.mean() : "0.0")+";"+ // entryAverage
						(entrySummaryStats != null ? entrySummaryStats.max() : "0.0")+";"+ // entryMax
						(entrySummaryStats != null ? entrySummaryStats.min() : "0.0")+";"+ // entryMin
						(entrySummaryStats != null ? entrySummaryStats.variance() : "0.0")+";"+ // entryVariance
						(int)frontier.results.size()+";"+                           // readyToParse
						frontier.readyURLs.size64()+";"+                            // readyURLs
						frontier.numberOfReceivedURLs.get()+";"+                    // receivedURLs
						(frontier.fetchedResources.get() + frontier.fetchedRobots.get())+";"+ // requests
						frontier.requiredFrontSize.get()+";"+                       // requiredFrontSize
						frontier.getStatsThread().resolvedVisitStates+";"+          // resolvedVisitStates
						frontier.fetchedResources.get()+";"+                        // resources
						(frontier.archetypes() + frontier.duplicates.get())+";"+      // storeSize
						frontier.todo.size()+";"+                                   // toDoSize
						frontier.unknownHosts.size()+";"+                           // unknownHosts
						frontier.getStatsThread().unresolved+";"+                   // unresolved
						frontier.getStatsThread().getVisitStates()+";"+             // visitStates
						frontier.getStatsThread().getVisitStatesOnDisk()+";"+       // visitStatesOnDisk
						(entrySummaryStats != null ? (long)entrySummaryStats.sum() : "0")+";"+// visitStatesOnWorkbench
						frontier.newVisitStates.size()+";"+                         // waitingVisitStates
						frontier.weightOfpathQueriesInQueues.get()                  // workbenchByteSize
					);

					frontier.virtualizer.collectIf(.50, .75);
				}

				if (now - HIGH_COST_STATS_INTERVAL > lastHighCostStat) {
					lastHighCostStat = Long.MAX_VALUE;
					final Thread thread = new Thread(statsThread, statsThread.getClass().getSimpleName());
					thread.start();
				}
				
				if (now - PURGE_CHECK_INTERVAL > lastPurgeCheck) {
					for(VisitState visitState: schemeAuthority2VisitState.visitStates())
						if (visitState != null) {
							/* We've been scheduled for purge, or we have fetched at least a
							 * URL but haven't seen a URL for a PURGE_DELAY interval. Note that in the second case
							 * we do not modify schemeAuthority2Count, as we might encounter some more URLs for the
							 * same visit state later, in which case we will create it again. */
							if (visitState.nextFetch == Long.MAX_VALUE || visitState.nextFetch != 0 && visitState.nextFetch < now - PURGE_DELAY && visitState.isEmpty() && ! visitState.acquired && visitState.lastExceptionClass == null && frontier.virtualizer.count(visitState) > 0) {
								LOGGER.info((visitState.nextFetch == Long.MAX_VALUE ? "Purging " : "Purging by delay ") + visitState);
								// This will modify the backing array on which we are enumerating, but it won't be a serious problem.
								frontier.virtualizer.remove(visitState);
								schemeAuthority2VisitState.remove(visitState);
							}
						}
					lastPurgeCheck = now;
				}

				if (now - REVISIT_CHECK_INTERVAL > lastRevisitCheck) {
					final long time = System.currentTimeMillis();
					long readyVisitState = 0;
					long readyToRefill = 0;
					for(VisitState visitState: schemeAuthority2VisitState.visitStates()) {
						// visit state ready to fetch
						if (visitState != null && frontier.virtualizer.isReadyVisitState(visitState)) {
							readyVisitState++;
							// visit state is empty
							if (visitState.isEmpty() && !visitState.acquired && !visitState.refill) {
								visitState.refill = true;
								readyToRefill++;
								frontier.refill.add( visitState );
							}
						}
					}
					final long time2 = System.currentTimeMillis();
					lastRevisitCheck = now;
					LOGGER.info("Revisit check spend {} ms, {} visit states ready to visit and {} visit states add to refill queue.", time2 - time, readyVisitState, readyToRefill);
				}

				if (round != -1) {
					final int sleepTime = 1 << Math.min(10, round);
					if (! frontIsSmall) largeFrontSleepTime += sleepTime;
					else if (workbenchIsFull) fullWorkbenchSleepTime += sleepTime;
					else noReadyURLsSleepTime += sleepTime;
					if (frontier.rc.stopping) break;
					Thread.sleep(sleepTime);
				}
			}

			LOGGER.info("Completed.");
		}
		catch (Throwable t) {
			LOGGER.error("Unexpected exception", t);
			return;
		}
	}

	/** Determines whether the front is small. The front size (in IPs) is obtained by adding the size of the {@link Frontier#todo} list and
	 *  the number of nonbroken workbench entries (i.e., IPs that are in the workbench having at least one nonbroken {@link VisitState} in them).
	 *  The front size is considered to be small if it is smaller than {@link Frontier#requiredFrontSize}.
	 *
	 * @return whether the front is small.
	 */
	private boolean frontIsSmall() {
		return frontier.todo.size() + frontier.workbench.approximatedSize() - frontier.workbench.broken.get() <= frontier.requiredFrontSize.get();
	}
}
