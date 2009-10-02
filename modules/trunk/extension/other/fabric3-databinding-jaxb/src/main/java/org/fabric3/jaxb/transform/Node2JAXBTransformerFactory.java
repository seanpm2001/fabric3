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
package org.fabric3.jaxb.transform;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;

import org.osoa.sca.annotations.Reference;
import org.w3c.dom.Node;

import org.fabric3.jaxb.factory.JAXBContextFactory;
import org.fabric3.model.type.contract.DataType;
import org.fabric3.spi.model.type.java.JavaType;
import org.fabric3.spi.model.type.xsd.XSDConstants;
import org.fabric3.spi.transform.TransformationException;
import org.fabric3.spi.transform.Transformer;
import org.fabric3.spi.transform.TransformerFactory;

/**
 * Creates transformers to convert from a DOM Node to a JAXB object.
 *
 * @version $Rev$ $Date$
 */
public class Node2JAXBTransformerFactory implements TransformerFactory<Node, Object> {
    private JAXBContextFactory contextFactory;

    public Node2JAXBTransformerFactory(@Reference JAXBContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    public boolean canTransform(DataType<?> source, DataType<?> target) {
        return Node.class.isAssignableFrom(source.getPhysical()) && target instanceof JavaType;
    }

    public Transformer<Node, Object> create(DataType<?> source, DataType<?> target, Class<?>... classes) throws TransformationException {
        try {
            if (classes == null || classes.length != 1) {
                throw new UnsupportedOperationException("Null and multiparameter operations not yet supported");
            }

            JAXBContext jaxbContext = contextFactory.createJAXBContext(classes);

            Class<?> type = classes[0];
            if (type.isAnnotationPresent(XmlRootElement.class)) {
                if (XSDConstants.PROPERTY_TYPE.equals(source)) {
                    // the value is a property
                    return new PropertyValue2JAXBTransformer(jaxbContext);
                } else {
                    return new Node2JAXBTransformer(jaxbContext);
                }
            } else {
                return new Node2JAXBElementTransformer(jaxbContext, type);
            }
        } catch (JAXBException e) {
            throw new TransformationException(e);
        }
    }


}