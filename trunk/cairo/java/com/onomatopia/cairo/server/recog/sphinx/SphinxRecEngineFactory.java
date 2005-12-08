package com.onomatopia.cairo.server.recog.sphinx;

import com.onomatopia.cairo.util.ConfigUtil;
import com.onomatopia.cairo.util.pool.AbstractPoolableObjectFactory;
import com.onomatopia.cairo.util.pool.PoolableObject;

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