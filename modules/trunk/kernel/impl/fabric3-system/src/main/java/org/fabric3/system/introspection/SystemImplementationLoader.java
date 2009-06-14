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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Reference;

import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.java.ImplementationProcessor;
import org.fabric3.spi.introspection.xml.LoaderUtil;
import org.fabric3.spi.introspection.xml.MissingAttribute;
import org.fabric3.spi.introspection.xml.TypeLoader;
import org.fabric3.spi.introspection.xml.UnrecognizedAttribute;
import org.fabric3.system.scdl.SystemImplementation;

/**
 * Loads information for a system implementation
 *
 * @version $Rev$ $Date$
 */
@EagerInit
public class SystemImplementationLoader implements TypeLoader<SystemImplementation> {

    private final ImplementationProcessor<SystemImplementation> implementationProcessor;

    /**
     * Constructor used during bootstrap and load scdl.
     *
     * @param implementationProcessor the component type loader to use
     */
    public SystemImplementationLoader(@Reference ImplementationProcessor<SystemImplementation> implementationProcessor) {
        this.implementationProcessor = implementationProcessor;
    }

    public SystemImplementation load(XMLStreamReader reader, IntrospectionContext introspectionContext) throws XMLStreamException {
        assert SystemImplementation.IMPLEMENTATION_SYSTEM.equals(reader.getName());
        validateAttributes(reader, introspectionContext);
        String implClass = reader.getAttributeValue(null, "class");
        if (implClass == null) {
            MissingAttribute failure = new MissingAttribute("Implementation class must be specified using the class attribute", reader);
            introspectionContext.addError(failure);
            return null;
        }
        LoaderUtil.skipToEndElement(reader);

        SystemImplementation implementation = new SystemImplementation();
        implementation.setImplementationClass(implClass);
        implementationProcessor.introspect(implementation, introspectionContext);
        return implementation;
    }

    private void validateAttributes(XMLStreamReader reader, IntrospectionContext context) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String name = reader.getAttributeLocalName(i);
            if (!"class".equals(name)) {
                context.addError(new UnrecognizedAttribute(name, reader));
            }
        }
    }


}
