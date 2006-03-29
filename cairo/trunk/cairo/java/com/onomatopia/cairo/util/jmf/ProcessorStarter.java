/**
 * TODOC
 */
package com.onomatopia.cairo.util.jmf;

import java.io.IOException;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Processor;
import javax.media.StartEvent;
import javax.media.protocol.DataSource;

import org.apache.log4j.Logger;

/**
 * TODOC
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class ProcessorStarter implements ControllerListener {

    private static Logger _logger = Logger.getLogger(ProcessorStarter.class);

    /* (non-Javadoc)
     * @see javax.media.ControllerListener#controllerUpdate(javax.media.ControllerEvent)
     */
    public void controllerUpdate(ControllerEvent event) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("ControllerEvent received: " + event);
        }

        try {
            if (event instanceof StartEvent) {
                Processor processor = (Processor) event.getSourceController();
                DataSource dataSource = processor.getDataOutput();
                _logger.debug("Starting data source...");
                dataSource.connect();
                dataSource.start();
            } else if (event instanceof EndOfMediaEvent) { //StopEvent) {
                event.getSourceController().close();
            }
        } catch (IOException e) {
            _logger.warn(e, e);
        }

    }

}
