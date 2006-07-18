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
package org.speechforge.cairo.server.rtp;

import org.speechforge.cairo.util.pool.ObjectPoolUtil;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

/**
 * TODOC
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class RTPStreamReplicatorFactory implements PoolableObjectFactory {

    private static Logger _logger = Logger.getLogger(RTPStreamReplicatorFactory.class);

    private int _nextPort;
    private List<Integer> _ports = new LinkedList<Integer>();

    /**
     * TODOC
     * @param basePort 
     */
    public RTPStreamReplicatorFactory(int basePort) {
        Validate.isTrue((basePort % 2 == 0), "Base port must be even, invalid port: ", basePort);
        Validate.isTrue(basePort >= 0, "Base port must not be less than zero, invalid port: ", basePort);
        Validate.isTrue(basePort <= RTPConsumer.TCP_PORT_MAX, "Base port exceeds max TCP port value, invalid port: ", basePort);
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
        if (_logger.isDebugEnabled()) {
            _logger.debug("creating new replicator pool... ports: " + rtpBasePort + '-' + maxPort);
        }

        PoolableObjectFactory factory = new RTPStreamReplicatorFactory(rtpBasePort);
        GenericObjectPool.Config config = ObjectPoolUtil.getGenericObjectPoolConfig(maxConnects);
        ObjectPool objectPool = new GenericObjectPool(factory, config);
        return objectPool;
    }

}
