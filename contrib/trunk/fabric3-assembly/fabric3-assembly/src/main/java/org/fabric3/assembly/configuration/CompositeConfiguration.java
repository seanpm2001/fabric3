package org.fabric3.assembly.configuration;

import java.io.File;

/**
 * @author Michal Capo
 */
public class CompositeConfiguration {

    private String serverName;

    private File path;

    public CompositeConfiguration(String pServerName, File pPath) {
        if (null == pServerName) {
            serverName = ServerConfiguration.SERVER_DEFAULT_NAME;
        } else {
            serverName = pServerName;
        }
        path = pPath;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String pServerName) {
        serverName = pServerName;
    }

    public File getPath() {
        return path;
    }

    public void setPath(String pPath) {
        path = new File(pPath);
    }

}
