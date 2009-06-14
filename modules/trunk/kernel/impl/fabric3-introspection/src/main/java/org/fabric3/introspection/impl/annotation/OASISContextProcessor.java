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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.oasisopen.sca.ComponentContext;
import org.oasisopen.sca.RequestContext;
import org.oasisopen.sca.annotation.Context;
import org.osoa.sca.annotations.Reference;

import org.fabric3.model.type.component.Implementation;
import org.fabric3.model.type.java.FieldInjectionSite;
import org.fabric3.model.type.java.InjectableAttribute;
import org.fabric3.model.type.java.InjectingComponentType;
import org.fabric3.model.type.java.InjectionSite;
import org.fabric3.model.type.java.MethodInjectionSite;
import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.IntrospectionHelper;
import org.fabric3.spi.introspection.java.AbstractAnnotationProcessor;

/**
 * Processes {@link Context} annotations.
 *
 * @version $Rev$ $Date$
 */
public class OASISContextProcessor<I extends Implementation<? extends InjectingComponentType>> extends AbstractAnnotationProcessor<Context, I> {
    private final IntrospectionHelper helper;

    public OASISContextProcessor(@Reference IntrospectionHelper helper) {
        super(Context.class);
        this.helper = helper;
    }

    public void visitField(Context annotation, Field field, I implementation, IntrospectionContext context) {
        Type type = field.getGenericType();
        FieldInjectionSite site = new FieldInjectionSite(field);
        visit(type, implementation, site, field.getDeclaringClass(), context);
    }

    public void visitMethod(Context annotation, Method method, I implementation, IntrospectionContext context) {
        Type type = helper.getGenericType(method);
        MethodInjectionSite site = new MethodInjectionSite(method, 0);
        visit(type, implementation, site, method.getDeclaringClass(), context);
    }

    private void visit(Type type, I implementation, InjectionSite site, Class<?> clazz, IntrospectionContext context) {
        if (!(type instanceof Class)) {
            context.addError(new InvalidContextType("Context type " + type + " is not supported in " + clazz.getName()));
        } else if (RequestContext.class.isAssignableFrom((Class<?>) type)) {
            implementation.getComponentType().addInjectionSite(InjectableAttribute.OASIS_REQUEST_CONTEXT, site);
        } else if (ComponentContext.class.isAssignableFrom((Class<?>) type)) {
            implementation.getComponentType().addInjectionSite(InjectableAttribute.OASIS_COMPONENT_CONTEXT, site);
        } else {
            context.addError(new InvalidContextType("Context type is not supported: " + type));
        }
    }
}