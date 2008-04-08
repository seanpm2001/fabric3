/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.fabric3.runtime.webapp.activator;

import java.net.URI;
import java.net.URL;
import javax.servlet.ServletContext;

import org.osoa.sca.annotations.Reference;

import org.fabric3.container.web.spi.WebApplicationActivator;
import org.fabric3.container.web.spi.WebApplicationActivationException;
import org.fabric3.spi.host.ServletHost;

/**
 * A WebApplicationActivator used in a runtime embedded in a WAR.
 *
 * @version $Revision$ $Date$
 */
public class EmbeddedWebApplicationActivator implements WebApplicationActivator {
    private ServletHost host;

    public EmbeddedWebApplicationActivator(@Reference ServletHost host) {
        this.host = host;
    }

    public ClassLoader getWebComponentClassLoader(URI componentId) {
        //  As the runtime is embedded in a web app, the TCCL is the webapp classloader
        return Thread.currentThread().getContextClassLoader();
    }

    public ServletContext activate(String contextPath, URL url, URI parentClassLoaderId) throws WebApplicationActivationException {
        // the web app has already been activated since it is embedded in a war
        return host.getServletContext();
    }

    public void deactivate(URL url) throws WebApplicationActivationException {
        // do nothing
    }
}
