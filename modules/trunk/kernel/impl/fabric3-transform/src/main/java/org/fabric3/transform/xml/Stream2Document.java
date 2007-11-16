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
package org.fabric3.transform.xml;

import static javax.xml.stream.XMLStreamConstants.CDATA;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.COMMENT;
import static javax.xml.stream.XMLStreamConstants.DTD;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.ENTITY_REFERENCE;
import static javax.xml.stream.XMLStreamConstants.PROCESSING_INSTRUCTION;
import static javax.xml.stream.XMLStreamConstants.SPACE;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.fabric3.scdl.DataType;
import org.fabric3.spi.model.type.JavaClass;
import org.fabric3.spi.transform.TransformContext;
import org.fabric3.transform.AbstractPullTransformer;
import org.fabric3.transform.TransformException;
import org.osoa.sca.annotations.EagerInit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * Pull transformer that will convert a Stax stream to a DOM representation. The 
 * transformer expects the cursor to be at the element from which the info set 
 * needs to transferred into the DOM tree.
 * 
 * @version $Revision$ $Date$
 */
@EagerInit
public class Stream2Document extends AbstractPullTransformer<XMLStreamReader, Document> {
    
    private static final JavaClass<Document> TARGET = new JavaClass<Document>(Document.class);
    private static final DocumentBuilderFactory FACTORY = DocumentBuilderFactory.newInstance();

    public DataType<?> getTargetType() {
        return TARGET;
    }

    public Document transform(XMLStreamReader reader, TransformContext context) throws Exception {
        
        if(reader.getEventType() != XMLStreamConstants.START_ELEMENT) {
            throw new TransformException("The stream needs to be at te start of an element");
        }
        
        DocumentBuilder builder = FACTORY.newDocumentBuilder();
        Document document = builder.newDocument();
        
        QName rootName = reader.getName();
        Element root = createElement(reader, document, rootName);
        
        document.appendChild(root);
        
        while (true) {
            
            int next = reader.next();
            switch (next) {
            case START_ELEMENT:
                
                QName childName = new QName(reader.getNamespaceURI(), reader.getLocalName());
                Element child = createElement(reader, document, childName);
                
                root.appendChild(child);
                root = child;
                
                break;
                
            case CHARACTERS:
            case CDATA:
                Text text = document.createTextNode(reader.getText());
                root.appendChild(text);
                break;
            case END_ELEMENT:
                if (rootName.equals(reader.getName())) {
                    return document;
                }
                root = (Element) root.getParentNode();
            case ENTITY_REFERENCE:
            case COMMENT:
            case SPACE:
            case PROCESSING_INSTRUCTION:
            case DTD:
                break;
            }
        }

    }

    /*
     * Creates the element and populates the namespace declarations and attributes.
     */
    private Element createElement(XMLStreamReader reader, Document document, QName rootName) {
        
        Element root = document.createElementNS(rootName.getNamespaceURI(), rootName.getLocalPart());
        
        // Handle namespace declarations
        for (int i = 0; i < reader.getNamespaceCount(); i++) {
            
            String prefix = reader.getNamespacePrefix(i);
            String uri = reader.getNamespaceURI(i);
            
            prefix = prefix == null ? "xmlns" : "xmlns:" + prefix; 
            
            root.setAttribute(prefix, uri);
            
        }
        
        // Handle attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            
            String attributeNs = reader.getAttributeNamespace(i);
            String localName = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            
            root.setAttributeNS(attributeNs, localName, value);
            
        }
        
        return root;
        
    }

}
