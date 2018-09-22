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

/** A filter accepting only URIs whose host is sub-host of one of a given set of hosts.
 */
public class HostIsSubhostOf extends AbstractFilter<URI> {
	private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

	private final Set<String> hosts;

	public HostIsSubhostOf(final String[] hosts) {
		this.hosts = new HashSet<>(Arrays.asList(hosts));
	}

	@Override
	public boolean apply(final URI uri) {
		String host = uri.getHost();
		if (host == null) throw new IllegalArgumentException("URI \"" + uri + "\" has no host");

		int idx = 0;

		do {
			if (this.hosts.contains(host.substring(idx))) return true;
			idx = host.indexOf('.', idx)+1;
		} while (idx != 0);

		return false;
	}

	public static HostIsSubhostOf valueOf(String spec) {
		return new HostIsSubhostOf(Iterables.toArray(SPLITTER.split(spec), String.class));
	}

	@Override
	public String toString() {
		return toString((Object[]) this.hosts.toArray());
	}

	@Override
	public Filter<URI> copy() {
		return this;
	}
}
