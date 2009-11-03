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

package org.fabric3.binding.ws.metro.generator;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;

import org.osoa.sca.annotations.Reference;
import org.w3c.dom.Element;

import org.fabric3.binding.ws.metro.provision.ConnectionConfiguration;
import org.fabric3.binding.ws.metro.provision.MetroSourceDefinition;
import org.fabric3.binding.ws.metro.provision.MetroTargetDefinition;
import org.fabric3.binding.ws.metro.provision.MetroWsdlSourceDefinition;
import org.fabric3.binding.ws.metro.provision.PolicyExpressionMapping;
import org.fabric3.binding.ws.metro.provision.ReferenceEndpointDefinition;
import org.fabric3.binding.ws.metro.provision.SecurityConfiguration;
import org.fabric3.binding.ws.metro.provision.ServiceEndpointDefinition;
import org.fabric3.binding.ws.model.WsBindingDefinition;
import org.fabric3.model.type.definitions.Intent;
import org.fabric3.model.type.definitions.PolicySet;
import org.fabric3.spi.generator.GenerationException;
import org.fabric3.spi.model.instance.LogicalBinding;
import org.fabric3.spi.policy.EffectivePolicy;
import org.fabric3.wsdl.model.WsdlServiceContract;

/**
 * Generates MetroSourceDefinitions and MetroTargetDefinitions for a WsdlServiceContact.
 *
 * @version $Rev$ $Date$
 */
public class WsdlGeneratorDelegate implements MetroGeneratorDelegate<WsdlServiceContract> {
    private WsdlResolver wsdlResolver;
    private EndpointResolver endpointResolver;
    private WSDLFactory wsdlFactory;

    public WsdlGeneratorDelegate(@Reference WsdlResolver wsdlResolver, @Reference EndpointResolver endpointResolver) throws WSDLException {
        this.wsdlResolver = wsdlResolver;
        this.endpointResolver = endpointResolver;
        wsdlFactory = WSDLFactory.newInstance();
    }

    public MetroSourceDefinition generateWireSource(LogicalBinding<WsBindingDefinition> binding, WsdlServiceContract contract, EffectivePolicy policy)
            throws GenerationException {
        URI targetUri = binding.getDefinition().getTargetUri();
        Definition wsdl;
        URL wsdlLocation = getWsdlLocation(binding);
        if (wsdlLocation != null) {
            wsdl = wsdlResolver.parseWsdl(wsdlLocation);
        } else {
            URI contributionUri = binding.getParent().getParent().getDefinition().getContributionUri();
            QName wsdlName = contract.getWsdlQName();
            wsdl = wsdlResolver.resolveWsdl(contributionUri, wsdlName);
        }

        ServiceEndpointDefinition endpointDefinition;
        if (targetUri != null) {
            String wsdlElementString = binding.getDefinition().getWsdlElement();
            if (wsdlElementString != null) {
                WsdlElement wsdlElement = GenerationHelper.parseWsdlElement(wsdlElementString);
                endpointDefinition = endpointResolver.resolveServiceEndpoint(wsdlElement, wsdl, targetUri);
            } else {
                throw new UnsupportedOperationException("wsdlElement must be specified for service:" + binding.getParent().getUri());
                // FIXME update wsdl4j model - clone and generate service and port definitions. Doc literal, rpc
                // TODO look to move policy generation here
                // String targetNamespace = UriHelper.getDefragmentedNameAsString(binding.getParent().getUri());
                // String localName = binding.getParent().getUri().getFragment();
                // QName serviceName = new QName(targetNamespace, localName);
                // QName portName = new QName(targetNamespace, localName + "Port");
                // endpointDefinition = new ServiceEndpointDefinition(serviceName, portName, targetUri);
            }
        } else {
            // no target uri specified, check wsdlElement
            String wsdlElementString = binding.getDefinition().getWsdlElement();
            if (wsdlElementString == null){
                URI bindableUri = binding.getParent().getUri();
                throw new GenerationException("Either a uri or wsdlElement must be specified for the web service binding on " + bindableUri);
            }
            WsdlElement wsdlElement = GenerationHelper.parseWsdlElement(wsdlElementString);
            endpointDefinition = endpointResolver.resolveServiceEndpoint(wsdlElement, wsdl, targetUri);
        }

        // handle endpoint-level intents provided by Metro
        List<QName> intentNames = new ArrayList<QName>();
        Set<Intent> endpointIntents = policy.getEndpointIntents();
        for (Intent intent : endpointIntents) {
            intentNames.add(intent.getName());
        }

        // handle endpoint-level policies
        List<Element> policyExpressions = new ArrayList<Element>();
        for (PolicySet policySet : policy.getEndpointPolicySets()) {
            policyExpressions.add(policySet.getExpression());
        }

        String serializedWsdl = convertToString(wsdl);

        // Note operation level provided intents are not currently supported. Intents are mapped to JAX-WS features, which are per endpoint.
        List<PolicyExpressionMapping> mappings = GenerationHelper.createMappings(policy);
        return new MetroWsdlSourceDefinition(endpointDefinition, serializedWsdl, intentNames, policyExpressions, mappings);
    }


    public MetroTargetDefinition generateWireTarget(LogicalBinding<WsBindingDefinition> binding,
                                                    WsdlServiceContract contract,
                                                    EffectivePolicy policy) throws GenerationException {
        WsBindingDefinition definition = binding.getDefinition();
        URI targetUri = definition.getTargetUri();
        ReferenceEndpointDefinition endpointDefinition;
        URL wsdlLocation = getWsdlLocation(binding);
        Definition wsdl;
        if (wsdlLocation != null) {
            wsdl = wsdlResolver.parseWsdl(wsdlLocation);
        } else {
            URI contributionUri = binding.getParent().getParent().getDefinition().getContributionUri();
            QName wsdlName = contract.getWsdlQName();
            wsdl = wsdlResolver.resolveWsdl(contributionUri, wsdlName);
        }
        if (targetUri != null) {
            try {
                // TODO get rid of need to decode
                URL url = new URL(URLDecoder.decode(targetUri.toASCIIString(), "UTF-8"));
                // FIXME null service name
                QName portTypeName = contract.getPortTypeQname();
                endpointDefinition = new ReferenceEndpointDefinition(null, false, portTypeName, portTypeName, url);
            } catch (MalformedURLException e) {
                throw new GenerationException(e);
            } catch (UnsupportedEncodingException e) {
                throw new GenerationException(e);
            }
        } else {
            // no target uri specified, check wsdlElement
            String wsdlElementString = binding.getDefinition().getWsdlElement();
            if (wsdlElementString == null){
                URI bindableUri = binding.getParent().getUri();
                throw new GenerationException("Either a uri or wsdlElement must be specified for the web service binding on " + bindableUri);
            }
            WsdlElement wsdlElement = GenerationHelper.parseWsdlElement(wsdlElementString);
            endpointDefinition = endpointResolver.resolveReferenceEndpoint(wsdlElement, wsdl);
        }

        String interfaze = contract.getQualifiedInterfaceName();

        Set<Intent> endpointIntents = policy.getEndpointIntents();
        List<QName> intentNames = new ArrayList<QName>();
        for (Intent intent : endpointIntents) {
            intentNames.add(intent.getName());
        }

        // handle endpoint-level policies
        List<Element> policyExpressions = new ArrayList<Element>();
        for (PolicySet policySet : policy.getEndpointPolicySets()) {
            policyExpressions.add(policySet.getExpression());
        }

        // Note operation level provided intents are not currently supported. Intents are mapped to JAX-WS features, which are per endpoint.

        // map operation-level policies
        List<PolicyExpressionMapping> mappings = GenerationHelper.createMappings(policy);

        // obtain security information
        SecurityConfiguration securityConfiguration = GenerationHelper.createSecurityConfiguration(definition);

        // obtain connection information
        ConnectionConfiguration connectionConfiguration = GenerationHelper.createConnectionConfiguration(definition);

        return new MetroTargetDefinition(endpointDefinition,
                                         wsdlLocation,
                                         interfaze,
                                         intentNames,
                                         policyExpressions,
                                         mappings,
                                         securityConfiguration,
                                         connectionConfiguration);
    }

    /**
     * Returns the WSDL location if one is defined in the binding configuration or null.
     *
     * @param binding the logical binding
     * @return the WSDL location or null
     * @throws GenerationException if the WSDL location is invalid
     */
    private URL getWsdlLocation(LogicalBinding<WsBindingDefinition> binding) throws GenerationException {
        WsBindingDefinition definition = binding.getDefinition();
        try {
            String location = definition.getWsdlLocation();
            if (location != null) {
                return new URL(location);
            }
        } catch (MalformedURLException e) {
            throw new GenerationException(e);
        }
        return null;
    }

    /**
     * Serializes the contents of a parsed WSDL as a string.
     *
     * @param wsdl the WSDL
     * @return the serialized WSDL
     * @throws GenerationException if an error occurs reading the URL
     */
    private String convertToString(Definition wsdl) throws GenerationException {
        try {
            WSDLWriter writer = wsdlFactory.newWSDLWriter();
            StringWriter stringWriter = new StringWriter();
            writer.writeWSDL(wsdl, stringWriter);
            return stringWriter.toString();
        } catch (WSDLException e) {
            throw new GenerationException(e);
        }
    }


}