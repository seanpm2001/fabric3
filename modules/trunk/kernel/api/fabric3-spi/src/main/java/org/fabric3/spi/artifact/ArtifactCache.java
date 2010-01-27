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
*/
package org.fabric3.spi.artifact;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * Temporarily stores artifacts locally to a runtime, maintaining an in-use count.
 *
 * @version $Rev$ $Date$
 */
public interface ArtifactCache {

    /**
     * Temporarily persists an artifact. The lifetime of the artifact will not extend past the lifetime of the runtime instance. When the operation
     * completes, the input stream will be closed.
     *
     * @param uri    The artifact URI
     * @param stream the artifact contents
     * @return a URL for the persisted artifact
     * @throws CacheException if an error occurs persisting the artifact
     */
    URL cache(URI uri, InputStream stream) throws CacheException;

    /**
     * Returns the URL for the cached artifact or null if not found.
     *
     * @param uri the artifact URI
     * @return the URL for the cached artifact or null if not found
     */
    URL get(URI uri);

    /**
     * Evicts an artifact.
     *
     * @param uri the artifact URI.
     * @return returns true if the artifact was evicted
     * @throws CacheException if an error occurs releasing the artifact
     */
    boolean release(URI uri) throws CacheException;

}
