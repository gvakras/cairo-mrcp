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

import org.speechforge.cairo.server.recog.RecogListenerDecorator;
import org.speechforge.cairo.server.recog.RecognitionResult;
import org.speechforge.cairo.server.rtp.PBDSReplicator;
import org.speechforge.cairo.util.jmf.JMFUtil;
import org.speechforge.cairo.util.jmf.ProcessorStarter;

import java.net.URL;

import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import edu.cmu.sphinx.util.props.ConfigurationManager;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Unit test for SphinxRecEngine.
 */
public class TestSphinxRecEngineReplicated extends TestCase {

    private static Logger _logger = Logger.getLogger(TestSphinxRecEngineReplicated.class);

    /**
     * Create the test case
     * 
     * @param testName
     *            name of the test case
     */
    public TestSphinxRecEngineReplicated(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TestSphinxRecEngineReplicated.class);
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
        String expected = "one two three four five";
        recognizeAudioFile(audioFileURL, expected);
    }

    private void recognizeAudioFile(URL audioFileURL, String expected) throws Exception {

        // configure sphinx
        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-TIDIGITS.xml");
        assertNotNull(sphinxConfigURL);
        _logger.debug("sphinxConfigURL: " + sphinxConfigURL);

        ConfigurationManager cm = new ConfigurationManager(sphinxConfigURL);
        SphinxRecEngine engine = new SphinxRecEngine(cm);

        Processor processor1 = JMFUtil.createRealizedProcessor(new MediaLocator(audioFileURL), SourceAudioFormat.PREFERRED_MEDIA_FORMAT);
        processor1.addControllerListener(new ProcessorStarter(false));

        PushBufferDataSource pbds1 = (PushBufferDataSource) processor1.getDataOutput();
        PBDSReplicator replicator = new PBDSReplicator(pbds1);

        DataSource dataSource = replicator.replicate();

        ProcessorModel pm = new ProcessorModel(
                dataSource,
                //new AudioFormat[] { replicator.getAudioFormat() },
                new AudioFormat[] { SourceAudioFormat.PREFERRED_MEDIA_FORMAT },
                JMFUtil.CONTENT_DESCRIPTOR_RAW
        );

        _logger.debug("Creating realized processor...");
        Processor processor2 = Manager.createRealizedProcessor(pm);
        _logger.debug("Processor realized.");

        processor2.addControllerListener(new ProcessorStarter(false));
        PushBufferDataSource pbds2 = (PushBufferDataSource) processor2.getDataOutput();

        engine.activate();

        PrivateRecogListener listener = new PrivateRecogListener();
        engine.startRecognition(pbds2, listener);

        processor2.start();
        Thread.sleep(1000);  // give processor2 a chance to start
        processor1.start();
        _logger.debug("Performing recognition...");
        engine.startRecogThread();

        // wait for result
        synchronized (listener) {
            while (listener._result == null) {
                listener.wait(1000);
            }
        }

        engine.passivate();

        _logger.debug("result=" + listener._result);
        assertEquals(expected, listener._result.toString());

    }

    private class PrivateRecogListener extends RecogListenerDecorator {

        private RecognitionResult _result;

        public PrivateRecogListener() {
            super(null);  // use RecogListenerDecorator as adaptor
        }

        /* (non-Javadoc)
         * @see org.speechforge.cairo.server.recog.RecogListener#recognitionComplete(org.speechforge.cairo.server.recog.RecognitionResult)
         */
        @Override
        public synchronized void recognitionComplete(RecognitionResult result) {
            _result = result;
            this.notify();
        }
        
    }
}
