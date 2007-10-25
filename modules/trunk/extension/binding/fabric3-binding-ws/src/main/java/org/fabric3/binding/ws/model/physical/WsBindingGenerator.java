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
package org.fabric3.binding.ws.model.physical;

import java.util.Map;
import java.util.Set;

import org.fabric3.binding.ws.model.logical.WsBindingDefinition;
import org.fabric3.extension.generator.BindingGeneratorExtension;
import org.fabric3.scdl.ReferenceDefinition;
import org.fabric3.scdl.ServiceDefinition;
import org.fabric3.scdl.definitions.Intent;
import org.fabric3.spi.generator.BindingGeneratorDelegate;
import org.fabric3.spi.generator.GenerationException;
import org.fabric3.spi.generator.GeneratorContext;
import org.fabric3.spi.model.instance.LogicalBinding;
import org.fabric3.spi.model.physical.PhysicalWireSourceDefinition;
import org.fabric3.spi.model.physical.PhysicalWireTargetDefinition;
import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Reference;

/**
 * Implementation of the WS binding generator.
 *
 * @version $Revision$ $Date$
 */
@EagerInit
public class WsBindingGenerator extends BindingGeneratorExtension<PhysicalWireSourceDefinition, PhysicalWireTargetDefinition, WsBindingDefinition> {
    
    private Map<String, BindingGeneratorDelegate<WsBindingDefinition>> delegates;
    
    @Reference
    public void setDelegates(Map<String, BindingGeneratorDelegate<WsBindingDefinition>> delegates) {
        this.delegates = delegates;
    }
    
    public PhysicalWireSourceDefinition generateWireSource(LogicalBinding<WsBindingDefinition> logicalBinding,
                                                     Set<Intent> intentsToBeProvided,
                                                     GeneratorContext generatorContext,
                                                     ServiceDefinition serviceDefinition) throws GenerationException {
        
        String implementation = logicalBinding.getBinding().getImplementation();
        BindingGeneratorDelegate<WsBindingDefinition> delegate = delegates.get(implementation);
        
        return delegate.generateWireSource(logicalBinding, intentsToBeProvided, generatorContext, serviceDefinition);
    
    }

    public PhysicalWireTargetDefinition generateWireTarget(LogicalBinding<WsBindingDefinition> logicalBinding,
                                                     Set<Intent> intentsToBeProvided,
                                                     GeneratorContext generatorContext,
                                                     ReferenceDefinition referenceDefinition)
            throws GenerationException {
        
        String implementation = logicalBinding.getBinding().getImplementation();
        BindingGeneratorDelegate<WsBindingDefinition> delegate = delegates.get(implementation);
        
        return delegate.generateWireTarget(logicalBinding, intentsToBeProvided, generatorContext, referenceDefinition);
    
    }

    /**
     * @see org.fabric3.extension.generator.BindingGeneratorExtension#getBindingDefinitionClass()
     */
    @Override
    protected Class<WsBindingDefinition> getBindingDefinitionClass() {
        return WsBindingDefinition.class;
    }

}
