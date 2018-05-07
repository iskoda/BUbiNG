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

/** Schedule next visit when individual sites have same interval between visits.
 * 
 * @author xondre09
 */
public class UniformRevisitScheduler implements RevisitScheduler {
	/** Default interval between visits. */
	long defaultInterval;

	/** Create the scheduler.
	 */
	public UniformRevisitScheduler() {
		this(TimeUnit.MINUTES.toMillis( 1 ));
	}

	/** Create the scheduler.
	 *
	 * @param defaultInterval the interval between visits.
	 */
	public UniformRevisitScheduler(long defaultInterval) {
		this.defaultInterval = defaultInterval;
	}
        
	/** Creates the shceduler. Version of constructor for configuration file.
	 * 
	 * @param defaultInterval the interval between visits.
	 */
	public UniformRevisitScheduler(String defaultInterval) {
		this( StartupConfiguration.parseTime(defaultInterval) / TimeUnit.MINUTES.toMillis(1) );
	}
            
	@Override
	public PathQueryState schedule(FetchData fetchData, PathQueryState pathQuery, boolean modified) {
                // first visit of page
		if (modified || pathQuery.modified == PathQueryState.FIRST_VISIT) {
			pathQuery.modified = fetchData.endTime;
		}
		pathQuery.nextFetch = fetchData.endTime + TimeUnit.MINUTES.toMillis(this.defaultInterval);
		pathQuery.fetchInterval = this.defaultInterval;

		return pathQuery;
	}
}
