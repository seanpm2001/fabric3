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
 * --- Original Apache License ---
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.fabric3.fabric.classloader;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.fabric3.spi.classloader.MultiParentClassLoader;
import org.fabric3.spi.classloader.ClassLoaderRegistry;
import org.fabric3.spi.classloader.DuplicateClassLoaderException;

/**
 * Implementation of a registry for classloaders.
 *
 * @version $Rev$ $Date$
 */
public class ClassLoaderRegistryImpl implements ClassLoaderRegistry {
    private final Map<URI, ClassLoader> registry = new ConcurrentHashMap<URI, ClassLoader>();
    private static final Map<String, Class<?>> PRIMITIVES;

    static {
        PRIMITIVES = new HashMap<String, Class<?>>();
        PRIMITIVES.put("boolean", Boolean.TYPE);
        PRIMITIVES.put("byte", Byte.TYPE);
        PRIMITIVES.put("short", Short.TYPE);
        PRIMITIVES.put("int", Integer.TYPE);
        PRIMITIVES.put("long", Long.TYPE);
        PRIMITIVES.put("float", Float.TYPE);
        PRIMITIVES.put("double", Double.TYPE);
        PRIMITIVES.put("void", Void.TYPE);
    }

    public synchronized void register(URI id, ClassLoader classLoader) throws DuplicateClassLoaderException {
        if (registry.containsKey(id)) {
            String identifier = id.toString();
            throw new DuplicateClassLoaderException("Duplicate class loader: " + identifier, identifier);
        }
        registry.put(id, classLoader);
    }

    public ClassLoader unregister(URI id) {
        return registry.remove(id);
    }

    public ClassLoader getClassLoader(URI id) {
        return registry.get(id);
    }

    public Map<URI, ClassLoader> getClassLoaders() {
        return registry;
    }


    public Class<?> loadClass(URI classLoaderId, String className) throws ClassNotFoundException {
        ClassLoader cl = getClassLoader(classLoaderId);
        return loadClass(cl, className);
    }

    public Class<?> loadClass(ClassLoader cl, String className) throws ClassNotFoundException {
        Class<?> clazz = PRIMITIVES.get(className);
        if (clazz == null) {
            clazz = Class.forName(className, true, cl);
        }
        return clazz;
    }

    public List<URI> resolveParentUris(ClassLoader cl) {
        if (cl instanceof MultiParentClassLoader) {
            MultiParentClassLoader loader = (MultiParentClassLoader) cl;
            List<ClassLoader> parents = loader.getParents();
            List<URI> uris = new ArrayList<URI>(parents.size());
            for (ClassLoader parent : parents) {
                URI resolved = resolveUri(parent);
                if (resolved != null) {
                    uris.add(resolved);
                }
            }
            return uris;
        } else {
            List<URI> uris = new ArrayList<URI>();
            ClassLoader parent = cl.getParent();
            URI resolved = resolveUri(parent);
            if (resolved != null) {
                uris.add(resolved);
            }
            return uris;
        }
    }

    private URI resolveUri(ClassLoader cl) {
        for (Map.Entry<URI, ClassLoader> entry : registry.entrySet()) {
            if (entry.getValue().equals(cl)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
