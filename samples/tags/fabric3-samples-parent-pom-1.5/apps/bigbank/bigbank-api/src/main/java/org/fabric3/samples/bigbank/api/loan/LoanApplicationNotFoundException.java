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
package org.fabric3.samples.bigbank.api.loan;

import org.fabric3.samples.bigbank.api.loan.LoanException;

/**
 * Raised when an attempt is made to access a non-existant loan application.
 *
 * @version $Revision$ $Date$
 */
public class LoanApplicationNotFoundException extends LoanException {
    private static final long serialVersionUID = -3162753331156928115L;

    public LoanApplicationNotFoundException(String message) {
        super(message);
    }
}