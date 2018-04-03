/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unimi.di.law.bubing.frontier.revisit;

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
 * @author karel
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
		testConfiguration = new RuntimeConfiguration( new StartupConfiguration( "test/it/unimi/di/law/data/bubing-test.properties", configuration ) );
		System.err.println( testConfiguration.rootDir );
	}

        public void process() throws NoSuchAlgorithmException, IllegalArgumentException, IOException {
            
                NonuniformRevisitScheduler revisitScheduler = new NonuniformRevisitScheduler( MIN_INTERVAL, MAX_INTERVAL, DEFAULT_INTERVAL, DEC_INTERVAL, INC_INTERVAL );
                
                PathQueryState pathQuery = new PathQueryState( null,  new byte[]{ '/' }  );
		FetchData fetchData = new FetchData( testConfiguration );
                
                long time = System.currentTimeMillis();
                
                long nextUpdate = time-1; 
                boolean modified = true;
                
                long miss = 0;
                long hit = 0;
                long changed = 0;
                
                long endSimulation = time + TimeUnit.DAYS.toMillis( 365 );
                
                Random generator = new Random();
                while ( time < endSimulation ) {
                    
                    if ( nextUpdate < time ) {
                        modified = true;
                        // nextUpdate += generator.nextGaussian() * (0.1*updateInterval) + updateInterval;
                        nextUpdate += updateInterval;
                        changed += 1;
                    }
                    
                    if ( pathQuery.nextFetch < time ) {
                        if ( modified ) hit += 1;
                        else miss += 1;
                        
                        fetchData.endTime = time;
                        revisitScheduler.schedule( fetchData, pathQuery, modified );
                        modified = false;
                    }
                    
                    time += 1000;
                }
                
                // System.err.println( this.toString() + ", Changed: " + changed + ", hit: " + hit + ", miss: " + miss );
                System.err.println( String.format( "%.2f", INC_INTERVAL ) + ";" + String.format( "%.2f", DEC_INTERVAL ) + ";" + changed + ";" + hit + ";" + miss + ";" );
        }
        
        @Override
        public String toString() {
            return "INTERVAL: " + MIN_INTERVAL/60000 + "m < " + DEFAULT_INTERVAL/60000 + "m < " + MAX_INTERVAL/60000 + ", RATION: +" + String.format( "%.2f", INC_INTERVAL ) + ", -" + String.format( "%.2f", DEC_INTERVAL );
        }
        
        @Test
        public void simulation() throws Exception {
            this.MIN_INTERVAL = TimeUnit.MINUTES.toMillis( 1 );
            this.MAX_INTERVAL = TimeUnit.DAYS.toMillis( 365 );
            this.DEFAULT_INTERVAL = TimeUnit.DAYS.toMillis( 10 );
            
            long[] updates = new long[] { TimeUnit.MINUTES.toMillis( 1 ), TimeUnit.HOURS.toMillis( 1 ), TimeUnit.DAYS.toMillis( 1 ), TimeUnit.DAYS.toMillis( 7 ), TimeUnit.DAYS.toMillis( 30 ), TimeUnit.DAYS.toMillis( 3*30 ) };
            
            for( long update : updates ) {
            
                updateInterval = update;

                for ( float ration = 0.f; ration < 1.0f; ration += 0.05f ) {
                    this.INC_INTERVAL = this.DEC_INTERVAL = ration;   
                    this.process();
                }   
            }
        }
}
