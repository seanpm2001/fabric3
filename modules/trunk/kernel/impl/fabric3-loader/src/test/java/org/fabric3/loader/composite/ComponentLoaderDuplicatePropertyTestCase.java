/*
 * See the NOTICE file distributed with this work for information
 * regarding copyright ownership.  This file is licensed
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
package org.fabric3.loader.composite;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.Location;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import static org.osoa.sca.Constants.SCA_NS;

import org.fabric3.scdl.ComponentType;
import org.fabric3.scdl.Implementation;
import org.fabric3.scdl.Property;
import org.fabric3.scdl.PropertyValue;
import org.fabric3.introspection.xml.Loader;
import org.fabric3.introspection.IntrospectionContext;
import org.fabric3.introspection.xml.LoaderException;
import org.fabric3.introspection.xml.LoaderHelper;
import org.fabric3.introspection.xml.TypeLoader;

/**
 * @version $Rev$ $Date$
 */
public class ComponentLoaderDuplicatePropertyTestCase extends TestCase {
    public static final String PROP_NAME = "notThere";

    private ComponentLoader loader;
    private XMLStreamReader reader;
    private IntrospectionContext ctx;

    /**
     * Verifies an exception is thrown if an attempt is made to configure a property twice.
     *
     * @throws Exception on test failure
     */
    public void testDuplicateProperty() throws Exception {
        EasyMock.replay(ctx);
        try {
            loader.load(reader, ctx);
        } catch (DuplicateConfiguredPropertyException e) {
            //expected
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        TypeLoader<PropertyValue> propLoader = createPropertyLoader();
        Loader registry = createRegistry();
        LoaderHelper helper = EasyMock.createNiceMock(LoaderHelper.class);
        EasyMock.replay(helper);
        loader = new ComponentLoader(registry, propLoader, null, null, helper);
        reader = createReader();
        ctx = EasyMock.createNiceMock(IntrospectionContext.class);
    }

    private Loader createRegistry() throws XMLStreamException, LoaderException {
        Loader registry = EasyMock.createMock(Loader.class);
        Implementation impl = createImpl();
        EasyMock.expect(registry.load(EasyMock.isA(XMLStreamReader.class),
                                      EasyMock.eq(Implementation.class),
                                      EasyMock.isA(IntrospectionContext.class))).andReturn(impl);

        EasyMock.replay(registry);
        return registry;
    }

    @SuppressWarnings({"unchecked"})
    private TypeLoader<PropertyValue> createPropertyLoader() throws XMLStreamException, LoaderException {
        TypeLoader<PropertyValue> loader = EasyMock.createMock(TypeLoader.class);
        PropertyValue value = new PropertyValue(PROP_NAME, "test");
        EasyMock.expect(loader.load(EasyMock.isA(XMLStreamReader.class),
                                    EasyMock.isA(IntrospectionContext.class))).andReturn(value).times(2);
        EasyMock.replay(loader);
        return loader;
    }

    private Implementation createImpl() {
        Implementation<ComponentType> impl = new Implementation<ComponentType>() {
            public QName getType() {
                return null;
            }
        };
        ComponentType type = new ComponentType();
        Property property = new Property(PROP_NAME, null);

        type.add(property);
        impl.setComponentType(type);
        return impl;
    }

    private XMLStreamReader createReader() throws XMLStreamException {
        Location location = EasyMock.createNiceMock(Location.class);
        EasyMock.replay(location);

        XMLStreamReader reader = EasyMock.createMock(XMLStreamReader.class);
        EasyMock.expect(reader.getAttributeValue(null, "name")).andReturn("component");
        EasyMock.expect(reader.getAttributeValue(null, "autowire")).andReturn(null);
        EasyMock.expect(reader.getAttributeValue(null, "runtimeId")).andReturn(null);
        EasyMock.expect(reader.getAttributeValue(null, "initLevel")).andReturn(null);
        EasyMock.expect(reader.getAttributeValue(EasyMock.isA(String.class), EasyMock.eq("key"))).andReturn(null);
        EasyMock.expect(reader.nextTag()).andReturn(1);
        EasyMock.expect(reader.next()).andReturn(1);
        EasyMock.expect(reader.getName()).andReturn(new QName(SCA_NS, "property"));
        EasyMock.expect(reader.nextTag()).andReturn(1);
        EasyMock.expect(reader.next()).andReturn(1);
        EasyMock.expect(reader.getName()).andReturn(new QName(SCA_NS, "property"));
        EasyMock.expect(reader.getLocation()).andReturn(location);
        EasyMock.replay(reader);
        return reader;
    }


}