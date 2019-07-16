/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unimi.di.law.warc.filters;

import it.unimi.di.law.TestUtil;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;
import static org.junit.Assert.*;

public class HostIsSubhostOfOneInFileTest {
    	private static final String[][] DATA = {
			{"https://www.example.com/", "test01.txt"},
			{"https://www.example.com/", "test02.txt"},
			{"https://example.com/", "test01.txt"}
	};

	private static final boolean[] RESULTS = {true, true, false};

	/**
	 * Test of apply method, of class HostIsSubhostOf.
	 */
	@Test
	public void testApply() throws URISyntaxException {
		for (int idx=0; idx < DATA.length; idx++) {
			URI uri = new URI(DATA[idx][0]);
			String hostsFile = TestUtil.getTestFile(HostIsSubhostOfOneInFileTest.class, DATA[idx][1], true);
			boolean result = HostIsSubhostOfOneInFile.valueOf(hostsFile).apply(uri);
			assertEquals(RESULTS[idx], result);
		}
	}
}
