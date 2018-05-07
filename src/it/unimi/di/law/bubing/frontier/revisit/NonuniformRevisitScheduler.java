package it.unimi.di.law.bubing.frontier.revisit;

/*
 * Copyright (C) 2018 Karel Ondřej
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
 */

import it.unimi.di.law.bubing.StartupConfiguration;
import it.unimi.di.law.bubing.frontier.PathQueryState;
import it.unimi.di.law.bubing.util.FetchData;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schedule next visits when individual sites have adaptive intervals between visits.
 * The interval between visits is calculated based on page changes. If the page changes, the interval is shortened or otherwise increase.
 * 
 * @author xondre09
 */
public class NonuniformRevisitScheduler implements RevisitScheduler {
	private static final Logger LOGGER = LoggerFactory.getLogger( NonuniformRevisitScheduler.class );
	/* Interval accuracy. Intervals are rounded to minutes. */
	private static final long BLOCK = TimeUnit.MINUTES.toMillis( 1 );
	/** Coefficient to increase the interval between visits. */
	private final float INC_INTERVAL;
	/** Coefficient to decrease the interval between visits. */
	private final float DEC_INTERVAL;
	/** Minimal interval between visits. */
	private final long MIN_INTERVAL;
	/** Maximal interval between visits. */
	private final long MAX_INTERVAL;
	/** Default interval between visits. */
	private final long DEFAULT_INTERVAL;
        
	/** Creates the shceduler.
	 */
	public NonuniformRevisitScheduler() {
		this( TimeUnit.DAYS.toMinutes( 5 ) );
	}

	/** Creates the shceduler.
	 * 
	 * @param defaultInterval default interval between visits.
	 */
	public NonuniformRevisitScheduler( long defaultInterval ) {
		this( defaultInterval, TimeUnit.HOURS.toMinutes( 1 ), TimeUnit.DAYS.toMinutes( 6 * 30 ) );
	}

	/** Creates the shceduler.
	 * 
	 * @param defaultInterval the default interval between visits.
	 * @param minInterval the minimal interval between visits.
	 * @param maxInterval the maximal interval between visits.
	 */
        public NonuniformRevisitScheduler( long defaultInterval, long minInterval, long maxInterval ) {
                this( defaultInterval, minInterval, maxInterval, 0.2f, 0.2f );
	}

	/** Creates the shceduler.
	 *
	 * @param defaultInterval the default interval between visits.
	 * @param minInterval the minimal interval between visits.
	 * @param maxInterval the maximal interval between visits.
	 * @param decInterval the coefficient to decrease the interval between visits.
	 * @param incInterval the coefficient to increase the interval between visits.
	 */
	public NonuniformRevisitScheduler( long defaultInterval, long minInterval, long maxInterval, float decInterval, float incInterval ) {
		if ( maxInterval < minInterval ) {
			throw new IllegalArgumentException( "The maximum inteval must be greater than minimum interval." );
		}

		this.DEC_INTERVAL = decInterval;
		this.INC_INTERVAL = incInterval;
		this.MIN_INTERVAL = minInterval / BLOCK;
		this.MAX_INTERVAL = maxInterval / BLOCK;
		this.DEFAULT_INTERVAL = defaultInterval / BLOCK;
	}

	/** Creates the shceduler. Version of constructor for configuration file.
	 *
	 * @param defaultInterval the default interval between visits.
	 */
	public NonuniformRevisitScheduler( String defaultInterval ) {
		this( StartupConfiguration.parseTime( defaultInterval ) );
	}

	/** Creates the shceduler. Version of constructor for configuration file.
	 *
	 * @param defaultInterval the default interval between visits.
	 * @param minInterval the minimal interval between visits.
	 * @param maxInterval the maximal interval between visits.
	 */
	public NonuniformRevisitScheduler( String defaultInterval, String minInterval, String maxInterval ) {
		this( StartupConfiguration.parseTime( defaultInterval ), StartupConfiguration.parseTime( minInterval ), StartupConfiguration.parseTime( maxInterval ) );
	}

	/** Creates the shceduler. Version of constructor for configuration file.
	 *
	 * @param defaultInterval the default interval between visits.
	 * @param minInterval the minimal interval between visits.
	 * @param maxInterval the maximal interval between visits.
	 * @param decInterval the coefficient to decrease the interval between visits.
	 * @param incInterval the coefficient to increase the interval between visits.
	 */
	public NonuniformRevisitScheduler(String defaultInterval, String minInterval, String maxInterval, String decInterval, String incInterval ) {
		this( StartupConfiguration.parseTime( defaultInterval ), StartupConfiguration.parseTime( minInterval ), StartupConfiguration.parseTime( maxInterval ), Float.parseFloat( decInterval ), Float.parseFloat( incInterval ) );
	}

	@Override
	public PathQueryState schedule( FetchData fetchData, PathQueryState pathQuery, boolean modified ) {

 		final long interval = pathQuery.fetchInterval;
                // first visit of page
		if ( pathQuery.modified == PathQueryState.FIRST_VISIT ) {
			pathQuery.modified = fetchData.endTime;
			pathQuery.fetchInterval = DEFAULT_INTERVAL;
		} else if ( modified == true ) {
                        // page was modified
			pathQuery.modified = fetchData.endTime;
			pathQuery.fetchInterval = Math.round( pathQuery.fetchInterval * (1.0 - DEC_INTERVAL) );
		} else {
                        // page was not modified
			pathQuery.fetchInterval = Math.round( pathQuery.fetchInterval * (1.0 + INC_INTERVAL) );
		}
                // the time since the last change detected
		final long intervalSinceChange = (fetchData.endTime - pathQuery.modified) / BLOCK;

		if ( pathQuery.fetchInterval < intervalSinceChange ) {
			pathQuery.fetchInterval = intervalSinceChange;
		}
                
		if ( pathQuery.fetchInterval < MIN_INTERVAL ) {
			pathQuery.fetchInterval = MIN_INTERVAL;
		} else if ( pathQuery.fetchInterval > MAX_INTERVAL ) {
			pathQuery.fetchInterval = MAX_INTERVAL;
		}
		LOGGER.debug( "Modified: {}, old interval: {} min, new interval: {} min", modified, interval, pathQuery.fetchInterval );
		pathQuery.nextFetch = fetchData.endTime + pathQuery.fetchInterval * BLOCK;

		return pathQuery;
	}
}