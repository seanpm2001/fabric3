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
package org.fabric3.resource.wire;

import java.net.URI;

import org.osoa.sca.annotations.Reference;

import org.fabric3.resource.model.SystemSourcedWireTargetDefinition;
import org.fabric3.spi.ObjectFactory;
import org.fabric3.spi.services.componentmanager.ComponentManager;
import org.fabric3.spi.builder.WiringException;
import org.fabric3.spi.builder.component.TargetWireAttacher;
import org.fabric3.spi.component.AtomicComponent;
import org.fabric3.spi.model.physical.PhysicalWireSourceDefinition;
import org.fabric3.spi.util.UriHelper;
import org.fabric3.spi.wire.Wire;

/**
 * @version $Revision$ $Date$
 */
public class SystemSourcedResourceWireAttacher implements TargetWireAttacher<SystemSourcedWireTargetDefinition> {
    private final ComponentManager manager;

    public SystemSourcedResourceWireAttacher(@Reference ComponentManager manager) {
        this.manager = manager;
    }

    public void attachToTarget(PhysicalWireSourceDefinition source, SystemSourcedWireTargetDefinition target, Wire wire)
            throws WiringException {
        throw new AssertionError();
    }

    public void detachFromTarget(PhysicalWireSourceDefinition source, SystemSourcedWireTargetDefinition target) throws WiringException {
        throw new AssertionError();
    }

    public ObjectFactory<?> createObjectFactory(SystemSourcedWireTargetDefinition target) throws WiringException {
        URI targetId = UriHelper.getDefragmentedName(target.getUri());
        AtomicComponent<?> targetComponent = (AtomicComponent<?>) manager.getComponent(targetId);
        return targetComponent.createObjectFactory();
    }
}
