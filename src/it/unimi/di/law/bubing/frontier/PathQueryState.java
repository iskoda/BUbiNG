/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unimi.di.law.bubing.frontier;

import com.google.common.primitives.Longs;
import it.unimi.di.law.bubing.util.BURL;
import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author karel
 */
public class PathQueryState implements Delayed, Serializable {

	public static final long FIRST_VISIT = 0;

	public byte[] pathQuery;
	public volatile long modified;
	public volatile long nextFetch;
	public volatile long fetchInterval;
	public volatile VisitState visitState;
        
	public PathQueryState( VisitState visitState, byte[] pathQuery ) {
		this( visitState, pathQuery, System.currentTimeMillis() );
	}

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
	 * byte[] pathQuery         16 + array length + paddig bytes
	 * long modified            8 bytes
	 * long nextFetch           8 bytes
	 * long fetchInterval       8 bytes
	 * VisitState visitState    4(8?) bytes
         * 
	 * @return its memory usage in bytes.
	 */
	public int memoryUsage() {
		return 40 + BURL.memoryUsageOf( pathQuery );
	}

	public static byte[] pathQueryStateToBytes( PathQueryState pathQuery) {
		byte[] buffer = new byte[24 + pathQuery.pathQuery.length];
		System.arraycopy( Longs.toByteArray( pathQuery.nextFetch ), 0, buffer, 0, 8 );
		System.arraycopy( Longs.toByteArray( pathQuery.modified ), 0, buffer, 8, 8 );
		System.arraycopy( Longs.toByteArray( pathQuery.fetchInterval ), 0, buffer, 16, 8 );
		System.arraycopy( pathQuery.pathQuery, 0, buffer, 24, pathQuery.pathQuery.length );

		return buffer;
	}

	public static PathQueryState bytesToPathQueryState( VisitState visitState, byte[] bytes ) {
		assert bytes.length >= 24;
		PathQueryState pathQuery = new PathQueryState( visitState, Arrays.copyOfRange( bytes, 24, bytes.length ) );
		pathQuery.nextFetch = Longs.fromByteArray( Arrays.copyOfRange( bytes, 0, 8 ) );
		pathQuery.modified = Longs.fromByteArray( Arrays.copyOfRange( bytes, 8, 16 ) );
		pathQuery.fetchInterval = Longs.fromByteArray( Arrays.copyOfRange( bytes, 16, 24 ) );

		return pathQuery;
	}
}
