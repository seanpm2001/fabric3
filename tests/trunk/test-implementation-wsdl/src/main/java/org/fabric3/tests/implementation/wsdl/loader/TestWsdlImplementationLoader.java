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
package org.fabric3.tests.implementation.wsdl.loader;

import java.net.URI;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.oasisopen.sca.annotation.Reference;
import org.osoa.sca.annotations.EagerInit;

import org.fabric3.host.contribution.StoreException;
import org.fabric3.model.type.component.ServiceDefinition;
import org.fabric3.spi.contribution.MetaDataStore;
import org.fabric3.spi.contribution.ResourceElement;
import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.xml.ElementLoadFailure;
import org.fabric3.spi.introspection.xml.InvalidPrefixException;
import org.fabric3.spi.introspection.xml.InvalidQNamePrefix;
import org.fabric3.spi.introspection.xml.LoaderHelper;
import org.fabric3.spi.introspection.xml.LoaderUtil;
import org.fabric3.spi.introspection.xml.MissingAttribute;
import org.fabric3.spi.introspection.xml.TypeLoader;
import org.fabric3.tests.implementation.wsdl.model.TestWsdlComponentType;
import org.fabric3.tests.implementation.wsdl.model.TestWsdlImplementation;
import org.fabric3.wsdl.contribution.WsdlServiceContractSymbol;
import org.fabric3.wsdl.model.WsdlServiceContract;

/**
 * @version $Revision$ $Date$
 */
@EagerInit
public class TestWsdlImplementationLoader implements TypeLoader<TestWsdlImplementation> {
    private LoaderHelper helper;
    private MetaDataStore store;

    public TestWsdlImplementationLoader(@Reference LoaderHelper helper, @Reference MetaDataStore store) {
        this.helper = helper;
        this.store = store;
    }

    public TestWsdlImplementation load(XMLStreamReader reader, IntrospectionContext context) throws XMLStreamException {
        String name = reader.getAttributeValue(null, "service");
        if (name == null) {
            MissingAttribute error = new MissingAttribute("Missing service attribute", reader);
            context.addError(error);
            return null;
        }

        QName qName;
        try {
            qName = helper.createQName(name, reader);
        } catch (InvalidPrefixException e) {
            String prefix = e.getPrefix();
            URI uri = context.getContributionUri();
            context.addError(new InvalidQNamePrefix("The prefix " + prefix + " specified in contribution " + uri
                    + " is invalid", reader));
            return null;
        }

        String stub = reader.getAttributeValue(null, "stub");

        LoaderUtil.skipToEndElement(reader);
        TestWsdlComponentType type = new TestWsdlComponentType();
        WsdlServiceContract contract = resolveContract(qName, reader, context);
        ServiceDefinition service = new ServiceDefinition(name, contract);
        type.add(service);
        TestWsdlImplementation impl = new TestWsdlImplementation(stub);
        impl.setComponentType(type);
        return impl;

    }

    private WsdlServiceContract resolveContract(QName portTypeName, XMLStreamReader reader, IntrospectionContext context) {
        WsdlServiceContractSymbol symbol = new WsdlServiceContractSymbol(portTypeName);
        URI contributionUri = context.getContributionUri();
        ResourceElement<WsdlServiceContractSymbol, WsdlServiceContract> element;
        try {
            element = store.resolve(contributionUri, WsdlServiceContract.class, symbol, context);
        } catch (StoreException e) {
            ElementLoadFailure failure = new ElementLoadFailure("Error loading element", e, reader);
            context.addError(failure);
            return null;
        }
        if (element == null) {
            PortTypeNotFound error = new PortTypeNotFound("Port type not found: " + portTypeName);
            context.addError(error);
            return null;
        }
        return element.getValue();
    }


}