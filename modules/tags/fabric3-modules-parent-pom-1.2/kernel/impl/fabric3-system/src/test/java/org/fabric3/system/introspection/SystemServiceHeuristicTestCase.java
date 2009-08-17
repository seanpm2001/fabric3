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
 *
 * ----------------------------------------------------
 *
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 *
 */
package org.fabric3.system.introspection;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import org.fabric3.model.type.component.ServiceDefinition;
import org.fabric3.model.type.java.InjectingComponentType;
import org.fabric3.model.type.service.ServiceContract;
import org.fabric3.spi.introspection.DefaultIntrospectionContext;
import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.IntrospectionException;
import org.fabric3.spi.introspection.IntrospectionHelper;
import org.fabric3.spi.introspection.TypeMapping;
import org.fabric3.spi.introspection.java.contract.ContractProcessor;
import org.fabric3.system.model.SystemImplementation;

/**
 * @version $Rev$ $Date$
 */
public class SystemServiceHeuristicTestCase extends TestCase {
    private static final Set<Class<?>> NOCLASSES = Collections.emptySet();
    private SystemServiceHeuristic heuristic;

    private ContractProcessor contractProcessor;
    private IntrospectionHelper helper;
    private IntrospectionContext context;
    private SystemImplementation impl;
    private InjectingComponentType componentType;
    private ServiceContract<Type> serviceInterfaceContract;
    private ServiceContract<Type> noInterfaceContract;
    private IMocksControl control;
    private TypeMapping typeMapping;

    public void testNoInterface() throws IntrospectionException {
        EasyMock.expect(helper.getImplementedInterfaces(NoInterface.class)).andReturn(NOCLASSES);
        IntrospectionContext context = new DefaultIntrospectionContext(null, null, null, null, typeMapping);
        EasyMock.expect(contractProcessor.introspect(typeMapping, NoInterface.class, context)).andReturn(noInterfaceContract);
        control.replay();

        heuristic.applyHeuristics(impl, NoInterface.class, context);
        Map<String, ServiceDefinition> services = componentType.getServices();
        assertEquals(1, services.size());
        assertEquals(noInterfaceContract, services.get("NoInterface").getServiceContract());
        control.verify();
    }

    public void testWithInterface() throws IntrospectionException {
        Set<Class<?>> interfaces = new HashSet<Class<?>>();
        interfaces.add(ServiceInterface.class);

        IntrospectionContext context = new DefaultIntrospectionContext(null, null, null, null, typeMapping);
        EasyMock.expect(helper.getImplementedInterfaces(OneInterface.class)).andReturn(interfaces);
        EasyMock.expect(contractProcessor.introspect(typeMapping, ServiceInterface.class, context)).andReturn(
                serviceInterfaceContract);
        control.replay();

        heuristic.applyHeuristics(impl, OneInterface.class, context);
        Map<String, ServiceDefinition> services = componentType.getServices();
        assertEquals(1, services.size());
        assertEquals(serviceInterfaceContract, services.get("ServiceInterface").getServiceContract());
        control.verify();
    }

    public void testServiceWithExistingServices() throws IntrospectionException {
        ServiceDefinition definition = new ServiceDefinition("Contract");
        impl.getComponentType().add(definition);
        control.replay();

        heuristic.applyHeuristics(impl, NoInterface.class, context);
        Map<String, ServiceDefinition> services = impl.getComponentType().getServices();
        assertEquals(1, services.size());
        assertSame(definition, services.get("Contract"));
        control.verify();
    }

    public static interface ServiceInterface {
    }

    public static class NoInterface {
    }

    public static class OneInterface implements ServiceInterface {
    }

    @SuppressWarnings("unchecked")
    protected void setUp() throws Exception {
        super.setUp();
        impl = new SystemImplementation();
        componentType = new InjectingComponentType(NoInterface.class.getName());
        impl.setComponentType(componentType);

        noInterfaceContract = createServiceContract(NoInterface.class);
        serviceInterfaceContract = createServiceContract(ServiceInterface.class);

        control = EasyMock.createControl();
        typeMapping = new TypeMapping();
        context = control.createMock(IntrospectionContext.class);
        EasyMock.expect(context.getTypeMapping()).andStubReturn(typeMapping);
        contractProcessor = control.createMock(ContractProcessor.class);
        helper = control.createMock(IntrospectionHelper.class);
        heuristic = new SystemServiceHeuristic(contractProcessor, helper);
    }

    private ServiceContract<Type> createServiceContract(Class<?> type) {
        @SuppressWarnings("unchecked")
        ServiceContract<Type> contract = EasyMock.createMock(ServiceContract.class);
        EasyMock.expect(contract.getInterfaceName()).andStubReturn(type.getSimpleName());
        EasyMock.replay(contract);
        return contract;
    }
}