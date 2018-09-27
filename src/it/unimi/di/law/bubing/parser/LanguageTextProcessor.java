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

/** An implementation of a {@link Parser.TextProcessor} that identifier language of text. */
public final class LanguageTextProcessor implements TextProcessor<NNetLanguageIdentifierWrapper.Result> {
	public static final String IDENTIFIED_LANGUAGE = "BUbiNG-Detected-Language";

	private static final NNetLanguageIdentifierWrapper IDENTIFIER = new NNetLanguageIdentifierWrapper();
	private StringBuilder textBuilder;

	public LanguageTextProcessor() {
		this.textBuilder = new StringBuilder();
	}

	@Override
	public Appendable append(CharSequence csq) throws IOException {
		this.textBuilder.append(csq);
		return this;
	}

	@Override
	public Appendable append(CharSequence csq, int start, int end) throws IOException {
		return this.append(csq.subSequence(start, end));
	}

	@Override
	public Appendable append(char c) throws IOException {
		this.textBuilder.append(c);
		return this;
	}

	@Override
	public void init(URI responseUrl) {
		this.textBuilder = new StringBuilder();
	}

	@Override
	public NNetLanguageIdentifierWrapper.Result result() {
		return IDENTIFIER.findLanguage(this.textBuilder.toString());
	}

	@Override
	public TextProcessor<NNetLanguageIdentifierWrapper.Result> copy() {
		return new LanguageTextProcessor();
	}
}
