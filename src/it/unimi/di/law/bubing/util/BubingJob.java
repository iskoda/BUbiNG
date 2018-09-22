package it.unimi.di.law.bubing.util;

/*
 * Copyright (C) 2012-2017 Paolo Boldi, Massimo Santini, and Sebastiano Vigna
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
 *
 * NOTICE: 09/2018 - IP distribution
 */

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.jai4j.Job;

import java.io.Serializable;

import com.google.common.base.Charsets;
import java.nio.ByteBuffer;
import java.util.Arrays;

//RELEASE-STATUS: DIST

/** The JAI4J {@link it.unimi.dsi.jai4j.Job} used by BUbiNG. */

public class BubingJob implements Job, Serializable {
	private static final long serialVersionUID = 1L;
	/** The {@linkplain BURL BUbiNG URL} that should be visited. */
	public final ByteArrayList url;
	/** The IP adress of this url. */
	public final ByteArrayList ipAddress;

	/** Creates a new BUbiNG job corresponding to a given {@linkplain BURL BUbiNG URL}.
	 *
	 * @param url the {@linkplain BURL BUbiNG URL} for this job.
	 */
	public BubingJob(final ByteArrayList url) {
		this(url, new ByteArrayList(0));
	}

	/** Creates a new BUbiNG job corresponding to a given {@linkplain BURL BUbiNG URL} and IP adress.
	 *
	 * @param url the {@linkplain BURL BUbiNG URL} for this job.
	 * @param ipAddress
	 */
	public BubingJob(final ByteArrayList url, final ByteArrayList ipAddress) {
		this.url = url;
		this.ipAddress = ipAddress;
	}

	/** A hash based on the host of {@link #url}.
	 *
	 * @return a hash based on the host of {@link #url}.
	 */
	@Override
	public long hash64() {
		if (this.ipAddress.size() == 0) {
			final byte[] urlBuffer = url.elements();
			final int startOfHost = BURL.startOfHost(urlBuffer);
			return MurmurHash3.hash(urlBuffer, startOfHost, BURL.lengthOfHost(urlBuffer, startOfHost));
		} else {
			final byte[] ipBuffer = ipAddress.elements();
			return MurmurHash3.hash(ipBuffer, 0, ipBuffer.length);
		}
	}

	 /**
	  * A string representation of this job
	  *
	  * @return the URI of this job in string format
	  */
	 @Override
	public String toString() {
		 return "[" + new String(url.elements(), 0, url.size(), Charsets.ISO_8859_1) + "]";
	 }

	public static byte[] toByteArray(BubingJob job) {
		ByteBuffer data = ByteBuffer.allocate(4+job.url.size()+job.ipAddress.size());
		data.putInt(job.url.size());
		data.put(job.url.toByteArray());
		data.put(job.ipAddress.toByteArray());
		return data.array();
	}

	public static BubingJob fromByteArray(byte[] array, int offset) {
		int size = ByteBuffer.wrap(array).getInt(offset);
		offset += 4;
		ByteArrayList url = ByteArrayList.wrap(Arrays.copyOfRange(array, offset, offset+size));
		ByteArrayList ipAddress = ByteArrayList.wrap(Arrays.copyOfRange(array, offset+size, array.length));
		return new BubingJob(url, ipAddress);
	}        
}
