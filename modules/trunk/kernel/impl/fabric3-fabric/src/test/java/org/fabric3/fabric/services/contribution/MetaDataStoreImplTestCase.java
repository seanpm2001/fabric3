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
package org.fabric3.fabric.services.contribution;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.fabric3.fabric.classloader.ClassLoaderRegistryImpl;
import org.fabric3.fabric.services.contribution.wire.ContributionWireInstantiatorRegistryImpl;
import org.fabric3.fabric.services.contribution.wire.QNameWireInstantiator;
import org.fabric3.fabric.services.contribution.wire.ContributionWireInstantiator;
import org.fabric3.spi.classloader.ClassLoaderRegistry;
import org.fabric3.spi.services.contribution.Contribution;
import org.fabric3.spi.services.contribution.ContributionManifest;
import org.fabric3.spi.services.contribution.ContributionWire;
import org.fabric3.spi.services.contribution.Import;
import org.fabric3.spi.services.contribution.QNameExport;
import org.fabric3.spi.services.contribution.QNameImport;
import org.fabric3.spi.services.contribution.QNameSymbol;
import org.fabric3.spi.services.contribution.Resource;
import org.fabric3.spi.services.contribution.ResourceElement;
import org.fabric3.util.io.FileHelper;

/**
 * @version $Rev$ $Date$
 */
public class MetaDataStoreImplTestCase extends TestCase {
    private static final URI RESOURCE_URI = URI.create("test-resource");
    private static final URI RESOURCE_URI2 = URI.create("test-resource2");
    private static final QName IMPORT_EXPORT_QNAME = new QName("test", "test");
    private static final QName IMPORT_EXPORT_QNAME2 = new QName("test2", "test2");
    private MetaDataStoreImpl store;

    public void testResolve() throws Exception {
        URI uri = URI.create("source");
        QNameImport imprt = new QNameImport(IMPORT_EXPORT_QNAME);
        ContributionWire<?, ?> wire = store.resolve(uri, imprt);
        assertEquals(RESOURCE_URI, wire.getExportContributionUri());
    }

    public void testResolveContainingResource() throws Exception {
        URI uri = URI.create("resource");
        Contribution contribution = new Contribution(uri);
        QName qname = new QName("foo", "bar");
        QNameSymbol symbol = new QNameSymbol(qname);
        ResourceElement<QNameSymbol, Serializable> element = new ResourceElement<QNameSymbol, Serializable>(symbol);
        Resource resource = new Resource(new URL("file://foo"), "resource");
        resource.addResourceElement(element);
        contribution.addResource(resource);
        store.store(contribution);
        assertEquals(resource, store.resolveContainingResource(uri, symbol));
    }

    public void testResolveDependentContributions() throws Exception {
        Set<Contribution> contributions = store.resolveDependentContributions(RESOURCE_URI);
        assertEquals(RESOURCE_URI2, contributions.iterator().next().getUri());
    }

    protected void setUp() throws Exception {
        super.setUp();
        ClassLoaderRegistry registry = new ClassLoaderRegistryImpl();
        registry.register(URI.create("resource"), getClass().getClassLoader());
        store = new MetaDataStoreImpl(registry, null);
        Map<Class<? extends Import>, ContributionWireInstantiator<?, ?, ?>> instantiators =
                new HashMap<Class<? extends Import>, ContributionWireInstantiator<?, ?, ?>>();
        instantiators.put(QNameImport.class, new QNameWireInstantiator());
        ContributionWireInstantiatorRegistryImpl instantiatorRegistry = new ContributionWireInstantiatorRegistryImpl();
        instantiatorRegistry.setInstantiators(instantiators);
        store.setInstantiatorRegistry(instantiatorRegistry);
        Contribution contribution = new Contribution(RESOURCE_URI);
        ContributionManifest manifest = contribution.getManifest();
        QNameExport export = new QNameExport(IMPORT_EXPORT_QNAME);
        manifest.addExport(export);
        store.store(contribution);

        Contribution contribution2 = new Contribution(RESOURCE_URI2);
        ContributionManifest manifest2 = contribution2.getManifest();
        QNameImport imprt = new QNameImport(IMPORT_EXPORT_QNAME);
        manifest2.addImport(imprt);
        QNameExport export2 = new QNameExport(IMPORT_EXPORT_QNAME2);
        manifest2.addExport(export2);
        store.store(contribution2);
        ContributionWire<?,?> wire = store.resolve(RESOURCE_URI2, imprt);
        contribution2.addWire(wire);

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        FileHelper.deleteDirectory(new File("target/repository"));
    }

}
