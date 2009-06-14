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
package org.fabric3.maven.test;

import java.util.Map;
import java.util.HashMap;

import org.apache.maven.surefire.suite.SurefireTestSuite;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.testset.TestSetFailedException;

/**
 * @version $Rev: 5918 $ $Date: 2008-11-14 13:38:29 -0800 (Fri, 14 Nov 2008) $
 */
public class SCATestSuite implements SurefireTestSuite {
    private final Map<String, SCATestSet> testSets = new HashMap<String, SCATestSet>();
    private int testSetCount = 0;
    private int testCount = 0;

    public void add(SCATestSet testSet) {
        testSets.put(testSet.getName(), testSet);
        testSetCount += 1;
        testCount += testSet.getTestCount();
    }

    public int getNumTests() {
        return testCount;
    }

    public int getNumTestSets() {
        return testSetCount;
    }

    public void execute(ReporterManager reporterManager, ClassLoader classLoader)
        throws ReporterException, TestSetFailedException {
        for (SCATestSet testSet : testSets.values()) {
            execute(testSet, reporterManager, classLoader);
        }
    }

    public void execute(String name, ReporterManager reporterManager, ClassLoader classLoader)
        throws ReporterException, TestSetFailedException {
        SCATestSet testSet = testSets.get(name);
        if (testSet == null) {
            throw new TestSetFailedException("Suite does not contain TestSet: " + name);
        }
        execute(testSet, reporterManager, classLoader);
    }

    protected void execute(SCATestSet testSet, ReporterManager reporterManager, ClassLoader classLoader)
        throws ReporterException, TestSetFailedException {
        reporterManager.testSetStarting(new ReportEntry(this, testSet.getName(), "Starting"));
        testSet.execute(reporterManager, classLoader);
        reporterManager.testSetCompleted(new ReportEntry(this, testSet.getName(), "Completed"));
        reporterManager.reset();
    }

    public Map<?, ?> locateTestSets(ClassLoader classLoader) throws TestSetFailedException {
        throw new UnsupportedOperationException();
    }
}
