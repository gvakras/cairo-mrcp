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
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import org.apache.log4j.Logger;

/**
 * Sphinx data processor used for logging speech data as it passes through the pipeline.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class SpeechDataLogger extends BaseDataProcessor {

    private static Logger _logger = Logger.getLogger(SpeechDataLogger.class);

	/**
	 * Property specifying the name of the log file to log speech data to.
	 */
	public static final String PROP_LOG_FILE_NAME = "logFileName";

	/**
	 * The default value of PROP_LOG_FILE_NAME.
	 */
	public static final String PROP_LOG_FILE_NAME_DEFAULT = "speechdata";

    private static final String NL = System.getProperty("line.separator");

	private FileWriter _fileWriter = null;

    /**
     * TODOC
     */
    public SpeechDataLogger() {
        super();
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String, edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry) throws PropertyException {
	    super.register(name, registry);
	    registry.register(PROP_LOG_FILE_NAME, PropertyType.STRING);
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        String logFileName = ps.getString(PROP_LOG_FILE_NAME, PROP_LOG_FILE_NAME_DEFAULT);
        try {
			_fileWriter = new FileWriter(constructLogFile(logFileName), false);
		} catch (IOException e) {
			throw (PropertyException) new PropertyException(this, PROP_LOG_FILE_NAME, e.getMessage()).initCause(e);
		}
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.frontend.DataProcessor#initialize()
     */
    public void initialize() {
        super.initialize();
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.frontend.BaseDataProcessor#getData()
     */
    @Override
    public Data getData() throws DataProcessingException {
        Data data = getPredecessor().getData();
        try {
			logData(data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return data;
    }
    
    private void logData(Data data) throws IOException {
        if (data instanceof Signal) {
        	_fileWriter.append(data.getClass().getName());
            _fileWriter.append(NL);
        } else if (data instanceof DoubleData) {
        	double[] values = ((DoubleData) data).getValues();
        	for (int i = 0; i < values.length; i++) {
            	_fileWriter.append(Double.toString(values[i]));
            	_fileWriter.append(NL);
        	}
        } else if (data instanceof FloatData) {
        	float[] values = ((FloatData) data).getValues();
        	for (int i = 0; i < values.length; i++) {
            	_fileWriter.append(Float.toString(values[i]));
            	_fileWriter.append(NL);
        	}
        }
        _fileWriter.flush();
    }

    private static File constructLogFile(String logFileName) {
        File dir = new File(".");
        File logFile = new File(dir, logFileName + '-' + System.currentTimeMillis() + ".txt");
        if (_logger.isDebugEnabled()) {
            try {
                URL logFileURL = logFile.toURL();
                _logger.debug("logging speech data to " + logFileURL);
            } catch (Exception e) {
                _logger.debug(logFile, e);
            }
        }
        return logFile;
    }

    public static SpeechDataLogger getInstanceForTesting(String logFileName) throws IOException{
        SpeechDataLogger instance = new SpeechDataLogger();
        instance._fileWriter = new FileWriter(constructLogFile(logFileName), false);
        return instance;
    }

}
