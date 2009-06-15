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
package org.fabric3.jpa.runtime;

import java.net.URI;
import javax.persistence.EntityManagerFactory;

import org.osoa.sca.annotations.Reference;

import org.fabric3.jpa.provision.PersistenceUnitWireTargetDefinition;
import org.fabric3.jpa.spi.EmfBuilderException;
import org.fabric3.jpa.spi.classloading.EmfClassLoaderService;
import org.fabric3.spi.ObjectFactory;
import org.fabric3.spi.SingletonObjectFactory;
import org.fabric3.spi.builder.WiringException;
import org.fabric3.spi.builder.component.TargetWireAttacher;
import org.fabric3.spi.model.physical.PhysicalWireSourceDefinition;
import org.fabric3.spi.wire.Wire;

/**
 * Attaches the target side of entity manager factories.
 *
 * @version $Rev$ $Date$
 */
public class PersistenceUnitWireAttacher implements TargetWireAttacher<PersistenceUnitWireTargetDefinition> {

    private final EmfBuilder emfBuilder;
    private EmfClassLoaderService classLoaderService;

    /**
     * Injects the dependencies.
     *
     * @param emfBuilder         Entity manager factory builder.
     * @param classLoaderService the classloader service for returning EMF classloaders
     */
    public PersistenceUnitWireAttacher(@Reference EmfBuilder emfBuilder, @Reference EmfClassLoaderService classLoaderService) {
        this.emfBuilder = emfBuilder;
        this.classLoaderService = classLoaderService;
    }

    public void attachToTarget(PhysicalWireSourceDefinition source, PersistenceUnitWireTargetDefinition target, Wire wire) throws WiringException {
        throw new AssertionError();
    }

    public void detachFromTarget(PhysicalWireSourceDefinition source, PersistenceUnitWireTargetDefinition target) throws WiringException {
        throw new AssertionError();
    }

    public ObjectFactory<?> createObjectFactory(PersistenceUnitWireTargetDefinition target) throws WiringException {

        final String unitName = target.getUnitName();
        URI classLoaderUri = target.getClassLoaderId();
        final ClassLoader appCl = classLoaderService.getEmfClassLoader(classLoaderUri);
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(appCl);
            EntityManagerFactory entityManagerFactory = emfBuilder.build(unitName, appCl);
            return new SingletonObjectFactory<EntityManagerFactory>(entityManagerFactory);
        } catch (EmfBuilderException e) {
            throw new WiringException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }

    }

}
