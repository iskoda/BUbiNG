package it.unimi.di.law.bubing.parser;

/*		 
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */

import it.unimi.di.law.bubing.parser.Parser.TextProcessor;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.ArrayList;

/** An implementation of a {@link Parser.TextProcessor} that normalize paragraphs.
 */
public final class KnotDedupTextProcessor implements TextProcessor<List<CharSequence>> {
	public final static class Paragraphs extends ArrayList<CharSequence> {
	};
                
	KnotDedupTextProcessor.Paragraphs paragraphs;
        
	public KnotDedupTextProcessor() { }	
	
	@Override
	public Appendable append( CharSequence csq ) throws IOException {
		paragraphs.add( process( csq ) );
		return this;
	}

	@Override
	public Appendable append( CharSequence csq, int start, int end ) throws IOException {
		paragraphs.add( process( csq.subSequence( start, end ) ) );
		return this;
	}

	@Override
	public Appendable append( char c ) throws IOException {
		return this;
	}
        
	@Override
	public void init( URI responseUrl ) {
		paragraphs = new Paragraphs();
	}

	@Override
	public List<CharSequence> result() {
                return paragraphs;
	}

	@Override
	public TextProcessor<List<CharSequence>> copy() {
		return new KnotDedupTextProcessor( );
	}

	/* Normalize paragraphs. */
	private CharSequence process( CharSequence csq ) throws IOException {
		boolean lastAppendedWasSpace = true;
		StringBuilder buffer = new StringBuilder();
		for(int i = 0; i < csq.length(); i++) {
			char c = csq.charAt(i);
			if (Character.isWhitespace(c) || !Character.isLetter(c)) {
				if (!lastAppendedWasSpace) {
					buffer.append(' ');
					lastAppendedWasSpace = true;
				}
			} else {
				buffer.append(Character.toLowerCase(c));
				lastAppendedWasSpace = false;
			}
		}
		return buffer.toString().trim();
	}
}
