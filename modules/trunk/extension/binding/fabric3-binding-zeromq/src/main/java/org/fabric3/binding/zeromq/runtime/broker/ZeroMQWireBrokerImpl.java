/*
 * Fabric3 Copyright (c) 2009-2011 Metaform Systems
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
package org.fabric3.binding.zeromq.runtime.broker;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.osoa.sca.annotations.Reference;
import org.zeromq.ZMQ;

import org.fabric3.api.annotation.monitor.Monitor;
import org.fabric3.binding.zeromq.runtime.BrokerException;
import org.fabric3.binding.zeromq.runtime.SocketAddress;
import org.fabric3.binding.zeromq.runtime.ZeroMQWireBroker;
import org.fabric3.binding.zeromq.runtime.context.ContextManager;
import org.fabric3.binding.zeromq.runtime.federation.AddressAnnouncement;
import org.fabric3.binding.zeromq.runtime.federation.AddressCache;
import org.fabric3.binding.zeromq.runtime.interceptor.ServiceMarshallingInterceptor;
import org.fabric3.binding.zeromq.runtime.interceptor.RequestReplyInterceptor;
import org.fabric3.binding.zeromq.runtime.interceptor.ReferenceMarshallingInterceptor;
import org.fabric3.binding.zeromq.runtime.message.MessagingMonitor;
import org.fabric3.binding.zeromq.runtime.message.NonReliableRequestReplyReceiver;
import org.fabric3.binding.zeromq.runtime.message.NonReliableRequestReplySender;
import org.fabric3.binding.zeromq.runtime.message.Receiver;
import org.fabric3.binding.zeromq.runtime.message.RequestReplySender;
import org.fabric3.host.runtime.HostInfo;
import org.fabric3.spi.host.PortAllocationException;
import org.fabric3.spi.host.PortAllocator;
import org.fabric3.spi.wire.InvocationChain;

/**
 * @version $Revision: 10212 $ $Date: 2011-03-15 18:20:58 +0100 (Tue, 15 Mar 2011) $
 */
public class ZeroMQWireBrokerImpl implements ZeroMQWireBroker {
    private static final String ZMQ = "zmq";

    private ContextManager manager;
    private AddressCache addressCache;
    private ExecutorService executorService;
    private PortAllocator allocator;
    private HostInfo info;
    private MessagingMonitor monitor;

    private Map<URI, SenderHolder<RequestReplySender>> senders = new HashMap<URI, SenderHolder<RequestReplySender>>();
    private Map<URI, Receiver> receivers = new HashMap<URI, Receiver>();

    public ZeroMQWireBrokerImpl(@Reference ContextManager manager,
                                @Reference AddressCache addressCache,
                                @Reference ExecutorService executorService,
                                @Reference PortAllocator allocator,
                                @Reference HostInfo info,
                                @Monitor MessagingMonitor monitor) {
        this.manager = manager;
        this.addressCache = addressCache;
        this.executorService = executorService;
        this.allocator = allocator;
        this.info = info;
        this.monitor = monitor;
    }

    public void connectToSender(String id, URI uri, List<InvocationChain> chains, ClassLoader loader) throws BrokerException {
        SenderHolder<RequestReplySender> holder = senders.get(uri);
        if (holder == null) {
            ZMQ.Context context = manager.getContext();
            String endpointId = uri.toString();
            List<SocketAddress> addresses = addressCache.getActiveAddresses(endpointId);

            RequestReplySender sender = new NonReliableRequestReplySender(endpointId, context, addresses, monitor);
            holder = new SenderHolder<RequestReplySender>(sender);
            sender.start();

            addressCache.subscribe(endpointId, sender);
            
            senders.put(uri, holder);
        }
        for (int i = 0, chainsSize = chains.size(); i < chainsSize; i++) {
            InvocationChain chain = chains.get(i);
            ReferenceMarshallingInterceptor serializingInterceptor = new ReferenceMarshallingInterceptor(loader);
            chain.addInterceptor(serializingInterceptor);
            RequestReplyInterceptor interceptor = new RequestReplyInterceptor(i, holder.getSender());
            chain.addInterceptor(interceptor);
        }
        holder.getIds().add(id);
    }

    public void releaseSender(String id, URI uri) throws BrokerException {
        SenderHolder<RequestReplySender> holder = senders.get(uri);
        if (holder == null) {
            throw new BrokerException("Sender not found for " + uri);
        }
        holder.getIds().remove(id);
        if (holder.getIds().isEmpty()) {
            senders.remove(uri);
            RequestReplySender sender = holder.getSender();
            sender.stop();
        }
    }

    public void connectToReceiver(URI uri, List<InvocationChain> chains, String callback, ClassLoader loader) throws BrokerException {
        if (receivers.containsKey(uri)) {
            throw new BrokerException("Receiver already defined for " + uri);
        }
        try {
            ZMQ.Context context = manager.getContext();
            String endpointId = uri.toString();

            int port = allocator.allocate(endpointId, ZMQ);
            // XCV FIXME localhost
            String runtimeName = info.getRuntimeName();
            SocketAddress address = new SocketAddress(runtimeName, "tcp", InetAddress.getLocalHost().getHostAddress(), port);

            for (InvocationChain chain : chains) {
                ServiceMarshallingInterceptor interceptor = new ServiceMarshallingInterceptor(loader);
                chain.addInterceptor(interceptor);
            }
            NonReliableRequestReplyReceiver receiver = new NonReliableRequestReplyReceiver(context, address, chains, callback, monitor);
            receiver.start();

            AddressAnnouncement event = new AddressAnnouncement(endpointId, AddressAnnouncement.Type.ACTIVATED, address);
            addressCache.publish(event);

            receivers.put(uri, receiver);
        } catch (PortAllocationException e) {
            throw new BrokerException("Error allocating port for " + uri, e);
        } catch (UnknownHostException e) {
            throw new BrokerException("Error allocating port for " + uri, e);
        }
    }

    public void releaseReceiver(URI uri) throws BrokerException {
        Receiver receiver = receivers.remove(uri);
        if (receiver == null) {
            throw new BrokerException("Receiver not found for " + uri);
        }
        receiver.stop();
    }

    private class SenderHolder<T> {
        private T sender;
        private List<String> ids;

        private SenderHolder(T sender) {
            this.sender = sender;
            ids = new ArrayList<String>();
        }

        public T getSender() {
            return sender;
        }

        public List<String> getIds() {
            return ids;
        }
    }


}
