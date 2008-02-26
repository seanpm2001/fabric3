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
package org.fabric3.tests.function.callback.binding;

import org.osoa.sca.annotations.OneWay;
import org.osoa.sca.annotations.Reference;
import org.osoa.sca.annotations.Service;

import org.fabric3.tests.function.callback.common.CallbackData;

/**
 * @version $Revision$ $Date$
 */
@Service(interfaces = {ClientService.class, CallbackService.class})
public class ClientServiceImpl implements ClientService, CallbackService {

    @Reference
    protected ForwardService forwardService;

    public void invoke(CallbackData data) {
        forwardService.invoke(data);
    }

    @OneWay
    public void onCallback(CallbackData data) {
        data.callback();
        data.getLatch().countDown();
    }
}