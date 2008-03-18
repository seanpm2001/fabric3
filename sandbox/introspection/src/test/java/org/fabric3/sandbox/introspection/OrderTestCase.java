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
package org.fabric3.sandbox.introspection;

import java.net.URL;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.fabric3.sandbox.introspection.impl.IntrospectionFactoryImpl;
import org.fabric3.introspection.xml.Loader;
import org.fabric3.introspection.xml.LoaderException;
import org.fabric3.introspection.IntrospectionContext;
import org.fabric3.introspection.IntrospectionException;
import org.fabric3.introspection.DefaultIntrospectionContext;
import org.fabric3.introspection.java.ImplementationProcessor;
import org.fabric3.scdl.Composite;
import org.fabric3.java.scdl.JavaImplementation;
import org.fabric3.pojo.scdl.PojoComponentType;

/**
 * @version $Rev$ $Date$
 */
public class OrderTestCase extends TestCase {
    private IntrospectionFactory factory;

    public void testIntrospectJava() throws IntrospectionException {

        IntrospectionContext context = new DefaultIntrospectionContext(getClass().getClassLoader(), null, null);
        ImplementationProcessor<JavaImplementation> processor = factory.getImplementationProcessor(JavaImplementation.class);

        JavaImplementation impl = new JavaImplementation();
        impl.setImplementationClass("com.example.order.OrderServiceImpl");
        processor.introspect(impl, context);
        PojoComponentType componentType = impl.getComponentType();
        assertTrue(componentType.getServices().containsKey("OrderService"));
        assertTrue(componentType.getReferences().containsKey("pricing"));
    }

    protected void setUp() throws Exception {
        super.setUp();

        factory = new IntrospectionFactoryImpl();
    }
}
