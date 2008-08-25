/*
 * See the NOTICE file distributed with this work for information
 * regarding copyright ownership.  This file is licensed
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
package org.fabric3.system.introspection;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.osoa.sca.annotations.Destroy;
import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Init;
import org.osoa.sca.annotations.Property;
import org.osoa.sca.annotations.Reference;

import org.fabric3.api.annotation.Monitor;
import org.fabric3.introspection.IntrospectionHelper;
import org.fabric3.introspection.contract.ContractProcessor;
import org.fabric3.introspection.impl.DefaultClassWalker;
import org.fabric3.introspection.impl.DefaultIntrospectionHelper;
import org.fabric3.introspection.impl.annotation.DestroyProcessor;
import org.fabric3.introspection.impl.annotation.EagerInitProcessor;
import org.fabric3.introspection.impl.annotation.InitProcessor;
import org.fabric3.introspection.impl.annotation.MonitorProcessor;
import org.fabric3.introspection.impl.annotation.PropertyProcessor;
import org.fabric3.introspection.impl.annotation.ReferenceProcessor;
import org.fabric3.introspection.impl.contract.DefaultContractProcessor;
import org.fabric3.introspection.java.AnnotationProcessor;
import org.fabric3.introspection.java.ClassWalker;
import org.fabric3.system.scdl.SystemImplementation;

/**
 * Instantiates an ImplementationProcessor for introspecting system components. System components are composite-scoped and support the standard SCA
 * lifecycle, including @Init, @Destroy, and @EagerInit.
 *
 * @version $Rev$ $Date$
 */
public class BootstrapIntrospectionFactory {

    /**
     * Returns a new ImplementationProcessor for system components.
     *
     * @return a new ImplementationProcessor for system components
     */
    public static SystemImplementationProcessor createSystemImplementationProcessor() {
        IntrospectionHelper helper = new DefaultIntrospectionHelper();
        ContractProcessor contractProcessor = new DefaultContractProcessor(helper);

        Map<Class<? extends Annotation>, AnnotationProcessor<? extends Annotation, SystemImplementation>> processors =
                new HashMap<Class<? extends Annotation>, AnnotationProcessor<? extends Annotation, SystemImplementation>>();

        // no constructor processor is needed as that is handled by heuristics
        processors.put(Property.class, new PropertyProcessor<SystemImplementation>(helper));
        processors.put(Reference.class, new ReferenceProcessor<SystemImplementation>(contractProcessor, helper));
        processors.put(EagerInit.class, new EagerInitProcessor<SystemImplementation>());
        processors.put(Init.class, new InitProcessor<SystemImplementation>());
        processors.put(Destroy.class, new DestroyProcessor<SystemImplementation>());
        processors.put(Monitor.class, new MonitorProcessor<SystemImplementation>(helper, contractProcessor));

        ClassWalker<SystemImplementation> classWalker = new DefaultClassWalker<SystemImplementation>(processors);

        // heuristics for system components
        SystemServiceHeuristic serviceHeuristic = new SystemServiceHeuristic(contractProcessor, helper);
        SystemConstructorHeuristic constructorHeuristic = new SystemConstructorHeuristic();
        SystemUnannotatedHeuristic unannotatedHeuristic = new SystemUnannotatedHeuristic(helper, contractProcessor);
        SystemHeuristic systemHeuristic = new SystemHeuristic(serviceHeuristic, constructorHeuristic, unannotatedHeuristic);

        return new SystemImplementationProcessorImpl(classWalker, systemHeuristic, helper);
    }

}