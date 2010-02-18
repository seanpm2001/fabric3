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
package org.fabric3.federation.deployment.executor;

import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Init;
import org.osoa.sca.annotations.Reference;

import org.fabric3.api.annotation.Monitor;
import org.fabric3.federation.deployment.cache.DeploymentCache;
import org.fabric3.federation.deployment.command.CommitCommand;
import org.fabric3.federation.deployment.command.CommitCommandResponse;
import org.fabric3.spi.executor.CommandExecutor;
import org.fabric3.spi.executor.CommandExecutorRegistry;
import org.fabric3.spi.executor.ExecutionException;

/**
 * Processes a {@link CommitCommand} by instructing the deployment cache to commit.
 *
 * @version $Rev: 7826 $ $Date: 2009-11-14 13:32:05 +0100 (Sat, 14 Nov 2009) $
 */
@EagerInit
public class CommitCommandExecutor implements CommandExecutor<CommitCommand> {
    private DeploymentCache cache;
    private CommandExecutorRegistry executorRegistry;
    private CommitRollbackMonitor monitor;

    public CommitCommandExecutor(@Reference DeploymentCache cache,
                                 @Reference CommandExecutorRegistry executorRegistry,
                                 @Monitor CommitRollbackMonitor monitor) {
        this.cache = cache;
        this.executorRegistry = executorRegistry;
        this.monitor = monitor;
    }

    @Init
    public void init() {
        executorRegistry.register(CommitCommand.class, this);
    }

    public void execute(CommitCommand command) throws ExecutionException {
        cache.commit();
        monitor.commit();
        CommitCommandResponse response = new CommitCommandResponse();
        command.setResponse(response);
    }
}