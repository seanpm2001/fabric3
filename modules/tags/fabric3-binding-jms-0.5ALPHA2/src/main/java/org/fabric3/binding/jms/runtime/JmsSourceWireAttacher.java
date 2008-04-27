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
package org.fabric3.binding.jms.runtime;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.fabric3.binding.jms.common.ConnectionFactoryDefinition;
import org.fabric3.binding.jms.common.CorrelationScheme;
import org.fabric3.binding.jms.common.CreateOption;
import org.fabric3.binding.jms.common.DestinationDefinition;
import org.fabric3.binding.jms.common.JmsBindingMetadata;
import org.fabric3.binding.jms.common.TransactionType;
import org.fabric3.binding.jms.provision.JmsWireSourceDefinition;
import org.fabric3.binding.jms.runtime.host.JmsHost;
import org.fabric3.binding.jms.runtime.lookup.connectionfactory.ConnectionFactoryStrategy;
import org.fabric3.binding.jms.runtime.lookup.destination.DestinationStrategy;
import org.fabric3.binding.jms.runtime.tx.TransactionHandler;
import org.fabric3.spi.ObjectFactory;
import org.fabric3.spi.builder.WiringException;
import org.fabric3.spi.builder.component.SourceWireAttacher;
import org.fabric3.spi.model.physical.PhysicalOperationDefinition;
import org.fabric3.spi.model.physical.PhysicalWireTargetDefinition;
import org.fabric3.spi.services.classloading.ClassLoaderRegistry;
import org.fabric3.spi.wire.InvocationChain;
import org.fabric3.spi.wire.Wire;

import org.osoa.sca.annotations.Property;
import org.osoa.sca.annotations.Reference;

/**
 * Wire attacher for JMS binding.
 *
 * @version $Revision$ $Date$
 */
public class JmsSourceWireAttacher implements SourceWireAttacher<JmsWireSourceDefinition> {

    // JMS host
    private JmsHost jmsHost;

    // Number of listeners
    private int receiverCount = 3;

    /**
     * Destination strategies.
     */
    private Map<CreateOption, DestinationStrategy> destinationStrategies =
            new HashMap<CreateOption, DestinationStrategy>();

    /**
     * Connection factory strategies.
     */
    private Map<CreateOption, ConnectionFactoryStrategy> connectionFactoryStrategies =
            new HashMap<CreateOption, ConnectionFactoryStrategy>();

    /**
     * Transaction handlers.
     */
    private TransactionHandler transactionHandler;

    /**
     * Classloader registry.
     */
    private ClassLoaderRegistry classLoaderRegistry;

    /**
     * Injects the wire attacher registries.
     */
    public JmsSourceWireAttacher() {
    }

    /**
     * Injects the destination strategies.
     *
     * @param strategies Destination strategies.
     */
    @Reference
    public void setDestinationStrategies(Map<CreateOption, DestinationStrategy> strategies) {
        this.destinationStrategies = strategies;
    }

    /**
     * Injects the connection factory strategies.
     *
     * @param strategies Connection factory strategies.
     */
    @Reference
    public void setConnectionFactoryStrategies(Map<CreateOption, ConnectionFactoryStrategy> strategies) {
        this.connectionFactoryStrategies = strategies;
    }

    /**
     * Injects the classloader registry.
     *
     * @param classLoaderRegistry Classloader registry.
     */
    @Reference
    public void setClassloaderRegistry(ClassLoaderRegistry classLoaderRegistry) {
        this.classLoaderRegistry = classLoaderRegistry;
    }

    /**
     * Injects the transaction handler.
     *
     * @param transactionHandler Transaction handler.
     */
    @Reference
    public void setTransactionHandler(TransactionHandler transactionHandler) {
        this.transactionHandler = transactionHandler;
    }

    /**
     * Injected JMS host.
     *
     * @param jmsHost JMS Host to use.
     */
    @Reference(required = true)
    public void setJmsHost(JmsHost jmsHost) {
        this.jmsHost = jmsHost;
    }

    /**
     * Configurable property for receiver count.
     *
     * @param receiverCount Receiver count.
     */
    @Property
    public void setReceiverCount(int receiverCount) {
        this.receiverCount = receiverCount;
    }

    public void attachToSource(JmsWireSourceDefinition sourceDefinition,
                               PhysicalWireTargetDefinition targetDefinition,
                               Wire wire) throws WiringException {

        ClassLoader cl = classLoaderRegistry.getClassLoader(sourceDefinition.getClassloaderURI());

        Map<String, Map.Entry<PhysicalOperationDefinition, InvocationChain>> ops =
                new HashMap<String, Map.Entry<PhysicalOperationDefinition, InvocationChain>>();

        for (Map.Entry<PhysicalOperationDefinition, InvocationChain> entry : wire.getInvocationChains().entrySet()) {
            ops.put(entry.getKey().getName(), entry);
        }

        JmsBindingMetadata metadata = sourceDefinition.getMetadata();
        Hashtable<String, String> env = metadata.getEnv();
        CorrelationScheme correlationScheme = metadata.getCorrelationScheme();
        TransactionType transactionType = sourceDefinition.getTransactionType();

        JMSObjectFactory requestJMSObjectFactory = buildGetObjectFactory(metadata.getConnectionFactory(), metadata.getDestination(), env);

        JMSObjectFactory responseJMSObjectFactory =
                buildGetObjectFactory(metadata.getResponseConnectionFactory(), metadata.getResponseDestination(), env);
        String callbackUri = null;
        if (targetDefinition.getCallbackUri() != null) {
            callbackUri = targetDefinition.getCallbackUri().toString();
        }
        ResponseMessageListener messageListener =
                new ResponseMessageListenerImpl(ops, correlationScheme, transactionHandler, transactionType, callbackUri);
        jmsHost.registerResponseListener(requestJMSObjectFactory,
                                         responseJMSObjectFactory,
                                         messageListener,
                                         transactionType,
                                         transactionHandler,
                                         receiverCount,
                                         cl);
    }

    public void attachObjectFactory(JmsWireSourceDefinition source, ObjectFactory<?> objectFactory) throws WiringException {
        throw new AssertionError();
    }

    /**
     * Build a JMS ObjectFactory from definition.
     */
    private JMSObjectFactory buildGetObjectFactory(ConnectionFactoryDefinition connectionFactoryDefinition,
                                                   DestinationDefinition destinationDefinition,
                                                   Hashtable<String, String> env) {
        CreateOption create = connectionFactoryDefinition.getCreate();

        ConnectionFactory connectionFactory =
                connectionFactoryStrategies.get(create).getConnectionFactory(connectionFactoryDefinition, env);
        create = destinationDefinition.getCreate();
        Destination reqDestination =
                destinationStrategies.get(create).getDestination(destinationDefinition, connectionFactory, env);
        return new JMSObjectFactory(connectionFactory, reqDestination, 1);
    }

}
