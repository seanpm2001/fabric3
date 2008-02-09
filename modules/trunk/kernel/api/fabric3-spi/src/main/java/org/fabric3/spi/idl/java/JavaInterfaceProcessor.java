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
package org.fabric3.spi.idl.java;

import java.lang.reflect.Method;

import org.fabric3.scdl.Operation;
import org.fabric3.spi.idl.InvalidServiceContractException;

/**
 * Implementations introspect metadata on a Java interface, populating the corresponding {@link JavaServiceContract}
 *
 * @version $Rev$ $Date$
 */
public interface JavaInterfaceProcessor {

    void visitInterface(Class<?> clazz, JavaServiceContract contract) throws InvalidServiceContractException;

    void visitOperation(Method mmethod, Operation operation) throws InvalidServiceContractException;

}
