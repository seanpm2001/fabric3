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
package org.fabric3.pojo.scdl;

import org.fabric3.scdl.InjectingComponentType;
import org.fabric3.scdl.InjectionSite;

/**
 * A component type specialization for POJO implementations
 *
 * @version $$Rev$$ $$Date$$
 */
public class PojoComponentType extends InjectingComponentType {
    private String implClass;
    private ConstructorDefinition constructorDefinition;
    private InjectionSite conversationIDInjectionSite;

    /**
     * Constructor used only for deserialization
     */
    public PojoComponentType() {
    }

    /**
     * Constructor specifying the java class name for the POJO this is describing.
     *
     * @param implClass the java class for the POJO this is describing
     */
    public PojoComponentType(String implClass) {
        this.implClass = implClass;
    }

    /**
     * Returns the java class name for the POJO this is describing.
     *
     * @return the java class name for the POJO this is describing
     */
    public String getImplClass() {
        return implClass;
    }

    /**
     * Returns the constructor used to instantiate implementation instances.
     *
     * @return the constructor used to instantiate implementation instances
     */
    public ConstructorDefinition getConstructorDefinition() {
        return constructorDefinition;
    }

    /**
     * Sets the constructor used to instantiate implementation instances
     *
     * @param definition the constructor used to instantiate implementation instances
     */
    public void setConstructorDefinition(ConstructorDefinition definition) {
        this.constructorDefinition = definition;
    }

    public InjectionSite getConversationIDMember() {
        return this.conversationIDInjectionSite;
    }

    public void setConversationIDMember(InjectionSite conversationIDInjectionSite) {
        this.conversationIDInjectionSite = conversationIDInjectionSite;
    }

}
