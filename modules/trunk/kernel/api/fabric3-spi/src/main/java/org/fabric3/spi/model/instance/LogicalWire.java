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
package org.fabric3.spi.model.instance;

import java.util.Set;
import javax.xml.namespace.QName;

import org.oasisopen.sca.Constants;

/**
 * Represents a wire from a reference to a service in the domain. A wire always targets a service in the domain (as opposed to a service hosted
 * externally) and hence is expressed using the SCA URI of the target service. A wire is expressed by using the <code>target</code> attribute of a
 * <code>reference</code> element or using the <code>wire</code> element. Furthermore, a wire may be unbound or explicitly configured with a binding.
 * If the wire is unbound and crosses process boundaries, it will be bound by the runtime using the SCA binding.
 * <p/>
 * During deployment, wires are created and resolved incrementally. A wire is created for When a wire is instantiated, its source reference and target
 * service URI are resolved against the domain.
 *
 * @version $Rev$ $Date$
 */
public class LogicalWire extends LogicalScaArtifact<LogicalComponent<?>> {
    private static final long serialVersionUID = -643283191171197255L;
    private static final QName TYPE = new QName(Constants.SCA_NS, "wire");

    private LogicalReference source;
    private LogicalService target;
    private QName deployable;
    private LogicalState state = LogicalState.NEW;

    /**
     * Instantiates a logical wire.
     *
     * @param parent     component within which the wire is defined.
     * @param source     the source reference of the wire
     * @param target     the target service
     * @param deployable the target service deployable
     */
    public LogicalWire(LogicalComponent<?> parent, LogicalReference source, LogicalService target, QName deployable) {
        super(null, parent, TYPE);
        this.source = source;
        this.target = target;
        this.deployable = deployable;
    }

    /**
     * Gets the source of the wire.
     *
     * @return source of the wire.
     */
    public LogicalReference getSource() {
        return source;
    }

    /**
     * Gets the target service of the wire.
     *
     * @return target service of the wire.
     */
    public LogicalService getTarget() {
        return target;
    }

    /**
     * Intents are not supported on wires.
     *
     * @return intents declared on the SCA artifact.
     */
    @Override
    public Set<QName> getIntents() {
        throw new UnsupportedOperationException("Intents are not supported on wires");
    }

    /**
     * Policy sets are not supported on wires.
     *
     * @return policy sets declared on the SCA artifact.
     */
    @Override
    public Set<QName> getPolicySets() {
        throw new UnsupportedOperationException("Policy sets are not supported on wires");
    }

    /**
     * Returns the wire state.
     *
     * @return the wire state
     */
    public LogicalState getState() {
        return state;
    }

    /**
     * Sets the wire state.
     *
     * @param state the wire state
     */
    public void setState(LogicalState state) {
        this.state = state;
    }

    /**
     * Returns the deployable of the target for this wire. A source of a wire may be deployed via a different deployable thant its target. This value
     * is used to track the target deployable so the wire may be undeployed along wih the target even if the source is not.
     *
     * @return the deployable that provisioned the wire.
     */
    public QName getTargetDeployable() {
        return deployable;
    }

    /**
     * Tests for quality whether the source and target URIs are the same.
     *
     * @param obj Object to be tested against.
     */
    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }

        LogicalWire test = (LogicalWire) obj;
        return target.equals(test.target) && source.equals(test.source);

    }

    /**
     * Hashcode based on the source and target URIs.
     *
     * @return Hashcode based on the source and target URIs.
     */
    public int hashCode() {

        int hash = 7;
        hash = 31 * hash + source.hashCode();
        hash = 31 * hash + target.hashCode();
        return hash;

    }


}
