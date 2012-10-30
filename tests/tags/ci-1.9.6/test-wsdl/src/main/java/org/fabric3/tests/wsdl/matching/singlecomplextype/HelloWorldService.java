package org.fabric3.tests.wsdl.matching.singlecomplextype;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.7-hudson-48-
 * Generated source version: 2.1
 * 
 */
@WebServiceClient(name = "HelloWorldService", targetNamespace = "urn:helloworld:sct", wsdlLocation = "file:/C:/F3Development/test/test-wsdl-matching/wsdl/SingleComplexType.wsdl")
public class HelloWorldService
    extends Service
{

    private final static URL HELLOWORLDSERVICE_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(org.fabric3.tests.wsdl.matching.singlecomplextype.HelloWorldService.class.getName());

    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = org.fabric3.tests.wsdl.matching.singlecomplextype.HelloWorldService.class.getResource(".");
            url = new URL(baseUrl, "file:/C:/F3Development/test/test-wsdl-matching/wsdl/SingleComplexType.wsdl");
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: 'file:/C:/F3Development/test/test-wsdl-matching/wsdl/SingleComplexType.wsdl', retrying as a local file");
            logger.warning(e.getMessage());
        }
        HELLOWORLDSERVICE_WSDL_LOCATION = url;
    }

    public HelloWorldService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public HelloWorldService() {
        super(HELLOWORLDSERVICE_WSDL_LOCATION, new QName("urn:helloworld", "HelloWorldService"));
    }

    /**
     * 
     * @return
     *     returns HelloWorldPortType
     */
    @WebEndpoint(name = "HelloBPELPort")
    public HelloWorldPortType getHelloBPELPort() {
        return super.getPort(new QName("urn:helloworld", "HelloBPELPort"), HelloWorldPortType.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns HelloWorldPortType
     */
    @WebEndpoint(name = "HelloBPELPort")
    public HelloWorldPortType getHelloBPELPort(WebServiceFeature... features) {
        return super.getPort(new QName("urn:helloworld", "HelloBPELPort"), HelloWorldPortType.class, features);
    }

}
