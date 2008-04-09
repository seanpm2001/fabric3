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

import junit.framework.TestCase;

import org.fabric3.sandbox.introspection.impl.IntrospectionFactoryImpl;
import org.fabric3.introspection.IntrospectionContext;
import org.fabric3.introspection.DefaultIntrospectionContext;
import org.fabric3.introspection.xml.Loader;
import org.fabric3.introspection.xml.LoaderException;
import org.fabric3.spi.assembly.ActivateException;
import org.fabric3.scdl.Composite;

/**
 * @version $Rev$ $Date$
 */
public class AutowireTestCase extends TestCase {
    private IntrospectionFactory factory;
    private IntrospectionContext context;
    private Loader loader;

    public void testAutowireWithMultipleCandidates() throws LoaderException, ActivateException {
        // load and activate the autowire composite
        URL autowireURL = getClass().getResource("/autowire/autowire.composite");
        context = new DefaultIntrospectionContext(getClass().getClassLoader(), null, autowireURL);
        Composite autowireComposite = loader.load(autowireURL, Composite.class, context);
        try {
            factory.initializeContext(autowireComposite);
            // fail();
        } catch (ActivateException e) {
            // expected becuase the autowire is ambiguous
        }
    }

    protected void setUp() throws Exception {
        super.setUp();

        factory = new IntrospectionFactoryImpl();
        loader = factory.getLoader();
    }
}
