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

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** A filter accepting only URIs whose host is a with one of a given set of hosts.
 */
public class HostIsOneOf extends AbstractFilter<URI> {
	private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

	private final Set<String> hosts;

	/** Creates a filter that only accepts URLs with a given hosts.
	 *
	 * @param hosts the accepted hosts.
	 */
	public HostIsOneOf(final String[] hosts) {
		this.hosts = new HashSet<>(Arrays.asList(hosts));
	}

	/**
	 * Apply the filter to a given URI
	 *
	 * @param uri the URI to be filtered
	 * @return true if the host part of <code>uri</code> is one of the inner hosts
	 */
	@Override
	public boolean apply(final URI uri) {
		String host = uri.getHost();
		if (host == null) throw new IllegalArgumentException("URI \"" + uri + "\" has no host");
		
		return this.hosts.contains(host);
	}

	/**
	 * Get a new <code>HostIsOneOf</code> that will accept only URIs whose host part is one of the given hosts.
	 *
	 * @param spec a String containing the allowed hosts (separated by ',')
	 * @return a new <code>HostIsOneOf</code> that will accept only URIs whose host is one of the strings specified by <code>spec</code>
	 */
	public static HostIsOneOf valueOf(String spec) {
		return new HostIsOneOf(Iterables.toArray(SPLITTER.split(spec), String.class));
	}

	/**
	 * A string representation of the state of this object.
	 *
	 * @return the string that is the host of the URIs allowed by this filter
	 */
	@Override
	public String toString() {
		return toString((Object[]) this.hosts.toArray());
	}

	@Override
	public Filter<URI> copy() {
		return this;
	}
}
