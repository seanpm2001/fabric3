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
package org.fabric3.groovy.scdl;

import javax.xml.namespace.QName;

import org.fabric3.pojo.scdl.PojoComponentType;
import org.fabric3.model.type.component.Implementation;

/**
 * A component implemented in Groovy. The implementation can be a script in source or compiled form.
 *
 * @version $Rev$ $Date$
 */
public class GroovyImplementation extends Implementation<PojoComponentType> {
    private static final long serialVersionUID = -8092204063300139457L;
    public static final QName IMPLEMENTATION_GROOVY = new QName("http://www.fabric3.org/xmlns/groovy/1.0", "groovy");

    private String scriptName;
    private String className;

    public GroovyImplementation() {
    }

    public GroovyImplementation(String scriptName, String className) {
        this.scriptName = scriptName;
        this.className = className;
    }

    public GroovyImplementation(String scriptName, String className, PojoComponentType componentType) {
        super(componentType);
        this.scriptName = scriptName;
        this.className = className;
    }

    public QName getType() {
        return IMPLEMENTATION_GROOVY;
    }

    /**
     * Returns the name of a file containing the script source.
     *
     * @return the name of a file containing the script source
     */
    public String getScriptName() {
        return scriptName;
    }

    /**
     * Sets the name of a file containing the script source.
     *
     * @param scriptName the name of a file containing the script source
     */
    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Returns the name of a compiled Groovy class.
     *
     * @return the name of a compiled Groovy class
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the name of a compiled Groovy class.
     *
     * @param className the name of a compiled Groovy class
     */
    public void setClassName(String className) {
        this.className = className;
    }
}
