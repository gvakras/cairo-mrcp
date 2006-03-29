package com.onomatopia.cairo.server.tts;

import com.onomatopia.cairo.util.ConfigUtil;
import com.onomatopia.cairo.util.pool.AbstractPoolableObjectFactory;
import com.onomatopia.cairo.util.pool.PoolableObject;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

public class PromptGeneratorFactory extends AbstractPoolableObjectFactory {

    private static Logger _logger = Logger.getLogger(PromptGeneratorFactory.class);

    /* (non-Javadoc)
     * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
     */
    @Override
    public PoolableObject makeObject() throws Exception {
        return new PromptGenerator();
    }

    /**
     * TODOC
     * @param instances
     * @return
     */
    public static ObjectPool createObjectPool(int instances) {

        if (_logger.isDebugEnabled()) {
            _logger.debug("creating new prompt generator pool... instances: " + instances);
        }

        PoolableObjectFactory factory = new PromptGeneratorFactory();

        // TODO: adapt config to prompt generator constraints
        GenericObjectPool.Config config = ConfigUtil.getGenericObjectPoolConfig(instances);
        ObjectPool objectPool = new GenericObjectPool(factory, config);
        return objectPool;
    }

}