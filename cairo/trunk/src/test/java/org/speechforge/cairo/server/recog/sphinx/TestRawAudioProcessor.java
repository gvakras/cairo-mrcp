/*
 * Cairo - Open source framework for control of speech media resources.
 *
 * Copyright (C) 2005-2006 SpeechForge - http://www.speechforge.org
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
package org.speechforge.cairo.server.recog.sphinx;

import org.speechforge.cairo.util.jmf.JMFUtil;
import org.speechforge.cairo.util.jmf.ProcessorStarter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.net.URL;

import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Unit test for SphinxRecEngine.
 */
public class TestRawAudioProcessor extends TestCase {

    private static Logger _logger = Logger.getLogger(TestRawAudioProcessor.class);

    /**
     * Create the test case
     * 
     * @param testName
     *            name of the test case
     */
    public TestRawAudioProcessor(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TestRawAudioProcessor.class);
    }

    public void setUp() throws Exception {

        // configure log4j
        URL log4jURL = this.getClass().getResource("/log4j.xml");
        assertNotNull(log4jURL);
        DOMConfigurator.configure(log4jURL);
    }

    public void test12345() throws Exception {
        URL audioFileURL = this.getClass().getResource("/prompts/12345.wav");
        assertNotNull(audioFileURL);

        URL speechDataURL = this.getClass().getResource("/prompts/12345.speechdata.txt");
        assertNotNull(speechDataURL);

        Reader r = new BufferedReader(new InputStreamReader(speechDataURL.openStream()));
        StreamTokenizer tokenizer = new StreamTokenizer(r);
        tokenizer.parseNumbers();

        Processor processor = JMFUtil.createRealizedProcessor(new MediaLocator(audioFileURL), SourceAudioFormat.PREFERRED_MEDIA_FORMAT);
        processor.addControllerListener(new ProcessorStarter());

        PushBufferDataSource pbds = (PushBufferDataSource) processor.getDataOutput();
        processor.start();

        PushBufferStream[] streams = pbds.getStreams();
        assert(streams.length == 1);

        RawAudioProcessor rawAudioProcessor = RawAudioProcessor.getInstanceForTesting();

        RawAudioTransferHandler rawAudioTransferHandler = new RawAudioTransferHandler(rawAudioProcessor);
        rawAudioTransferHandler.startProcessing(streams[0]);

        int ttype = tokenizer.nextToken();
        assertEquals(StreamTokenizer.TT_WORD, ttype);

        _logger.debug("expected=edu.cmu.sphinx.frontend.DataStartSignal actual=" + tokenizer.sval);
        assertEquals("edu.cmu.sphinx.frontend.DataStartSignal", tokenizer.sval);

        Data data = rawAudioProcessor.getData();
        assertTrue(data instanceof DataStartSignal);

        ttype = tokenizer.nextToken();

        while (ttype == StreamTokenizer.TT_NUMBER) {

            data = rawAudioProcessor.getData();
            assertTrue(data instanceof DoubleData);

            double[] values = ((DoubleData) data).getValues();
            for (int i=0; i < values.length; i++) {
                _logger.trace("expected=" + tokenizer.nval + " actual=" + values[i]);
                assertEquals(tokenizer.nval, values[i]);
                ttype = tokenizer.nextToken();
            }

        }

        _logger.debug("expected=edu.cmu.sphinx.frontend.DataEndSignal actual=" + tokenizer.sval);
        assertEquals("edu.cmu.sphinx.frontend.DataEndSignal", tokenizer.sval);

        data = rawAudioProcessor.getData();
        assertTrue(data instanceof DataEndSignal);

    }

}
