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
import it.unimi.di.law.bubing.parser.LanguageTextProcessor;
import it.unimi.di.law.bubing.util.FetchData;
import it.unimi.di.law.warc.records.WarcHeader;
import org.apache.http.Header;

/** A filter accepting only FetchData whose content is in a certain language. */
public class LanguageEquals extends AbstractFilter<FetchData> {
	private final String language;

	public LanguageEquals(final String language) {
		this.language = language;
	}

	@Override
	public boolean apply(final FetchData response) {
		final Header header = response.response().getFirstHeader(HttpHeaders.CONTENT_LANGUAGE);
		if (header != null && header.getValue().startsWith(this.language)) return true;

		final String identifiedLanguage;
		identifiedLanguage = response.additionalInformation.get(WarcHeader.Name.BUBING_DETECTED_LANGUAGE.toString());
		if (identifiedLanguage == null) return false;

		return this.language.equals(identifiedLanguage);
	}

	public static LanguageEquals valueOf(final String spec) {
		return new LanguageEquals(spec);
	}

	@Override
	public String toString() {
		return toString(language);
	}

	@Override
	public Filter<FetchData> copy() {
		return this;
	}
}
