package org.speechforge.cairo.sip;

import java.rmi.RemoteException;

public interface Resource {
    public void bye(String sessionId) throws  RemoteException, InterruptedException;

}
