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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.media.CannotRealizeException;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSourceException;
import javax.media.NoProcessorException;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;

import org.apache.log4j.Logger;

import org.speechforge.cairo.server.recog.RecogListenerDecorator;
import org.speechforge.cairo.server.recog.RecognitionResult;
import org.speechforge.cairo.server.rtp.PBDSReplicator;
import org.speechforge.cairo.util.jmf.ProcessorStarter;

import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * Provides main method for running SphinxRecEngine in standalone mode using the microphone for input.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class RunSphinxRecEngine extends RecogListenerDecorator {

    private static Logger _logger = Logger.getLogger(SphinxRecEngine.class);

    public static final MediaLocator MICROPHONE = new MediaLocator("dsound://");

    private static final AudioFormat[] PREFERRED_MEDIA_FORMATS = {SourceAudioFormat.PREFERRED_MEDIA_FORMAT};
    private static final ContentDescriptor CONTENT_DESCRIPTOR_RAW = new ContentDescriptor(ContentDescriptor.RAW);

    private SphinxRecEngine _engine;
    private RecognitionResult _result;
    private PBDSReplicator _replicator;

    public RunSphinxRecEngine(SphinxRecEngine engine, MediaLocator mediaLocator)
      throws NoProcessorException, NoDataSourceException, CannotRealizeException, IOException {
        super(null);
        _engine = engine;
        _replicator = createReplicator(mediaLocator);
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.recog.RecogListener#recognitionComplete(org.speechforge.cairo.server.recog.RecognitionResult)
     */
    @Override
    public synchronized void recognitionComplete(RecognitionResult result) {
        _result = result;
        this.notify();
    }

    public RecognitionResult doRecognize() throws IOException, NoProcessorException, CannotRealizeException, InterruptedException {

        _result = null;
        _engine.activate();

        Processor processor = createReplicatedProcessor();
        processor.addControllerListener(new ProcessorStarter());

        PushBufferDataSource pbds = (PushBufferDataSource) processor.getDataOutput();
        _engine.startRecognition(pbds, this);
        processor.start();
        _logger.debug("Performing recognition...");
        _engine.startRecogThread();

        // wait for result
        RecognitionResult result = null;
        synchronized (this) {
            while (_result == null) {
                this.wait(1000);
            }
            result = _result;
            _result = null;
        }

        _engine.passivate();

        return result;
    }

    private Processor createReplicatedProcessor() throws IOException,
            IllegalStateException, NoProcessorException,
            CannotRealizeException {
        
        ProcessorModel pm = new ProcessorModel(
                _replicator.replicate(),
                PREFERRED_MEDIA_FORMATS,
                CONTENT_DESCRIPTOR_RAW
        );
        
        _logger.debug("Creating realized processor...");
        Processor processor = Manager.createRealizedProcessor(pm);
        _logger.debug("Processor realized.");
        
        return processor;
    }

    private static Processor createProcessor(MediaLocator mediaLocator)
      throws NoDataSourceException, IOException, NoProcessorException, CannotRealizeException {

        DataSource dataSource = Manager.createDataSource(mediaLocator);
        ProcessorModel pm = new ProcessorModel(dataSource,
                PREFERRED_MEDIA_FORMATS, CONTENT_DESCRIPTOR_RAW);
        Processor processor = Manager.createRealizedProcessor(pm);
        return processor;
    }

    private static PBDSReplicator createReplicator(MediaLocator mediaLocator)
      throws NoProcessorException, NoDataSourceException, CannotRealizeException, IOException {
        Processor processor = createProcessor(mediaLocator);
        processor.addControllerListener(new ProcessorStarter());
        PushBufferDataSource pbds = (PushBufferDataSource) processor.getDataOutput();
        PBDSReplicator replicator = new PBDSReplicator(pbds);
        processor.start();
        return replicator;
    }

    public static void main(String[] args) throws Exception {
        URL url;
        if (args.length > 0) {
            url = new File(args[0]).toURL();
        } else {
            url = SphinxRecEngine.class.getResource("/config/sphinx-config.xml");
        }
        
        if (url == null) {
            throw new RuntimeException("Sphinx config file not found!");
        }

        System.out.println("Loading...");
        ConfigurationManager cm = new ConfigurationManager(url);
        SphinxRecEngine engine = new SphinxRecEngine(cm);

//        for (int i=0; i < 12; i++) {
//            System.out.println(engine._jsgfGrammar.getRandomSentence());
//        }

        RunSphinxRecEngine runner = new RunSphinxRecEngine(engine, MICROPHONE);
        

        RecognitionResult result;
        while (true) {
            result = runner.doRecognize();
        }

//        RuleParse ruleParse = engine.parse("", "main");

    }


}
