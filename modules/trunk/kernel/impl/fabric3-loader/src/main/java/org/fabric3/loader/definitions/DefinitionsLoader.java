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
   */
package org.fabric3.loader.definitions;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static org.oasisopen.sca.Constants.SCA_NS;
import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Init;
import org.osoa.sca.annotations.Reference;

import org.fabric3.host.contribution.InstallException;
import org.fabric3.model.type.definitions.AbstractDefinition;
import org.fabric3.model.type.definitions.BindingType;
import org.fabric3.model.type.definitions.ImplementationType;
import org.fabric3.model.type.definitions.Intent;
import org.fabric3.model.type.definitions.PolicySet;
import org.fabric3.spi.contribution.Resource;
import org.fabric3.spi.contribution.ResourceElement;
import org.fabric3.spi.contribution.ResourceElementNotFoundException;
import org.fabric3.spi.contribution.Symbol;
import org.fabric3.spi.contribution.manifest.QNameSymbol;
import org.fabric3.spi.contribution.xml.XmlResourceElementLoader;
import org.fabric3.spi.contribution.xml.XmlResourceElementLoaderRegistry;
import org.fabric3.spi.introspection.DefaultIntrospectionContext;
import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.xml.Loader;
import org.fabric3.spi.introspection.xml.UnrecognizedAttribute;
import org.fabric3.spi.introspection.xml.UnrecognizedElementException;

/**
 * Loader for definitions.
 *
 * @version $Revision$ $Date$
 */
@EagerInit
public class DefinitionsLoader implements XmlResourceElementLoader {

    static final QName INTENT = new QName(SCA_NS, "intent");
    static final QName DESCRIPTION = new QName(SCA_NS, "description");
    static final QName POLICY_SET = new QName(SCA_NS, "policySet");
    static final QName BINDING_TYPE = new QName(SCA_NS, "bindingType");
    static final QName IMPLEMENTATION_TYPE = new QName(SCA_NS, "implementationType");

    private static final QName DEFINITIONS = new QName(SCA_NS, "definitions");

    private XmlResourceElementLoaderRegistry elementLoaderRegistry;
    private Loader loaderRegistry;

    public DefinitionsLoader(@Reference XmlResourceElementLoaderRegistry elementLoaderRegistry,
                             @Reference Loader loader) {
        this.elementLoaderRegistry = elementLoaderRegistry;
        this.loaderRegistry = loader;
    }

    @Init
    public void init() {
        elementLoaderRegistry.register(this);
    }

    public QName getType() {
        return DEFINITIONS;
    }

    public void load(XMLStreamReader reader, URI contributionUri, Resource resource, IntrospectionContext context, ClassLoader loader)
            throws InstallException, XMLStreamException {
        validateAttributes(reader, context);

        List<AbstractDefinition> definitions = new ArrayList<AbstractDefinition>();

        String targetNamespace = reader.getAttributeValue(null, "targetNamespace");

        IntrospectionContext childContext = new DefaultIntrospectionContext(contributionUri, loader, targetNamespace);

        while (true) {
            switch (reader.next()) {
            case START_ELEMENT:
                QName qname = reader.getName();
                AbstractDefinition definition = null;
                if (INTENT.equals(qname)) {
                    try {
                        definition = loaderRegistry.load(reader, Intent.class, childContext);
                    } catch (UnrecognizedElementException e) {
                        throw new InstallException(e);
                    }
                } else if (POLICY_SET.equals(qname)) {
                    try {
                        definition = loaderRegistry.load(reader, PolicySet.class, childContext);
                    } catch (UnrecognizedElementException e) {
                        throw new InstallException(e);
                    }
                } else if (BINDING_TYPE.equals(qname)) {
                    try {
                        definition = loaderRegistry.load(reader, BindingType.class, childContext);
                    } catch (UnrecognizedElementException e) {
                        throw new InstallException(e);
                    }
                } else if (IMPLEMENTATION_TYPE.equals(qname)) {
                    try {
                        definition = loaderRegistry.load(reader, ImplementationType.class, childContext);
                    } catch (UnrecognizedElementException e) {
                        throw new InstallException(e);
                    }
                }
                if (definition != null) {
                    definitions.add(definition);
                }
                break;
            case END_ELEMENT:
                assert DEFINITIONS.equals(reader.getName());
                // update indexed elements with the loaded definitions
                for (AbstractDefinition candidate : definitions) {
                    boolean found = false;
                    for (ResourceElement element : resource.getResourceElements()) {
                        Symbol candidateSymbol = new QNameSymbol(candidate.getName());
                        if (element.getSymbol().equals(candidateSymbol)) {
                            element.setValue(candidate);
                            found = true;
                        }
                    }
                    if (!found) {
                        String id = candidate.toString();
                        // xcv should not throw?
                        throw new ResourceElementNotFoundException("Definition not found: " + id, id);
                    }
                }
                if (childContext.hasErrors()) {
                    context.addErrors(childContext.getErrors());
                }
                if (childContext.hasWarnings()) {
                    context.addWarnings(childContext.getWarnings());
                }
                return;
            }
        }

    }

    private void validateAttributes(XMLStreamReader reader, IntrospectionContext context) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String name = reader.getAttributeLocalName(i);
            if (!"targetNamespace".equals(name)) {
                context.addError(new UnrecognizedAttribute(name, reader));
            }
        }
    }


}
