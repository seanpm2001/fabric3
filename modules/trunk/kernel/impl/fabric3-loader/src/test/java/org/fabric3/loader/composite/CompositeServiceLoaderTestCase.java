/*
 * Fabric3
 * Copyright (C) 2009 Metaform Systems
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
 *
 * ----------------------------------------------------
 *
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 *
 */
package org.fabric3.loader.composite;

import java.net.URI;
import javax.xml.namespace.QName;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.xml.LoaderException;
import org.fabric3.spi.introspection.xml.LoaderHelper;
import org.fabric3.spi.introspection.xml.LoaderRegistry;
import org.fabric3.model.type.component.BindingDefinition;
import org.fabric3.model.type.component.CompositeService;
import org.fabric3.model.type.component.ServiceDefinition;
import org.fabric3.model.type.ModelObject;
import org.fabric3.model.type.service.ServiceContract;

/**
 * Verifies loading of a service definition from an XML-based assembly
 *
 * @version $Rev$ $Date$
 */
public class CompositeServiceLoaderTestCase extends TestCase {
    private static final QName NAME = new QName("test", "binding");
    private final String serviceName = "service";
    private final String componentName = "component";
    private final String componentServiceName = "component/service";
    private final URI componentURI = URI.create("component");
    private final URI componentServiceURI = URI.create("component#service");

    private CompositeServiceLoader loader;
    private IntrospectionContext introspectionContext;
    private XMLStreamReader mockReader;
    private LoaderRegistry mockRegistry;
    private LoaderHelper mockLoaderHelper;

    public void testPromotedComponent() throws XMLStreamException {
        expect(mockReader.getAttributeCount()).andReturn(0);
        expect(mockReader.getAttributeValue(null, "name")).andReturn(serviceName);
        expect(mockReader.getAttributeValue(null, "promote")).andReturn(componentName);
        expect(mockLoaderHelper.getURI(componentName)).andReturn(componentURI);
        mockLoaderHelper.loadPolicySetsAndIntents(EasyMock.isA(CompositeService.class),
                                                  EasyMock.same(mockReader),
                                                  EasyMock.same(introspectionContext));
        expect(mockReader.next()).andReturn(END_ELEMENT);
        replay(mockReader, mockLoaderHelper);
        CompositeService serviceDefinition = loader.load(mockReader, introspectionContext);
        assertNotNull(serviceDefinition);
        assertEquals(serviceName, serviceDefinition.getName());
        assertEquals(componentURI, serviceDefinition.getPromote());
        verify(mockReader, mockLoaderHelper);
    }

    public void testPromotedService() throws XMLStreamException {
        expect(mockReader.getAttributeCount()).andReturn(0);
        expect(mockReader.getAttributeValue(null, "name")).andReturn(serviceName);
        expect(mockReader.getAttributeValue(null, "promote")).andReturn(componentServiceName);
        expect(mockLoaderHelper.getURI(componentServiceName)).andReturn(componentServiceURI);
        mockLoaderHelper.loadPolicySetsAndIntents(EasyMock.isA(CompositeService.class),
                                                  EasyMock.same(mockReader),
                                                  EasyMock.same(introspectionContext));
        expect(mockReader.next()).andReturn(END_ELEMENT);
        replay(mockReader, mockLoaderHelper);
        CompositeService serviceDefinition = loader.load(mockReader, introspectionContext);
        assertNotNull(serviceDefinition);
        assertEquals(serviceName, serviceDefinition.getName());
        assertEquals(componentServiceURI, serviceDefinition.getPromote());
        verify(mockReader, mockLoaderHelper);
    }

    public void testMultipleBindings() throws LoaderException, XMLStreamException {
        expect(mockReader.getAttributeCount()).andReturn(0);
        expect(mockReader.getAttributeValue(null, "name")).andReturn(serviceName);
        expect(mockReader.getAttributeValue(null, "promote")).andReturn(componentName);
        expect(mockLoaderHelper.getURI(componentName)).andReturn(componentURI);
        mockLoaderHelper.loadPolicySetsAndIntents(EasyMock.isA(CompositeService.class),
                                                  EasyMock.same(mockReader),
                                                  EasyMock.same(introspectionContext));

        expect(mockReader.next()).andReturn(START_ELEMENT);
        expect(mockReader.getName()).andReturn(NAME);
        expect(mockReader.getName()).andReturn(NAME);
        expect(mockReader.getName()).andReturn(NAME);
        expect(mockReader.getEventType()).andReturn(END_ELEMENT);
        expect(mockReader.getName()).andReturn(NAME);
        expect(mockReader.getName()).andReturn(NAME);
        expect(mockReader.next()).andReturn(START_ELEMENT);
        expect(mockReader.getName()).andReturn(NAME);
        expect(mockReader.getEventType()).andReturn(END_ELEMENT);

        expect(mockReader.next()).andReturn(END_ELEMENT);

        BindingDefinition binding = new BindingDefinition(null, null, null) {
        };
        expect(mockRegistry.load(mockReader, ModelObject.class, introspectionContext)).andReturn(binding).times(2);
        replay(mockReader, mockLoaderHelper, mockRegistry);

        ServiceDefinition serviceDefinition = loader.load(mockReader, introspectionContext);
        assertEquals(2, serviceDefinition.getBindings().size());
        verify(mockReader, mockLoaderHelper, mockRegistry);
    }

    public void testWithInterface() throws LoaderException, XMLStreamException {
        ServiceContract sc = new ServiceContract<Object>() {
            public boolean isAssignableFrom(ServiceContract<?> contract) {
                return false;
            }

            @Override
            public String getQualifiedInterfaceName() {
                return null;
            }

        };
        expect(mockReader.getAttributeCount()).andReturn(0);
        expect(mockReader.getAttributeValue(null, "name")).andReturn(serviceName);
        expect(mockReader.getAttributeValue(null, "promote")).andReturn(componentName);
        expect(mockLoaderHelper.getURI(componentName)).andReturn(componentURI);
        mockLoaderHelper.loadPolicySetsAndIntents(EasyMock.isA(CompositeService.class),
                                                  EasyMock.same(mockReader),
                                                  EasyMock.same(introspectionContext));

        expect(mockReader.next()).andReturn(START_ELEMENT);
        expect(mockReader.getName()).andReturn(NAME);
        expect(mockReader.getName()).andReturn(NAME);
        expect(mockRegistry.load(mockReader, ModelObject.class, introspectionContext)).andReturn(sc);
        expect(mockReader.getName()).andReturn(NAME);
        expect(mockReader.getEventType()).andReturn(END_ELEMENT);
        expect(mockReader.next()).andReturn(END_ELEMENT);

        replay(mockReader, mockLoaderHelper, mockRegistry);

        ServiceDefinition serviceDefinition = loader.load(mockReader, introspectionContext);
        assertSame(sc, serviceDefinition.getServiceContract());
        verify(mockReader, mockLoaderHelper, mockRegistry);
    }

    protected void setUp() throws Exception {
        super.setUp();
        mockReader = EasyMock.createMock(XMLStreamReader.class);
        mockRegistry = EasyMock.createMock(LoaderRegistry.class);
        mockLoaderHelper = EasyMock.createMock(LoaderHelper.class);
        loader = new CompositeServiceLoader(mockRegistry, mockLoaderHelper);
        introspectionContext = EasyMock.createMock(IntrospectionContext.class);
        EasyMock.replay(introspectionContext);
    }
}
