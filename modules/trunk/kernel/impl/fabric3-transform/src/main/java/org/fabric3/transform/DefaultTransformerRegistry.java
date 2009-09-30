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
package org.fabric3.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osoa.sca.annotations.Reference;

import org.fabric3.model.type.contract.DataType;
import org.fabric3.spi.transform.SingleTypeTransformer;
import org.fabric3.spi.transform.TransformerFactory;
import org.fabric3.spi.transform.TransformerRegistry;
import org.fabric3.spi.transform.TransformationException;
import org.fabric3.spi.transform.Transformer;

/**
 * Default TransformerRegistry implementation.
 *
 * @version $Rev$ $Date$
 */
public class DefaultTransformerRegistry implements TransformerRegistry {
    // cache of single type transformers
    private Map<Key, SingleTypeTransformer<?, ?>> transformers = new HashMap<Key, SingleTypeTransformer<?, ?>>();

    // cache of transformer factories
    private List<TransformerFactory<?, ?>> factories = new ArrayList<TransformerFactory<?, ?>>();

    @Reference(required = false)
    public void setTransformers(List<SingleTypeTransformer<?, ?>> transformers) {
        for (SingleTypeTransformer<?, ?> transformer : transformers) {
            Key pair = new Key(transformer.getSourceType(), transformer.getTargetType());
            this.transformers.put(pair, transformer);
        }
    }

    @Reference(required = false)
    public void setFactories(List<TransformerFactory<?, ?>> factories) {
        this.factories = factories;
    }

    public Transformer<?, ?> getTransformer(DataType<?> source, DataType<?> target, Class<?>... classes) throws TransformationException {
        Key key = new Key(source, target);
        Transformer<?, ?> transformer = transformers.get(key);
        if (transformer != null) {
            return transformer;
        }

        for (TransformerFactory<?, ?> factory : factories) {
            if (factory.canTransform(source, target)) {
                return factory.create(classes);
            }
        }

        return null;
    }

    private static class Key {
        private final DataType<?> source;
        private final DataType<?> target;

        public Key(DataType<?> source, DataType<?> target) {
            this.source = source;
            this.target = target;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key that = (Key) o;

            return source.equals(that.source) && target.equals(that.target);

        }

        public int hashCode() {
            int result;
            result = source.hashCode();
            result = 31 * result + target.hashCode();
            return result;
        }
    }

}