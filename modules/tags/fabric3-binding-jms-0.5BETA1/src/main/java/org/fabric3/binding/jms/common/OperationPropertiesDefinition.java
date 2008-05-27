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
package org.fabric3.binding.jms.common;

/**
 * ModelObject represents binding.jms\operationProperties.
 */
public class OperationPropertiesDefinition extends PropertyAwareObject {
    /**
     *
     */
    private static final long serialVersionUID = -1325680761205311178L;
    private String name;
    private String nativeOperation;
    private HeadersDefinition header;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNativeOperation() {
        return nativeOperation;
    }

    public void setNativeOperation(String nativeOperation) {
        this.nativeOperation = nativeOperation;
    }

    public HeadersDefinition getHeaders() {
        return header;
    }

    public void setHeaders(HeadersDefinition header) {
        this.header = header;
    }

    public OperationPropertiesDefinition cloneOperationPropertiesDefinition() {
        OperationPropertiesDefinition clone = new OperationPropertiesDefinition();
        clone.setName(this.name);
        clone.setNativeOperation(this.nativeOperation);
        if(this.header!=null){
            clone.setHeaders(header.cloneHeadersDefinition());
        }
        return clone;
    }
}
