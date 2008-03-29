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
package org.fabric3.jpa.control;

import java.net.URI;

import org.fabric3.jpa.provision.PersistenceUnitWireTargetDefinition;
import org.fabric3.jpa.scdl.PersistenceUnitResource;
import org.fabric3.spi.generator.GenerationException;
import org.fabric3.spi.generator.GeneratorRegistry;
import org.fabric3.spi.generator.ResourceWireGenerator;
import org.fabric3.spi.model.instance.LogicalResource;
import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Init;
import org.osoa.sca.annotations.Reference;

/**
 *
 * @version $Revision$ $Date$
 */
@EagerInit
public class PersistenceUnitResourceWireGenerator implements ResourceWireGenerator<PersistenceUnitWireTargetDefinition, PersistenceUnitResource> {

    private GeneratorRegistry registry;

    
    /**
     * @param registry Injected registry.
     */
    @Reference
    public void setRegistry(@Reference GeneratorRegistry registry) {
        this.registry = registry;
    }
    
    /**
     * Registers with the registry.
     */
    @Init
    public void start() {
        registry.register(PersistenceUnitResource.class, this);
    }

    /**
     * @see org.fabric3.spi.generator.ResourceWireGenerator#genearteWireTargetDefinition(org.fabric3.spi.model.instance.LogicalResource)
     */
    public PersistenceUnitWireTargetDefinition generateWireTargetDefinition(LogicalResource<PersistenceUnitResource> logicalResource) 
        throws GenerationException {
        
        URI classLoaderUri = logicalResource.getParent().getParent().getUri();
            
        PersistenceUnitWireTargetDefinition pwtd = new PersistenceUnitWireTargetDefinition();
        pwtd.setOptimizable(false);
        pwtd.setUnitName(logicalResource.getResourceDefinition().getUnitName());
        pwtd.setClassLoaderUri(classLoaderUri);
            
        return pwtd;
        
    }

}
