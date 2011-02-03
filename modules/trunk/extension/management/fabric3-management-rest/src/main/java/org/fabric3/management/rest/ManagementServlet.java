/*
 * Fabric3
 * Copyright (c) 2009-2011 Metaform Systems
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
package org.fabric3.management.rest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fabric3.spi.invocation.WorkContext;
import org.fabric3.spi.invocation.WorkContextTunnel;
import org.fabric3.spi.objectfactory.ObjectCreationException;
import org.fabric3.spi.objectfactory.ObjectFactory;

/**
 * @version $Rev$ $Date$
 */
public class ManagementServlet extends HttpServlet {
    private static final long serialVersionUID = 5554150494161533656L;

    public final static String APPLICATION_JSON = "application/json";
    public final static String APPLICATION_XML = "application/xml";

    private Map<String, ManagedArtifactMapping> getMappings = new ConcurrentHashMap<String, ManagedArtifactMapping>();
    private Map<String, ManagedArtifactMapping> postMappings = new ConcurrentHashMap<String, ManagedArtifactMapping>();
    private Map<String, ManagedArtifactMapping> putMappings = new ConcurrentHashMap<String, ManagedArtifactMapping>();
    private Map<String, ManagedArtifactMapping> deleteMappings = new ConcurrentHashMap<String, ManagedArtifactMapping>();

    /**
     * Registers a mapping, making the managed resource available via HTTP.
     *
     * @param mapping the mapping
     * @throws DuplicateArtifactNameException if a managed resource has already been registered for the path
     */
    public void register(ManagedArtifactMapping mapping) throws DuplicateArtifactNameException {
        Verb verb = mapping.getVerb();
        if (verb == Verb.GET) {
            register(mapping, getMappings);
        } else if (verb == Verb.POST) {
            register(mapping, postMappings);
        } else if (verb == Verb.PUT) {
            register(mapping, putMappings);
        } else if (verb == Verb.DELETE) {
            register(mapping, deleteMappings);
        }
    }

    /**
     * Removes a mapping and the associated managed resource.
     *
     * @param mapping the mapping
     */
    public void unRegister(ManagedArtifactMapping mapping) {
        String path = mapping.getPath();
        Verb verb = mapping.getVerb();
        if (verb == Verb.GET) {
            getMappings.remove(path);
        } else if (verb == Verb.POST) {
            postMappings.remove(path);
        } else if (verb == Verb.PUT) {
            putMappings.remove(path);
        } else if (verb == Verb.DELETE) {
            deleteMappings.remove(path);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handle(Verb.GET, request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handle(Verb.POST, request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handle(Verb.DELETE, request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handle(Verb.PUT, request, response);
    }

    private void register(ManagedArtifactMapping mapping, Map<String, ManagedArtifactMapping> mappings) throws DuplicateArtifactNameException {
        String path = mapping.getPath();
        if (mappings.containsKey(path)) {
            throw new DuplicateArtifactNameException("Artifact already registered at: " + path);
        }
        mappings.put(path, mapping);
    }

    private void handle(Verb verb, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        ManagedArtifactMapping mapping;
        if (verb == Verb.GET) {
            mapping = getMappings.get(pathInfo);
        } else if (verb == Verb.POST) {
            mapping = postMappings.get(pathInfo);
        } else if (verb == Verb.PUT) {
            mapping = putMappings.get(pathInfo);
        } else {
            mapping = deleteMappings.get(pathInfo);
        }

        if (mapping == null) {
            response.setStatus(404);
            response.getWriter().print("Management resource not found");
            return;
        }
        if (!securityCheck(mapping, response)) {
            return;
        }
        Object[] params = null;
        if (mapping.getMethod().getParameterTypes().length > 0) {
            // avoid derserialization if the method does not take parameters
            params = deserialize(request);
        }
        Object ret = invoke(mapping, params);
        if (ret != null) {
            serialize(ret, request, response);
        }
    }

    private boolean securityCheck(ManagedArtifactMapping mapping, HttpServletResponse response) {
        return true;
    }

    private Object[] deserialize(HttpServletRequest request) {
        if (APPLICATION_JSON.equals(request.getContentType())) {

        } else if (APPLICATION_XML.equals(request.getContentType())) {

        } else {
            // TODO throw illegal content type
        }
        return new Object[0];
    }

    private void serialize(Object payload, HttpServletRequest request, HttpServletResponse response) {
        // TODO
    }

    private Object invoke(ManagedArtifactMapping mapping, Object[] params) {
        WorkContext workContext = new WorkContext();
        WorkContext old = WorkContextTunnel.setThreadWorkContext(workContext);
        try {
            Object instance = mapping.getInstance();
            if (instance instanceof ObjectFactory) {
                instance = ((ObjectFactory) instance).getInstance();
            }
            return mapping.getMethod().invoke(instance, params);
        } catch (IllegalAccessException e) {
            // TODO return error
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            // TODO return error
            throw new AssertionError(e);
        } catch (ObjectCreationException e) {
            // TODO return error
            throw new AssertionError(e);
        } finally {
            WorkContextTunnel.setThreadWorkContext(old);
        }
    }


}
