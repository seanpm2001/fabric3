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
package tests.rs;

import javax.ws.rs.POST;
import org.osoa.sca.annotations.Property;
import org.osoa.sca.annotations.Reference;
import org.osoa.sca.annotations.Service;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.Path;

/**
 * @version $Rev$ $Date$
 */
@Service(EchoService.class)
@Path("/Hello")
public class EchoResource implements EchoService {

    public 
    @Reference
    EchoService service;
    public 
    @Property
    String message;

    public EchoResource() {
        System.out.println("Hello");
    }

    @POST
    @ProduceMime("text/plain")
    public String hello(String name) {
        return message + " " + service.hello(name);
    }

    @POST
    @ProduceMime("application/entity")
    public Entity hello(Entity entity) {
        entity.setValue(message + " " + service.hello(entity).getValue());
        return entity;
    }
}
