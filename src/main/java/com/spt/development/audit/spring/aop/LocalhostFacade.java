package com.spt.development.audit.spring.aop;

import java.net.InetAddress;
import java.net.UnknownHostException;

class LocalhostFacade {

    String getServerHostName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }
}
