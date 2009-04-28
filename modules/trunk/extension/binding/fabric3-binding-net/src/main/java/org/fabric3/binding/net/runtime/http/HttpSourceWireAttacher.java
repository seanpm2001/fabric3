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
package org.fabric3.binding.net.runtime.http;

import java.net.URI;

import org.osoa.sca.annotations.Reference;

import org.fabric3.binding.net.provision.HttpWireSourceDefinition;
import org.fabric3.binding.net.provision.TransportType;
import org.fabric3.binding.net.runtime.TransportService;
import org.fabric3.spi.ObjectFactory;
import org.fabric3.spi.builder.WiringException;
import org.fabric3.spi.builder.component.SourceWireAttacher;
import org.fabric3.spi.model.physical.PhysicalWireTargetDefinition;
import org.fabric3.spi.wire.Wire;

/**
 * Attaches services to an HTTP channel.
 *
 * @version $Revision$ $Date$
 */
public class HttpSourceWireAttacher implements SourceWireAttacher<HttpWireSourceDefinition> {
    private TransportService service;

    public HttpSourceWireAttacher(@Reference TransportService service) {
        this.service = service;
    }

    public void attachToSource(HttpWireSourceDefinition source, PhysicalWireTargetDefinition target, Wire wire) throws WiringException {
        URI uri = source.getUri();
        if (uri.getScheme() != null) {
            throw new WiringException("Absolute URIs not supported: " + uri);
        }
        String sourceUri = uri.toString();
        String callbackUri = null;
        if (target.getCallbackUri() != null) {
            callbackUri = target.getCallbackUri().toString();
        }
        service.register(TransportType.HTTP, sourceUri, callbackUri, wire);
    }

    public void detachFromSource(HttpWireSourceDefinition source, PhysicalWireTargetDefinition target) throws WiringException {
        service.unregister(TransportType.HTTP, source.getUri().toString());
    }

    public void attachObjectFactory(HttpWireSourceDefinition source, ObjectFactory<?> objectFactory, PhysicalWireTargetDefinition target)
            throws WiringException {
        throw new UnsupportedOperationException();
    }

    public void detachObjectFactory(HttpWireSourceDefinition source, PhysicalWireTargetDefinition target) throws WiringException {
        throw new UnsupportedOperationException();
    }


}
