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
package org.fabric3.spi.wire;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;

import org.osoa.sca.CallableReference;

import org.fabric3.spi.ObjectFactory;

/**
 * Creates proxies that implement Java interfaces and invocation handlers for fronting wires
 *
 * @version $Rev$ $Date$
 */

public interface ProxyService {
    /**
     * Create an ObjectFactory that provides proxies for the forward wire.
     *
     * @param interfaze      the interface the proxy implements
     * @param conversational true if conversational
     * @param wire           the wire to proxy @return an ObjectFactory that will create proxies
     * @return the factory
     * @throws ProxyCreationException if there was a problem creating the proxy
     */
    <T> ObjectFactory<T> createObjectFactory(Class<T> interfaze, boolean conversational, Wire wire) throws ProxyCreationException;

    /**
     * Create an ObjectFactory that provides proxies for the callback wire.
     *
     * @param interfaze      the interface the proxy implements
     * @param conversational true if the target callback service is conversational
     * @param targetUri      the callback service uri
     * @param wire           the wire to proxy
     * @param <T>            the type of the proxy
     * @return an ObjectFactory that will create proxies
     * @throws ProxyCreationException if there was a problem creating the proxy
     */
    <T> ObjectFactory<T> createCallbackObjectFactory(Class<T> interfaze, boolean conversational, URI targetUri, Wire wire)
            throws ProxyCreationException;

    /**
     * Creates a Java proxy for the given wire.
     *
     * @param interfaze      the interface the proxy implements
     * @param conversational true if conversational
     * @param wire           the wire to proxy
     * @param mappings       the method to invocation chain mappings
     * @return the proxy
     * @throws ProxyCreationException if there was a problem creating the proxy
     */
    <T> T createProxy(Class<T> interfaze, boolean conversational, Wire wire, Map<Method, InvocationChain> mappings) throws ProxyCreationException;

    /**
     * Creates a Java proxy for the callback invocations chains.
     *
     * @param interfaze      the interface the proxy should implement
     * @param conversational if the target callback service is conversational
     * @param mappings       the invocation chain mappings keyed by target URI @return the proxy
     * @return the proxy instance
     * @throws ProxyCreationException if an error is encountered during proxy generation
     */
    <T> T createCallbackProxy(Class<T> interfaze, boolean conversational, Map<String, Map<Method, InvocationChain>> mappings)
            throws ProxyCreationException;

    /**
     * Cast a proxy to a CallableReference.
     *
     * @param target a proxy generated by this implementation
     * @return a CallableReference (or subclass) equivalent to this prozy
     * @throws IllegalArgumentException if the object supplied is not a proxy
     */
    <B, R extends CallableReference<B>> R cast(B target) throws IllegalArgumentException;

}
