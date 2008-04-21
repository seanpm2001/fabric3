/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
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
package org.fabric3.runtime.webapp.smoketest;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osoa.sca.ComponentContext;

/**
 * @version $Rev$ $Date$
 */
public class TestServlet extends HttpServlet {
    private static final long serialVersionUID = 7698155043124726164L;

    private ServletContext servletContext;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        servletContext = config.getServletContext();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String testName = request.getParameter("test");
        if (testName == null || testName.length() == 0) {
            response.sendError(500, "No test specified");
            return;
        }
        // verify the component context was bound to the session
        HttpSession session = request.getSession();
        ComponentContext context = (ComponentContext) session.getAttribute("org.osoa.sca.ComponentContext");
        TestService test = context.getService(TestService.class, testName);
        if (test == null) {
            response.sendError(500, "Unknown test: " + testName);
            return;
        }
        // verify the reference was bound to the servlet context as it is non-conversational
        test = (TestService) servletContext.getAttribute(testName);
        if (test == null) {
            response.sendError(500, "Unknown test: " + testName);
            return;
        }

        test.service(request, response, servletContext);

        CounterService counter = context.getService(CounterService.class, "counter");
        counter.increment();
        if (counter.getCount() != 1) {
            response.sendError(500, "Counter expected to be 1");
            return;
        }
        counter.end();
        if (counter.getCount() != 0) {
            response.sendError(500, "Counter expected to be 0");
            return;
        }
        CounterService sessionCounter = (CounterService) request.getSession().getAttribute("counter");
        if (sessionCounter.getCount() != 0) {
            response.sendError(500, "Session counter expected to be 0");
            return;
        }

    }
}
