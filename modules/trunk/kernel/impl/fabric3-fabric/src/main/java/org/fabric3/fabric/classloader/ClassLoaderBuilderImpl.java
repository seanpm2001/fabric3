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
package org.fabric3.fabric.classloader;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Reference;

import org.fabric3.spi.classloader.MultiParentClassLoader;
import org.fabric3.spi.services.classloading.ClassLoaderRegistry;
import org.fabric3.spi.services.contribution.ArtifactResolverRegistry;
import org.fabric3.spi.services.contribution.ClasspathProcessorRegistry;
import org.fabric3.spi.services.contribution.ResolutionException;
import org.fabric3.spi.model.physical.PhysicalClassLoaderDefinition;

/**
 * Default implementation of ClassLoaderBuilder.
 *
 * @version $Rev$ $Date$
 */
@EagerInit
public class ClassLoaderBuilderImpl implements ClassLoaderBuilder {

    private ClassLoaderRegistry classLoaderRegistry;
    private ArtifactResolverRegistry artifactResolverRegistry;
    private ClasspathProcessorRegistry classpathProcessorRegistry;

    public ClassLoaderBuilderImpl(@Reference ClassLoaderRegistry classLoaderRegistry,
                                  @Reference ArtifactResolverRegistry artifactResolverRegistry,
                                  @Reference ClasspathProcessorRegistry classpathProcessorRegistry) {
        this.classLoaderRegistry = classLoaderRegistry;
        this.artifactResolverRegistry = artifactResolverRegistry;
        this.classpathProcessorRegistry = classpathProcessorRegistry;
    }

    public void build(PhysicalClassLoaderDefinition definition) throws ClassLoaderBuilderException {

        if (classLoaderRegistry.getClassLoader(definition.getUri()) != null) {
            updateClassLoader(definition);
        } else {
            createClassLoader(definition);
        }

    }

    /**
     * Creates a new classloader from a PhysicalClassLoaderDefinition.
     *
     * @param definition the PhysicalClassLoaderDefinition to create the classloader from
     * @throws ClassLoaderBuilderException if an error occurs creating the classloader
     */
    private void createClassLoader(PhysicalClassLoaderDefinition definition) throws ClassLoaderBuilderException {

        URI name = definition.getUri();
        URL[] classpath = resolveClasspath(definition.getResourceUrls());

        // build the classloader using the locally cached resources
        MultiParentClassLoader loader = new MultiParentClassLoader(name, classpath, null);
        for (URI uri : definition.getParentClassLoaders()) {
            ClassLoader parent = classLoaderRegistry.getClassLoader(uri);
            if (parent == null) {
                String identifier = uri.toString();
                throw new ClassLoaderBuilderException("Parent classloader not found [" + identifier + "]", identifier);
            }
            loader.addParent(parent);
        }

        classLoaderRegistry.register(name, loader);

    }

    /**
     * Updates the given classloader with additional artifacts from the PhysicalClassLoaderDefinition. Classloader updates are typically performed
     * during an include operation where the included component requires additional libraries or classes not currently on the composite classpath.
     *
     * @param definition the definition to update the classloader with
     * @throws ClassLoaderBuilderException if an error occurs updating the classloader
     */
    private void updateClassLoader(PhysicalClassLoaderDefinition definition) throws ClassLoaderBuilderException {

        ClassLoader cl = classLoaderRegistry.getClassLoader(definition.getUri());
        assert cl instanceof MultiParentClassLoader;
        MultiParentClassLoader loader = (MultiParentClassLoader) cl;

        List<URL> classpath = new ArrayList<URL>();
        Set<URL> urls = definition.getResourceUrls();
        URL[] loaderUrls = loader.getURLs();

        for (URL url : urls) {

            boolean found = false;
            for (URL loaderUrl : loaderUrls) {
                if (loaderUrl.equals(url)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                try {
                    // resolve the remote artifact URL and add it to the classloader
                    URL resolvedUrl = artifactResolverRegistry.resolve(url);
                    // introspect and expand if necessary
                    classpath.addAll(classpathProcessorRegistry.process(resolvedUrl));
                } catch (ResolutionException e) {
                    throw new ClassLoaderBuilderException("Error resolving artifact", e);
                } catch (IOException e) {
                    throw new ClassLoaderBuilderException("Error processing", url.toString(), e);
                }
            }
        }

        for (URL url : classpath) {
            loader.addURL(url);
        }

        for (URI uri : definition.getParentClassLoaders()) {
            ClassLoader parent = classLoaderRegistry.getClassLoader(uri);
            if (!loader.getParents().contains(parent)) {
                loader.addParent(parent);
            }
        }

    }

    /**
     * Resolves classpath urls
     *
     * @param urls urls to resolve
     * @return the resolved classpath urls
     * @throws ClassLoaderBuilderException if an error occurs resolving a url
     */
    private URL[] resolveClasspath(Set<URL> urls) throws ClassLoaderBuilderException {

        List<URL> classpath = new ArrayList<URL>();

        for (URL url : urls) {
            try {
                // resolve the remote artifact URLs and cache them locally
                URL resolvedUrl = artifactResolverRegistry.resolve(url);
                // introspect and expand if necessary
                classpath.addAll(classpathProcessorRegistry.process(resolvedUrl));
            } catch (ResolutionException e) {
                throw new ClassLoaderBuilderException("Error resolving artifact", e);
            } catch (IOException e) {
                throw new ClassLoaderBuilderException("Error processing", url.toString(), e);
            }
        }

        return classpath.toArray(new URL[classpath.size()]);

    }

}
