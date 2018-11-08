package it.unimi.di.law.warc.processors;

/*
 * Copyright (C) 2004-2017 Paolo Boldi, Massimo Santini, and Sebastiano Vigna
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

// RELEASE-STATUS: DIST

import it.unimi.di.law.bubing.frontier.ParsingThread;
import it.unimi.di.law.bubing.parser.HTMLParser;
import it.unimi.di.law.bubing.parser.Parser;
import it.unimi.di.law.warc.processors.ParallelFilteredProcessorRunner.Processor;
import it.unimi.di.law.warc.records.HttpResponseWarcRecord;
import it.unimi.di.law.warc.records.WarcRecord;

import java.io.IOException;
import org.slf4j.LoggerFactory;

public class URLExtractor implements Processor<Parser.LinkReceiver> {
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ParsingThread.class);
	
	private static final URLExtractor INSTANCE = new URLExtractor();
	private static final HTMLParser HTML_PARSER = new HTMLParser();
	private URLExtractor() {}

	public static URLExtractor getInstance() {
		return INSTANCE;
	}

	@Override
	public Parser.LinkReceiver process(final WarcRecord record, final long storePosition) {
		Parser.LinkReceiver linkReceiver = new HTMLParser.SetLinkReceiver();
		try {
			HTML_PARSER.parse(record.getWarcTargetURI(), (HttpResponseWarcRecord)record, linkReceiver);
		} catch (IOException ex) {
			LOGGER.warn("Exception during parsing of " + record.getWarcTargetURI(), ex);
		}
		return linkReceiver;
	}

	@Override
	public void close() throws IOException {}

	@Override
	public URLExtractor copy() {
		return INSTANCE;
	}
}
