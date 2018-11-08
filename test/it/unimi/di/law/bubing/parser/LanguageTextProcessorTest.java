/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unimi.di.law.bubing.parser;

import it.unimi.di.law.warc.util.StringHttpMessages;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import org.junit.Test;
import static org.junit.Assert.*;

public class LanguageTextProcessorTest {
    
	// text source: http://www.randomtextgenerator.com/
	public final static String doc01 =
		"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Strict//EN\" \"http://www.w3.org/TR/REC-html40/strict.dtd\">\n" +
		"\n" +
		"<html>\n" +
		"<head>\n" +
		"<title>English</title>\n" +
		"</HEAD>\n" +
		"<p>Dissuade ecstatic and properly saw entirely sir why laughter endeavor.</p>\n" +
		"<p>In on my jointure horrible margaret suitable he followed speedily.</p>\n" +
		"<p>Indeed vanity excuse or mr lovers of on.</p>\n" +                
		"</body>\n" +
		"</html>";
    
        public final static String doc02 =
		"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Strict//EN\" \"http://www.w3.org/TR/REC-html40/strict.dtd\">\n" +
		"\n" +
		"<html>\n" +
		"<head>\n" +
		"<title>French</title>\n" +
		"</head>\n" +
		"<body>\n" +
		"<p>Gens quoi son nez dieu dans fait ifs.</p>\n" +
		"<p>Net quoi foi vin crie jeu pale. </p>\n" +
		"<p>Epaissies perruches tarderait seulement fourneaux age ete eux moi cauchemar.</p>\n" +                
		"</body>\n" +
		"</html>";
    
	public LanguageTextProcessorTest() {}

	/**
	 * Test of append method, of class LanguageTextProcessor.
	 */
	@Test
	public void testAppend() throws Exception {
		URI url = new URI("http://exmaple.com");
		LanguageTextProcessor processor = new LanguageTextProcessor();
		processor.init(url);
		processor.append("Dissuade ecstatic and properly saw entirely sir why laughter endeavor.");
		processor.append("In on my jointure horrible margaret suitable he followed speedily.");
		processor.append("Indeed vanity excuse or mr lovers of on.");
		assertNotNull(processor.result());
		assertEquals(processor.result().language, "en");
	}

	@Test
	public void testDocuments() throws NoSuchAlgorithmException, IOException, URISyntaxException {
		HTMLParser<?> parser = new HTMLParser<>(BinaryParser.forName("MD5"), new LanguageTextProcessor(), false);
		LanguageTextProcessor processor = new LanguageTextProcessor();
		
		URI uri01 = new URI("http://exmaple.com/doc01");
		URI uri02 = new URI("http://exmaple.com/doc02");
		parser.parse(uri01, new StringHttpMessages.HttpResponse(doc01), Parser.NULL_LINK_RECEIVER);

		assertNotNull(processor.result());
		assertEquals(processor.result().language, "en");

		parser.parse(uri02, new StringHttpMessages.HttpResponse(doc02), Parser.NULL_LINK_RECEIVER);
		assertNotNull(processor.result());
		assertEquals(processor.result().language, "fr");
	}
}
