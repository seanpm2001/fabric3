/*
 * Fabric3
 * Copyright � 2008 Metaform Systems Limited
 *
 * This proprietary software may be used only connection with the Fabric3 license
 * (the �License�), a copy of which is included in the software or may be
 * obtained at: http://www.metaformsystems.com/licenses/license.html.

 * Software distributed under the License is distributed on an �as is� basis,
 * without warranties or conditions of any kind.  See the License for the
 * specific language governing permissions and limitations of use of the software.
 * This software is distributed in conjunction with other software licensed under
 * different terms.  See the separate licenses for those programs included in the
 * distribution for the permitted and restricted uses of such software.
 *
 */
package org.fabric3.binding.rmi.wire;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Reference;

import org.fabric3.binding.rmi.model.physical.RmiWireSourceDefinition;
import org.fabric3.binding.rmi.transport.RmiServiceHandler;
import org.fabric3.binding.codegen.ProxyGenerator;
import org.fabric3.spi.ObjectFactory;
import org.fabric3.spi.builder.WiringException;
import org.fabric3.spi.builder.component.SourceWireAttacher;
import org.fabric3.spi.builder.component.WireAttachException;
import org.fabric3.spi.classloader.MultiParentClassLoader;
import org.fabric3.spi.model.physical.PhysicalOperationDefinition;
import org.fabric3.spi.model.physical.PhysicalWireTargetDefinition;
import org.fabric3.spi.services.classloading.ClassLoaderRegistry;
import org.fabric3.spi.wire.InvocationChain;
import org.fabric3.spi.wire.Wire;

@EagerInit
public class RmiSourceWireAttacher implements SourceWireAttacher<RmiWireSourceDefinition> {

    static {
        System.setProperty("java.rmi.server.ignoreStubClasses", "true");
    }

    private final ClassLoaderRegistry classLoaderRegistry;
    private final Map<String, CodeGenClassLoader> classLoaderMap = new WeakHashMap<String, CodeGenClassLoader>(11);
    private final Map<Integer, Registry> registryMap = new ConcurrentHashMap<Integer, Registry>(11);
    private final Map<String, Remote> remoteObjects = new ConcurrentHashMap<String, Remote>(11);
    private final ProxyGenerator generator;

    /**
     * Injects the wire attacher classLoaderRegistry and servlet host.
     *
     * @param classLoaderRegistry the classloader registry for loading application classes
     */
    public RmiSourceWireAttacher(@Reference ClassLoaderRegistry classLoaderRegistry,
                                 @Reference ProxyGenerator generator) {
        this.classLoaderRegistry = classLoaderRegistry;
        this.generator = generator;
    }

    public void attachToSource(RmiWireSourceDefinition sourceDefinition,
                               PhysicalWireTargetDefinition targetDefinition,
                               Wire wire) throws WiringException {

        Map<Method, Map.Entry<PhysicalOperationDefinition, InvocationChain>> ops =
                new HashMap<Method, Map.Entry<PhysicalOperationDefinition, InvocationChain>>();
        Class interfaceClass;
        try {
            String interfaceName = sourceDefinition.getInterfaceName();
            interfaceClass = generateRemoteInterface(interfaceName, sourceDefinition.getClassLoaderId());
            for (Map.Entry<PhysicalOperationDefinition, InvocationChain> entry : wire.getInvocationChains().entrySet()) {

                Signature signature = new Signature(entry.getKey().getName(), entry.getKey().getParameters());
                ops.put(signature.getMethod(interfaceClass), entry);
            }
        } catch (IOException ioe) {
            throw new WireAttachException("Error attaching Rmi binding source", sourceDefinition.getUri(), targetDefinition.getUri(), ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new WireAttachException("Error attaching Rmi binding source", sourceDefinition.getUri(), targetDefinition.getUri(), cnfe);
        } catch (NoSuchMethodException nsme) {
            throw new WireAttachException("Error attaching Rmi binding source", sourceDefinition.getUri(), targetDefinition.getUri(), nsme);
        }
        RmiServiceHandler handler = new RmiServiceHandler(ops);
        Remote proxy = generateProxy(interfaceClass, handler, sourceDefinition.getUri(), targetDefinition.getUri());

        String serviceName = sourceDefinition.getBindingDefinition().getServiceName();
        int port = sourceDefinition.getBindingDefinition().getPort();
        if (serviceName != null) {
            try {
                Registry registry = findOrCreateRegistry(port);
                Remote stub = UnicastRemoteObject.exportObject(proxy, port);
                registry.rebind(serviceName, stub);
                //TODO We should have a way to remove objects from map upon undeploy
                remoteObjects.put(serviceName, proxy);
            } catch (RemoteException ne) {
                throw new WireAttachException("Error binding Rmi binding to JNDI name: " + serviceName,
                                              sourceDefinition.getUri(), targetDefinition.getUri(), ne);
            }
        }


    }

    public void detachFromSource(RmiWireSourceDefinition source, PhysicalWireTargetDefinition target) throws WireAttachException {
        String serviceName = source.getBindingDefinition().getServiceName();
        int port = source.getBindingDefinition().getPort();
        if (serviceName != null) {
            try {
                Registry registry = findOrCreateRegistry(port);
                registry.unbind(serviceName);
                //TODO We should have a way to remove objects from map upon undeploy
                remoteObjects.remove(serviceName);
            } catch (NotBoundException nbe) {
                throw new WireAttachException("Error while performing unbind operation JNDI name: " + serviceName,
                                              source.getUri(), target.getUri(), nbe);                
            } catch (RemoteException re) {
                throw new WireAttachException("Error binding Rmi binding to JNDI name: " + serviceName,
                                              source.getUri(), target.getUri(), re);
            }
        }

    }

    public void detachObjectFactory(RmiWireSourceDefinition source, PhysicalWireTargetDefinition target) throws WiringException {
        throw new AssertionError();
    }

    private Class generateRemoteInterface(String name, URI uri)
            throws IOException, ClassNotFoundException {
        String key = uri.toString();
        CodeGenClassLoader cl = classLoaderMap.get(name);
        MultiParentClassLoader multiParentCL;
        if (cl == null) {
            multiParentCL =
                    (MultiParentClassLoader) classLoaderRegistry.getClassLoader(uri);
            ClassLoader ccl = Thread.currentThread().getContextClassLoader();
            if (ccl != null)
                multiParentCL.addParent(ccl);
            cl = new CodeGenClassLoader(key, multiParentCL);
            classLoaderMap.put(name, cl);
        }
        String resourceName = name.replace('.', '/') + ".class";
        return InterfacePreProcessor.generateRemoteInterface(name, cl.getResourceAsStream(resourceName), cl);

    }

    private Registry findOrCreateRegistry(int port) throws RemoteException {
        Registry r = null;
        try {
            r = registryMap.get(port);
            if (r == null) {
                r = LocateRegistry.createRegistry(port);
                registryMap.put(port, r);
            }
        } catch (RemoteException re) {

        }
        return r;
    }

    private Remote generateProxy(Class clazz, RmiServiceHandler handler,
                                 URI source,
                                 URI target) throws WireAttachException {
        try {
            Object proxy = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, handler);
            return (Remote) generator.getWrapper(clazz, proxy);
        } catch (ClassNotFoundException cnfe) {
            throw new WireAttachException("Error attaching Rmi binding source", source, target, cnfe);
        } catch (IllegalAccessException iae) {
            throw new WireAttachException("Error attaching Rmi binding source", source, target, iae);
        } catch (InvocationTargetException ite) {
            throw new WireAttachException("Error attaching Rmi binding source", source, target, ite);
        } catch (InstantiationException ie) {
            throw new WireAttachException("Error attaching Rmi binding source", source, target, ie);
        }
    }


    public void attachObjectFactory(RmiWireSourceDefinition source, ObjectFactory<?> objectFactory, PhysicalWireTargetDefinition definition) throws WiringException {
        throw new AssertionError();
    }
}
