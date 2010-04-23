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
package org.fabric3.fabric.runtime.bootstrap;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.fabric3.fabric.xml.DocumentLoader;
import org.fabric3.fabric.xml.DocumentLoaderImpl;
import org.fabric3.host.runtime.InitializationException;
import org.fabric3.host.stream.Source;

/**
 * Creates a configuration property value for the runtime domain.
 *
 * @version $Rev$ $Date$
 */
public class BootstrapSystemConfigFactory {
    private static final DocumentLoader LOADER = new DocumentLoaderImpl();

    /**
     * Returns a configuration property value for the runtime domain from the given source.
     *
     * @param source the source to read
     * @return the domain configuration property
     * @throws InitializationException if an error reading the source is encountered
     */
    public static Document createSystemConfig(Source source) throws InitializationException {
        try {
            InputSource inputSource = new InputSource(source.openStream());
            Document document = LOADER.load(inputSource, true);
            // all properties have a root <values> element, append the existing root to it. The existing root will be taken as a property <value>.
            Element oldRoot = document.getDocumentElement();
            Element newRoot = document.createElement("values");
            document.removeChild(oldRoot);
            document.appendChild(newRoot);
            newRoot.appendChild(oldRoot);
            return document;
        } catch (IOException e) {
            throw new InitializationException(e);
        } catch (SAXException e) {
            throw new InitializationException(e);
        }
    }

    /**
     * Creates a default configuration property value for the runtime domain.
     *
     * @return a document representing the configuration property
     */
    public static Document createDefaultSystemConfig() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().newDocument();
            Element root = document.createElement("values");
            document.appendChild(root);
            Element config = document.createElement("config");
            root.appendChild(config);
            return document;
        } catch (ParserConfigurationException e) {
            throw new AssertionError(e);
        }
    }


}
