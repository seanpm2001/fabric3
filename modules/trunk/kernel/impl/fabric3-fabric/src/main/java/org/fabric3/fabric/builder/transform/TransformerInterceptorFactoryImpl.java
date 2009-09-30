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
package org.fabric3.fabric.builder.transform;

import java.util.HashSet;
import java.util.Set;

import org.osoa.sca.annotations.Reference;

import org.fabric3.model.type.contract.DataType;
import org.fabric3.spi.builder.WiringException;
import org.fabric3.spi.builder.transform.TransformerInterceptorFactory;
import org.fabric3.spi.model.physical.ParameterTypeHelper;
import org.fabric3.spi.model.physical.PhysicalOperationDefinition;
import org.fabric3.spi.transform.TransformerRegistry;
import org.fabric3.spi.transform.TransformationException;
import org.fabric3.spi.transform.Transformer;
import org.fabric3.spi.wire.Interceptor;

/**
 * @version $Rev$ $Date$
 */
public class TransformerInterceptorFactoryImpl implements TransformerInterceptorFactory {
    private TransformerRegistry registry;

    public TransformerInterceptorFactoryImpl(@Reference TransformerRegistry registry) {
        this.registry = registry;
    }

    @SuppressWarnings({"unchecked"})
    public Interceptor createInputInterceptor(PhysicalOperationDefinition definition, DataType<?> source, DataType<?> target, ClassLoader loader)
            throws WiringException {
        Class<?>[] types = loadInputTypes(definition, loader);
        try {
            Transformer<Object, Object> transformer = (Transformer<Object, Object>) registry.getTransformer(source, target, types);
            return new InputTransformerInterceptor(transformer, loader);
        } catch (TransformationException e) {
            throw new WiringException(e);
        }
    }

    @SuppressWarnings({"unchecked"})
    public Interceptor createOutputInterceptor(PhysicalOperationDefinition definition, DataType<?> source, DataType<?> target, ClassLoader loader)
            throws WiringException {
        Class<?>[] types = loadOutputTypes(definition, loader);
        try {
            Transformer<Object, Object> transformer = (Transformer<Object, Object>) registry.getTransformer(source, target, types);
            return new OutputTransformerInterceptor(transformer, loader);
        } catch (TransformationException e) {
            throw new WiringException(e);
        }
    }

    /**
     * Loads the input physical parameter types in the contribution classloader associated of the target service.
     *
     * @param definition the physical operation definition
     * @param loader     the  contribution classloader
     * @return a collection of loaded parameter types
     * @throws WiringException if an error occurs loading the parameter types
     */
    private Class<?>[] loadInputTypes(PhysicalOperationDefinition definition, ClassLoader loader) throws WiringException {
        try {
            Set<Class<?>> types = ParameterTypeHelper.loadInParameterTypes(definition, loader);
            return types.toArray(new Class<?>[types.size()]);
        } catch (ClassNotFoundException e) {
            throw new WiringException(e);
        }
    }

    /**
     * Loads the output physical parameter types in the contribution classloader associated of the target service.
     *
     * @param definition the physical operation definition
     * @param loader     the  contribution classloader
     * @return a collection of loaded parameter types
     * @throws WiringException if an error occurs loading the parameter types
     */
    private Class<?>[] loadOutputTypes(PhysicalOperationDefinition definition, ClassLoader loader) throws WiringException {
        Set<Class<?>> types = new HashSet<Class<?>>();
        try {
            Class<?> outParam = ParameterTypeHelper.loadOutputType(definition, loader);
            types.add(outParam);
            Set<Class<?>> faults = ParameterTypeHelper.loadFaultTypes(definition, loader);
            types.addAll(faults);
        } catch (ClassNotFoundException e) {
            throw new WiringException(e);
        }
        return types.toArray(new Class<?>[types.size()]);
    }

}