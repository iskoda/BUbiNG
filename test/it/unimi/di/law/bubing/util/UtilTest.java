package it.unimi.di.law.bubing.util;

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

//RELEASE-STATUS: DIST

import java.net.UnknownHostException;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** A class to test {@link Util}. */

public class UtilTest {

	@Test
	public void testMethodIpAddrToString() throws UnknownHostException {
		// IPv4 address 127.0.0.1
		String ipv4String = "127.0.0.1";
		byte[] ipv4 = new byte[]{127, 0, 0};
		// IPv6 address 2001:db8:85a3::8a2e:370:7334
		String ipv6String = "2001:db8:85a3:0:0:8a2e:370:7334";
		byte[] ipv6 = new byte[]{
				(byte)0x20, (byte)0x01, (byte)0x0d, (byte)0xb8, 
				(byte)0x85, (byte)0xa3, (byte)0   , (byte)0,
				(byte)0   , (byte)0   , (byte)0x8a, (byte)0x2e, 
				(byte)0x03, (byte)0x70, (byte)0x73, (byte)0x34};

		assertEquals(ipv4String, Util.ipAddrToString(ipv4));
		assertEquals(ipv6String, Util.ipAddrToString(ipv6));
	}
}
