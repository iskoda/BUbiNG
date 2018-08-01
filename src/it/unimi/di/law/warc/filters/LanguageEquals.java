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

import com.google.common.net.HttpHeaders;
import cz.vutbr.fit.knot.NNetLanguageIdentifierWrapper;

import it.unimi.di.law.bubing.parser.LanguageTextProcessor;
import org.apache.http.Header;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A filter accepting only URIResponse whose content is in a certain language. */
public class LanguageEquals extends AbstractFilter<URIResponse> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LanguageEquals.class);

	private final String language;

	public LanguageEquals(final String language) {
		this.language = language;
	}

	@Override
	public boolean apply(final URIResponse response) {
		final Header header = response.response().getFirstHeader(HttpHeaders.CONTENT_LANGUAGE);
		if (header != null && header.getValue().startsWith(this.language)) {
                    return true;
                }
                
		final NNetLanguageIdentifierWrapper.Result lang = LanguageTextProcessor.result(response.uri());
		if (lang == null || lang.isReliable == false) return false;

		if (LOGGER.isDebugEnabled()) LOGGER.debug("Language of page " + response.uri() + " is " + lang.language);
		return lang.isReliable && this.language.equals(lang.language);
	}

	public static LanguageEquals valueOf(final String spec) {
		return new LanguageEquals(spec);
	}

	@Override
	public String toString() {
		return toString(language);
	}

	@Override
	public Filter<URIResponse> copy() {
		return this;
	}
}
