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
package org.fabric3.web.provision;

import java.lang.annotation.ElementType;

import org.fabric3.scdl.InjectionSite;

/**
 * An injection site specialized for web applications.
 *
 * @version $Revision$ $Date$
 */
public class WebContextInjectionSite extends InjectionSite {
    private ContextType contextType;

    public static enum ContextType {
        SERVLET_CONTEXT,
        SESSION_CONTEXT
    }

    public WebContextInjectionSite(String type, ContextType contextType) {
        super(ElementType.PARAMETER, type);
        this.contextType = contextType;
    }

    public ContextType getContextType() {
        return contextType;
    }


}
