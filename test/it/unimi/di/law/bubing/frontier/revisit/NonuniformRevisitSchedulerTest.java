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

import it.unimi.di.law.bubing.RuntimeConfiguration;
import it.unimi.di.law.bubing.StartupConfiguration;
import it.unimi.di.law.bubing.frontier.PathQueryState;
import it.unimi.di.law.bubing.util.FetchData;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Test;

/** Simulation for adaptive revisit strategy.
 *
 * @author xondre09
 */
public class NonuniformRevisitSchedulerTest {
	private RuntimeConfiguration testConfiguration;

	public float INC_INTERVAL = 0.2f;
	public float DEC_INTERVAL = 0.2f;

	public long MIN_INTERVAL;
	public long MAX_INTERVAL;
	public long DEFAULT_INTERVAL;

	public long updateInterval;

        @Before
	public void setupTestConfiguration() throws ConfigurationException, IOException, IllegalArgumentException, ClassNotFoundException {
		final BaseConfiguration configuration = new BaseConfiguration();
		configuration.addProperty( "name", "BUbiNG" );
		configuration.addProperty( "group", "test" );
		configuration.addProperty( "weight", "1" );
		configuration.addProperty( "crawlIsNew", "false" );
		testConfiguration = new RuntimeConfiguration( new StartupConfiguration( "data/it/unimi/di/law/bubing/util/bubing-test.properties", configuration ) );
	}

	public void process() throws NoSuchAlgorithmException, IllegalArgumentException, IOException {

		NonuniformRevisitScheduler revisitScheduler = new NonuniformRevisitScheduler( DEFAULT_INTERVAL, MIN_INTERVAL, MAX_INTERVAL, DEC_INTERVAL, INC_INTERVAL );

		PathQueryState pathQuery = new PathQueryState( null,  new byte[]{ '/' }  );
		FetchData fetchData = new FetchData( testConfiguration );

		long start = System.currentTimeMillis();
		long time = start;  // time of simulation

		long nextUpdate = time-1;   // time of next update of page
		boolean modified = true;    // page is modified

		long miss = 0; 
		long hit = 0; 
		long changed = 0;

		long endSimulation = time + TimeUnit.DAYS.toMillis( 365 );

		Random generator = new Random();
		while ( time < endSimulation ) {
			// page changed
			if ( nextUpdate < time ) {
				modified = true;
				// nextUpdate += generator.nextGaussian() * (0.1*updateInterval) + updateInterval;
				nextUpdate += updateInterval;
				changed += 1;
			}
			// page may be visit
			if ( pathQuery.nextFetch < time ) {
				if ( modified ) hit += 1;
				else miss += 1;

				fetchData.endTime = time;
				revisitScheduler.schedule( fetchData, pathQuery, modified );
				modified = false;
				System.err.println( (time-start) + ";" + changed + ";" + hit + ";" + miss + ";" );
			}

			time += 1000;
		}
		System.err.println( this.toString() + ", Changed: " + changed + ", hit: " + hit + ", miss: " + miss );
        }

        @Override
	public String toString() {
		return "INTERVAL: " + MIN_INTERVAL/60000 + "m < " + DEFAULT_INTERVAL/60000 + "m < " + MAX_INTERVAL/60000 + ", RATION: +" + String.format( "%.2f", INC_INTERVAL ) + ", -" + String.format( "%.2f", DEC_INTERVAL );
	}

	@Test
	public void simulation() throws Exception {
		this.MIN_INTERVAL = TimeUnit.MINUTES.toMillis( 1 );
		this.MAX_INTERVAL = TimeUnit.DAYS.toMillis( 180 );
		this.DEFAULT_INTERVAL = TimeUnit.DAYS.toMillis( 5 );

		updateInterval = TimeUnit.DAYS.toMillis( 1 );

		this.INC_INTERVAL = this.DEC_INTERVAL = 0.3f;   
		this.process();        
	}
}