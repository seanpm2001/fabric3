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
package org.fabric3.model.type.component;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import org.fabric3.model.type.AbstractPolicyAware;
import org.fabric3.model.type.CapabilityAware;

/**
 * The base representation of a binding specified in an assembly
 *
 * @version $Rev: 5650 $ $Date: 2008-10-12 09:43:10 -0700 (Sun, 12 Oct 2008) $
 */
public abstract class BindingDefinition extends AbstractPolicyAware implements CapabilityAware {
    private static final long serialVersionUID = 8780407747984243865L;

    private URI targetUri;
    private QName type;
    private Document key;
    private Set<String> requiredCapabilities = new HashSet<String>();

    public BindingDefinition(URI targetUri, QName type, Document key) {
        this.targetUri = targetUri;
        this.type = type;
        this.key = key;
    }

    public URI getTargetUri() {
        return targetUri;
    }

    public QName getType() {
        return type;
    }

    public Document getKey() {
        return key;
    }

    public Set<String> getRequiredCapabilities() {
        return requiredCapabilities;
    }

    public void addRequiredCapability(String capability) {
        requiredCapabilities.add(capability);
    }

}
