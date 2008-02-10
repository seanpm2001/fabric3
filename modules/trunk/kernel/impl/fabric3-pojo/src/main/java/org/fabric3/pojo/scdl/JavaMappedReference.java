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

import org.fabric3.scdl.ReferenceDefinition;
import org.fabric3.scdl.ServiceContract;
import org.fabric3.scdl.MemberSite;

/**
 * A ReferenceDefinition definition that is mapped to a specific location in the implementation class. This location
 * will typically be used to inject reference values.
 *
 * @version $Rev$ $Date$
 */
public class JavaMappedReference extends ReferenceDefinition {
    private MemberSite memberSite;

    public JavaMappedReference(String name, ServiceContract serviceContract, MemberSite memberSite) {
        super(name, serviceContract);
        this.memberSite = memberSite;
    }

    /**
     * Returns the memberSiteSite that this reference is mapped to.
     *
     * @return the memberSiteSite that this reference is mapped to
     */
    public MemberSite getMemberSite() {
        return memberSite;
    }

}
