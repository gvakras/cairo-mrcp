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

import org.speechforge.cairo.util.ConfigUtil;
import org.speechforge.cairo.util.pool.AbstractPoolableObjectFactory;
import org.speechforge.cairo.util.pool.PoolableObject;

import java.net.URL;

import edu.cmu.sphinx.util.props.ConfigurationManager;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

public class SphinxRecEngineFactory extends AbstractPoolableObjectFactory {

    private static Logger _logger = Logger.getLogger(SphinxRecEngineFactory.class);

    URL _sphinxConfigURL = null;

    public SphinxRecEngineFactory(URL sphinxConfigURL) {
        _sphinxConfigURL = sphinxConfigURL;
    }

    /* (non-Javadoc)
     * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
     */
    @Override
    public PoolableObject makeObject() throws Exception {
        ConfigurationManager cm = new ConfigurationManager(_sphinxConfigURL);
        return new SphinxRecEngine(cm);
    }

    /**
     * TODOC
     * @param sphinxConfigURL
     * @param instances
     * @return
     * @throws InstantiationException
     */
    public static ObjectPool createObjectPool(URL sphinxConfigURL, int instances)
      throws InstantiationException {
        
        if (_logger.isDebugEnabled()) {
            _logger.debug("creating new rec engine pool... instances: " + instances);
        }

        PoolableObjectFactory factory = new SphinxRecEngineFactory(sphinxConfigURL);
        GenericObjectPool.Config config = ConfigUtil.getGenericObjectPoolConfig(instances);

        ObjectPool objectPool = new GenericObjectPool(factory, config);
        try {
            initPool(objectPool);
        } catch (Exception e) {
            try {
                objectPool.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            throw (InstantiationException) new InstantiationException(e.getMessage()).initCause(e);
        }
        return objectPool;
    }

}