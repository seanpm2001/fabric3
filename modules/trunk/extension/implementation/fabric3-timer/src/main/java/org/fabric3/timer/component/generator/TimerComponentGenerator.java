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
package org.fabric3.timer.component.generator;

import javax.xml.namespace.QName;

import org.oasisopen.sca.Constants;
import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Reference;

import org.fabric3.java.generator.JavaGenerationHelper;
import org.fabric3.java.provision.JavaSourceDefinition;
import org.fabric3.model.type.service.ServiceContract;
import org.fabric3.spi.generator.ComponentGenerator;
import org.fabric3.spi.generator.GenerationException;
import org.fabric3.spi.model.instance.LogicalComponent;
import org.fabric3.spi.model.instance.LogicalReference;
import org.fabric3.spi.model.instance.LogicalResource;
import org.fabric3.spi.model.instance.LogicalService;
import org.fabric3.spi.model.physical.PhysicalComponentDefinition;
import org.fabric3.spi.model.physical.PhysicalSourceDefinition;
import org.fabric3.spi.model.physical.PhysicalTargetDefinition;
import org.fabric3.spi.policy.Policy;
import org.fabric3.timer.component.model.TimerImplementation;
import org.fabric3.timer.component.provision.TimerComponentDefinition;
import org.fabric3.timer.component.provision.TriggerData;

/**
 * Generates a TimerComponentDefinition from a ComponentDefinition corresponding to a timer component implementation
 *
 * @version $Rev$ $Date$
 */
@EagerInit
public class TimerComponentGenerator implements ComponentGenerator<LogicalComponent<TimerImplementation>> {
    private static final QName MANAGED_TRANSACTION = new QName(Constants.SCA_NS, "managedTransaction");
    private JavaGenerationHelper generationHelper;

    public TimerComponentGenerator(@Reference JavaGenerationHelper generationHelper) {
        this.generationHelper = generationHelper;
    }

    public PhysicalComponentDefinition generate(LogicalComponent<TimerImplementation> component) throws GenerationException {
        TimerComponentDefinition definition = new TimerComponentDefinition();
        generationHelper.generate(definition, component);
        TimerImplementation implementation = component.getDefinition().getImplementation();
        definition.setTransactional(implementation.getIntents().contains(MANAGED_TRANSACTION));
        TriggerData data = implementation.getTriggerData();
        definition.setTriggerData(data);
        return definition;
    }

    public PhysicalSourceDefinition generateWireSource(LogicalReference reference, Policy policy) throws GenerationException {
        JavaSourceDefinition definition = new JavaSourceDefinition();
        generationHelper.generateWireSource(definition, reference, policy);
        return definition;
    }

    public PhysicalSourceDefinition generateCallbackWireSource(LogicalComponent<TimerImplementation> source,
                                                                   ServiceContract<?> serviceContract,
                                                                   Policy policy) throws GenerationException {
        JavaSourceDefinition definition = new JavaSourceDefinition();
        generationHelper.generateCallbackWireSource(definition, source, serviceContract, policy);
        return definition;
    }

    public PhysicalSourceDefinition generateResourceWireSource(LogicalResource<?> resource) throws GenerationException {
        JavaSourceDefinition definition = new JavaSourceDefinition();
        generationHelper.generateResourceWireSource(definition, resource);
        return definition;
    }

    public PhysicalTargetDefinition generateWireTarget(LogicalService service, Policy policy) throws GenerationException {
        throw new UnsupportedOperationException("Cannot wire to timer components");
    }
}