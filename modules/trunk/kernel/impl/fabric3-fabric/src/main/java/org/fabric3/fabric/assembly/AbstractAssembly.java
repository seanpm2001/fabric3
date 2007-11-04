/*
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
package org.fabric3.fabric.assembly;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;

import static org.osoa.sca.Constants.SCA_NS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.fabric3.fabric.assembly.allocator.AllocationException;
import org.fabric3.fabric.assembly.allocator.Allocator;
import org.fabric3.fabric.assembly.normalizer.PromotionNormalizer;
import org.fabric3.fabric.assembly.resolver.WireResolver;
import org.fabric3.fabric.command.InitializeComponentCommand;
import org.fabric3.fabric.generator.DefaultGeneratorContext;
import org.fabric3.fabric.services.routing.RoutingException;
import org.fabric3.fabric.services.routing.RoutingService;
import org.fabric3.scdl.AbstractComponentType;
import org.fabric3.scdl.Autowire;
import org.fabric3.scdl.BindingDefinition;
import org.fabric3.scdl.ComponentDefinition;
import org.fabric3.scdl.ComponentReference;
import org.fabric3.scdl.ComponentService;
import org.fabric3.scdl.Composite;
import org.fabric3.scdl.CompositeImplementation;
import org.fabric3.scdl.CompositeReference;
import org.fabric3.scdl.CompositeService;
import org.fabric3.scdl.Implementation;
import org.fabric3.scdl.Property;
import org.fabric3.scdl.PropertyValue;
import org.fabric3.scdl.ReferenceDefinition;
import org.fabric3.scdl.ResourceDefinition;
import org.fabric3.scdl.Scope;
import org.fabric3.scdl.ServiceDefinition;
import org.fabric3.spi.assembly.ActivateException;
import org.fabric3.spi.assembly.Assembly;
import org.fabric3.spi.assembly.AssemblyException;
import org.fabric3.spi.assembly.AssemblyStore;
import org.fabric3.spi.assembly.BindException;
import org.fabric3.spi.assembly.RecordException;
import org.fabric3.spi.command.Command;
import org.fabric3.spi.command.CommandSet;
import org.fabric3.spi.generator.GenerationException;
import org.fabric3.spi.generator.GeneratorContext;
import org.fabric3.spi.generator.GeneratorRegistry;
import org.fabric3.spi.model.instance.LogicalBinding;
import org.fabric3.spi.model.instance.LogicalComponent;
import org.fabric3.spi.model.instance.LogicalReference;
import org.fabric3.spi.model.instance.LogicalResource;
import org.fabric3.spi.model.instance.LogicalService;
import org.fabric3.spi.model.physical.PhysicalChangeSet;
import org.fabric3.spi.services.contribution.MetaDataStore;
import org.fabric3.spi.services.contribution.QNameSymbol;
import org.fabric3.spi.services.contribution.ResourceElement;
import org.fabric3.spi.util.UriHelper;

/**
 * Base class for abstract assemblies
 *
 * @version $Rev$ $Date$
 */
public abstract class AbstractAssembly implements Assembly {

    public static final QName COMPOSITE = new QName(SCA_NS, "composite");

    private static final DocumentBuilderFactory DOCUMENT_FACTORY;
    private static final XPathFactory XPATH_FACTORY;

    static {
        DOCUMENT_FACTORY = DocumentBuilderFactory.newInstance();
        DOCUMENT_FACTORY.setNamespaceAware(true);

        XPATH_FACTORY = XPathFactory.newInstance();
    }

    protected final URI domainUri;
    protected final GeneratorRegistry generatorRegistry;
    protected final WireResolver wireResolver;
    protected final Allocator allocator;
    protected final RoutingService routingService;
    protected final MetaDataStore metadataStore;
    protected final PromotionNormalizer promotionNormalizer;
    protected LogicalComponent<CompositeImplementation> domain;
    protected AssemblyStore assemblyStore;

    public AbstractAssembly(URI domainUri,
                            GeneratorRegistry generatorRegistry,
                            WireResolver wireResolver,
                            PromotionNormalizer normalizer,
                            Allocator allocator,
                            RoutingService routingService,
                            AssemblyStore assemblyStore,
                            MetaDataStore metadataStore) {
        this.domainUri = domainUri;
        this.generatorRegistry = generatorRegistry;
        this.wireResolver = wireResolver;
        this.promotionNormalizer = normalizer;
        this.allocator = allocator;
        this.routingService = routingService;
        this.assemblyStore = assemblyStore;
        this.metadataStore = metadataStore;
    }

    public void initialize() throws AssemblyException {
        // read the logical model from the store
        domain = assemblyStore.read();
        Collection<LogicalComponent<?>> components = domain.getComponents();

        // TODO we've recovered the domain content so should not need to generate/provision things
        // TODO once everything is recovered though we may decide to reoptimize the domain
        // TODO but we should add an API call for that
        try {
            for (LogicalComponent<?> component : components) {
                allocator.allocate(component, false);
            }
        } catch (AllocationException e) {
            throw new ActivateException(e);
        }

        // generate and provision components on nodes that have gone down
        Map<URI, GeneratorContext> contexts = generate(domain, components);
        provision(contexts);
        // TODO end temporary recovery code
    }

    public LogicalComponent<CompositeImplementation> getDomain() {
        return domain;
    }

    public void includeInDomain(QName deployable) throws ActivateException {
        ResourceElement<QNameSymbol, ?> element = metadataStore.resolve(new QNameSymbol(deployable));
        if (element == null) {
            throw new ArtifactNotFoundException("Deployable not found", deployable.toString());
        }
        Object object = element.getValue();
        if (!(object instanceof Composite)) {
            throw new IllegalContributionTypeException("Deployable must be a composite", deployable.toString());
        }
        Composite composite = (Composite) object;
        includeInDomain(composite);
    }

    public void includeInDomain(Composite composite) throws ActivateException {
        include(domain, composite);
        try {
            // record the operation
            assemblyStore.store(domain);
        } catch (RecordException e) {
            throw new ActivateException("Error activating deployable", composite.getName().toString(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public void include(LogicalComponent<CompositeImplementation> parent, Composite composite)
            throws ActivateException {

        // merge the property values into the parent
        for (Property<?> property : composite.getProperties().values()) {
            String name = property.getName();
            if (parent.getPropertyValues().containsKey(name)) {
                throw new ActivateException("Duplicate property", name);
            }
            Document value = property.getDefaultValue();
            parent.setPropertyValue(name, value);
        }

        // instantiate all the components in the composite and add them to the parent
        String base = parent.getUri().toString();
        Collection<ComponentDefinition<? extends Implementation<?>>> definitions = composite.getComponents().values();
        List<LogicalComponent<?>> components = new ArrayList<LogicalComponent<?>>(definitions.size());
        for (ComponentDefinition<? extends Implementation<?>> definition : definitions) {
            LogicalComponent<?> logicalComponent = instantiate(parent, definition);
            // use autowire settings on the original composite as an override if they are specified
            Autowire autowire = composite.getAutowire();
            if (autowire == Autowire.ON || autowire == Autowire.OFF) {
                logicalComponent.setAutowireOverride(autowire);
            }
            components.add(logicalComponent);
            parent.addComponent(logicalComponent);
        }

        // merge the composite service declarations into the parent
        for (CompositeService compositeService : composite.getServices().values()) {
            URI serviceURI = URI.create(base + '#' + compositeService.getName());
            LogicalService logicalService = new LogicalService(serviceURI, compositeService, parent);
            if (compositeService.getPromote() != null) {
                logicalService.setPromote(URI.create(base + "/" + compositeService.getPromote()));
            }
            for (BindingDefinition binding : compositeService.getBindings()) {
                logicalService.addBinding(new LogicalBinding<BindingDefinition>(binding, logicalService));
            }
            parent.addService(logicalService);
        }

        // merge the composite reference definitions into the parent
        for (CompositeReference compositeReference : composite.getReferences().values()) {
            URI referenceURi = URI.create(base + '#' + compositeReference.getName());
            LogicalReference logicalReference = new LogicalReference(referenceURi, compositeReference, parent);
            for (URI promotedUri : compositeReference.getPromoted()) {
                URI componentId = URI.create(base + "/" + promotedUri.getPath());
                LogicalComponent<?> promotedComponent = parent.getComponent(componentId);
                if (promotedComponent == null) {
                    throw new MissingPromotedComponentException("No component for reference to promote: " + referenceURi, referenceURi.toString());
                }
                if (promotedComponent.getReference(promotedUri.getFragment()) == null) {
                    throw new MissingPromotedReferenceException("No reference on promoted component for: " + referenceURi, referenceURi.toString());
                }
                URI resolvedUri = URI.create(base + "/" + promotedUri.toString());
                logicalReference.addPromotedUri(resolvedUri);
            }

            for (BindingDefinition binding : compositeReference.getBindings()) {
                logicalReference.addBinding(new LogicalBinding<BindingDefinition>(binding, logicalReference));
            }
            parent.addReference(logicalReference);
        }

        // resolve wires for each new component
        try {
            for (LogicalComponent<?> component : components) {
                wireResolver.resolve(component);
            }
        } catch (ResolutionException e) {
            throw new ActivateException(e);
        }

        // normalize bindings for each new component
        for (LogicalComponent<?> component : components) {
            normalize(component);
        }

        // Allocate the components to runtime nodes
        try {
            for (LogicalComponent<?> component : components) {
                allocator.allocate(component, false);
            }
        } catch (AllocationException e) {
            throw new ActivateException(e);
        }

        // generate and provision the new components
        Map<URI, GeneratorContext> contexts = generate(parent, components);
        provision(contexts);

    }

    public void bindService(URI serviceUri, BindingDefinition bindingDefinition) throws BindException {
        LogicalComponent<?> currentComponent = findComponent(serviceUri);
        if (currentComponent == null) {
            throw new BindException("Component not found", serviceUri.toString());
        }
        String fragment = serviceUri.getFragment();
        LogicalService service;
        if (fragment == null) {
            if (currentComponent.getServices().size() != 1) {
                String uri = serviceUri.toString();
                throw new BindException("Component must implement one service if no service name specified", uri);
            }
            Collection<LogicalService> services = currentComponent.getServices();
            service = services.iterator().next();
        } else {
            service = currentComponent.getService(fragment);
            if (service == null) {
                throw new BindException("Service not found", serviceUri.toString());
            }
        }
        LogicalBinding<?> binding = new LogicalBinding<BindingDefinition>(bindingDefinition, service);
        PhysicalChangeSet changeSet = new PhysicalChangeSet();
        CommandSet commandSet = new CommandSet();
        GeneratorContext context = new DefaultGeneratorContext(changeSet, commandSet);
        try {
            generatorRegistry.generateBoundServiceWire(service, binding, currentComponent, context);
            routingService.route(currentComponent.getRuntimeId(), changeSet);
            service.addBinding(binding);
            // TODO record to recovery service
        } catch (GenerationException e) {
            throw new BindException("Error binding service", serviceUri.toString(), e);
        } catch (RoutingException e) {
            throw new BindException(e);
        }
    }

    /**
     * Instantiates a logical component from a component definition
     *
     * @param parent     the parent logical component
     * @param definition the component definition to instantiate from
     * @return the instantiated logical component
     * @throws InstantiationException if an error occurs during instantiation
     */
    protected <I extends Implementation<?>> LogicalComponent<I> instantiate(
            LogicalComponent<CompositeImplementation> parent,
            ComponentDefinition<I> definition) throws InstantiationException {
        URI uri = URI.create(parent.getUri() + "/" + definition.getName());
        I impl = definition.getImplementation();
        if (CompositeImplementation.IMPLEMENTATION_COMPOSITE.equals(impl.getType())) {
            return instantiateComposite(parent, definition, uri);
        } else {
            return instantiateAtomicComponent(parent, definition, uri);
        }
    }

    private <I extends Implementation<?>> LogicalComponent<I> instantiateAtomicComponent(
            LogicalComponent<CompositeImplementation> parent,
            ComponentDefinition<I> definition,
            URI uri) throws InstantiationException {
        URI runtimeId = definition.getRuntimeId();
        LogicalComponent<I> component = new LogicalComponent<I>(uri, runtimeId, definition, parent);
        initializeProperties(component, definition);
        // this is an atomic component so create and bind its services, references and resources
        I impl = definition.getImplementation();
        AbstractComponentType<?, ?, ?, ?> componentType = impl.getComponentType();

        for (ServiceDefinition service : componentType.getServices().values()) {
            String name = service.getName();
            URI serviceUri = uri.resolve('#' + name);
            LogicalService logicalService = new LogicalService(serviceUri, service, component);
            ComponentService componentService = definition.getServices().get(name);
            if (componentService != null) {
                // service is configured in the component definition
                for (BindingDefinition binding : componentService.getBindings()) {
                    logicalService.addBinding(new LogicalBinding<BindingDefinition>(binding, logicalService));
                }
            }
            component.addService(logicalService);
        }

        for (ReferenceDefinition reference : componentType.getReferences().values()) {
            String name = reference.getName();
            URI referenceUri = uri.resolve('#' + name);
            LogicalReference logicalReference = new LogicalReference(referenceUri, reference, component);
            ComponentReference componentReference = definition.getReferences().get(name);
            if (componentReference != null) {
                // reference is configured
                for (BindingDefinition binding : componentReference.getBindings()) {
                    logicalReference.addBinding(new LogicalBinding<BindingDefinition>(binding, logicalReference));
                }
            }
            component.addReference(logicalReference);
        }

        for (ResourceDefinition resource : componentType.getResources().values()) {
            URI resourceUri = uri.resolve('#' + resource.getName());
            LogicalResource<?> logicalResource = createLogicalResource(resource, resourceUri, component);
            component.addResource(logicalResource);
        }
        return component;

    }

    private <I extends Implementation<?>> LogicalComponent<I> instantiateComposite(
            LogicalComponent<CompositeImplementation> parent,
            ComponentDefinition<I> definition,
            URI uri) throws InstantiationException {
        // this component is implemented by a composite so we need to create its children
        // and promote services and references
        URI runtimeId = definition.getRuntimeId();
        LogicalComponent<I> component = new LogicalComponent<I>(uri, runtimeId, definition, parent);
        initializeProperties(component, definition);

        @SuppressWarnings({"unchecked"})
        LogicalComponent<CompositeImplementation> compositeComponent =
                (LogicalComponent<CompositeImplementation>) component;
        Composite composite = compositeComponent.getDefinition().getImplementation().getComponentType();

        // create the child components
        for (ComponentDefinition<? extends Implementation<?>> child : composite.getComponents().values()) {
            component.addComponent(instantiate(compositeComponent, child));
        }
        instantiateCompositeServices(uri, component, composite);
        instantiateCompositeReferences(parent, definition, uri, component, composite);


        return component;
    }

    private <I extends Implementation<?>> void instantiateCompositeServices(URI uri,
                                                                            LogicalComponent<I> component,
                                                                            Composite composite) {
        // promote services
        for (CompositeService service : composite.getServices().values()) {
            URI serviceUri = uri.resolve('#' + service.getName());
            LogicalService logicalService = new LogicalService(serviceUri, service, component);
            if (service.getPromote() != null) {
                logicalService.setPromote(URI.create(uri.toString() + "/" + service.getPromote()));
            }
            for (BindingDefinition binding : service.getBindings()) {
                logicalService.addBinding(new LogicalBinding<BindingDefinition>(binding, logicalService));
            }
            component.addService(logicalService);
        }
    }

    private <I extends Implementation<?>> void instantiateCompositeReferences(
            LogicalComponent<CompositeImplementation> parent,
            ComponentDefinition<I> definition,
            URI uri, LogicalComponent<I> component,
            Composite composite) {
        // create logical references based on promoted references in the composite definition
        for (CompositeReference reference : composite.getReferences().values()) {
            String name = reference.getName();
            URI referenceUri = uri.resolve('#' + name);
            LogicalReference logicalReference = new LogicalReference(referenceUri, reference, component);
            for (BindingDefinition binding : reference.getBindings()) {
                logicalReference.addBinding(new LogicalBinding<BindingDefinition>(binding, logicalReference));
            }
            for (URI promotedUri : reference.getPromoted()) {
                URI resolvedUri = URI.create(uri.toString() + "/" + promotedUri.toString());
                logicalReference.addPromotedUri(resolvedUri);
            }
            ComponentReference componentReference = definition.getReferences().get(name);
            if (componentReference != null) {
                // Merge/override logical reference configuration created above with reference configuration on the
                // composite use. For example, when the component is used as an implementation, it may contain
                // reference configuration. This information must be merged with or used to override any
                // configuration that was created by reference promotions within the composite
                if (!componentReference.getBindings().isEmpty()) {
                    List<LogicalBinding<?>> bindings = new ArrayList<LogicalBinding<?>>();
                    for (BindingDefinition binding : componentReference.getBindings()) {
                        bindings.add(new LogicalBinding<BindingDefinition>(binding, logicalReference));
                    }
                    logicalReference.overrideBindings(bindings);
                }
                if (!componentReference.getTargets().isEmpty()) {
                    List<URI> targets = new ArrayList<URI>();
                    for (URI targetUri : componentReference.getTargets()) {
                        // the target is relative to the component's parent, not the component
                        targets.add(URI.create(parent.getUri().toString() + "/" + targetUri));
                    }
                    logicalReference.overrideTargets(targets);
                }
            }
            component.addReference(logicalReference);
        }
    }

    /**
     * Set the initial actual property values of a component.
     *
     * @param component  the component to initialize
     * @param definition the definition of the component
     * @throws InstantiationException if there was a problem initializing a property value
     */
    protected <I extends Implementation<?>> void initializeProperties(LogicalComponent<I> component,
                                                                      ComponentDefinition<I> definition)
            throws InstantiationException {
        Map<String, PropertyValue> propertyValues = definition.getPropertyValues();
        AbstractComponentType<?, ?, ?, ?> componentType = definition.getComponentType();
        for (Property<?> property : componentType.getProperties().values()) {
            String name = property.getName();
            PropertyValue propertyValue = propertyValues.get(name);
            Document value;
            if (propertyValue == null) {
                // use default value from component type
                value = property.getDefaultValue();
            } else {
                // the spec defines the following sequence
                if (propertyValue.getFile() != null) {
                    // load the value from an external resource
                    value = loadValueFromFile(property.getName(), propertyValue.getFile());
                } else if (propertyValue.getSource() != null) {
                    // get the value by evaluating an XPath against the composite properties
                    try {
                        value = deriveValueFromXPath(propertyValue.getSource(), component.getParent());
                    } catch (XPathExpressionException e) {
                        throw new InstantiationException(e.getMessage(), name, e);
                    }
                } else {
                    // use inline XML file
                    value = propertyValue.getValue();
                }

            }
            component.setPropertyValue(name, value);
        }
    }

    protected Document loadValueFromFile(String name, URI file) throws InvalidPropertyFileException {
        DocumentBuilder builder;
        try {
            builder = DOCUMENT_FACTORY.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new AssertionError();
        }

        URL resource;
        try {
            resource = file.toURL();
        } catch (MalformedURLException e) {
            throw new InvalidPropertyFileException(e.getMessage(), name, e, file);
        }

        InputStream inputStream;
        try {
            inputStream = resource.openStream();
        } catch (IOException e) {
            throw new InvalidPropertyFileException(e.getMessage(), name, e, file);
        }

        try {
            return builder.parse(inputStream);
        } catch (IOException e) {
            throw new InvalidPropertyFileException(e.getMessage(), name, e, file);
        } catch (SAXException e) {
            throw new InvalidPropertyFileException(e.getMessage(), name, e, file);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    protected Document deriveValueFromXPath(String source, final LogicalComponent<?> parent)
            throws XPathExpressionException {
        XPathVariableResolver variableResolver = new XPathVariableResolver() {
            public Object resolveVariable(QName qName) {
                String name = qName.getLocalPart();
                Document value = parent.getPropertyValue(name);
                if (value == null) {
                    return null;
                }
                return value.getDocumentElement();
            }
        };
        XPath xpath = XPATH_FACTORY.newXPath();
        xpath.setXPathVariableResolver(variableResolver);

        DocumentBuilder builder;
        try {
            builder = DOCUMENT_FACTORY.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new AssertionError();
        }

        Document value = builder.newDocument();
        Element root = value.createElement("value");
        // TODO do we need to copy namespace declarations to this root
        value.appendChild(root);
        try {
            NodeList result = (NodeList) xpath.evaluate(source, root, XPathConstants.NODESET);
            for (int i = 0; i < result.getLength(); i++) {
                Node node = result.item(i);
                value.adoptNode(node);
                root.appendChild(node);
            }
        } catch (XPathExpressionException e) {
            // FIXME rethrow this for now, fix if people find it confusing
            // the Apache and Sun implementations of XPath throw a nested NullPointerException
            // if the xpath contains an unresolvable variable. It might be better to throw
            // a more descriptive cause, but that also might be confusing for people who
            // are used to this behaviour
            throw e;
        }
        return value;
    }

    /**
     * Normalizes the component and any children
     *
     * @param component the component to normalize
     */
    protected void normalize(LogicalComponent<?> component) {
        Implementation<?> implementation = component.getDefinition().getImplementation();
        if (CompositeImplementation.IMPLEMENTATION_COMPOSITE.equals(implementation.getType())) {
            for (LogicalComponent<?> child : component.getComponents()) {
                normalize(child);
            }
        } else {
            promotionNormalizer.normalize(component);
        }
    }

    /**
     * Generate and provision physical change sets for a set of new components.
     *
     * @param parent     the composite containing the new components
     * @param components the components to generate
     * @return a Map of Generation contexts keyed by runtimeId
     * @throws ActivateException if there was a problem
     */
    protected Map<URI, GeneratorContext> generate(LogicalComponent<CompositeImplementation> parent,
                                                  Collection<LogicalComponent<?>> components) throws ActivateException {
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

    protected void provision(Map<URI, GeneratorContext> contexts) throws ActivateException {
        // provision the generated change sets
        try {
            // route the change sets to service nodes
            for (Map.Entry<URI, GeneratorContext> entry : contexts.entrySet()) {
                routingService.route(entry.getKey(), entry.getValue().getPhysicalChangeSet());
            }
            // route command sets
            for (Map.Entry<URI, GeneratorContext> entry : contexts.entrySet()) {
                routingService.route(entry.getKey(), entry.getValue().getCommandSet());
            }
        } catch (RoutingException e) {
            throw new ActivateException(e);
        }
    }

    protected void generateChangeSets(LogicalComponent<?> component, Map<URI, GeneratorContext> contexts)
            throws GenerationException, ResolutionException {
        ComponentDefinition<? extends Implementation<?>> definition = component.getDefinition();
        Implementation<?> implementation = definition.getImplementation();
        if (CompositeImplementation.IMPLEMENTATION_COMPOSITE.equals(implementation.getType())) {
            for (LogicalComponent<?> child : component.getComponents()) {
                // if the component is already running on a node (e.g. during recovery), skip provisioning
                if (child.isActive()) {
                    continue;
                }
                // generate changesets recursively for children
                generateChangeSets(child, contexts);
                generatePhysicalWires(child, contexts);
            }
        } else {
            // leaf component, generate a physical component and update the change sets
            // if component is already running on a node (e.g. during recovery), skip provisioning
            if (component.isActive()) {
                return;
            }
            generatePhysicalComponent(component, contexts);
            generatePhysicalWires(component, contexts);
        }
    }

    /**
     * Generates physical wire definitions for a logical component, updating the GeneratorContext. Wire targets will be
     * resolved against the given parent.
     * <p/>
     *
     * @param component the component to generate wires for
     * @param contexts  the GeneratorContexts to update with physical wire definitions
     * @throws GenerationException if an error occurs generating phyasical wire definitions
     * @throws ResolutionException if an error occurs resolving a wire target
     */
    protected void generatePhysicalWires(LogicalComponent<?> component, Map<URI, GeneratorContext> contexts)
            throws GenerationException, ResolutionException {

        URI runtimeId = component.getRuntimeId();
        GeneratorContext context = contexts.get(runtimeId);

        if (context == null) {
            PhysicalChangeSet changeSet = new PhysicalChangeSet();
            CommandSet commandSet = new CommandSet();
            context = new DefaultGeneratorContext(changeSet, commandSet);
            contexts.put(runtimeId, context);
        }

        for (LogicalReference entry : component.getReferences()) {
            if (entry.getBindings().isEmpty()) {
                for (URI uri : entry.getTargetUris()) {
                    LogicalComponent<?> target = findComponent(uri);
                    String serviceName = uri.getFragment();
                    LogicalService targetService = target.getService(serviceName);
                    assert targetService != null;
                    while (CompositeImplementation.class.isInstance(target.getDefinition().getImplementation())) {
                        URI promoteUri = targetService.getPromote();
                        URI promotedComponent = UriHelper.getDefragmentedName(promoteUri);
                        target = target.getComponent(promotedComponent);
                        targetService = target.getService(promoteUri.getFragment());
                    }
                    LogicalReference reference = component.getReference(entry.getUri().getFragment());

                    generatorRegistry.generateUnboundWire(component,
                                                          reference,
                                                          targetService,
                                                          target,
                                                          context);

                }
            } else {
                // TODO this should be extensible and moved out
                LogicalBinding<?> logicalBinding = entry.getBindings().get(0);
                generatorRegistry.generateBoundReferenceWire(component, entry, logicalBinding, context);
            }

        }

        // generate changesets for bound service wires
        for (LogicalService service : component.getServices()) {
            List<LogicalBinding<?>> bindings = service.getBindings();
            if (bindings.isEmpty()) {
                // service is not bound, skip
                continue;
            }
            for (LogicalBinding<?> binding : service.getBindings()) {
                generatorRegistry.generateBoundServiceWire(service, binding, component, context);
            }
        }

        // generate wire definitions for resources
        for (LogicalResource<?> resource : component.getResources()) {
            generatorRegistry.generateResourceWire(component, resource, context);
        }

    }

    protected void generateCommandSets(LogicalComponent<?> component,
                                       Map<URI, GeneratorContext> contexts) throws GenerationException {

        GeneratorContext context = contexts.get(component.getRuntimeId());
        if (context != null) {
            generatorRegistry.generateCommandSet(component, context);
            if (isEagerInit(component)) {
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
        for (LogicalComponent<?> child : component.getComponents()) {
            generateCommandSets(child, contexts);
        }
    }

    /**
     * Generates a physical component from the given logical component, updating the appropriate GeneratorContext or
     * creating a new one if necessary. A GeneratorContext is created for each service node a physical compnent is
     * provisioned to.
     * <p/>
     *
     * @param component the logical component to generate from
     * @param contexts  the collection of generator contexts
     * @throws GenerationException if an exception occurs during generation
     */
    protected void generatePhysicalComponent(LogicalComponent<?> component, Map<URI, GeneratorContext> contexts)
            throws GenerationException {
        URI id = component.getRuntimeId();
        GeneratorContext context = contexts.get(id);
        if (context == null) {
            PhysicalChangeSet changeSet = new PhysicalChangeSet();
            CommandSet commandSet = new CommandSet();
            context = new DefaultGeneratorContext(changeSet, commandSet);
            contexts.put(id, context);
        }
        context.getPhysicalChangeSet().addComponentDefinition(generatorRegistry.generatePhysicalComponent(component,
                                                                                                          context));
    }

    protected boolean isEagerInit(LogicalComponent<?> component) {
        ComponentDefinition<? extends Implementation<?>> definition = component.getDefinition();
        AbstractComponentType<?, ?, ?, ?> componentType = definition.getImplementation().getComponentType();
        if (!componentType.getImplementationScope().equals(Scope.COMPOSITE)) {
            return false;
        }

        Integer level = definition.getInitLevel();
        if (level == null) {
            level = componentType.getInitLevel();
        }
        return level > 0;
    }

    /**
     * Returns the component for the given uri in the domain or null if not found.
     *
     * @param uri the fully qualified component uri
     * @return the component for the given uri or null if not found
     */
    protected LogicalComponent<?> findComponent(URI uri) {
        String defragmentedUri = UriHelper.getDefragmentedNameAsString(uri);
        String domainString = domainUri.toString();
        String[] hierarchy = defragmentedUri.substring(domainString.length() + 1).split("/");
        String currentUri = domainString;
        LogicalComponent<?> currentComponent = domain;
        for (String name : hierarchy) {
            currentUri = currentUri + "/" + name;
            currentComponent = currentComponent.getComponent(URI.create(currentUri));
            if (currentComponent == null) {
                return null;
            }
        }
        return currentComponent;
    }

    /*
     * Creates a logical resource.
     */
    <RD extends ResourceDefinition> LogicalResource<RD> createLogicalResource(RD resourceDefinition, URI resourceUri, LogicalComponent<?> component) {
        return new LogicalResource<RD>(resourceUri, resourceDefinition, component);
    }


}
