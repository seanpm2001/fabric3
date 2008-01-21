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
package org.fabric3.runtime.webapp.implementation.webapp;

import javax.xml.namespace.QName;

import org.fabric3.scdl.ComponentType;
import org.fabric3.scdl.Implementation;
import org.fabric3.spi.Constants;

/**
 * @version $Rev$ $Date$
 */
public class WebappImplementation extends Implementation<ComponentType> {
    public static final QName IMPLEMENTATION_WEBAPP = new QName(Constants.FABRIC3_NS, "web");

    public QName getType() {
        return IMPLEMENTATION_WEBAPP;
    }
}
