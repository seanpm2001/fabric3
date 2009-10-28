/*
 * Fabric3
 * Copyright (c) 2009 Metaform Systems
 *
 * Fabric3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version, with the
 * following exception:
 *
 * Linking this software statically or dynamically with other
 * modules is making a combined work based on this software.
 * Thus, the terms and conditions of the GNU General Public
 * License cover the whole combination.
 *
 * As a special exception, the copyright holders of this software
 * give you permission to link this software with independent
 * modules to produce an executable, regardless of the license
 * terms of these independent modules, and to copy and distribute
 * the resulting executable under terms of your choice, provided
 * that you also meet, for each linked independent module, the
 * terms and conditions of the license of that module. An
 * independent module is a module which is not derived from or
 * based on this software. If you modify this software, you may
 * extend this exception to your version of the software, but
 * you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version.
 *
 * Fabric3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the
 * GNU General Public License along with Fabric3.
 * If not, see <http://www.gnu.org/licenses/>.
*/
package org.fabric3.binding.ws.metro.runtime.core;

import java.io.File;
import java.net.URL;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceFeature;

import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.server.Invoker;

/**
 * Configuration for provisioning a service as a web service endpoint.
 *
 * @version $Rev$ $Date$
 */
public class EndpointConfiguration {
    private Class<?> seiClass;
    private QName serviceName;
    private QName portName;
    private String servicePath;
    private Invoker invoker;
    private WebServiceFeature[] features;
    private BindingID bindingId;
    private File generatedWsdl;
    private List<File> generatedSchemas;
    private URL wsdlLocation;

    /**
     * Constructor that takes a WSDL document at a given URL. If the URL is null, a WSDL will be generated from the service endpoint interface.
     *
     * @param seiClass         service endpoint interface.
     * @param serviceName      service name
     * @param portName         port name
     * @param servicePath      Relative path on which the service is provisioned.
     * @param wsdlLocation     URL to the WSDL document.
     * @param invoker          Invoker for receiving the web service request.
     * @param features         Web service features to enable.
     * @param bindingId        Binding ID to use.
     * @param generatedWsdl    the generated WSDL used for WSIT configuration or null if no policy is configured
     * @param generatedSchemas the handles to schemas (XSDs) imported by the WSDL or null if none exist
     */
    public EndpointConfiguration(Class<?> seiClass,
                                 QName serviceName,
                                 QName portName,
                                 String servicePath,
                                 URL wsdlLocation,
                                 Invoker invoker,
                                 WebServiceFeature[] features,
                                 BindingID bindingId,
                                 File generatedWsdl,
                                 List<File> generatedSchemas) {
        this.seiClass = seiClass;
        this.serviceName = serviceName;
        this.portName = portName;
        this.servicePath = servicePath;
        this.wsdlLocation = wsdlLocation;
        this.invoker = invoker;
        this.features = features;
        this.bindingId = bindingId;
        this.generatedWsdl = generatedWsdl;
        this.generatedSchemas = generatedSchemas;
    }

    public URL getWsdlLocation() {
        return wsdlLocation;
    }

    public QName getServiceName() {
        return serviceName;
    }

    public QName getPortName() {
        return portName;
    }

    public String getServicePath() {
        return servicePath;
    }

    public Invoker getInvoker() {
        return invoker;
    }

    public WebServiceFeature[] getFeatures() {
        return features;
    }

    public BindingID getBindingId() {
        return bindingId;
    }

    public File getGeneratedWsdl() {
        return generatedWsdl;
    }

    public List<File> getGeneratedSchemas() {
        return generatedSchemas;
    }

    public Class<?> getSeiClass() {
        return seiClass;
    }
}