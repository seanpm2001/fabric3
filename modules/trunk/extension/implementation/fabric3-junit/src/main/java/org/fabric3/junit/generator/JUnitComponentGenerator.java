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
package org.fabric3.junit.generator;

import java.net.URI;

import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Reference;

import org.fabric3.java.provision.JavaComponentDefinition;
import org.fabric3.java.provision.JavaSourceDefinition;
import org.fabric3.java.provision.JavaTargetDefinition;
import org.fabric3.junit.model.JUnitImplementation;
import org.fabric3.model.type.component.ComponentDefinition;
import org.fabric3.model.type.component.Scope;
import org.fabric3.model.type.contract.ServiceContract;
import org.fabric3.model.type.contract.DataType;
import org.fabric3.spi.model.type.java.Injectable;
import org.fabric3.spi.model.type.java.InjectableType;
import org.fabric3.spi.model.type.java.InjectingComponentType;
import org.fabric3.pojo.generator.GenerationHelper;
import org.fabric3.pojo.provision.InstanceFactoryDefinition;
import org.fabric3.spi.generator.ComponentGenerator;
import org.fabric3.spi.generator.GenerationException;
import org.fabric3.spi.model.instance.LogicalComponent;
import org.fabric3.spi.model.instance.LogicalReference;
import org.fabric3.spi.model.instance.LogicalResource;
import org.fabric3.spi.model.instance.LogicalService;
import org.fabric3.spi.model.physical.InteractionType;
import org.fabric3.spi.model.physical.PhysicalComponentDefinition;
import org.fabric3.spi.model.physical.PhysicalSourceDefinition;
import org.fabric3.spi.model.physical.PhysicalTargetDefinition;
import org.fabric3.spi.policy.EffectivePolicy;

/**
 * @version $Rev$ $Date$
 */
@EagerInit
public class JUnitComponentGenerator implements ComponentGenerator<LogicalComponent<JUnitImplementation>> {
    private final GenerationHelper helper;

    public JUnitComponentGenerator(@Reference GenerationHelper helper) {
        this.helper = helper;
    }

    public PhysicalComponentDefinition generate(LogicalComponent<JUnitImplementation> component) throws GenerationException {

        ComponentDefinition<JUnitImplementation> definition = component.getDefinition();
        JUnitImplementation implementation = definition.getImplementation();
        InjectingComponentType type = implementation.getComponentType();
        String scope = type.getScope();

        InstanceFactoryDefinition providerDefinition = new InstanceFactoryDefinition();
        providerDefinition.setReinjectable(Scope.COMPOSITE.getScope().equals(scope));
        providerDefinition.setConstructor(type.getConstructor());
        providerDefinition.setInitMethod(type.getInitMethod());
        providerDefinition.setDestroyMethod(type.getDestroyMethod());
        providerDefinition.setImplementationClass(implementation.getImplementationClass());
        helper.processInjectionSites(type, providerDefinition);

        JavaComponentDefinition physical = new JavaComponentDefinition();

        physical.setScope(scope);
        physical.setProviderDefinition(providerDefinition);
        helper.processPropertyValues(component, physical);
        return physical;
    }

    public PhysicalSourceDefinition generateWireSource(LogicalReference reference, EffectivePolicy policy) throws GenerationException {
        URI uri = reference.getUri();
        ServiceContract serviceContract = reference.getDefinition().getServiceContract();
        String interfaceName = getInterfaceName(serviceContract);

        JavaSourceDefinition wireDefinition = new JavaSourceDefinition();
        wireDefinition.setUri(uri);
        wireDefinition.setInjectable(new Injectable(InjectableType.REFERENCE, uri.getFragment()));
        wireDefinition.setInterfaceName(interfaceName);
        if (serviceContract.isConversational()) {
            wireDefinition.setInteractionType(InteractionType.CONVERSATIONAL);
        }

        // assume for now that any wire from a JUnit component can be optimized
        wireDefinition.setOptimizable(true);

        if (reference.getDefinition().isKeyed()){
            wireDefinition.setKeyed(true);
            DataType<?> type = reference.getDefinition().getKeyDataType();
            String className = type.getPhysical().getName();
            wireDefinition.setKeyClassName(className);
        }

        return wireDefinition;
    }

    public PhysicalSourceDefinition generateCallbackWireSource(LogicalComponent<JUnitImplementation> source,
                                                               ServiceContract serviceContract,
                                                               EffectivePolicy policy) throws GenerationException {
        throw new UnsupportedOperationException();
    }

    public PhysicalSourceDefinition generateResourceWireSource(LogicalResource<?> resource) throws GenerationException {

        URI uri = resource.getUri();
        ServiceContract serviceContract = resource.getResourceDefinition().getServiceContract();
        String interfaceName = getInterfaceName(serviceContract);

        JavaSourceDefinition wireDefinition = new JavaSourceDefinition();
        wireDefinition.setUri(uri);
        wireDefinition.setInjectable(new Injectable(InjectableType.RESOURCE, uri.getFragment()));
        wireDefinition.setInterfaceName(interfaceName);
        return wireDefinition;
    }

    private String getInterfaceName(ServiceContract contract) {
        return contract.getQualifiedInterfaceName();
    }

    public PhysicalTargetDefinition generateWireTarget(LogicalService service, EffectivePolicy policy) throws GenerationException {
        JavaTargetDefinition wireDefinition = new JavaTargetDefinition();
        wireDefinition.setUri(service.getUri());
        return wireDefinition;
    }
}
