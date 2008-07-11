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
package org.fabric3.binding.jms.control;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;

import static org.osoa.sca.Constants.SCA_NS;
import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Reference;

import org.fabric3.binding.jms.common.JmsBindingMetadata;
import org.fabric3.binding.jms.common.TransactionType;
import org.fabric3.binding.jms.provision.JmsWireSourceDefinition;
import org.fabric3.binding.jms.provision.JmsWireTargetDefinition;
import org.fabric3.binding.jms.provision.MessageType;
import org.fabric3.binding.jms.scdl.JmsBindingDefinition;
import org.fabric3.scdl.Operation;
import org.fabric3.scdl.ReferenceDefinition;
import org.fabric3.scdl.ServiceContract;
import org.fabric3.scdl.ServiceDefinition;
import org.fabric3.scdl.definitions.Intent;
import org.fabric3.spi.generator.BindingGenerator;
import org.fabric3.spi.generator.GenerationException;
import org.fabric3.spi.model.instance.LogicalBinding;
import org.fabric3.spi.policy.Policy;

/**
 * Binding generator that creates the physical source and target definitions for wires. Message acknowledgement is always expected to be using
 * transactions, either local or global, as expressed by the intents transactedOneWay, transactedOneWay.local or transactedOneWay.global.
 *
 * @version $Revision$ $Date$
 */
@EagerInit
public class JmsBindingGenerator implements BindingGenerator<JmsWireSourceDefinition, JmsWireTargetDefinition, JmsBindingDefinition> {

    // Transacted one way intent
    private static final QName TRANSACTED_ONEWAY = new QName(SCA_NS, "transactedOneWay");
    private static final QName TRANSACTED_ONEWAY_LOCAL = new QName(SCA_NS, "transactedOneWay.local");
    private static final QName TRANSACTED_ONEWAY_GLOBAL = new QName(SCA_NS, "transactedOneWay.global");
    private static final QName ONEWAY = new QName(SCA_NS, "oneWay");

    private MessageTypeIntrospector introspector;

    public JmsBindingGenerator(@Reference MessageTypeIntrospector introspector) {
        this.introspector = introspector;
    }

    public JmsWireSourceDefinition generateWireSource(LogicalBinding<JmsBindingDefinition> logicalBinding,
                                                      Policy policy,
                                                      ServiceDefinition serviceDefinition) throws GenerationException {

        ServiceContract<?> serviceContract = serviceDefinition.getServiceContract();
        TransactionType transactionType = getTransactionType(policy, serviceContract);
        Set<String> oneWayOperations = getOneWayOperations(policy, serviceContract);

        URI classloaderId = logicalBinding.getParent().getParent().getClassLoaderId();

        JmsBindingMetadata metadata = logicalBinding.getBinding().getMetadata();
        Map<String, MessageType> messageTypes = processMessageTypes(serviceContract);
        URI uri = logicalBinding.getBinding().getTargetUri();
        return new JmsWireSourceDefinition(uri, metadata, messageTypes, transactionType, oneWayOperations, classloaderId);
    }

    public JmsWireTargetDefinition generateWireTarget(LogicalBinding<JmsBindingDefinition> logicalBinding,
                                                      Policy policy,
                                                      ReferenceDefinition referenceDefinition) throws GenerationException {

        ServiceContract<?> serviceContract = referenceDefinition.getServiceContract();

        TransactionType transactionType = getTransactionType(policy, serviceContract);
        Set<String> oneWayOperations = getOneWayOperations(policy, serviceContract);

        URI classloaderId = logicalBinding.getParent().getParent().getClassLoaderId();

        URI uri = logicalBinding.getBinding().getTargetUri();
        JmsBindingMetadata metadata = logicalBinding.getBinding().getMetadata();
        Map<String, MessageType> messageTypes = processMessageTypes(serviceContract);
        return new JmsWireTargetDefinition(uri, metadata, messageTypes, transactionType, oneWayOperations, classloaderId);
    }

    /*
     * Gets the transaction type.
     */
    private TransactionType getTransactionType(Policy policy, ServiceContract<?> serviceContract) {

        // If any operation has the intent, return that
        for (Operation<?> operation : serviceContract.getOperations()) {
            for (Intent intent : policy.getProvidedIntents(operation)) {
                if (TRANSACTED_ONEWAY_GLOBAL.equals(intent.getName())) {
                    return TransactionType.GLOBAL;
                } else if (TRANSACTED_ONEWAY_LOCAL.equals(intent.getName())) {
                    return TransactionType.LOCAL;
                } else if (TRANSACTED_ONEWAY.equals(intent.getName())) {
                    return TransactionType.GLOBAL;
                }
            }
        }
        //no transaction policy specified, use local
        return TransactionType.LOCAL;

    }

    /*
     * Gets one way method names.
     */
    private Set<String> getOneWayOperations(Policy policy, ServiceContract<?> serviceContract) {
        Set<String> result = null;
        // If any operation has the intent, return that
        for (Operation<?> operation : serviceContract.getOperations()) {
            for (Intent intent : policy.getProvidedIntents(operation)) {
                if (ONEWAY.equals(intent.getName())) {
                    if (result == null) {
                        result = new HashSet<String>();
                    }
                    result.add(operation.getName());
                    break;
                }
            }
        }
        if (result != null) {
            return result;
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Determines the the message type to use based on the service contract.
     *
     * @param serviceContract the service contract
     * @return the collection of message types keyed by operation name
     * @throws JmsGenerationException if an error occurs
     */
    private Map<String, MessageType> processMessageTypes(ServiceContract<?> serviceContract) throws JmsGenerationException {
        Map<String, MessageType> types = new HashMap<String, MessageType>();
        for (Operation<?> operation : serviceContract.getOperations()) {
            MessageType messageType = introspector.introspect(operation);
            types.put(operation.getName(), messageType);
        }
        return types;
    }
}
