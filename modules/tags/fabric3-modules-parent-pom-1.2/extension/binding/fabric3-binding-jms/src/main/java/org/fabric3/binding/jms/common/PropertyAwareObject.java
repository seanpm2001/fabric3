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
package org.fabric3.binding.jms.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.fabric3.model.type.ModelObject;

public abstract class PropertyAwareObject extends ModelObject {
    private static final long serialVersionUID = 7862305926561642783L;
    private Map<String, String> properties = null;

    /**
     * Sets the properties used to create the administered object.
     *
     * @param properties used to create the administered object.
     */
    public void setProperties(Map<String, String> properties) {
        ensurePropertiesNotNull();
        this.properties.putAll(properties);
    }

    /**
     * Add a Property.
     *
     * @param name  Name of the property.
     * @param value Value of the property.
     */
    public void addProperty(String name, String value) {
        ensurePropertiesNotNull();
        properties.put(name, value);
    }

    private void ensurePropertiesNotNull() {
        if (properties == null) {
            properties = new HashMap<String, String>();
        }
    }

    /**
     * Returns properties used to create the administered object.
     *
     * @return Properties used to create the administered object.
     */
    public Map<String, String> getProperties() {
        if (this.properties != null) {
            return properties;
        } else {
            return Collections.emptyMap();
        }
    }
}