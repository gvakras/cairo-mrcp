/**
 * TODOC
 * 
 */
package com.onomatopia.cairo.server.rtp;

import com.onomatopia.cairo.util.ConfigUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * TODOC
 * @author Niels
 *
 */
public class RTPStreamReplicatorFactory implements PoolableObjectFactory {
    
    private int _nextPort;
    private List<Integer> _ports = new LinkedList<Integer>();

    /**
     * TODOC
     * @param basePort 
     */
    public RTPStreamReplicatorFactory(int basePort) {
        Validate.isTrue((basePort % 2 == 0), "Base port must be even, invalid port: ", basePort);
        Validate.isTrue(basePort > 0, "Base port must positive, invalid port: ", basePort);
        Validate.isTrue(basePort < 65535, "Base port must not exceed 65534, invalid port: ", basePort);
        _nextPort = basePort;
    }

    /* (non-Javadoc)
     * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
     */
    public Object makeObject() throws Exception {
        return new RTPStreamReplicator(borrowPort());
    }

    /* (non-Javadoc)
     * @see org.apache.commons.pool.PoolableObjectFactory#destroyObject(java.lang.Object)
     */
    public void destroyObject(Object obj) throws Exception {
        RTPStreamReplicator replicator = (RTPStreamReplicator) obj;
        replicator.shutdown();
        returnPort(replicator.getPort());
    }

    /* (non-Javadoc)
     * @see org.apache.commons.pool.PoolableObjectFactory#validateObject(java.lang.Object)
     */
    public boolean validateObject(Object arg0) {
        //RTPStreamReplicator replicator = (RTPStreamReplicator) obj;
        return true;
    }

    /* (non-Javadoc)
     * @see org.apache.commons.pool.PoolableObjectFactory#activateObject(java.lang.Object)
     */
    public void activateObject(Object arg0) throws Exception {
        //RTPStreamReplicator replicator = (RTPStreamReplicator) obj;
    }

    /* (non-Javadoc)
     * @see org.apache.commons.pool.PoolableObjectFactory#passivateObject(java.lang.Object)
     */
    public void passivateObject(Object arg0) throws Exception {
        //RTPStreamReplicator replicator = (RTPStreamReplicator) obj;
    }

    private int borrowPort() {
        int port;
        synchronized(_ports) {
            if (_ports.isEmpty()) {
                port = _nextPort;
                _nextPort += 2;
            } else {
                port = _ports.remove(0).intValue();
            }
        }
        return port;
    }

    private void returnPort(int port) {
        synchronized(_ports) {
            _ports.add(new Integer(port));
        }
    }

    /**
     * TODOC
     * @param rtpBasePort
     * @param maxConnects
     * @return
     */
    public static ObjectPool createObjectPool(int rtpBasePort, int maxConnects) {
        int maxPort = rtpBasePort + (maxConnects *2);
        System.out.println("creating new replicator pool... ports: " + rtpBasePort + '-' + maxPort);

        PoolableObjectFactory factory = new RTPStreamReplicatorFactory(rtpBasePort);
        GenericObjectPool.Config config = ConfigUtil.getGenericObjectPoolConfig(maxConnects);
        ObjectPool objectPool = new GenericObjectPool(factory, config);
        return objectPool;
    }

}
