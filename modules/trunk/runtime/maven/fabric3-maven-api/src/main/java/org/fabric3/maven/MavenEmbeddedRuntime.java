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
package org.fabric3.maven;

import java.net.URL;
import javax.xml.namespace.QName;

import org.apache.maven.surefire.suite.SurefireTestSuite;

import org.fabric3.host.contribution.ContributionException;
import org.fabric3.host.domain.DeploymentException;
import org.fabric3.host.runtime.Fabric3Runtime;

/**
 * API for the Maven runtime. The Maven runtime requires system component of type Map<String, Wire> named "TestWireHolder" that contains wires to
 * integration test operations to be invoked. Extensions such as JUnit are required to introspect test implementation and generate the appropriate
 * metadata to instantiate wires to test operations. These wires must then be attached to the TestWireHolder component.
 *
 * @version $Rev$ $Date$
 */
public interface MavenEmbeddedRuntime extends Fabric3Runtime<MavenHostInfo> {

    /**
     * Deploys a composite by qualified name contained in the Maven module the runtime is currently executing for.
     *
     * @param base      the module output directory location
     * @param composite the composite qname to activate
     * @throws ContributionException if a contribution is thrown. The cause may a ValidationException resulting from  errors in the contribution. In
     *                               this case the errors should be reported back to the user.
     * @throws DeploymentException   if there is an error activating the test composite
     */
    void deploy(URL base, QName composite) throws ContributionException, DeploymentException;

    /**
     * Deploys a composite pointed to by the SCDL location.
     * <p/>
     * Note this method preserves backward compatibility through specifying the composite by location. When possible, use {@link #deploy(java.net.URL,
     * javax.xml.namespace.QName)} instead.
     *
     * @param base         the module output directory location
     * @param scdlLocation the composite file location
     * @return the  QName of the composite that was deployed
     * @throws DeploymentException   if there is an error activating the test composite
     * @throws ContributionException if a contribution is thrown. The cause may a ValidationException resulting from  errors in the contribution. In
     *                               this case the errors should be reported back to the user.
     */
    QName deploy(URL base, URL scdlLocation) throws ContributionException, DeploymentException;

    /**
     * Creates a test suite for testing components in the deployed composite.
     *
     * @return the test suite
     */
    SurefireTestSuite createTestSuite();

}
