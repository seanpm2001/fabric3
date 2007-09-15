/*
 * See the NOTICE file distributed with this work for information
 * regarding copyright ownership.  This file is licensed
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
package loanapp.loanservice;

import org.osoa.sca.annotations.Reference;

import loanapp.message.LoanRequest;
import loanapp.message.LoanResult;

/**
 * @version $Rev$ $Date$
 */
public class LoanServiceClient implements LoanApplicationService {
    private LoanApplicationService service;

    public LoanServiceClient(@Reference(name = "loanService")LoanApplicationService service) {
        this.service = service;
    }

    public LoanResult apply(LoanRequest request) {
        return service.apply(request);
    }
}
