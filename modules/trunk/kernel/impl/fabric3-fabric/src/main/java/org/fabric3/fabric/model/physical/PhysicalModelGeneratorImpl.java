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
package org.fabric3.fabric.model.physical;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fabric3.fabric.assembly.resolver.ResolutionException;
import org.fabric3.fabric.command.InitializeComponentCommand;
import org.fabric3.fabric.generator.DefaultGeneratorContext;
import org.fabric3.fabric.implementation.singleton.SingletonImplementation;
import org.fabric3.fabric.services.routing.RoutingException;
import org.fabric3.fabric.services.routing.RoutingService;
import org.fabric3.scdl.ComponentDefinition;
import org.fabric3.scdl.CompositeImplementation;
import org.fabric3.scdl.Implementation;
import org.fabric3.spi.assembly.ActivateException;
import org.fabric3.spi.command.Command;
import org.fabric3.spi.command.CommandSet;
import org.fabric3.spi.generator.CommandGenerator;
import org.fabric3.spi.generator.ComponentGenerator;
import org.fabric3.spi.generator.GenerationException;
import org.fabric3.spi.generator.GeneratorContext;
import org.fabric3.spi.generator.GeneratorRegistry;
import org.fabric3.spi.model.instance.LogicalBinding;
import org.fabric3.spi.model.instance.LogicalComponent;
import org.fabric3.spi.model.instance.LogicalCompositeComponent;
import org.fabric3.spi.model.instance.LogicalReference;
import org.fabric3.spi.model.instance.LogicalResource;
import org.fabric3.spi.model.instance.LogicalService;
import org.fabric3.spi.model.instance.LogicalWire;
import org.fabric3.spi.model.physical.PhysicalChangeSet;
import org.fabric3.spi.model.physical.PhysicalComponentDefinition;
import org.fabric3.spi.runtime.assembly.LogicalComponentManager;
import org.fabric3.spi.util.UriHelper;

import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Reference;

/**
 * Default implementation of the physical model generator.
 *
 * @version $Revision$ $Date$
 */
@EagerInit
public class PhysicalModelGeneratorImpl implements PhysicalModelGenerator {

    private final GeneratorRegistry generatorRegistry;
    private final RoutingService routingService;
    private final LogicalComponentManager logicalComponentManager;
    private final PhysicalWireGenerator physicalWireGenerator;

    /**
     * Injects generator registry and assembly store.
     *
     * @param generatorRegistry       Generator registry.
     * @param routingService
     * @param logicalComponentManager
     * @param physicalWireGenerator
     * @param physicalWireGenerator
     * @param logicalComponentManager
     */
    public PhysicalModelGeneratorImpl(@Reference GeneratorRegistry generatorRegistry,
                                      @Reference RoutingService routingService,
                                      @Reference(name = "logicalComponentManager")
                                      LogicalComponentManager logicalComponentManager,
                                      @Reference PhysicalWireGenerator physicalWireGenerator) {
        this.generatorRegistry = generatorRegistry;
        this.routingService = routingService;
        this.logicalComponentManager = logicalComponentManager;
        this.physicalWireGenerator = physicalWireGenerator;
    }

    /**
     * Generate and provision physical change sets for a set of new components.
     *
     * @param components the components to generate
     * @return a Map of Generation contexts keyed by runtimeId
     * @throws ActivateException if there was a problem
     */
    public Map<URI, GeneratorContext> generate(Collection<LogicalComponent<?>> components) throws ActivateException {

        Map<URI, GeneratorContext> contexts = new HashMap<URI, GeneratorContext>();

        try {
            for (LogicalComponent<?> component : components) {
                generateChangeSets(component, contexts);
            }
            for (LogicalComponent<?> component : components) {
                generateCommandSets(component, contexts);
            }
        } catch (GenerationException e) {
            throw new ActivateException(e);
        } catch (ResolutionException e) {
            throw new ActivateException(e);
        }

        return contexts;

    }

    public Map<URI, GeneratorContext> generate(Set<LogicalWire> logicalWires, LogicalComponent<?> logicalComponent) throws ActivateException {

        Map<URI, GeneratorContext> contexts = new HashMap<URI, GeneratorContext>();

        URI runtimeId = logicalComponent.getRuntimeId();
        PhysicalChangeSet changeSet = new PhysicalChangeSet();
        CommandSet commandSet = new CommandSet();
        GeneratorContext context = new DefaultGeneratorContext(changeSet, commandSet);
        contexts.put(runtimeId, context);

        try {
            generateUnboundReferenceWires(logicalComponent, context, logicalWires, true);
        } catch (GenerationException e) {
            throw new ActivateException(e);
        }

        return contexts;
    }

    public void provision(Map<URI, GeneratorContext> contexts) throws ActivateException {

        try {

            for (Map.Entry<URI, GeneratorContext> entry : contexts.entrySet()) {
                routingService.route(entry.getKey(), entry.getValue().getPhysicalChangeSet());
            }

            for (Map.Entry<URI, GeneratorContext> entry : contexts.entrySet()) {
                routingService.route(entry.getKey(), entry.getValue().getCommandSet());
            }

        } catch (RoutingException e) {
            throw new ActivateException(e);
        }

    }

    @SuppressWarnings({"unchecked"})
    private <C extends LogicalComponent<?>> PhysicalComponentDefinition generatePhysicalComponent(C component, GeneratorContext context)
            throws GenerationException {

        ComponentGenerator<C> generator = (ComponentGenerator<C>)
                generatorRegistry.getComponentGenerator(component.getDefinition().getImplementation().getClass());

        return generator.generate(component, context);

    }

    private void generateCommandSet(LogicalComponent<?> component, GeneratorContext context)
            throws GenerationException {

        for (CommandGenerator generator : generatorRegistry.getCommandGenerators()) {
            generator.generate(component, context);
        }

    }

    private void generateChangeSets(LogicalComponent<?> component, Map<URI, GeneratorContext> contexts)
            throws GenerationException, ResolutionException {

        ComponentDefinition<? extends Implementation<?>> definition = component.getDefinition();
        Implementation<?> implementation = definition.getImplementation();
        if (CompositeImplementation.IMPLEMENTATION_COMPOSITE.equals(implementation.getType())) {
            LogicalCompositeComponent composite = (LogicalCompositeComponent) component;
            for (LogicalComponent<?> child : composite.getComponents()) {
                // if the component is already running on a node (e.g. during recovery), skip provisioning
                if (child.isActive()) {
                    continue;
                }
                // generate changesets recursively for children
                generateChangeSets(child, contexts);
            }
        } else {
            // leaf component, generate a physical component and update the change sets
            // if component is already running on a node (e.g. during recovery), skip provisioning
            if (component.isActive() || 
                    component.isProvisioned() || 
                        SingletonImplementation.IMPLEMENTATION_SINGLETON.equals(implementation.getType())) {
                return;
            }
            generatePhysicalComponent(component, contexts);
            generatePhysicalWires(component, contexts);
        }

    }

    /**
     * Generates physical wire definitions for a logical component, updating the GeneratorContext. Wire targets will be resolved against the given
     * parent.
     * <p/>
     *
     * @param component the component to generate wires for
     * @param contexts  the GeneratorContexts to update with physical wire definitions
     * @throws GenerationException if an error occurs generating phyasical wire definitions
     * @throws ResolutionException if an error occurs resolving a wire target
     */
    private void generatePhysicalWires(LogicalComponent<?> component, Map<URI, GeneratorContext> contexts)
            throws GenerationException, ResolutionException {

        URI runtimeId = component.getRuntimeId();
        GeneratorContext context = contexts.get(runtimeId);

        if (context == null) {
            PhysicalChangeSet changeSet = new PhysicalChangeSet();
            CommandSet commandSet = new CommandSet();
            context = new DefaultGeneratorContext(changeSet, commandSet);
            contexts.put(runtimeId, context);
        }

        generateReferenceWires(component, context);

        generateServiceWires(component, context);

        generateResourceWires(component, context);

    }

    private void generateResourceWires(LogicalComponent<?> component, GeneratorContext context) throws GenerationException {

        // generate wire definitions for resources
        for (LogicalResource<?> resource : component.getResources()) {
            physicalWireGenerator.generateResourceWire(component, resource, context);
        }

    }

    private void generateServiceWires(LogicalComponent<?> component, GeneratorContext context) throws GenerationException {

        // generate changesets for bound service wires
        for (LogicalService service : component.getServices()) {

            List<LogicalBinding<?>> bindings = service.getBindings();
            if (bindings.isEmpty()) {
                // service is not bound, skip
                continue;
            }
            for (LogicalBinding<?> binding : service.getBindings()) {
                physicalWireGenerator.generateBoundServiceWire(service, binding, component, context);
            }

        }

    }

    private void generateReferenceWires(LogicalComponent<?> component, GeneratorContext context) throws GenerationException {

        for (LogicalReference logicalReference : component.getReferences()) {

            if (logicalReference.getBindings().isEmpty()) {
                generateUnboundReferenceWires(component, context, logicalReference.getWires(), false);
            } else {
                // TODO this should be extensible and moved out
                LogicalBinding<?> logicalBinding = logicalReference.getBindings().get(0);
                physicalWireGenerator.generateBoundReferenceWire(component, logicalReference, logicalBinding, context);
            }

        }
    }

    private void generateUnboundReferenceWires(LogicalComponent<?> component, GeneratorContext context, Set<LogicalWire> logicalWires, boolean rewire)
            throws GenerationException {

        for (LogicalWire logicalWire : logicalWires) {
            if (logicalWire.isProvisioned()) {
                continue;
            }
            URI uri = logicalWire.getTargetUri();
            LogicalComponent<?> target = logicalComponentManager.getComponent(uri);
            String serviceName = uri.getFragment();
            LogicalService targetService = target.getService(serviceName);
            assert targetService != null;
            while (CompositeImplementation.class.isInstance(target.getDefinition().getImplementation())) {
                LogicalCompositeComponent composite = (LogicalCompositeComponent) target;
                URI promoteUri = targetService.getPromotedUri();
                URI promotedComponent = UriHelper.getDefragmentedName(promoteUri);
                target = composite.getComponent(promotedComponent);
                targetService = target.getService(promoteUri.getFragment());
            }

            LogicalReference reference = logicalWire.getSource();
            physicalWireGenerator.generateUnboundWire(component, reference, targetService, target, context);
            // generate physical callback wires if the forward service is bidirectional
            if (reference.getDefinition().getServiceContract().getCallbackContract() != null) {
                generateResourceWires(component, context);
                physicalWireGenerator.generateUnboundCallbackWire(target, reference, component, context);
            }
            logicalWire.setProvisioned(true);
        }

    }

    private void generateCommandSets(LogicalComponent<?> component, Map<URI, GeneratorContext> contexts) throws GenerationException {
        GeneratorContext context = contexts.get(component.getRuntimeId());
        if (context != null) {
            generateCommandSet(component, context);
            if (component.isEagerInit()) {
                // if the component is eager init, add it to the list of components to initialize on the node it
                // will be provisioned to
                CommandSet commandSet = context.getCommandSet();
                List<Command> set = commandSet.getCommands(CommandSet.Phase.LAST);
                boolean found = false;
                for (Command command : set) {
                    // check if the command exists, and if so update it
                    if (command instanceof InitializeComponentCommand) {
                        ((InitializeComponentCommand) command).addUri(component.getUri());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // a previous command was not found so create one
                    // @FIXME a trailing slash is needed since group ids are set on ComponentDefinitions using URI#resolve(",")
                    URI groupId = URI.create(component.getParent().getUri().toString() + "/");
                    InitializeComponentCommand initCommand = new InitializeComponentCommand(groupId);
                    initCommand.addUri(component.getUri());
                    commandSet.add(CommandSet.Phase.LAST, initCommand);
                }
            }
        }

        if (component instanceof LogicalCompositeComponent) {
            LogicalCompositeComponent composite = (LogicalCompositeComponent) component;
            for (LogicalComponent<?> child : composite.getComponents()) {
                generateCommandSets(child, contexts);
            }
        }

    }

    private void generatePhysicalComponent(LogicalComponent<?> component, Map<URI, GeneratorContext> contexts) throws GenerationException {

        URI id = component.getRuntimeId();
        GeneratorContext context = contexts.get(id);
        if (context == null) {
            PhysicalChangeSet changeSet = new PhysicalChangeSet();
            CommandSet commandSet = new CommandSet();
            context = new DefaultGeneratorContext(changeSet, commandSet);
            contexts.put(id, context);
        }
        context.getPhysicalChangeSet().addComponentDefinition(generatePhysicalComponent(component, context));

        component.setProvisioned(true);

    }

}
