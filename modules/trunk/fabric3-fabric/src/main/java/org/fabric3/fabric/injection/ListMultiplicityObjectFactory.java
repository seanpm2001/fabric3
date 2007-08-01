/*
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
package org.fabric3.fabric.injection;

import java.util.LinkedList;
import java.util.List;

import org.fabric3.spi.ObjectCreationException;
import org.fabric3.spi.ObjectFactory;

/**
 * Resolves targets configured in a multiplicity by delegating to object factories and returning an <code>List</code>
 * containing object instances
 *
 * @version $Rev$ $Date$
 */
public class ListMultiplicityObjectFactory<T> implements ObjectFactory<List<T>> {

    // Object factories
    private List<ObjectFactory<T>> factories = new LinkedList<ObjectFactory<T>>();
    
    /**
     * Adds an object factory.
     * @param objectFactory Object factory to be used.
     */
    public void addObjectFactory(ObjectFactory<T> objectFactory) {
        factories.add(objectFactory);
    }

    /**
     * @see org.fabric3.spi.ObjectFactory#getInstance()
     */
    public List<T> getInstance() throws ObjectCreationException {
        List<T> list = new LinkedList<T>();
        for (ObjectFactory<T> factory : factories) {
            list.add(factory.getInstance());
        }
        return list;
    }

}
