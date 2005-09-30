package com.onomatopia.cairo.server.recog.sphinx;

import com.onomatopia.cairo.server.config.ReceiverConfig;
import com.onomatopia.cairo.util.ConfigUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import edu.cmu.sphinx.util.props.ConfigurationManager;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

public class SphinxRecEngineFactory implements PoolableObjectFactory {

    URL _sphinxConfigURL = null;

    public SphinxRecEngineFactory(URL sphinxConfigURL) {
        _sphinxConfigURL = sphinxConfigURL;
    }

    public Object makeObject() throws Exception {
        ConfigurationManager cm = new ConfigurationManager(_sphinxConfigURL);
        return new SphinxRecEngine(cm);
    }

    public void activateObject(Object obj) throws Exception {
        SphinxRecEngine recEngine = (SphinxRecEngine) obj;
        recEngine.activate();
    }

    public void passivateObject(Object obj) {
        SphinxRecEngine recEngine = (SphinxRecEngine) obj;
        recEngine.passivate();
    }

    public boolean validateObject(Object obj) {
        return true;
    }

    public void destroyObject(Object obj) {
        //SphinxRecEngine recEngine = (SphinxRecEngine) obj;
        // TODO: destroy object
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

        System.out.println("creating new rec engine pool... instances: " + instances);
        PoolableObjectFactory factory = new SphinxRecEngineFactory(sphinxConfigURL);
        GenericObjectPool.Config config = ConfigUtil.getGenericObjectPoolConfig(instances);
        // TODO: initialize instances
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

    private static void initPool(ObjectPool pool) throws Exception {
        List<Object> objects = new ArrayList<Object>();
        while (true) try {
            objects.add(pool.borrowObject());
        } catch (NoSuchElementException e){
            // ignore, max active reached
            //e.printStackTrace();
            break;
        }
        for (Object obj : objects) {
            pool.returnObject(obj);
        }
    }

}