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
package org.fabric3.fabric.policy;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * @version $Revision$ $Date$
 */
public class DummyTransactionManager implements TransactionManager {

    public void begin() throws NotSupportedException, SystemException {
        // TODO Auto-generated method stub

    }

    public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException, SecurityException, SystemException {
        // TODO Auto-generated method stub

    }

    public int getStatus() throws SystemException {
        // TODO Auto-generated method stub
        return 0;
    }

    public Transaction getTransaction() throws SystemException {
        // TODO Auto-generated method stub
        return null;
    }

    public void resume(Transaction arg0) throws IllegalStateException, InvalidTransactionException, SystemException {
        // TODO Auto-generated method stub

    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        // TODO Auto-generated method stub

    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        // TODO Auto-generated method stub

    }

    public void setTransactionTimeout(int arg0) throws SystemException {
        // TODO Auto-generated method stub

    }

    public Transaction suspend() throws SystemException {
        // TODO Auto-generated method stub
        return null;
    }

}
