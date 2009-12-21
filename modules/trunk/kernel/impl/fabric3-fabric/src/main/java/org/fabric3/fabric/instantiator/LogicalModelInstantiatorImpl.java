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
package org.fabric3.fabric.instantiator;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.osoa.sca.annotations.Reference;

import org.fabric3.host.Namespaces;
import org.fabric3.model.type.component.Autowire;
import org.fabric3.model.type.component.ComponentDefinition;
import org.fabric3.model.type.component.Composite;
import org.fabric3.model.type.component.CompositeImplementation;
import org.fabric3.model.type.component.Implementation;
import org.fabric3.model.type.component.Include;
import org.fabric3.model.type.component.Property;
import org.fabric3.spi.model.instance.LogicalComponent;
import org.fabric3.spi.model.instance.LogicalCompositeComponent;

/**
 * @version $Rev$ $Date$
 */
public class LogicalModelInstantiatorImpl implements LogicalModelInstantiator {
    /**
     * Represents a synthetic composite. Synthetic composites are created to instantiate multiple deployable composites in a single operation.
     */
    private QName SYNTHENTIC_COMPOSITE = new QName(Namespaces.IMPLEMENTATION, "SyntheticComposite");

    private PromotionNormalizer promotionNormalizer;
    private AtomicComponentInstantiator atomicComponentInstantiator;
    private CompositeComponentInstantiator compositeComponentInstantiator;
    private WireInstantiator wireInstantiator;
    private PromotionResolutionService promotionResolutionService;
    private AutowireInstantiator autowireService;

    public LogicalModelInstantiatorImpl(@Reference CompositeComponentInstantiator compositeComponentInstantiator,
                                        @Reference AtomicComponentInstantiator atomicComponentInstantiator,
                                        @Reference WireInstantiator wireInstantiator,
                                        @Reference PromotionNormalizer promotionNormalizer,
                                        @Reference PromotionResolutionService promotionResolutionService,
                                        @Reference AutowireInstantiator autowireService) {
        this.promotionNormalizer = promotionNormalizer;
        this.atomicComponentInstantiator = atomicComponentInstantiator;
        this.compositeComponentInstantiator = compositeComponentInstantiator;
        this.wireInstantiator = wireInstantiator;
        this.promotionResolutionService = promotionResolutionService;
        this.autowireService = autowireService;
    }

    public InstantiationContext include(Composite composite, LogicalCompositeComponent domain) {
        return include(domain, composite, false);
    }

    public InstantiationContext include(List<Composite> composites, LogicalCompositeComponent domain) {
        Composite composite = synthesizeComposite(composites);
        return include(domain, composite, true);
    }

    private InstantiationContext include(LogicalCompositeComponent domain, Composite composite, boolean synthetic) {
        InstantiationContext context = new InstantiationContext();

        // merge the property values into the parent
        includeProperties(composite, domain, context);

        // instantiate all the components in the composite and add them to the parent
        List<LogicalComponent<?>> newComponents = instantiate(composite, domain, synthetic, context);

        // @FIXME XCV log warning about domain level services and references being ignored: composite.getServices(), composite.getReferences()

        // normalize bindings for each new component - this must come before resolution since target URIs may be inherited
        for (LogicalComponent<?> component : newComponents) {
            normalize(component, context);
        }

        // resolve services and references - evaluate all references since reinjection may apply
        for (LogicalComponent<?> component : domain.getComponents()) {
            promotionResolutionService.resolve(component, context);
            autowireService.instantiate(component, context);
        }
        return context;
    }

    private void includeProperties(Composite composite, LogicalCompositeComponent domain, InstantiationContext context) {
        for (Property property : composite.getProperties().values()) {
            String name = property.getName();
            if (domain.getPropertyValues().containsKey(name)) {
                URI parentUri = domain.getUri();
                URI contributionUri = domain.getDefinition().getContributionUri();
                DuplicateProperty error = new DuplicateProperty(name, parentUri, contributionUri);
                context.addError(error);
            } else {
                domain.setPropertyValue(name, property.getDefaultValue());
            }
        }
    }

    /**
     * Instantiates a composite and its children in a domain
     *
     * @param composite the composite definition to instantiate
     * @param domain    the domain logical components
     * @param synthetic true if the composite is synthetic and its children should be treated as a single deployment unit
     * @param context   the instantiation context
     * @return the newly instantiated domain-level components
     */
    private List<LogicalComponent<?>> instantiate(Composite composite,
                                                  LogicalCompositeComponent domain,
                                                  boolean synthetic,
                                                  InstantiationContext context) {

        // instantiate the declared components
        Collection<ComponentDefinition<? extends Implementation<?>>> definitions = composite.getDeclaredComponents().values();
        List<LogicalComponent<?>> newComponents = new ArrayList<LogicalComponent<?>>(definitions.size());
        for (ComponentDefinition<? extends Implementation<?>> definition : definitions) {
            LogicalComponent<?> logicalComponent = instantiate(definition, domain, context);
            setAutowire(logicalComponent, definition, composite);
            setDeployable(logicalComponent, composite.getName());
            newComponents.add(logicalComponent);
        }

        // instantiate the included components
        for (Include include : composite.getIncludes().values()) {
            // xcv FIXME need to recurse down included hierarchy
            for (ComponentDefinition<? extends Implementation<?>> definition : include.getIncluded().getComponents().values()) {
                LogicalComponent<?> logicalComponent = instantiate(definition, domain, context);
                setAutowire(logicalComponent, definition, composite);
                if (synthetic) {
                    // If it is a synthetic composite, included composites are the deployables.
                    // Synthetic composites are used to deploy multiple composites as a group. They include the composites (deployables).
                    // Adding the deployable name to domain-level components allows them to be managed as a group after they are deployed.
                    setDeployable(logicalComponent, include.getIncluded().getName());
                } else {
                    setDeployable(logicalComponent, composite.getName());
                }
                newComponents.add(logicalComponent);
                domain.addComponent(logicalComponent);
            }
        }

        // instantiate wires - note this must be done after the included components as wire targets may resolve to an included service
        wireInstantiator.instantiateCompositeWires(composite, domain, context);
        for (LogicalComponent<?> component : newComponents) {
            wireInstantiator.instantiateReferenceWires(component, context);
        }

        return newComponents;
    }

    /**
     * Instantiates a component in the context of a parent logical composite component.
     *
     * @param definition the component definition to instantiate
     * @param parent     the parent logical composite
     * @param context    the instantiation context
     * @return the instantiated component
     */
    @SuppressWarnings("unchecked")
    private LogicalComponent<?> instantiate(ComponentDefinition<?> definition, LogicalCompositeComponent parent, InstantiationContext context) {

        if (definition.getImplementation() instanceof CompositeImplementation) {
            ComponentDefinition<CompositeImplementation> componentDefinition = (ComponentDefinition<CompositeImplementation>) definition;
            return compositeComponentInstantiator.instantiate(componentDefinition, parent, context);
        } else {
            ComponentDefinition<Implementation<?>> componentDefinition = (ComponentDefinition<Implementation<?>>) definition;
            return atomicComponentInstantiator.instantiate(componentDefinition, parent, context);
        }
    }


    /**
     * Normalizes the component and any children
     *
     * @param component the component to normalize
     * @param context   the instantiation context
     */
    private void normalize(LogicalComponent<?> component, InstantiationContext context) {
        if (component instanceof LogicalCompositeComponent) {
            LogicalCompositeComponent composite = (LogicalCompositeComponent) component;
            for (LogicalComponent<?> child : composite.getComponents()) {
                normalize(child, context);
            }
        } else {
            promotionNormalizer.normalize(component, context);
        }

    }

    /**
     * Synthesizes a composite from a collection of composites using inclusion.
     *
     * @param composites the composites to synthesize
     * @return the synthesized composite
     */
    private Composite synthesizeComposite(List<Composite> composites) {
        Composite synthesized = new Composite(SYNTHENTIC_COMPOSITE);
        for (Composite composite : composites) {
            Include include = new Include();
            include.setName(composite.getName());
            include.setIncluded(composite);
            synthesized.add(include);

        }
        return synthesized;
    }

    /**
     * Recursively sets the deployable composite the logical component was instantiated from.
     *
     * @param component  the logical component
     * @param deployable the deployable
     */
    private void setDeployable(LogicalComponent<?> component, QName deployable) {
        if (component instanceof LogicalCompositeComponent) {
            LogicalCompositeComponent composite = (LogicalCompositeComponent) component;
            for (LogicalComponent<?> child : composite.getComponents()) {
                setDeployable(child, deployable);
            }
        }
        component.setDeployable(deployable);
    }

    /**
     * Calculates the effective autowire value for a logical component.
     *
     * @param logicalComponent the logical component
     * @param definition       the component definition of the logical component
     * @param composite        the original composite containing the component definition
     */
    private void setAutowire(LogicalComponent<?> logicalComponent, ComponentDefinition<?> definition, Composite composite) {
        // use autowire settings on the original composite as an override if they are not specified on the component
        Autowire autowire;
        if (definition.getAutowire() == Autowire.INHERITED) {
            autowire = composite.getAutowire();
        } else {
            autowire = definition.getAutowire();
        }
        if (autowire == Autowire.ON || autowire == Autowire.OFF) {
            logicalComponent.setAutowireOverride(autowire);
        }
    }


}
