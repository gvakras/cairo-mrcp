/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.util;

import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * TODOC
 * @author Niels
 *
 */
public class ConfigUtil {

    /**
     * Make default constructor private to prevent instantiation.
     */
    private ConfigUtil() {
        super();
    }

    /**
     * TODOC
     * @param maxActive
     * @return
     */
    public static GenericObjectPool.Config getGenericObjectPoolConfig(int maxActive) {
        GenericObjectPool.Config config = new GenericObjectPool.Config();

        config.maxActive                        = maxActive;
        config.maxIdle                          = -1;
        config.maxWait                          = 200;
        config.minEvictableIdleTimeMillis       = -1;
        config.minIdle                          = config.maxActive;
        config.numTestsPerEvictionRun           = -1;
        //config.softMinEvictableIdleTimeMillis   = -1;
        config.testOnBorrow                     = false;
        config.testOnReturn                     = false;
        config.testWhileIdle                    = false;
        config.timeBetweenEvictionRunsMillis    = -1;
        config.whenExhaustedAction              = GenericObjectPool.WHEN_EXHAUSTED_FAIL;

        return config;
    }

}
