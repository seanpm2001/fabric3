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
package org.fabric3.spi.lcm;

import java.net.URI;
import java.util.Collection;

import org.fabric3.spi.model.instance.LogicalComponent;
import org.fabric3.spi.model.instance.LogicalCompositeComponent;

/**
 * Manages logical components in a domain. There is one LogicalComponentManager per domain. Implementations may bve transient or persistent.
 *
 * @version $Rev$ $Date$
 */
public interface LogicalComponentManager {

    /**
     * Returns the root component in the domain.
     *
     * @return the root component in the domain.
     */
    LogicalCompositeComponent getRootComponent();

    /**
     * Replaces the root component in the domain. This is generally used during deployment to update the domain with a modified copy of the logical
     * model.
     *
     * @param component the replacement
     * @throws WriteException if an error occurs replacing the root component
     */
    void replaceRootComponent(LogicalCompositeComponent component) throws WriteException;

    /**
     * Returns the component uniquely identified by an id.
     *
     * @param uri the unique id of the component
     * @return the component uniquely identified by an id, or null
     */
    LogicalComponent<?> getComponent(URI uri);

    /**
     * Gets the top level logical components in the domain (the immediate children of the domain component).
     *
     * @return the top level components in the domain
     */
    Collection<LogicalComponent<?>> getComponents();

    /**
     * Initializes the manager.
     *
     * @throws ReadException if there was a problem initializing the components
     */
    void initialize() throws ReadException;


}