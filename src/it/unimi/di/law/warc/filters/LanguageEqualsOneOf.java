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

import cz.vutbr.fit.knot.NNetLanguageIdentifierWrapper;

import it.unimi.di.law.bubing.parser.LanguageTextProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.net.HttpHeaders;
import org.apache.http.Header;

/** A filter accepting only URIResponse whose content is in a certain language. */
public class LanguageEqualsOneOf extends AbstractFilter<URIResponse> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LanguageEqualsOneOf.class);
	private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

	private final String[] languages;

	public LanguageEqualsOneOf(final String[] languages) {
		this.languages = languages;
	}

	@Override
	public boolean apply(final URIResponse response) {
		final Header header = response.response().getFirstHeader(HttpHeaders.CONTENT_LANGUAGE);
		if (header != null) {
			if (LOGGER.isDebugEnabled()) LOGGER.debug("The http header 'Content-Language' of page " + response.uri() + " is " + header.getValue());
			for (String language: this.languages) {
				if (header.getValue().startsWith(language)) return true;
			}
		}

		final NNetLanguageIdentifierWrapper.Result lang = LanguageTextProcessor.result(response.uri());
		if (lang == null || lang.isReliable == false) return false;

		if (LOGGER.isDebugEnabled()) LOGGER.debug("Language of page " + response.uri() + " is " + lang.language);
		for (String language: this.languages) {
			if (lang.language.equals(language)) return true;
		}

		return false;
	}

	public static LanguageEqualsOneOf valueOf(final String spec) {
		return new LanguageEqualsOneOf(Iterables.toArray(SPLITTER.split(spec), String.class));
	}

	@Override
	public String toString() {
		return toString((Object[]) this.languages);
	}

	@Override
	public Filter<URIResponse> copy() {
		return this;
	}
}
