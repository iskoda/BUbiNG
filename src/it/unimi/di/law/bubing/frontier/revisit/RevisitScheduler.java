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

import it.unimi.di.law.bubing.frontier.PathQueryState;
import it.unimi.di.law.bubing.util.FetchData;

/**
 * Interface for schedule a re-visit.
 * @author karel
 */
public interface RevisitScheduler {
	/** Schedule for next visit.
	 *  
         * Method will get fetch data with the page download time, path+query
	 * with information about the last visit and modified flag.
	 * 
	 * @param fetchData information about fetch web page
	 * @param pathQuery information about last visit and re-visit
	 * @param modified the page was modified
	 * @return 
	 */
	public PathQueryState schedule(FetchData fetchData, PathQueryState pathQuery, boolean modified);
}
