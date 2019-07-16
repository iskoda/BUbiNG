package it.unimi.di.law.warc.processors;

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

// RELEASE-STATUS: DIST

import it.unimi.di.law.bubing.parser.Parser;
import it.unimi.di.law.warc.processors.ParallelFilteredProcessorRunner.Writer;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;

public class LinkReceiverWriter implements Writer<Parser.LinkReceiver> {
	@Override
	public void write(final Parser.LinkReceiver linkReceiver, final long storePosition, final PrintStream out) throws IOException {
		for (URI url: linkReceiver) {
			out.print(url);
			out.write('\n');
		}
	}

	@Override
	public void close() throws IOException {}
}
