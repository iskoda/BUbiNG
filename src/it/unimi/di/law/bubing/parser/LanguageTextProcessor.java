package it.unimi.di.law.bubing.parser;

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

import cz.vutbr.fit.knot.NNetLanguageIdentifierWrapper;

import it.unimi.di.law.bubing.parser.Parser.TextProcessor;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An implementation of a {@link Parser.TextProcessor} that identifier language of text. */
public final class LanguageTextProcessor implements TextProcessor<NNetLanguageIdentifierWrapper.Result> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LanguageTextProcessor.class);

	private static final NNetLanguageIdentifierWrapper IDENTIFIER = new NNetLanguageIdentifierWrapper();
	private static final Map<URI, LanguageTextProcessor> languageIdentifiers = new ConcurrentHashMap<>();

	private URI uri;
	private StringBuilder textBuilder;
	private boolean lastIsWhitespace;

	public LanguageTextProcessor() {
		this.uri = null;
		this.textBuilder = new StringBuilder();
		this.lastIsWhitespace = true;
	}

	@Override
	public Appendable append(CharSequence csq) throws IOException {
		for (int idx = 0; idx < csq.length(); idx++) {
			this.textBuilder.append(csq.charAt(idx));
                }
		return this;
	}

	@Override
	public Appendable append(CharSequence csq, int start, int end) throws IOException {
		return this.append(csq.subSequence(start, end));
	}

	@Override
	public Appendable append(char c) throws IOException {
		if (Character.isWhitespace(c)) {
			if (this.lastIsWhitespace == false) {
				this.textBuilder.append(" ");
				this.lastIsWhitespace = true;
			}
		} else {
			this.textBuilder.append(c);
			this.lastIsWhitespace = false;
		}
		return this;
	}

	@Override
	public void init(URI responseUrl) {
		if (this.uri != null) languageIdentifiers.remove(this.uri);
		languageIdentifiers.put(responseUrl, this);

		this.uri = responseUrl;
		this.textBuilder = new StringBuilder();
		this.lastIsWhitespace = true;
        }

	@Override
	public NNetLanguageIdentifierWrapper.Result result() {
		return IDENTIFIER.findLanguage(this.textBuilder.toString());
	}

	static public NNetLanguageIdentifierWrapper.Result result(URI responseUrl) {
		// find a better way to pass the result to language filters
		LanguageTextProcessor textProcessor = languageIdentifiers.get(responseUrl);
		if (textProcessor == null) {
			if (LOGGER.isDebugEnabled()) LOGGER.debug("I can not find a text processor for uri: " + responseUrl);
			return null;
		}

		return textProcessor.result();
	}

	@Override
	public TextProcessor<NNetLanguageIdentifierWrapper.Result> copy() {
		return new LanguageTextProcessor();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		IDENTIFIER.dispose();
	}
}