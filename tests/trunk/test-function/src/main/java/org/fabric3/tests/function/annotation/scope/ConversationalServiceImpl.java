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

package org.fabric3.tests.function.annotation.scope;

import org.fabric3.api.annotation.scope.Scopes;
import org.osoa.sca.annotations.Scope;

@Scope(Scopes.CONVERSATION)
public class ConversationalServiceImpl implements ConversationalService {

    private int value;
    
    public int getValue() {
        return value;
    }

    public void incrementValue() {
        value++;        
    }

}
