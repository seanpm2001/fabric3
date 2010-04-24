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
package org.fabric3.fabric.runtime.bootstrap;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServer;

import org.w3c.dom.Document;

import org.fabric3.contribution.manifest.ContributionExport;
import org.fabric3.fabric.instantiator.AtomicComponentInstantiator;
import org.fabric3.fabric.instantiator.component.AtomicComponentInstantiatorImpl;
import org.fabric3.fabric.runtime.FabricNames;
import org.fabric3.fabric.runtime.RuntimeServices;
import org.fabric3.fabric.synthesizer.SingletonComponentSynthesizer;
import org.fabric3.host.Names;
import static org.fabric3.host.Names.BOOT_CONTRIBUTION;
import static org.fabric3.host.Names.HOST_CONTRIBUTION;
import org.fabric3.host.contribution.ContributionException;
import org.fabric3.host.domain.DeploymentException;
import org.fabric3.host.domain.Domain;
import org.fabric3.host.monitor.MonitorFactory;
import org.fabric3.host.repository.Repository;
import org.fabric3.host.runtime.ComponentRegistration;
import org.fabric3.host.runtime.Fabric3Runtime;
import org.fabric3.host.runtime.HostInfo;
import org.fabric3.host.runtime.InitializationException;
import org.fabric3.host.runtime.BootConfiguration;
import org.fabric3.implementation.system.model.SystemImplementation;
import org.fabric3.introspection.java.DefaultIntrospectionHelper;
import org.fabric3.introspection.java.contract.JavaContractProcessorImpl;
import org.fabric3.model.type.component.Composite;
import org.fabric3.spi.classloader.ClassLoaderRegistry;
import org.fabric3.spi.cm.ComponentManager;
import org.fabric3.spi.component.ScopeContainer;
import org.fabric3.spi.component.ScopeRegistry;
import org.fabric3.spi.contribution.Contribution;
import org.fabric3.spi.contribution.ContributionManifest;
import org.fabric3.spi.contribution.ContributionState;
import org.fabric3.spi.contribution.MetaDataStore;
import org.fabric3.spi.contribution.manifest.JavaExport;
import org.fabric3.spi.contribution.manifest.PackageInfo;
import org.fabric3.spi.contribution.manifest.PackageVersion;
import org.fabric3.spi.introspection.java.ImplementationProcessor;
import org.fabric3.spi.introspection.java.IntrospectionHelper;
import org.fabric3.spi.introspection.java.contract.JavaContractProcessor;
import org.fabric3.spi.lcm.LogicalComponentManager;
import org.fabric3.spi.model.instance.LogicalCompositeComponent;
import org.fabric3.spi.model.instance.LogicalProperty;
import org.fabric3.spi.synthesize.ComponentRegistrationException;
import org.fabric3.spi.synthesize.ComponentSynthesizer;

/**
 * The default Bootstrapper implementation.
 *
 * @version $Rev$ $Date$
 */
public class DefaultBootstrapper implements Bootstrapper {

    private static final URI RUNTIME_SERVICES = URI.create("fabric3://RuntimeServices");

    // bootstrap components - these are disposed of after the core runtime system components are booted
    private JavaContractProcessor contractProcessor;
    private AtomicComponentInstantiator instantiator;
    private ImplementationProcessor<SystemImplementation> implementationProcessor;
    private ComponentSynthesizer synthesizer;

    // runtime components - these are persistent and supplied by the runtime implementation
    private MonitorFactory monitorFactory;
    private ClassLoaderRegistry classLoaderRegistry;
    private MetaDataStore metaDataStore;
    private ScopeRegistry scopeRegistry;
    private LogicalCompositeComponent domain;
    private LogicalComponentManager logicalComponetManager;
    private ComponentManager componentManager;
    private ScopeContainer scopeContainer;
    private Repository repository;
    private MBeanServer mbeanServer;
    private HostInfo hostInfo;

    private Domain runtimeDomain;

    private Fabric3Runtime runtime;
    private URL systemCompositeUrl;
    private Document systemConfig;
    private ClassLoader bootClassLoader;
    private Map<String, String> exportedPackages;
    private ClassLoader hostClassLoader;
    private Contribution bootContribution;

    public DefaultBootstrapper() {
        // create components needed to bootstrap the runtime
        IntrospectionHelper helper = new DefaultIntrospectionHelper();
        contractProcessor = new JavaContractProcessorImpl(helper);
        instantiator = new AtomicComponentInstantiatorImpl();
        implementationProcessor = BootstrapIntrospectionFactory.createSystemImplementationProcessor();
    }

    public void bootRuntimeDomain(BootConfiguration configuration) throws InitializationException {

        this.runtime = configuration.getRuntime();
        this.systemCompositeUrl = configuration.getSystemCompositeUrl();
        this.systemConfig = configuration.getSystemConfig();
        this.hostClassLoader = configuration.getHostClassLoader();
        this.bootClassLoader = configuration.getBootClassLoader();
        this.exportedPackages = configuration.getExportedPackages();
        // classloader shared by extension and application classes


        RuntimeServices runtimeServices = runtime.getComponent(RuntimeServices.class, RUNTIME_SERVICES);
        hostInfo = runtimeServices.getHostInfo();
        monitorFactory = runtimeServices.getMonitorFactory();
        logicalComponetManager = runtimeServices.getLogicalComponentManager();
        componentManager = runtimeServices.getComponentManager();
        domain = logicalComponetManager.getRootComponent();
        classLoaderRegistry = runtimeServices.getClassLoaderRegistry();
        metaDataStore = runtimeServices.getMetaDataStore();
        scopeRegistry = runtimeServices.getScopeRegistry();
        scopeContainer = runtimeServices.getScopeContainer();
        repository = runtimeServices.getRepository();
        mbeanServer = runtimeServices.getMBeanServer();
        hostInfo = runtimeServices.getHostInfo();

        synthesizer = new SingletonComponentSynthesizer(implementationProcessor,
                                                        instantiator,
                                                        logicalComponetManager,
                                                        componentManager,
                                                        contractProcessor,
                                                        scopeContainer);

        // register primordial components provided by the runtime itself
        List<ComponentRegistration> registrations = configuration.getRegistrations();
        registerRuntimeComponents(registrations);

        runtimeDomain = BootstrapAssemblyFactory.createDomain(monitorFactory,
                                                              classLoaderRegistry,
                                                              scopeRegistry,
                                                              componentManager,
                                                              logicalComponetManager,
                                                              metaDataStore,
                                                              mbeanServer,
                                                              hostInfo);

        // create and register bootstrap components provided by this bootstrapper
        registerDomain();

        // register the classloaders
        synthesizeContributions();

    }

    public void bootSystem() throws InitializationException {
        try {
            // load the system composite
            Composite composite =
                    BootstrapCompositeFactory.createSystemComposite(systemCompositeUrl, bootContribution, bootClassLoader, implementationProcessor);

            // create the property and merge it into the composite
            LogicalProperty logicalProperty = new LogicalProperty("systemConfig", systemConfig, false, domain);
            domain.setProperties(logicalProperty);

            // deploy the composite to the runtime domain
            runtimeDomain.include(composite);
        } catch (DeploymentException e) {
            throw new InitializationException(e);
        }
    }

    /**
     * Registers the primordial runtime components.
     *
     * @param registrations host components to register
     * @throws InitializationException if there is an error during registration
     */
    @SuppressWarnings({"unchecked"})
    private <S, I extends S> void registerRuntimeComponents(List<ComponentRegistration> registrations) throws InitializationException {

        // services available through the outward facing Fabric3Runtime API
        registerComponent("MonitorFactory", MonitorFactory.class, monitorFactory, true);
        Class<HostInfo> type = getHostInfoType(hostInfo);
        registerComponent("HostInfo", type, hostInfo, true);
        if (mbeanServer != null) {
            registerComponent("MBeanServer", MBeanServer.class, mbeanServer, false);
        }

        // services available through the inward facing RuntimeServices SPI
        registerComponent("ComponentManager", ComponentManager.class, componentManager, true);
        registerComponent("RuntimeLogicalComponentManager", LogicalComponentManager.class, logicalComponetManager, true);
        registerComponent("CompositeScopeContainer", ScopeContainer.class, scopeContainer, true);
        registerComponent("ClassLoaderRegistry", ClassLoaderRegistry.class, classLoaderRegistry, true);

        registerComponent("ScopeRegistry", ScopeRegistry.class, scopeRegistry, true);

        registerComponent("MetaDataStore", MetaDataStore.class, metaDataStore, true);

        registerComponent("Repository", Repository.class, repository, true);

        // register other components provided by the host environment
        for (ComponentRegistration registration : registrations) {
            String name = registration.getName();
            Class<S> service = (Class<S>) registration.getService();
            I instance = (I) registration.getInstance();
            boolean introspect = registration.isIntrospect();
            registerComponent(name, service, instance, introspect);
        }
    }

    /**
     * Determines the specific HostInfo interface subtype to register the HostInfo instance with.
     *
     * @param info the HostInfo
     * @return the interface to register the HostInfo instance with
     */
    @SuppressWarnings({"unchecked"})
    private Class<HostInfo> getHostInfoType(HostInfo info) {
        Class<?>[] interfaces = info.getClass().getInterfaces();
        if (interfaces.length == 1) {
            return HostInfo.class;
        }
        for (Class<?> interfaze : interfaces) {
            if (!HostInfo.class.equals(interfaze) && HostInfo.class.isAssignableFrom(interfaze)) {
                return (Class<HostInfo>) interfaze;
            }
        }
        return HostInfo.class;
    }

    /**
     * Registers the runtime domain.
     *
     * @throws InitializationException if there is an error during registration
     */
    private void registerDomain() throws InitializationException {
        registerComponent("RuntimeDomain", Domain.class, runtimeDomain, true);
        // the following initializes the Domain and MetaDataStore so they may be reinjected
        runtime.getComponent(Domain.class, Names.RUNTIME_DOMAIN_SERVICE_URI);
        runtime.getComponent(MetaDataStore.class, FabricNames.METADATA_STORE_URI);
    }

    /**
     * Registers a primordial component.
     *
     * @param name       the component name
     * @param type       the service interface type
     * @param instance   the component instance
     * @param introspect true if the component should be introspected for references
     * @throws InitializationException if there is an error during registration
     */
    private <S, I extends S> void registerComponent(String name, Class<S> type, I instance, boolean introspect) throws InitializationException {
        try {
            synthesizer.registerComponent(name, type, instance, introspect);
        } catch (ComponentRegistrationException e) {
            throw new InitializationException(e);
        }
    }

    /**
     * Creates contributions for the host and boot classloaders. These contributions may be imported by extensions and user contributions.
     *
     * @throws InitializationException if there is an error synthesizing the contributions
     */
    private void synthesizeContributions() throws InitializationException {
        try {
            // export packages included in JDK 6
            synthesizeContribution(HOST_CONTRIBUTION, Java6HostExports.getExports(), hostClassLoader);
            // add default boot exports
            exportedPackages.putAll(BootExports.getExports());
            bootContribution = synthesizeContribution(BOOT_CONTRIBUTION, exportedPackages, bootClassLoader);
        } catch (ContributionException e) {
            throw new InitializationException(e);
        }
    }

    /**
     * Synthesizes a contribution from a classloader and installs it.
     *
     * @param contributionUri  the contribution URI
     * @param exportedPackages the packages exported by the contribution
     * @param loader           the classloader
     * @return the synthesized contribution
     * @throws ContributionException if there is an error synthesizing the contribution
     */
    private Contribution synthesizeContribution(URI contributionUri, Map<String, String> exportedPackages, ClassLoader loader)
            throws ContributionException {
        Contribution contribution = new Contribution(contributionUri);
        contribution.setState(ContributionState.INSTALLED);
        ContributionManifest manifest = contribution.getManifest();
        // add the ContributionExport
        manifest.addExport(new ContributionExport(contributionUri));
        for (Map.Entry<String, String> entry : exportedPackages.entrySet()) {
            PackageVersion version = new PackageVersion(entry.getValue());
            PackageInfo info = new PackageInfo(entry.getKey(), version);
            JavaExport export = new JavaExport(info);
            manifest.addExport(export);
        }
        metaDataStore.store(contribution);
        classLoaderRegistry.register(contributionUri, loader);
        return contribution;
    }


}