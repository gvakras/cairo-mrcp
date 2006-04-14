/*
 * Cairo - Open source framework for control of speech media resources.
 *
 * Copyright (C) 2005 Onomatopia, Inc. - http://www.onomatopia.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contact: ngodfredsen@users.sourceforge.net
 *
 */
package com.onomatopia.cairo.server.recog.sphinx;

import java.io.File;
import java.net.URL;
import java.util.Map;

import javax.media.MediaLocator;

import org.apache.log4j.xml.DOMConfigurator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.onomatopia.cairo.server.recog.RecognitionResult;

import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * Unit test for SphinxRecEngine.
 */
public class TestSphinxRecEngine extends TestCase {
	
	private RunSphinxRecEngine _runner = null;

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public TestSphinxRecEngine(String testName){
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TestSphinxRecEngine.class );
    }

    public void setUp() throws Exception {
/*    	
    	// configure log4j
    	URL log4jURL = new URL(new File(".").toURL(), "src/test/resources/log4j.xml");
    	DOMConfigurator.configure(log4jURL);

    	// configure sphinx
    	URL promptURL = new URL(new File(".").toURL(), "src/test/resources/prompts/get_me_a_stock_quoteX2.wav");
    	URL sphinxConfigURL = new URL(new File(".").toURL(), "src/main/resources/config/sphinx-config.xml");
    	ConfigurationManager cm = new ConfigurationManager(sphinxConfigURL);
        SphinxRecEngine engine = new SphinxRecEngine(cm);
    	_runner = new RunSphinxRecEngine(engine, RunSphinxRecEngine.MICROPHONE);
//    	_runner = new RunSphinxRecEngine(engine, new MediaLocator(promptURL));
*/    	
    }

    public void testSphinxRecEngine() throws Exception {
    	/*for (Map.Entry entry : System.getProperties().entrySet()) {
        	System.out.println(entry.getKey().toString() + '=' + entry.getValue().toString());
    	}*/
/*
    	assertTrue(_runner != null);

    	RecognitionResult result = null;

    	result =  _runner.doRecognize();
    	System.out.println(result);
    	assertEquals("weather", result.toString());

    	result =  _runner.doRecognize();
    	System.out.println(result);
    	assertEquals("get me sports news", result.toString());
*/
    }

}
