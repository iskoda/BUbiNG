/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unimi.di.law.bubing.parser;

import java.io.IOException;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author karel
 */
public class KnotDedupTextProcessorTest extends TestCase {

    /**
     * Test of result method, of class KnotDedupTextProcessor.
     */
    public void testResult() throws IOException {
        KnotDedupTextProcessor instance = new KnotDedupTextProcessor();
        instance.init(null);
        instance.append("    First   Test!  ");
        instance.append("Příliš žluťoučký kůň úpěl ďábelské ódy. ");
        instance.append("myname@domain.com ");
        
        final List<CharSequence> result = instance.result();
        
        assertEquals("first test", result.get(0));
        assertEquals("příliš žluťoučký kůň úpěl ďábelské ódy",  result.get(1));
        assertEquals("myname domain com",  result.get(2));
    }
}
