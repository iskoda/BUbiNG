package it.unimi.di.law.warc.filters;

/*
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

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;
import static org.junit.Assert.*;

public class HostIsSubhostOfTest {
	private static final String[][] DATA = {
			{"https://www.example.com/", "www.example0.com, www.example.com"},
			{"https://www.example.com/", "www.example0.com, example.com"},
			{"https://example.com/", "www.example0.com, www.example.com"}
	};

	private static final boolean[] RESULTS = {true, true, false};

	/**
	 * Test of apply method, of class HostIsSubhostOf.
	 */
	@Test
	public void testApply() throws URISyntaxException {
		for (int idx=0; idx < DATA.length; idx++) {
			URI uri = new URI(DATA[idx][0]);
			boolean result = HostIsSubhostOf.valueOf(DATA[idx][1]).apply(uri);
			assertEquals(result, RESULTS[idx]);
		}
	}
}
