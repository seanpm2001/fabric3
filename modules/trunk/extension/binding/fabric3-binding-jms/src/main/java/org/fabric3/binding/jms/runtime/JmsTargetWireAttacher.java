/*
 * Fabric3
 * Copyright � 2008 Metaform Systems Limited
 *
 * This proprietary software may be used only connection with the Fabric3 license
 * (the �License�), a copy of which is included in the software or may be
 * obtained at: http://www.metaformsystems.com/licenses/license.html.

 * Software distributed under the License is distributed on an �as is� basis,
 * without warranties or conditions of any kind.  See the License for the
 * specific language governing permissions and limitations of use of the software.
 * This software is distributed in conjunction with other software licensed under
 * different terms.  See the separate licenses for those programs included in the
 * distribution for the permitted and restricted uses of such software.
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
package org.fabric3.binding.jms.runtime;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.osoa.sca.annotations.Reference;

import org.fabric3.binding.jms.common.ConnectionFactoryDefinition;
import org.fabric3.binding.jms.common.CorrelationScheme;
import org.fabric3.binding.jms.common.DestinationDefinition;
import org.fabric3.binding.jms.common.JmsBindingMetadata;
import org.fabric3.binding.jms.provision.JmsWireTargetDefinition;
import org.fabric3.binding.jms.provision.PayloadType;
import org.fabric3.binding.jms.runtime.lookup.AdministeredObjectResolver;
import org.fabric3.binding.jms.runtime.lookup.JmsLookupException;
import org.fabric3.spi.ObjectFactory;
import org.fabric3.spi.binding.format.EncoderException;
import org.fabric3.spi.binding.format.MessageEncoder;
import org.fabric3.spi.binding.format.ParameterEncoder;
import org.fabric3.spi.binding.format.ParameterEncoderFactory;
import org.fabric3.spi.builder.WiringException;
import org.fabric3.spi.builder.component.TargetWireAttacher;
import org.fabric3.spi.classloader.ClassLoaderRegistry;
import org.fabric3.spi.model.physical.PhysicalOperationDefinition;
import org.fabric3.spi.model.physical.PhysicalWireSourceDefinition;
import org.fabric3.spi.wire.Interceptor;
import org.fabric3.spi.wire.InvocationChain;
import org.fabric3.spi.wire.Wire;

/**
 * Attaches the reference end of a wire to a JMS queue.
 *
 * @version $Revision$ $Date$
 */
public class JmsTargetWireAttacher implements TargetWireAttacher<JmsWireTargetDefinition> {
    private AdministeredObjectResolver resolver;
    private ClassLoaderRegistry classLoaderRegistry;
    private Map<String, ParameterEncoderFactory> parameterEncoderFactories = new HashMap<String, ParameterEncoderFactory>();
    private Map<String, MessageEncoder> messageFormatters = new HashMap<String, MessageEncoder>();


    public JmsTargetWireAttacher(@Reference AdministeredObjectResolver resolver, @Reference ClassLoaderRegistry classLoaderRegistry) {
        this.resolver = resolver;
        this.classLoaderRegistry = classLoaderRegistry;
    }

    @Reference
    public void setParameterEncoderFactories(Map<String, ParameterEncoderFactory> parameterEncoderFactories) {
        this.parameterEncoderFactories = parameterEncoderFactories;
    }

    @Reference
    public void setMessageFormatters(Map<String, MessageEncoder> messageFormatters) {
        this.messageFormatters = messageFormatters;
    }

    public void attachToTarget(PhysicalWireSourceDefinition sourceDefinition, JmsWireTargetDefinition targetDefinition, Wire wire)
            throws WiringException {

        ClassLoader cl = classLoaderRegistry.getClassLoader(targetDefinition.getClassLoaderId());

        JmsBindingMetadata metadata = targetDefinition.getMetadata();

        Hashtable<String, String> env = metadata.getEnv();
        CorrelationScheme correlationScheme = metadata.getCorrelationScheme();

        ConnectionFactoryDefinition connectionFactoryDefinition = metadata.getConnectionFactory();
        Destination responseDestination = null;
        Destination reqDestination;
        ConnectionFactory responseConnectionFactory = null;
        ConnectionFactory reqCf;
        try {
            reqCf = resolver.resolve(connectionFactoryDefinition, env);
            DestinationDefinition destinationDefinition = metadata.getDestination();
            reqDestination = resolver.resolve(destinationDefinition, reqCf, env);

            if (metadata.isResponse()) {
                connectionFactoryDefinition = metadata.getResponseConnectionFactory();
                responseConnectionFactory = resolver.resolve(connectionFactoryDefinition, env);

                destinationDefinition = metadata.getResponseDestination();
                responseDestination = resolver.resolve(destinationDefinition, responseConnectionFactory, env);
            }
        } catch (JmsLookupException e) {
            throw new WiringException(e);
        }
        Map<String, PayloadType> payloadTypes = targetDefinition.getPayloadTypes();

        for (InvocationChain chain : wire.getInvocationChains()) {

            PhysicalOperationDefinition op = chain.getPhysicalOperation();

            String operationName = op.getName();
            PayloadType payloadType = payloadTypes.get(operationName);
            String dataBinding = op.getDatabinding();

            MessageEncoder messageEncoder = null;
            ParameterEncoder parameterEncoder = null;

            if (dataBinding != null) {
                ParameterEncoderFactory factory = parameterEncoderFactories.get(dataBinding);
                if (factory == null) {
                    throw new WiringException("Parameter encoder factory not found for: " + dataBinding);
                }
                try {
                    parameterEncoder = factory.getInstance(wire, cl);
                } catch (EncoderException e) {
                    throw new WiringException(e);
                }
                messageEncoder = messageFormatters.get(dataBinding);
                if (messageEncoder == null) {
                    throw new WiringException("Message encoder not found for: " + dataBinding);
                }
            }

            Interceptor interceptor;
            if (metadata.isResponse()) {
                // setup a request-response interceptor
                JmsResponseMessageListener receiver = new JmsResponseMessageListener(responseDestination, responseConnectionFactory);
                interceptor = new JmsInterceptor(operationName,
                                                 payloadType,
                                                 reqDestination,
                                                 reqCf,
                                                 correlationScheme,
                                                 receiver,
                                                 messageEncoder,
                                                 parameterEncoder,
                                                 cl);
            } else {
                // setup a one-way interceptor
                interceptor = new JmsInterceptor(operationName,
                                                 payloadType,
                                                 reqDestination,
                                                 reqCf,
                                                 correlationScheme,
                                                 messageEncoder,
                                                 parameterEncoder,
                                                 cl);
            }
            chain.addInterceptor(interceptor);
        }

    }

    public void detachFromTarget(PhysicalWireSourceDefinition source, JmsWireTargetDefinition target) throws WiringException {
        // release the connection
        resolver.release(target.getMetadata().getConnectionFactory());
    }

    public ObjectFactory<?> createObjectFactory(JmsWireTargetDefinition target) throws WiringException {
        throw new UnsupportedOperationException();
    }


}