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
package org.fabric3.fabric.runtime.bootstrap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Returns the packages that should be exported by the host contribution for the Java 6 platform.
 *
 * @version $Revision$ $Date$
 */
public final class Java6HostExports {
    private static final Map<String, String> HOST_EXPORTS;

    static {
        Map<String, String> hostMap = new HashMap<String, String>();

        hostMap.put("javax.accessibility", "0.0");
        hostMap.put("javax.activation", "0.0");
        hostMap.put("javax.activation", "1.1.0");
        hostMap.put("javax.activity", "0.0");
        hostMap.put("javax.crypto", "0.0");
        hostMap.put("javax.crypto.interfaces", "0.0");
        hostMap.put("javax.crypto.spec", "0.0");
        hostMap.put("javax.imageio", "0.0");
        hostMap.put("javax.imageio.event", "0.0");
        hostMap.put("javax.imageio.metadata", "0.0");
        hostMap.put("javax.imageio.plugins.bmp", "0.0");
        hostMap.put("javax.imageio.plugins.jpeg", "0.0");
        hostMap.put("javax.imageio.spi", "0.0");
        hostMap.put("javax.imageio.stream", "0.0");
        hostMap.put("javax.jws", "0.0");
        hostMap.put("javax.jws", "2.0");
        hostMap.put("javax.jws.soap", "0.0");
        hostMap.put("javax.jws.soap", "2.0");
        hostMap.put("javax.management", "0.0");
        hostMap.put("javax.management.loading", "0.0");
        hostMap.put("javax.management.modelmbean", "0.0");
        hostMap.put("javax.management.monitor", "0.0");
        hostMap.put("javax.management.openmbean", "0.0");
        hostMap.put("javax.management.relation", "0.0");
        hostMap.put("javax.management.remote", "0.0");
        hostMap.put("javax.management.remote.rmi", "0.0");
        hostMap.put("javax.management.timer", "0.0");
        hostMap.put("javax.naming", "0.0");
        hostMap.put("javax.naming.directory", "0.0");
        hostMap.put("javax.naming.event", "0.0");
        hostMap.put("javax.naming.ldap", "0.0");
        hostMap.put("javax.naming.spi", "0.0");
        hostMap.put("javax.net", "0.0");
        hostMap.put("javax.net.ssl", "0.0");
        hostMap.put("javax.print", "0.0");
        hostMap.put("javax.print.attribute", "0.0");
        hostMap.put("javax.print.attribute.standard", "0.0");
        hostMap.put("javax.print.event", "0.0");
        hostMap.put("javax.rmi", "0.0");
        hostMap.put("javax.rmi.CORBA", "0.0");
        hostMap.put("javax.rmi.ssl", "0.0");
        hostMap.put("javax.security.auth", "0.0");
        hostMap.put("javax.security.auth.callback", "0.0");
        hostMap.put("javax.security.auth.kerberos", "0.0");
        hostMap.put("javax.security.auth.login", "0.0");
        hostMap.put("javax.security.auth.spi", "0.0");
        hostMap.put("javax.security.auth.x500", "0.0");
        hostMap.put("javax.security.cert", "0.0");
        hostMap.put("javax.security.sasl", "0.0");
        hostMap.put("javax.sound.midi", "0.0");
        hostMap.put("javax.sound.midi.spi", "0.0");
        hostMap.put("javax.sound.sampled", "0.0");
        hostMap.put("javax.sound.sampled.spi", "0.0");
        hostMap.put("javax.sql", "0.0");
        hostMap.put("javax.sql.rowset", "0.0");
        hostMap.put("javax.sql.rowset.serial", "0.0");
        hostMap.put("javax.sql.rowset.spi", "0.0");
        hostMap.put("javax.swing", "0.0");
        hostMap.put("javax.swing.border", "0.0");
        hostMap.put("javax.swing.colorchooser", "0.0");
        hostMap.put("javax.swing.event", "0.0");
        hostMap.put("javax.swing.filechooser", "0.0");
        hostMap.put("javax.swing.plaf", "0.0");
        hostMap.put("javax.swing.plaf.basic", "0.0");
        hostMap.put("javax.swing.plaf.metal", "0.0");
        hostMap.put("javax.swing.plaf.multi", "0.0");
        hostMap.put("javax.swing.plaf.synth", "0.0");
        hostMap.put("javax.swing.table", "0.0");
        hostMap.put("javax.swing.text", "0.0");
        hostMap.put("javax.swing.text.html", "0.0");
        hostMap.put("javax.swing.text.html.parser", "0.0");
        hostMap.put("javax.swing.text.rtf", "0.0");
        hostMap.put("javax.swing.tree", "0.0");
        hostMap.put("javax.swing.undo", "0.0");
        hostMap.put("javax.xml", "0.0");
        hostMap.put("javax.xml", "1.0.1");
        hostMap.put("javax.xml.bind", "0.0");
        hostMap.put("javax.xml.bind", "2.0");
        hostMap.put("javax.xml.bind.annotation", "0.0");
        hostMap.put("javax.xml.bind.annotation", "2.0");
        hostMap.put("javax.xml.bind.annotation.adapters", "0.0");
        hostMap.put("javax.xml.bind.annotation.adapters", "2.0");
        hostMap.put("javax.xml.bind.attachment", "0.0");
        hostMap.put("javax.xml.bind.attachment", "2.0");
        hostMap.put("javax.xml.bind.helpers", "0.0");
        hostMap.put("javax.xml.bind.helpers", "2.0");
        hostMap.put("javax.xml.bind.util", "0.0");
        hostMap.put("javax.xml.bind.util", "2.0");
        hostMap.put("javax.xml.datatype", "0.0");
        hostMap.put("javax.xml.namespace", "0.0");
        hostMap.put("javax.xml.parsers", "0.0");
        hostMap.put("javax.xml.soap", "0.0");
        hostMap.put("javax.xml.soap", "1.3.0");

        hostMap.put("javax.xml.stream", "0.0");
        hostMap.put("javax.xml.stream", "1.0.1");
        hostMap.put("javax.xml.stream.events", "0.0");
        hostMap.put("javax.xml.stream.events", "1.0.1");
        hostMap.put("javax.xml.stream.util", "0.0");
        hostMap.put("javax.xml.stream.util", "1.0.1");

        hostMap.put("javax.xml.transform", "0.0");
        hostMap.put("javax.xml.transform.dom", "0.0");
        hostMap.put("javax.xml.transform.sax", "0.0");
        hostMap.put("javax.xml.transform.stream", "0.0");
        hostMap.put("javax.xml.validation", "0.0");
        hostMap.put("javax.xml.ws", "0.0");
        hostMap.put("javax.xml.ws", "2.1.1");
        hostMap.put("javax.xml.ws.handler", "0.0");
        hostMap.put("javax.xml.ws.handler", "2.1.1");
        hostMap.put("javax.xml.ws.handler.soap", "0.0");
        hostMap.put("javax.xml.ws.handler.soap", "2.1.1");
        hostMap.put("javax.xml.ws.http", "0.0");
        hostMap.put("javax.xml.ws.http", "2.1.1");
        hostMap.put("javax.xml.ws.soap", "0.0");
        hostMap.put("javax.xml.ws.soap", "2.1.1");
        hostMap.put("javax.xml.ws.spi", "0.0");
        hostMap.put("javax.xml.ws.spi", "2.1.1");
        hostMap.put("javax.xml.xpath", "0.0");
        hostMap.put("org.ietf.jgss", "0.0");
        hostMap.put("org.omg.CORBA", "0.0");
        hostMap.put("org.omg.CORBA_2_3", "0.0");
        hostMap.put("org.omg.CORBA_2_3.portable", "0.0");
        hostMap.put("org.omg.CORBA.DynAnyPackage", "0.0");
        hostMap.put("org.omg.CORBA.ORBPackage", "0.0");
        hostMap.put("org.omg.CORBA.portable", "0.0");
        hostMap.put("org.omg.CORBA.TypeCodePackage", "0.0");
        hostMap.put("org.omg.CosNaming", "0.0");
        hostMap.put("org.omg.CosNaming.NamingContextExtPackage", "0.0");
        hostMap.put("org.omg.CosNaming.NamingContextPackage", "0.0");
        hostMap.put("org.omg.Dynamic", "0.0");
        hostMap.put("org.omg.DynamicAny", "0.0");
        hostMap.put("org.omg.DynamicAny.DynAnyFactoryPackage", "0.0");
        hostMap.put("org.omg.DynamicAny.DynAnyPackage", "0.0");
        hostMap.put("org.omg.IOP", "0.0");
        hostMap.put("org.omg.IOP.CodecFactoryPackage", "0.0");
        hostMap.put("org.omg.IOP.CodecPackage", "0.0");
        hostMap.put("org.omg.Messaging", "0.0");
        hostMap.put("org.omg.PortableInterceptor", "0.0");
        hostMap.put("org.omg.PortableInterceptor.ORBInitInfoPackage", "0.0");
        hostMap.put("org.omg.PortableServer", "0.0");
        hostMap.put("org.omg.PortableServer.CurrentPackage", "0.0");
        hostMap.put("org.omg.PortableServer.POAManagerPackage", "0.0");
        hostMap.put("org.omg.PortableServer.POAPackage", "0.0");
        hostMap.put("org.omg.PortableServer.portable", "0.0");
        hostMap.put("org.omg.PortableServer.ServantLocatorPackage", "0.0");
        hostMap.put("org.omg.SendingContext", "0.0");
        hostMap.put("org.omg.stub.java.rmi", "0.0");
        hostMap.put("org.w3c.dom", "0.0");
        hostMap.put("org.w3c.dom.bootstrap", "0.0");
        hostMap.put("org.w3c.dom.css", "0.0");
        hostMap.put("org.w3c.dom.events", "0.0");
        hostMap.put("org.w3c.dom.html", "0.0");
        hostMap.put("org.w3c.dom.ls", "0.0");
        hostMap.put("org.w3c.dom.ranges", "0.0");
        hostMap.put("org.w3c.dom.stylesheets", "0.0");
        hostMap.put("org.w3c.dom.traversal", "0.0");
        hostMap.put("org.w3c.dom.views ", "0.0");
        hostMap.put("org.xml.sax", "0.0");
        hostMap.put("org.xml.sax.ext", "0.0");
        hostMap.put("org.xml.sax.helpers", "0.0");
        hostMap.put("javax.annotation.processing", "0.0");
        hostMap.put("javax.lang.model", "0.0");
        hostMap.put("javax.lang.model.element", "0.0");
        hostMap.put("javax.lang.model.type", "0.0");
        hostMap.put("javax.lang.model.util", "0.0");
        hostMap.put("javax.script", "0.0");
        hostMap.put("javax.script", "1.1");
        hostMap.put("javax.tools", "0.0");
        hostMap.put("javax.xml.crypto", "0.0");
        hostMap.put("javax.xml.crypto", "1.0");
        hostMap.put("javax.xml.crypto.dom", "0.0");
        hostMap.put("javax.xml.crypto.dom", "1.0");
        hostMap.put("javax.xml.crypto.dsig", "0.0");
        hostMap.put("javax.xml.crypto.dsig", "1.0");
        hostMap.put("javax.xml.crypto.dsig.dom", "0.0");
        hostMap.put("javax.xml.crypto.dsig.dom", "1.0");
        hostMap.put("javax.xml.crypto.dsig.keyinfo", "0.0");
        hostMap.put("javax.xml.crypto.dsig.keyinfo", "1.0");
        hostMap.put("javax.xml.crypto.dsig.spec", "0.0");
        hostMap.put("javax.xml.crypto.dsig.spec", "1.0");

        // Commons annotations included
        hostMap.put("javax.annotation", "0.0");
        hostMap.put("javax.annotation", "1.0.0");
        hostMap.put("javax.annotation.security", "1.0.0");
        
        HOST_EXPORTS = Collections.unmodifiableMap(hostMap);

    }

    private Java6HostExports() {
    }

    public static Map<String, String> getExports() {
        return HOST_EXPORTS;
    }
}