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

import it.unimi.di.law.bubing.frontier.VisitState;
import java.io.Serializable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/** UNUSED. Item of revisit queue.
 *
 * @author xondre09
 */
public class RevisitState implements Delayed, Serializable {
	public VisitState visitState;
	public long nextFetch;

	public RevisitState( VisitState visitState ) {
		this( visitState,  Long.MAX_VALUE );
	}

	public RevisitState( VisitState visitState, long nextFetch ) {
		this.visitState = visitState;
		this.nextFetch = nextFetch;
	}

	@Override
	public long getDelay( final TimeUnit unit ) {
		return unit.convert( Math.max( 0, nextFetch - System.currentTimeMillis() ), TimeUnit.MILLISECONDS );
	}

	@Override
	public int compareTo( final Delayed o ) {
		return Long.signum( nextFetch - ( (RevisitState)o ).nextFetch );
	}
}
