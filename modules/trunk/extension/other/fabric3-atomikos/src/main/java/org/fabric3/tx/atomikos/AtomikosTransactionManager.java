/*
 * Fabric3
 * Copyright (c) 2009 Metaform Systems
 *
 * Fabric3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version, with the
 * following exception:
 *
 * Linking this software statically or dynamically with other
 * modules is making a combined work based on this software.
 * Thus, the terms and conditions of the GNU General Public
 * License cover the whole combination.
 *
 * As a special exception, the copyright holders of this software
 * give you permission to link this software with independent
 * modules to produce an executable, regardless of the license
 * terms of these independent modules, and to copy and distribute
 * the resulting executable under terms of your choice, provided
 * that you also meet, for each linked independent module, the
 * terms and conditions of the license of that module. An
 * independent module is a module which is not derived from or
 * based on this software. If you modify this software, you may
 * extend this exception to your version of the software, but
 * you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version.
 *
 * Fabric3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the
 * GNU General Public License along with Fabric3.
 * If not, see <http://www.gnu.org/licenses/>.
*/

package org.fabric3.tx.atomikos;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import com.atomikos.icatch.jta.UserTransactionManager;
import org.osoa.sca.annotations.Destroy;
import org.osoa.sca.annotations.Init;
import org.osoa.sca.annotations.Service;

/**
 * Wraps an Atomikos transaction manager. The transaction manager will startup and perform recovery on first use. Configured JDBC and JMS resource
 * registration is handled implicity by Atomikos.
 *
 * @version $Rev$ $Date$
 */
@Service(javax.transaction.TransactionManager.class)
public class AtomikosTransactionManager implements TransactionManager {
    private UserTransactionManager tm;

    @Init
    public void init() {
        tm = new UserTransactionManager();
    }

    @Destroy
    public void destroy() {
        tm.close();
    }

    public void begin() throws NotSupportedException, SystemException {
        tm.begin();
    }

    public void commit()
            throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        tm.commit();
    }

    public int getStatus() throws SystemException {
        return tm.getStatus();
    }

    public Transaction getTransaction() throws SystemException {
        return tm.getTransaction();
    }

    public void resume(Transaction trx) throws InvalidTransactionException, IllegalStateException, SystemException {
        tm.resume(trx);
    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        tm.rollback();
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        tm.setRollbackOnly();
    }

    public void setTransactionTimeout(int seconds) throws SystemException {
        tm.setTransactionTimeout(seconds);
    }

    public Transaction suspend() throws SystemException {
        return tm.suspend();
    }
}