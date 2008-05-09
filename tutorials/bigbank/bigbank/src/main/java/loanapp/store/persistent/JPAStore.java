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
package loanapp.store.persistent;

import loanapp.message.LoanApplication;
import loanapp.store.StoreException;
import loanapp.store.StoreService;
import loanapp.store.ApplicationNotFoundException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Demonstrates using JPA persistence. By default, the persistence context is transaction-scoped. As this component
 * implementation requires managed transactions, operations will be invoked in the context of a transaction resulting
 * in persistence context changes being written to the database when the transaction completes.
 *
 * @version $Revision$ $Date$
 */
public class JPAStore implements StoreService {
    private EntityManager em;

    @PersistenceContext(name = "loanApplicationEmf", unitName = "loanApplication")
    public void setEm(EntityManager em) {
        this.em = em;
    }

    public void save(LoanApplication application) throws StoreException {
        em.persist(application);
    }

    public void update(LoanApplication application) throws StoreException {
        em.merge(application);
    }

    public void remove(long id) throws StoreException {
        LoanApplication application = em.find(LoanApplication.class, id);
        if (application == null) {
            throw new ApplicationNotFoundException("Loan application not found: " + id);
        }
        em.remove(application);
    }

    public LoanApplication find(long id) throws StoreException {
        return em.find(LoanApplication.class, id);
    }
}
