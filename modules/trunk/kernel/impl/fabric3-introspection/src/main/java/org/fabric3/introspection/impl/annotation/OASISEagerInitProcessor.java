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

import javax.xml.namespace.QName;

import org.oasisopen.sca.annotation.EagerInit;
import org.oasisopen.sca.annotation.Scope;

import org.fabric3.model.type.component.Implementation;
import org.fabric3.model.type.java.InjectingComponentType;
import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.java.AbstractAnnotationProcessor;

/**
 * @version $Rev$ $Date$
 */
public class OASISEagerInitProcessor<I extends Implementation<? extends InjectingComponentType>> extends AbstractAnnotationProcessor<EagerInit, I> {

	public static final QName IMPLEMENTATION_SYSTEM = new QName("urn:fabric3.org:implementation", "implementation.system");

    public OASISEagerInitProcessor() {
        super(EagerInit.class);
    }

    public void visitType(EagerInit annotation, Class<?> type, I implementation, IntrospectionContext context) {
        if (!validateScope(type, implementation, context)) {
            return;
        }
        InjectingComponentType componentType = implementation.getComponentType();
        componentType.setInitLevel(50);
    }

    private boolean validateScope(Class<?> type, I implementation, IntrospectionContext context) {
        if (IMPLEMENTATION_SYSTEM.equals(implementation.getType())) {
            // system implementations are composite scoped by default
            return true;
        }
        Scope scope = type.getAnnotation(Scope.class);
        if (scope == null || !org.fabric3.model.type.component.Scope.COMPOSITE.getScope().equals(scope.value())) {
            EagerInitNotSupported warning = new EagerInitNotSupported(type);
            context.addWarning(warning);
            return false;
        }
        return true;
    }

}