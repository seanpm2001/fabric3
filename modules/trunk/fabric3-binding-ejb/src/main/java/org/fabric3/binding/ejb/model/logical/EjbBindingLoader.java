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
package org.fabric3.binding.ejb.model.logical;

import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static org.osoa.sca.Constants.SCA_NS;
import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Reference;

import org.fabric3.extension.loader.LoaderExtension;
import org.fabric3.spi.loader.LoaderContext;
import org.fabric3.spi.loader.LoaderException;
import org.fabric3.spi.loader.LoaderRegistry;
import org.fabric3.spi.loader.LoaderUtil;


/**
 * @version $Revision: 1 $ $Date: 2007-05-14 10:40:37 -0700 (Mon, 14 May 2007) $
 */
@EagerInit
public class EjbBindingLoader extends LoaderExtension<EjbBindingDefinition> {

    /** Qualified name for the binding element. */
    public static final QName BINDING_QNAME =
        new QName(SCA_NS, "binding.ejb");
    
    /**
     * Injects the registry.
     * @param registry Loader registry.
     */
    public EjbBindingLoader(@Reference LoaderRegistry registry) {
        super(registry);
    }

    @Override
    public QName getXMLType() {
        return BINDING_QNAME;
    }

    public EjbBindingDefinition load(XMLStreamReader reader, LoaderContext loaderContext)
        throws XMLStreamException, LoaderException {

        String uri = reader.getAttributeValue(null, "uri");

        // In EJB 3, the @Stateless & @Stateful annotations contain an attribute named mappedName.
        // Although the specification doesn't spell out what this attribute is used for, it is
        // commonly used to specify a JNDI name for the EJB.  However, EJB 3 beans can have multiple
        // interfaces.  As a result, most containers including Glassfish and WebLogic calculate a JNDI
        // name for each interface based on the mappedName.  In both Glassfish and WebLogic, the JNDI
        // name for each interface is calculated using the following formula:
        // <mappedName>#<fully qualified interface name>
        // The problem is that the '#' char is a URI fragment delimitor and therefore can't legally be used
        // in a URI.  Constructing a URI from such a JNDI name leads to an URISyntaxException being thrown.
        // As such, we'll strip off the "corbaname:rir:#" portion of the URI before setting the targetURI on
        // the Binding definition.

        URI targetUri = null;
        if (uri != null) {

            // TODO: This really needs to be cleaned up.
            if(uri.startsWith("corbaname:rir:#")) {
                uri = uri.substring(uri.indexOf('#') + 1);
            }

            try {
                targetUri = new URI(uri);
            } catch (URISyntaxException ex) {
                throw new LoaderException(ex);
            }
        }

        EjbBindingDefinition bd = new EjbBindingDefinition(targetUri);
        bd.setJndiName(uri);
        String homeInterface = reader.getAttributeValue(null, "homeInterface");
        bd.setHomeInterface(homeInterface);

        bd.setEjbLink(reader.getAttributeValue(null, "ejb-link-name"));

        if("stateful".equals(reader.getAttributeValue(null, "session-type"))) {
            bd.setStateless(false);
        }

        boolean isEjb3 = true;
        String ejbVersion = reader.getAttributeValue(null, "ejb-version");
        if(ejbVersion != null) {
            isEjb3 = "EJB3".equals(ejbVersion);
        } else {
            isEjb3 = (homeInterface == null);
        }
        bd.setEjb3(isEjb3);

        if(!isEjb3 && homeInterface == null) {
            throw new LoaderException("homeInterface must be specified for EJB 2.x bindings");
        }

        bd.setName(reader.getAttributeValue(null, "name"));


        LoaderUtil.skipToEndElement(reader);
        return bd;
        
    }

}
