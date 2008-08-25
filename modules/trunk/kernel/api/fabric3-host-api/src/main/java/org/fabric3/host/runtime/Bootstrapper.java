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
package org.fabric3.host.runtime;

/**
 * Implementations bootstrap a runtime in two phases. The primordial phase initializes the essential services required to load the core runtime
 * services. The second phase initializes the core runtime services.
 *
 * @version $Rev$ $Date$
 */
public interface Bootstrapper {
    /**
     * Iniitialize the supplied runtime and and its primordial system components.
     *
     * @param runtime         the runtime to boot
     * @param bootClassLoader the bootstrap classloader
     * @param appClassLoader  the top-level application classloader
     * @throws InitializationException if there was a problem bootstrapping the runtime
     */
    public void bootPrimordial(Fabric3Runtime<?> runtime, ClassLoader bootClassLoader, ClassLoader appClassLoader)
            throws InitializationException;

    /**
     * Initialize the core system components for the supplied runtime.
     *
     * @throws InitializationException if there was a problem bootstrapping the runtime
     */
    public void bootSystem() throws InitializationException;

}
