/*
 * Fabric3
 * Copyright (C) 2009 Metaform Systems
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
 * --- Original Apache License ---
 *
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
package org.fabric3.test.runtime;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.surefire.suite.SurefireTestSuite;

import org.fabric3.fabric.runtime.AbstractRuntime;
import org.fabric3.fabric.runtime.DefaultCoordinator;
import org.fabric3.fabric.runtime.bootstrap.ScdlBootstrapperImpl;
import org.fabric3.host.Names;
import static org.fabric3.host.Names.APPLICATION_DOMAIN_URI;
import static org.fabric3.host.Names.CONTRIBUTION_SERVICE_URI;
import org.fabric3.host.contribution.ContributionException;
import org.fabric3.host.contribution.ContributionService;
import org.fabric3.host.contribution.ContributionSource;
import org.fabric3.host.domain.DeploymentException;
import org.fabric3.host.domain.Domain;
import org.fabric3.host.runtime.BootConfiguration;
import org.fabric3.host.runtime.RuntimeLifecycleCoordinator;
import org.fabric3.host.runtime.ScdlBootstrapper;
import org.fabric3.spi.wire.Wire;
import org.fabric3.test.runtime.api.DeployException;
import org.fabric3.test.runtime.api.MavenHostInfo;
import org.fabric3.test.runtime.api.MavenRuntime;
import org.fabric3.test.runtime.api.StartException;
import org.fabric3.test.spi.TestWireHolder;

/**
 * Maven runtime implementation.
 */
public class MavenRuntimeImpl extends AbstractRuntime<MavenHostInfo> implements MavenRuntime {

    /**
     * Initiates the host information.
     */
    public MavenRuntimeImpl() {
        super(MavenHostInfo.class);
    }

    /**
     * Starts the runtime.
     *
     * @param hostProperties Host properties.
     * @param extensions     Extensions to activate on the runtime.
     * @throws StartException If unable to start the runtime.
     */
    public void start(Properties hostProperties, List<ContributionSource> extensions) throws StartException {

        BootConfiguration bootConfiguration = getBootConfiguration(extensions);

        RuntimeLifecycleCoordinator coordinator = new DefaultCoordinator();
        coordinator.setConfiguration(bootConfiguration);

        MavenHostInfo mavenHostInfo = new MavenHostInfoImpl(hostProperties);
        setHostInfo(mavenHostInfo);

        boot(coordinator);

    }

    /**
     * Deploys a list contributions.
     *
     * @param contributions List of contributions.
     */
    public void deploy(List<ContributionSource> contributions) {

        try {

            ContributionService contributionService = getSystemComponent(ContributionService.class, CONTRIBUTION_SERVICE_URI);
            Domain domain = getSystemComponent(Domain.class, APPLICATION_DOMAIN_URI);

            List<URI> uris = contributionService.contribute(contributions);
            domain.include(uris, false);

        } catch (DeploymentException e) {
            throw new DeployException(e.getMessage(), e);
        } catch (ContributionException e) {
            throw new DeployException(e.getMessage(), e);
        }

    }

    /**
     * Gets the test suite from the SCA contribution.
     *
     * @return SCA test suite.
     */
    public SurefireTestSuite getTestSuite() {

        TestWireHolder testWireHolder = getSystemComponent(TestWireHolder.class, TestWireHolder.COMPONENT_URI);
        SCATestSuite suite = new SCATestSuite();
        for (Map.Entry<String, Wire> entry : testWireHolder.getWires().entrySet()) {
            SCATestSet testSet = new SCATestSet(entry.getKey(), entry.getValue());
            suite.add(testSet);
        }
        return suite;

    }

    /*
     * Boots the runtime.
     */
    private void boot(RuntimeLifecycleCoordinator coordinator) {

        try {
            coordinator.bootPrimordial();
            coordinator.initialize();
            coordinator.recover();
            coordinator.joinDomain(-1);
            coordinator.start();
        } catch (Exception e) {
            throw new StartException(e.getMessage(), e);
        }

    }

    /*
     * Create the boot configuration.
     */
    private BootConfiguration getBootConfiguration(List<ContributionSource> extensions) {

        BootConfiguration bootConfiguration = new BootConfiguration();

        bootConfiguration.setExtensionContributions(extensions);
        bootConfiguration.setRuntime(this);
        bootConfiguration.setBootClassLoader(getClass().getClassLoader());

        setExportedPackages(bootConfiguration);
        setBootstrapper(bootConfiguration);

        return bootConfiguration;

    }

    /*
     * Set the bootstrapper.
     */
    private void setBootstrapper(BootConfiguration bootConfiguration) {

        ScdlBootstrapper bootstrapper = new ScdlBootstrapperImpl();
        URL systemScdl = getClass().getClassLoader().getResource("META-INF/fabric3/embeddedMaven.composite");
        bootstrapper.setScdlLocation(systemScdl);
        bootConfiguration.setBootstrapper(bootstrapper);

    }

    /*
     * Set the packages to be exported.
     */
    private void setExportedPackages(BootConfiguration bootConfiguration) {

        Map<String, String> exportedPackages = new HashMap<String, String>();
        exportedPackages.put("org.fabric3.test.spi", Names.VERSION);
        exportedPackages.put("org.fabric3.maven", Names.VERSION);

        bootConfiguration.setExportedPackages(exportedPackages);

    }

}
