package it.unimi.di.law.bubing.frontier;

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

import com.google.common.primitives.Longs;
import it.unimi.di.law.bubing.util.BURL;
import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * A class maintaining the current state of the visit of a specific path+query.
 * @author karel
 */
public class PathQueryState implements Delayed, Serializable {
	/** Value of modified for first visit. */
	public static final long FIRST_VISIT = 0;
	/** Path+query of URL. */
	public byte[] pathQuery;
        /** The time of last detect change. */
	public volatile long modified;
        /** The time of the next scheduled visit. */
	public volatile long nextFetch;
        /** The time betwen two visits. */
	public volatile long fetchInterval;
        /** Visit state to witch this path+query belongs. */
	public volatile VisitState visitState;

	/**
	 * Creates a PathQueryState.
	 * 
	 * @param visitState the visit state of this PathQueryState
	 * @param pathQuery the path+query of url
	 */
	public PathQueryState( VisitState visitState, byte[] pathQuery ) {
		this( visitState, pathQuery, System.currentTimeMillis() );
	}

	/**
	 * Creates a PathQueryState
	 * 
	 * @param visitState the visit state of this PathQueryState
	 * @param pathQuery the path+query of an URL
	 * @param nextFetch the time of a next visit
	 */
	public PathQueryState( VisitState visitState, byte[] pathQuery, long nextFetch ) {
		this.pathQuery = pathQuery;
		this.modified = PathQueryState.FIRST_VISIT;
		this.nextFetch = nextFetch;
		this.fetchInterval = 0;
		this.visitState = visitState;
	}

	@Override
	public String toString() {
		return BURL.fromNormalizedSchemeAuthorityAndPathQuery( this.visitState.schemeAuthority, this.pathQuery ).toString();
	}

	@Override
	public long getDelay( final TimeUnit unit ) {
		return unit.convert( Math.max( 0, nextFetch - System.currentTimeMillis() ), TimeUnit.MILLISECONDS );
	}

	@Override
	public int compareTo( final Delayed o ) {
		return Long.signum( nextFetch - ( (PathQueryState)o ).nextFetch );
	}

	/** Returns the memory usage in bytes.
	 * 
	 * header of instance       8 bytes
	 * byte[] pathQuery         8 bytes + memory usage of byte array (16 + array length)
	 * long modified            8 bytes
	 * long nextFetch           8 bytes
	 * long fetchInterval       8 bytes
	 * VisitState visitState    8 bytes
         * 
	 * @return its memory usage in bytes.
	 */
	public int memoryUsage() {
		return 48 + BURL.memoryUsageOf( pathQuery );
	}

	public static byte[] pathQueryStateToBytes( PathQueryState pathQuery) {
		byte[] buffer = new byte[24 + pathQuery.pathQuery.length];
		System.arraycopy( Longs.toByteArray( pathQuery.nextFetch ), 0, buffer, 0, 8 );
		System.arraycopy( Longs.toByteArray( pathQuery.modified ), 0, buffer, 8, 8 );
		System.arraycopy( Longs.toByteArray( pathQuery.fetchInterval ), 0, buffer, 16, 8 );
		System.arraycopy( pathQuery.pathQuery, 0, buffer, 24, pathQuery.pathQuery.length );

		return buffer;
	}

	/**
	 * Convert array of byte to PathQueryState.
	 * Byte array consists of: the time of the next visit, the time of the 
	 * last change, the interval between visits and path+query.
	 * 
	 * @param visitState Visit state to witch path+query belongs
	 * @param bytes Array of byte with PathQueryState information
	 * @return 
	 */
	public static PathQueryState bytesToPathQueryState( VisitState visitState, byte[] bytes ) {
		assert bytes.length >= 24;
		PathQueryState pathQuery = new PathQueryState( visitState, Arrays.copyOfRange( bytes, 24, bytes.length ) );
		pathQuery.nextFetch = Longs.fromByteArray( Arrays.copyOfRange( bytes, 0, 8 ) );
		pathQuery.modified = Longs.fromByteArray( Arrays.copyOfRange( bytes, 8, 16 ) );
		pathQuery.fetchInterval = Longs.fromByteArray( Arrays.copyOfRange( bytes, 16, 24 ) );

		return pathQuery;
	}
}
