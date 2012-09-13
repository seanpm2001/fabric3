/*
 * Fabric3 Copyright (c) 2009-2012 Metaform Systems
 * 
 * Fabric3 is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version, with the following exception:
 * 
 * Linking this software statically or dynamically with other modules is making
 * a combined work based on this software. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 * 
 * As a special exception, the copyright holders of this software give you
 * permission to link this software with independent modules to produce an
 * executable, regardless of the license terms of these independent modules, and
 * to copy and distribute the resulting executable under terms of your choice,
 * provided that you also meet, for each linked independent module, the terms
 * and conditions of the license of that module. An independent module is a
 * module which is not derived from or based on this software. If you modify
 * this software, you may extend this exception to your version of the software,
 * but you are not obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 * 
 * Fabric3 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Fabric3. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fabric3.binding.zeromq.runtime.message;

import java.net.URI;

import org.fabric3.binding.zeromq.runtime.federation.AddressListener;
import org.fabric3.spi.channel.ChannelConnection;

/**
 * Implementations receive messages on a ZeroMQ SUB socket. Qualities of service such as reliability may be provided by an implementation.
 *
 * @version $Revision: 10212 $ $Date: 2011-03-15 18:20:58 +0100 (Tue, 15 Mar 2011) $
 */
public interface Subscriber extends AddressListener {

    /**
     * Initializes the server and its underlying socket.
     */
    void start();

    /**
     * Disposes the server and any open resources.
     */
    void stop();

    /**
     * Adds a connection to a component consumer for dispatching received messages.
     *
     * @param subscriberId the consumer unique id
     * @param connection   the connection
     */
    void addConnection(URI subscriberId, ChannelConnection connection);

    /**
     * Removes a connection to a component consumer for dispatching received messages.
     *
     * @param subscriberId the consumer unique id
     */
    void removeConnection(URI subscriberId);

    /**
     * True if this subscriber has active consumer connections.
     *
     * @return true if this subscriber has active consumer connections
     */
    boolean hasConnections();
}
