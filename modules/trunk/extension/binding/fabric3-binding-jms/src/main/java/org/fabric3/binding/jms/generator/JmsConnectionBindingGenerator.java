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
package org.fabric3.binding.jms.generator;

import java.net.URI;

import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Reference;

import org.fabric3.binding.jms.model.JmsBindingDefinition;
import org.fabric3.binding.jms.spi.common.JmsBindingMetadata;
import org.fabric3.binding.jms.spi.generator.JmsResourceProvisioner;
import org.fabric3.binding.jms.spi.provision.JmsConnectionSourceDefinition;
import org.fabric3.binding.jms.spi.provision.JmsConnectionTargetDefinition;
import org.fabric3.spi.generator.ConnectionBindingGenerator;
import org.fabric3.spi.generator.GenerationException;
import org.fabric3.spi.model.instance.LogicalBinding;
import org.fabric3.spi.model.physical.PhysicalConnectionSourceDefinition;
import org.fabric3.spi.model.physical.PhysicalConnectionTargetDefinition;

/**
 * Connection binding generator that creates source and target definitions for bound channels, producers, and consumers.
 *
 * @version $Revision$ $Date$
 */
@EagerInit
public class JmsConnectionBindingGenerator implements ConnectionBindingGenerator<JmsBindingDefinition> {
    // optional provisioner for host runtimes to receive callbacks
    private JmsResourceProvisioner provisioner;

    @Reference(required = false)
    public void setProvisioner(JmsResourceProvisioner provisioner) {
        this.provisioner = provisioner;
    }

    public PhysicalConnectionSourceDefinition generateConnectionSource(LogicalBinding<JmsBindingDefinition> binding) throws GenerationException {
        JmsBindingMetadata metadata = binding.getDefinition().getJmsMetadata();
        URI uri = binding.getDefinition().getTargetUri();
        JmsConnectionSourceDefinition definition = new JmsConnectionSourceDefinition(uri, metadata);
        if (provisioner != null) {
            provisioner.generateConnectionSource(definition);
        }
        return definition;
    }

    public PhysicalConnectionTargetDefinition generateConnectionTarget(LogicalBinding<JmsBindingDefinition> binding) throws GenerationException {
        URI uri = binding.getDefinition().getTargetUri();
        JmsBindingMetadata metadata = binding.getDefinition().getJmsMetadata();
        JmsConnectionTargetDefinition definition = new JmsConnectionTargetDefinition(uri, metadata);
        if (provisioner != null) {
            provisioner.generateConnectionTarget(definition);
        }
        return definition;
    }
}