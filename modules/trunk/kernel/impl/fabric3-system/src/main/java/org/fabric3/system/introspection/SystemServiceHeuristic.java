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
package org.fabric3.system.introspection;

import java.lang.reflect.Type;
import java.util.Set;

import org.osoa.sca.annotations.Reference;

import org.fabric3.api.annotation.Management;
import org.fabric3.model.type.component.ServiceDefinition;
import org.fabric3.model.type.java.InjectingComponentType;
import org.fabric3.model.type.service.ServiceContract;
import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.IntrospectionHelper;
import org.fabric3.spi.introspection.TypeMapping;
import org.fabric3.spi.introspection.java.contract.ContractProcessor;
import org.fabric3.spi.introspection.java.annotation.HeuristicProcessor;
import org.fabric3.spi.model.type.JMXBinding;
import org.fabric3.system.scdl.SystemImplementation;

/**
 * Heuristic that identifies the services provided by an implementation class.
 *
 * @version $Rev$ $Date$
 */
public class SystemServiceHeuristic implements HeuristicProcessor<SystemImplementation> {
    private final ContractProcessor contractProcessor;
    private final IntrospectionHelper helper;

    public SystemServiceHeuristic(@Reference ContractProcessor contractProcessor, @Reference IntrospectionHelper helper) {
        this.contractProcessor = contractProcessor;
        this.helper = helper;
    }

    public void applyHeuristics(SystemImplementation implementation, Class<?> implClass, IntrospectionContext context) {
        InjectingComponentType componentType = implementation.getComponentType();
        TypeMapping typeMapping = context.getTypeMapping();

        // if the service contracts have not already been defined then introspect them
        if (componentType.getServices().isEmpty()) {
            // get the most specific interfaces implemented by the class
            Set<Class<?>> interfaces = helper.getImplementedInterfaces(implClass);

            // if the class does not implement any interfaces, then the class itself is the service contract
            // we don't have to worry about proxies because all wires to system components are optimized
            if (interfaces.isEmpty()) {
                ServiceDefinition serviceDefinition = createServiceDefinition(implClass, typeMapping, context);
                componentType.add(serviceDefinition);
            } else {
                // otherwise, expose all of the implemented interfaces
                for (Class<?> serviceInterface : interfaces) {
                    ServiceDefinition serviceDefinition = createServiceDefinition(serviceInterface, typeMapping, context);
                    componentType.add(serviceDefinition);
                }
            }
        }

        // Add the JMX Management binding to all services tagged as management
        for (ServiceDefinition service : componentType.getServices().values()) {
            if (service.isManagement()) {
                JMXBinding binding = new JMXBinding();
                service.addBinding(binding);
            }
        }
    }

    ServiceDefinition createServiceDefinition(Class<?> serviceInterface, TypeMapping typeMapping, IntrospectionContext context) {
        ServiceContract<Type> contract = contractProcessor.introspect(typeMapping, serviceInterface, context);
        ServiceDefinition service = new ServiceDefinition(contract.getInterfaceName(), contract);
        service.setManagement(serviceInterface.isAnnotationPresent(Management.class));
        return service;
    }
}
