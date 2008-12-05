/*
 * Fabric3
 * Copyright � 2008 Metaform Systems Limited
 *
 * This proprietary software may be used only connection with the Fabric3 license
 * (the �License�), a copy of which is included in the software or may be
 * obtained at: http://www.metaformsystems.com/licenses/license.html.

 * Software distributed under the License is distributed on an �as is� basis,
 * without warranties or conditions of any kind.  See the License for the
 * specific language governing permissions and limitations of use of the software.
 * This software is distributed in conjunction with other software licensed under
 * different terms.  See the separate licenses for those programs included in the
 * distribution for the permitted and restricted uses of such software.
 *
 * --- Original Apache License ---
 *
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
package org.fabric3.pojo.instancefactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.fabric3.pojo.provision.InstanceFactoryDefinition;
import org.fabric3.pojo.reflection.ReflectiveInstanceFactoryBuilder;
import org.fabric3.scdl.ConstructorInjectionSite;
import org.fabric3.scdl.FieldInjectionSite;
import org.fabric3.scdl.InjectableAttribute;
import org.fabric3.scdl.InjectableAttributeType;
import org.fabric3.scdl.InjectionSite;
import org.fabric3.scdl.MethodInjectionSite;
import org.fabric3.scdl.Signature;
import org.fabric3.spi.classloader.ClassLoaderRegistry;
import org.fabric3.spi.classloader.DuplicateClassLoaderException;

/**
 * @version $Date$ $Revision$
 */
public class ReflectiveIFProviderBuilderTestCase extends TestCase {
    private InstanceFactoryBuildHelper helper = new BuildHelperImpl(new MockClassLoaderRegistry());
    private ReflectiveInstanceFactoryBuilder builder = new ReflectiveInstanceFactoryBuilder(null, helper);
    private InstanceFactoryDefinition definition;
    private Constructor<Foo> constructor;
    private ClassLoader cl;

    /**
     * Verifies an InjectableAttribute is set properly for constructor parameters
     *
     * @throws Exception
     */
    public void testCdiSource() throws Exception {
        InstanceFactoryProvider provider = builder.build(definition, cl);
        assertEquals(String.class, provider.getMemberType(new InjectableAttribute(InjectableAttributeType.PROPERTY, "a")));
    }

    /**
     * Verifies an InjectableAttribute is set properly for protected fields
     *
     * @throws Exception
     */
    public void testProtectedFieldInjectionSource() throws Exception {
        InjectableAttribute injectableAttribute = new InjectableAttribute(InjectableAttributeType.REFERENCE, "xyz");
        Field field = Foo.class.getDeclaredField("xyz");
        InjectionSite injectionSite = new FieldInjectionSite(field);
        definition.getPostConstruction().put(injectionSite, injectableAttribute);

        InstanceFactoryProvider provider = builder.build(definition, cl);
        Class<?> clazz = provider.getMemberType(injectableAttribute);
        assertEquals(Bar.class, clazz);
    }

    /**
     * Verifies an InjectableAttribute is set properly for setter methods
     *
     * @throws Exception
     */
    public void testMethodInjectionSource() throws Exception {
        InjectableAttribute injectableAttribute = new InjectableAttribute(InjectableAttributeType.REFERENCE, "abc");
        Method method = Foo.class.getMethod("setAbc", Bar.class);
        InjectionSite injectionSite = new MethodInjectionSite(method, 0);
        definition.getPostConstruction().put(injectionSite, injectableAttribute);

        InstanceFactoryProvider provider = builder.build(definition, cl);
        Class<?> clazz = provider.getMemberType(injectableAttribute);
        assertEquals(Bar.class, clazz);
    }


    protected void setUp() throws Exception {
        super.setUp();
        cl = getClass().getClassLoader();
        constructor = Foo.class.getConstructor(String.class, Long.class);

        definition = new InstanceFactoryDefinition();
        definition.setImplementationClass(Foo.class.getName());
        definition.setConstructor(new Signature(constructor));
        definition.setInitMethod(new Signature("init"));
        definition.setDestroyMethod(new Signature("destroy"));
        Map<InjectionSite, InjectableAttribute> construction = definition.getConstruction();
        construction.put(new ConstructorInjectionSite(constructor, 0), new InjectableAttribute(InjectableAttributeType.PROPERTY, "a"));
        construction.put(new ConstructorInjectionSite(constructor, 1), new InjectableAttribute(InjectableAttributeType.REFERENCE, "b"));
    }

    public static class Foo {

        protected Bar xyz;

        public Foo(String a, Long b) {
        }

        public void setAbc(Bar abc) {
        }

        public void init() {
        }

        public void destroy() {
        }

    }

    public static class Bar {

    }

    private class MockClassLoaderRegistry implements ClassLoaderRegistry {

        public void register(URI id, ClassLoader classLoader) throws DuplicateClassLoaderException {

        }

        public ClassLoader unregister(URI id) {
            return null;
        }

        public ClassLoader getClassLoader(URI id) {
            return null;
        }

        public Map<URI, ClassLoader> getClassLoaders() {
            return null;
        }

        public Class<?> loadClass(URI classLoaderId, String className) throws ClassNotFoundException {
            return null;
        }

        public Class<?> loadClass(ClassLoader cl, String className) throws ClassNotFoundException {
            return Class.forName(className, true, cl);
        }

        public List<URI> resolveParentUris(ClassLoader cl) {
            return null;
        }
    }
}
