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
 *
 * ----------------------------------------------------
 *
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 *
 */
package org.fabric3.spi.model.physical;

import java.net.URI;
import java.io.Serializable;

import org.w3c.dom.Document;

import org.fabric3.model.type.java.InjectableAttribute;

/**
 * Represents the source side of a wire.
 *
 * @version $Revision$ $Date$
 */
public class PhysicalWireSourceDefinition implements Serializable {
    private static final long serialVersionUID = 2560576437284123839L;

    private URI uri;
    private URI classLoaderId;
    private InjectableAttribute injectableAttribute;
    private boolean optimizable;
    private Document key;
    private InteractionType type = InteractionType.STATELESS;

    /**
     * Returns the URI of the physical component that is the source of invocations on this wire.
     *
     * @return the URI of the physical component that is the source of invocations on this wire
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Sets the URI of the physical component that is the source of invocations on this wire.
     *
     * @param uri the URI of the physical component that is the source of invocations on this wire
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    /**
     * Returns the id of the classloader associated with the source.
     *
     * @return the id of the classloader associated with the source
     */
    public URI getClassLoaderId() {
        return classLoaderId;
    }

    /**
     * Sets the id of the classloader associated with the source..
     *
     * @param classLoaderId the id of the classloader associated with the source
     */
    public void setClassLoaderId(URI classLoaderId) {
        this.classLoaderId = classLoaderId;
    }


    /**
     * Returns the value source for this wire. This identifies which reference or resource on the component this wire applies to.
     *
     * @return the value source for this wire
     */
    public InjectableAttribute getValueSource() {
        return injectableAttribute;
    }

    /**
     * Sets the value source for this wire.
     *
     * @param injectableAttribute the value source for this wire
     */
    public void setValueSource(InjectableAttribute injectableAttribute) {
        this.injectableAttribute = injectableAttribute;
    }

    /**
     * Returns whether the source side of the wire is optimizable.
     *
     * @return true if the source side of the wire is optimizable
     */
    public boolean isOptimizable() {
        return optimizable;
    }

    /**
     * Sets whether the source side of the wire is optimizable.
     *
     * @param optimizable whether the source side of the wire is optimizable
     */
    public void setOptimizable(boolean optimizable) {
        this.optimizable = optimizable;
    }

    /**
     * Returns the interaction style for the wire.
     *
     * @return the interaction style for the wire
     */
    public InteractionType getInteractionType() {
        return type;
    }

    /**
     * Sets the interaction style for the wire. If none is set, the default {@link InteractionType#STATELESS} is used.
     *
     * @param type the interaction style for the wire
     */
    public void setInteractionType(InteractionType type) {
        this.type = type;
    }

    /**
     * Returns the key to be used when this wire is part of a Map reference.
     *
     * @return the key to be used when this wire is part of a Map reference
     */
    public Document getKey() {
        return key;
    }

    /**
     * Sets the key to be used when this wire is part of a Map reference.
     *
     * @param key the key to be used when this wire is part of a Map reference
     */
    public void setKey(Document key) {
        this.key = key;
    }
}
