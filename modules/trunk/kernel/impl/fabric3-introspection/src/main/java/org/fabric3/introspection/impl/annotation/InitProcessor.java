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
package org.fabric3.introspection.impl.annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.xml.namespace.QName;

import org.osoa.sca.annotations.Init;

import org.fabric3.spi.introspection.java.AbstractAnnotationProcessor;
import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.model.type.component.Implementation;
import org.fabric3.model.type.java.InjectingComponentType;
import org.fabric3.model.type.java.Signature;

/**
 * @version $Rev$ $Date$
 */
public class InitProcessor<I extends Implementation<? extends InjectingComponentType>> extends AbstractAnnotationProcessor<Init, I> {
    public static final QName IMPLEMENTATION_SYSTEM = new QName("urn:fabric3.org:implementation", "implementation.system");

    public InitProcessor() {
        super(Init.class);
    }

    public void visitMethod(Init annotation, Method method, I implementation, IntrospectionContext context) {
        if (!validateAccessor(method, context)) {
            return;
        }
        implementation.getComponentType().setInitMethod(new Signature(method));
    }

    private boolean validateAccessor(Method method, IntrospectionContext context) {
        if (!Modifier.isProtected(method.getModifiers()) && !Modifier.isPublic(method.getModifiers())) {
            Class<?> clazz = method.getDeclaringClass();
            InvalidAccessor warning =
                    new InvalidAccessor("Ignoring " + method + " annotated with @Init. Initializers must be public or protected.", clazz);
            context.addWarning(warning);
            return false;
        }
        return true;
    }
}
