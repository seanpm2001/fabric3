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
package org.fabric3.loader.definitions;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static org.osoa.sca.Constants.SCA_NS;
import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Init;
import org.osoa.sca.annotations.Reference;

import org.fabric3.host.contribution.InstallException;
import org.fabric3.introspection.xml.LoaderUtil;
import org.fabric3.introspection.xml.MissingAttribute;
import org.fabric3.scdl.ValidationContext;
import org.fabric3.scdl.definitions.AbstractDefinition;
import org.fabric3.spi.services.contribution.QNameSymbol;
import org.fabric3.spi.services.contribution.Resource;
import org.fabric3.spi.services.contribution.ResourceElement;
import org.fabric3.spi.services.contribution.XmlIndexer;
import org.fabric3.spi.services.contribution.XmlIndexerRegistry;

/**
 * Indexer for definitions.
 *
 * @version $Revision$ $Date$
 */
@EagerInit
public class DefinitionsIndexer implements XmlIndexer {
    private static final QName DEFINITIONS = new QName(SCA_NS, "definitions");
    private static final QName INTENT = new QName(SCA_NS, "intent");
    private static final QName POLICY_SET = new QName(SCA_NS, "policySet");
    private static final QName BINDING_TYPE = new QName(SCA_NS, "bindingType");
    private static final QName IMPLEMENTATION_TYPE = new QName(SCA_NS, "implementationType");
    private XmlIndexerRegistry registry;


    public DefinitionsIndexer(@Reference XmlIndexerRegistry registry) {
        this.registry = registry;
    }

    @Init
    public void init() {
        registry.register(this);
    }

    public QName getType() {
        return DEFINITIONS;
    }

    public void index(Resource resource, XMLStreamReader reader, ValidationContext context) throws InstallException {
        String targetNamespace = reader.getAttributeValue(null, "targetNamespace");

        while (true) {
            try {
                switch (reader.next()) {
                case START_ELEMENT:
                    QName qname = reader.getName();
                    if (!INTENT.equals(qname)
                            && !POLICY_SET.equals(qname)
                            && !BINDING_TYPE.equals(qname)
                            && !IMPLEMENTATION_TYPE.equals(qname)) {
                        continue;
                    }
                    String nameAttr = reader.getAttributeValue(null, "name");
                    if (nameAttr == null) {
                        context.addError(new MissingAttribute("Definition name not specified", "name", reader));
                        return;
                    }
                    NamespaceContext namespaceContext = reader.getNamespaceContext();
                    QName name = LoaderUtil.getQName(nameAttr, targetNamespace, namespaceContext);
                    QNameSymbol symbol = new QNameSymbol(name);
                    ResourceElement<QNameSymbol, AbstractDefinition> element =
                            new ResourceElement<QNameSymbol, AbstractDefinition>(symbol);
                    resource.addResourceElement(element);
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    return;
                }
            } catch (XMLStreamException e) {
                throw new InstallException(e);
            }
        }

    }

}