/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unimi.di.law.bubing.frontier.revisit;

import it.unimi.di.law.bubing.StartupConfiguration;
import it.unimi.di.law.bubing.frontier.PathQueryState;
import it.unimi.di.law.bubing.util.FetchData;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author karel
 */
public class NonuniformRevisitScheduler implements RevisitScheduler {
	private static final Logger LOGGER = LoggerFactory.getLogger( NonuniformRevisitScheduler.class );
	private static final long BLOCK = TimeUnit.MINUTES.toMillis( 1 );

	private final float INC_INTERVAL;
	private final float DEC_INTERVAL;

	private final long MIN_INTERVAL;
	private final long MAX_INTERVAL;
	private final long DEFAULT_INTERVAL;

	public NonuniformRevisitScheduler() {
		this( TimeUnit.DAYS.toMinutes( 5 ) );
	}

	public NonuniformRevisitScheduler( long defaultInterval ) {
		this( defaultInterval, TimeUnit.HOURS.toMinutes( 1 ), TimeUnit.DAYS.toMinutes( 6 * 30 ) );
	}
        
        public NonuniformRevisitScheduler( long defaultInterval, long minInterval, long maxInterval ) {
                this( defaultInterval, minInterval, maxInterval, 0.2f, 0.2f );
	}

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

	public NonuniformRevisitScheduler( String defaultInterval ) {
		this( StartupConfiguration.parseTime( defaultInterval ) );
	}

	public NonuniformRevisitScheduler( String defaultInterval, String minInterval, String maxInterval ) {
		this( StartupConfiguration.parseTime( defaultInterval ), StartupConfiguration.parseTime( minInterval ), StartupConfiguration.parseTime( maxInterval ) );
	}

       	public NonuniformRevisitScheduler(String defaultInterval, String minInterval, String maxInterval, String decInterval, String incInterval ) {
		this( StartupConfiguration.parseTime( defaultInterval ), StartupConfiguration.parseTime( minInterval ), StartupConfiguration.parseTime( maxInterval ), Float.parseFloat( decInterval ), Float.parseFloat( incInterval ) );
	}

	@Override
	public PathQueryState schedule( FetchData fetchData, PathQueryState pathQuery, boolean modified ) {

 		long inter = pathQuery.fetchInterval;

		if ( pathQuery.modified == PathQueryState.FIRST_VISIT ) {
			pathQuery.modified = fetchData.endTime;
			pathQuery.fetchInterval = DEFAULT_INTERVAL;
		} else if ( modified == true ) {
			pathQuery.modified = fetchData.endTime;
			pathQuery.fetchInterval = Math.round( pathQuery.fetchInterval * (1.0 - DEC_INTERVAL) );
		} else {
			pathQuery.fetchInterval = Math.round( pathQuery.fetchInterval * (1.0 + INC_INTERVAL) );
		}

		final long intervalSinceChange = (fetchData.endTime - pathQuery.modified) / BLOCK;

		if ( pathQuery.fetchInterval < intervalSinceChange ) {
			pathQuery.fetchInterval = intervalSinceChange;
		}

		if ( pathQuery.fetchInterval < MIN_INTERVAL ) {
			pathQuery.fetchInterval = MIN_INTERVAL;
		} else if ( pathQuery.fetchInterval > MAX_INTERVAL ) {
			pathQuery.fetchInterval = MAX_INTERVAL;
		}
		LOGGER.debug( "Modified: {}, old interval: {} min, new interval: {} min", modified, inter, pathQuery.fetchInterval );
		pathQuery.nextFetch = fetchData.endTime + pathQuery.fetchInterval * BLOCK;

		return pathQuery;
	}
}