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
 *
 * ----------------------------------------------------
 *
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 *
 */
package org.fabric3.fabric.component.scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import javax.xml.namespace.QName;

import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Service;

import org.fabric3.api.annotation.Monitor;
import org.fabric3.model.type.component.Scope;
import org.fabric3.spi.ObjectCreationException;
import org.fabric3.spi.ObjectFactory;
import org.fabric3.spi.component.AtomicComponent;
import org.fabric3.spi.component.ExpirationPolicy;
import org.fabric3.spi.component.GroupInitializationException;
import org.fabric3.spi.component.InstanceDestructionException;
import org.fabric3.spi.component.InstanceInitializationException;
import org.fabric3.spi.component.InstanceLifecycleException;
import org.fabric3.spi.component.InstanceWrapper;
import org.fabric3.spi.component.ScopeContainer;
import org.fabric3.spi.invocation.WorkContext;

/**
 * Scope container for the standard COMPOSITE scope. Composite components have one instance which is associated with a scope context. Components
 * deployed via a deployable composite are associated with the same context. When a context starts and stops, components will recieve initalization
 * and destruction callbacks.
 *
 * @version $Rev$ $Date$
 */
@EagerInit
@Service(ScopeContainer.class)
public class CompositeScopeContainer extends AbstractScopeContainer {
    private final Map<AtomicComponent<?>, InstanceWrapper<?>> instanceWrappers;
    // The map of InstanceWrappers to destroy keyed by the deployable composite the component was deployed with.
    // The queues of InstanceWrappers are ordered by the sequence in which the deployables were deployed.
    // The InstanceWrappers themselves are ordered by the sequence in which they were instantiated.
    private final Map<QName, List<InstanceWrapper<?>>> destroyQueues;

    // the queue of components to eagerly initialize in each group
    private final Map<QName, List<AtomicComponent<?>>> initQueues = new HashMap<QName, List<AtomicComponent<?>>>();

    // components that are in the process of being created
    private final Map<AtomicComponent<?>, CountDownLatch> pending;

    public CompositeScopeContainer(@Monitor ScopeContainerMonitor monitor) {
        super(Scope.COMPOSITE, monitor);
        instanceWrappers = new ConcurrentHashMap<AtomicComponent<?>, InstanceWrapper<?>>();
        pending = new ConcurrentHashMap<AtomicComponent<?>, CountDownLatch>();
        destroyQueues = new LinkedHashMap<QName, List<InstanceWrapper<?>>>();
    }

    public void register(AtomicComponent<?> component) {
        super.register(component);
        if (component.isEagerInit()) {
            QName deployable = component.getDeployable();
            synchronized (initQueues) {
                List<AtomicComponent<?>> initQueue = initQueues.get(deployable);
                if (initQueue == null) {
                    initQueue = new ArrayList<AtomicComponent<?>>();
                    initQueues.put(deployable, initQueue);
                }
                initQueue.add(component);
                Collections.sort(initQueue, COMPARATOR);
            }
        }
        instanceWrappers.put(component, EMPTY);
    }

    public void unregister(AtomicComponent<?> component) {
        super.unregister(component);
        // FIXME should this component be destroyed already or do we need to stop it?
        instanceWrappers.remove(component);
        if (component.isEagerInit()) {
            QName deployable = component.getDeployable();
            synchronized (initQueues) {
                List<AtomicComponent<?>> initQueue = initQueues.get(deployable);
                initQueue.remove(component);
                if (initQueue.isEmpty()) {
                    initQueues.remove(deployable);
                }
            }
        }
    }

    public void startContext(WorkContext workContext) throws GroupInitializationException {
        QName contextId = workContext.peekCallFrame().getCorrelationId(QName.class);
        synchronized (destroyQueues) {
            destroyQueues.put(contextId, new ArrayList<InstanceWrapper<?>>());
        }
        eagerInitialize(workContext, contextId);
    }

    public void startContext(WorkContext workContext, ExpirationPolicy policy) throws GroupInitializationException {
        // scope does not support expiration policies
        startContext(workContext);
    }

    public void joinContext(WorkContext workContext) throws GroupInitializationException {
        QName contextId = workContext.peekCallFrame().getCorrelationId(QName.class);
        synchronized (destroyQueues) {
            if (!destroyQueues.containsKey(contextId)) {
                destroyQueues.put(contextId, new ArrayList<InstanceWrapper<?>>());
            }
        }
    }

    public void joinContext(WorkContext workContext, ExpirationPolicy policy) throws GroupInitializationException {
        // scope does not support expiration policies
        joinContext(workContext);
    }

    public void stopContext(WorkContext workContext) {
        QName contextId = workContext.peekCallFrame().getCorrelationId(QName.class);
        synchronized (destroyQueues) {
            List<InstanceWrapper<?>> list = destroyQueues.get(contextId);
            if (list == null) {
                throw new IllegalStateException("Context does not exist: " + contextId);
            }
            destroyInstances(list, workContext);
        }
    }

    public void stopAllContexts(WorkContext workContext) {
        synchronized (destroyQueues) {
            for (List<InstanceWrapper<?>> queue : destroyQueues.values()) {
                destroyInstances(queue, workContext);
            }
        }
    }

    public synchronized void stop() {
        super.stop();
        synchronized (destroyQueues) {
            destroyQueues.clear();
        }
        synchronized (initQueues) {
            initQueues.clear();
        }
        instanceWrappers.clear();
    }

    @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter"})
    public <T> InstanceWrapper<T> getWrapper(AtomicComponent<T> component, WorkContext workContext) throws InstanceLifecycleException {
        InstanceWrapper<T> wrapper = (InstanceWrapper<T>) instanceWrappers.get(component);
        if (wrapper != EMPTY) {
            return wrapper;
        }
        CountDownLatch latch;
        // Avoid race condition where two or more threads attempt to initialize the component instance concurrently.
        // Pending component instantiations are tracked and threads block on a latch until they are complete.
        synchronized (component) {
            latch = pending.get(component);
            if (latch != null) {
                try {
                    // wait on the instantiation
                    latch.await();
                } catch (InterruptedException e) {
                    String uri = component.getUri().toString();
                    throw new InstanceLifecycleException("Error initializing: " + uri, uri, e);
                }
                // an instance wrapper is now available as the instantiation has completed
                return (InstanceWrapper<T>) instanceWrappers.get(component);
            } else {
                latch = new CountDownLatch(1);
                pending.put(component, latch);
            }
        }
        try {
            wrapper = component.createInstanceWrapper(workContext);
            // some component instances such as system singletons may already be started
            if (!wrapper.isStarted()) {
                wrapper.start(workContext);
                List<InstanceWrapper<?>> queue;
                QName deployable = component.getDeployable();
                synchronized (destroyQueues) {
                    queue = destroyQueues.get(deployable);
                }
                if (queue == null) {
                    throw new IllegalStateException("Context not started: " + deployable);
                }
                queue.add(wrapper);
            }
            instanceWrappers.put(component, wrapper);
            latch.countDown();
            return wrapper;
        } catch (ObjectCreationException e) {
            String uri = component.getUri().toString();
            throw new InstanceLifecycleException("Error initializing: " + uri, uri, e);
        } finally {
            pending.remove(component);
        }
    }

    public <T> void returnWrapper(AtomicComponent<T> component, WorkContext workContext, InstanceWrapper<T> wrapper)
            throws InstanceDestructionException {
    }

    public void addObjectFactory(AtomicComponent<?> component, ObjectFactory<?> factory, String referenceName, Object key) {
        InstanceWrapper<?> wrapper = instanceWrappers.get(component);
        if (wrapper != null) {
            wrapper.addObjectFactory(referenceName, factory, key);
        }
    }

    public void removeObjectFactory(AtomicComponent<?> component, String referenceName) {
        InstanceWrapper<?> wrapper = instanceWrappers.get(component);
        if (wrapper != null) {
            wrapper.removeObjectFactory(referenceName);
        }
    }

    public void reinject() throws InstanceLifecycleException {
        for (InstanceWrapper<?> instanceWrapper : instanceWrappers.values()) {
            instanceWrapper.reinject();
        }
    }


    private void eagerInitialize(WorkContext workContext, QName contextId) throws GroupInitializationException {
        // get and clone initialization queue
        List<AtomicComponent<?>> initQueue;
        synchronized (initQueues) {
            initQueue = initQueues.get(contextId);
            if (initQueue != null) {
                initQueue = new ArrayList<AtomicComponent<?>>(initQueue);
            }
        }
        if (initQueue != null) {
            initializeComponents(initQueue, workContext);
        }
    }

    private static final Comparator<AtomicComponent<?>> COMPARATOR = new Comparator<AtomicComponent<?>>() {
        public int compare(AtomicComponent<?> o1, AtomicComponent<?> o2) {
            return o1.getInitLevel() - o2.getInitLevel();
        }
    };


    private static final InstanceWrapper<Object> EMPTY = new InstanceWrapper<Object>() {
        public Object getInstance() {
            return null;
        }

        public boolean isStarted() {
            return true;
        }

        public void start(WorkContext workContext) throws InstanceInitializationException {
        }

        public void stop(WorkContext workContext) throws InstanceDestructionException {
        }

        public void reinject() {
        }

        public void addObjectFactory(String referenceName, ObjectFactory<?> factory, Object key) {

        }

        public void removeObjectFactory(String referenceName) {

        }

    };

}