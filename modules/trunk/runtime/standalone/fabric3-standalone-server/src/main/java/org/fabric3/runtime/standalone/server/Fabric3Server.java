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
package org.fabric3.runtime.standalone.server;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.w3c.dom.Document;

import org.fabric3.api.annotation.monitor.*;
import org.fabric3.host.Fabric3Exception;
import static org.fabric3.host.Names.MONITOR_FACTORY_URI;
import static org.fabric3.host.Names.RUNTIME_MONITOR_CHANNEL_URI;
import org.fabric3.host.RuntimeMode;
import org.fabric3.host.monitor.MonitorEventDispatcher;
import org.fabric3.host.monitor.MonitorProxyService;
import org.fabric3.host.runtime.BootConfiguration;
import static org.fabric3.host.runtime.BootConstants.APP_MONITOR;
import static org.fabric3.host.runtime.BootConstants.RUNTIME_MONITOR;
import org.fabric3.host.runtime.BootstrapFactory;
import org.fabric3.host.runtime.BootstrapHelper;
import org.fabric3.host.runtime.BootstrapService;
import org.fabric3.host.runtime.Fabric3Runtime;
import org.fabric3.host.runtime.HostInfo;
import org.fabric3.host.runtime.MaskingClassLoader;
import org.fabric3.host.runtime.PortRange;
import org.fabric3.host.runtime.RuntimeConfiguration;
import org.fabric3.host.runtime.RuntimeCoordinator;
import org.fabric3.host.runtime.ScanResult;
import org.fabric3.host.runtime.ShutdownException;
import org.fabric3.host.util.FileHelper;
import org.fabric3.jmx.agent.rmi.RmiAgent;

/**
 * This class provides the commandline interface for starting the Fabric3 standalone server. The class boots a Fabric3 runtime and launches a daemon
 * that listens for a shutdown command.
 * <p/>
 * The administration port can be specified using the system property <code>fabric3.adminPort</code>.If not specified the default port that is used is
 * <code>1199</code>
 *
 * @version $Rev$ $Date$
 */
public class Fabric3Server implements Fabric3ServerMBean {
    private static final String RUNTIME_MBEAN = "fabric3:SubDomain=runtime, type=component, name=RuntimeMBean";

    private RuntimeCoordinator coordinator;
    private ServerMonitor monitor;
    private CountDownLatch latch;

    /**
     * Main method.
     *
     * @param args Commandline arguments.
     * @throws Fabric3Exception if there is a catostrophic problem starting the runtime
     */
    public static void main(String[] args) throws Fabric3Exception {

        RuntimeMode runtimeMode = getRuntimeMode(args);

        Fabric3Server server = new Fabric3Server();
        server.start(runtimeMode);
        System.exit(0);
    }

    /**
     * Starts the runtime in a blocking fashion and only returns after it has been released from another thread.
     *
     * @param runtimeMode the mode to start the runtime in
     * @throws Fabric3ServerException if catostrophic exception was encountered leaving the runtime in an unstable state
     */
    public void start(RuntimeMode runtimeMode) throws Fabric3ServerException {
        try {
            //  calculate config directories based on the mode the runtime is booted in
            File installDirectory = BootstrapHelper.getInstallDirectory(Fabric3Server.class);
            File configDir = BootstrapHelper.getDirectory(installDirectory, "config");
            File modeConfigDir = BootstrapHelper.getDirectory(configDir, runtimeMode.toString().toLowerCase());

            // create the classloaders for booting the runtime
            File bootDir = BootstrapHelper.getDirectory(installDirectory, "boot");

            File hostDir = BootstrapHelper.getDirectory(installDirectory, "host");

            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            ClassLoader maskingClassLoader = new MaskingClassLoader(systemClassLoader, HiddenPackages.getPackages());
            ClassLoader hostLoader = BootstrapHelper.createClassLoader(maskingClassLoader, hostDir);
            ClassLoader bootLoader = BootstrapHelper.createClassLoader(hostLoader, bootDir);

            BootstrapService bootstrapService = BootstrapFactory.getService(bootLoader);

            // load the system configuration
            Document systemConfig = bootstrapService.loadSystemConfig(modeConfigDir);

            URI domainName = bootstrapService.parseDomainName(systemConfig);

            // create the HostInfo and runtime
            HostInfo hostInfo = BootstrapHelper.createHostInfo(runtimeMode, domainName, installDirectory, configDir, modeConfigDir);

            // clear out the tmp directory
            FileHelper.cleanDirectory(hostInfo.getTempDir());

            // create the JMX agent
            PortRange range = bootstrapService.parseJmxPort(systemConfig);
            RmiAgent agent = new RmiAgent(range.getMinimum(), range.getMaximum());
            MBeanServer mbServer = agent.getMBeanServer();

            // create and configure the monitor dispatchers
            MonitorEventDispatcher runtimeDispatcher = bootstrapService.createMonitorDispatcher(RUNTIME_MONITOR, systemConfig);
            MonitorEventDispatcher appDispatcher = bootstrapService.createMonitorDispatcher(APP_MONITOR, systemConfig);

            RuntimeConfiguration runtimeConfig = new RuntimeConfiguration(hostInfo, mbServer, runtimeDispatcher, appDispatcher);

            Fabric3Runtime runtime = bootstrapService.createDefaultRuntime(runtimeConfig);

            URL systemComposite = new File(configDir, "system.composite").toURI().toURL();

            ScanResult result = bootstrapService.scanRepository(hostInfo.getRepositoryDirectory());

            BootConfiguration configuration = new BootConfiguration();
            configuration.setRuntime(runtime);
            configuration.setHostClassLoader(hostLoader);
            configuration.setBootClassLoader(bootLoader);
            configuration.setSystemCompositeUrl(systemComposite);
            configuration.setSystemConfig(systemConfig);
            configuration.setExtensionContributions(result.getExtensionContributions());
            configuration.setUserContributions(result.getUserContributions());

            // start the runtime
            coordinator = bootstrapService.createCoordinator(configuration);
            coordinator.start();

            // register the runtime with the MBean server
            ObjectName name = new ObjectName(RUNTIME_MBEAN);
            mbServer.registerMBean(this, name);

            agent.start();
            // create the shutdown daemon
            latch = new CountDownLatch(1);

            MonitorProxyService monitorService = runtime.getComponent(MonitorProxyService.class, MONITOR_FACTORY_URI);
            monitor = monitorService.createMonitor(ServerMonitor.class, RUNTIME_MONITOR_CHANNEL_URI);
            monitor.started(runtimeMode.toString(), agent.getAssignedPort());

            try {
                latch.await();
                monitor.stopped();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (RuntimeException ex) {
            shutdown();
            handleStartException(ex);
        } catch (Exception ex) {
            shutdown();
            handleStartException(ex);
        }
    }

    public void shutdownRuntime() {
        latch.countDown();
        shutdown();
    }

    private void shutdown() {
        try {
            if (coordinator != null) {
                coordinator.shutdown();
            }
        } catch (ShutdownException ex) {
            monitor.runError(ex);
        }
    }

    private static RuntimeMode getRuntimeMode(String[] args) {
        RuntimeMode runtimeMode = RuntimeMode.VM;
        if (args.length > 0) {
            if ("controller".equals(args[0])) {
                runtimeMode = RuntimeMode.CONTROLLER;
            } else if ("participant".equals(args[0])) {
                runtimeMode = RuntimeMode.PARTICIPANT;
            } else if (!"vm".equals(args[0])) {
                throw new IllegalArgumentException("Invalid runtime mode: " + args[0]
                        + ". Valid modes are 'controller', 'participant' or 'vm' (default).");
            }
        }
        return runtimeMode;
    }

    private void handleStartException(Exception ex) throws Fabric3ServerException {
        if (monitor != null) {
            // there could have been an error initializing the monitor
            monitor.exited(ex);
        } else {
            // throw the exception if the monitor is not available, e.g. due to an error
            throw new Fabric3ServerException(ex);
        }
    }

    public interface ServerMonitor {
        @Severe
        void runError(Exception e);

        @Info
        void started(String mode, int jmxPort);

        @Info
        void stopped();

        @Info
        void exited(Exception e);

    }


}
