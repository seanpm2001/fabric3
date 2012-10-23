/*
 * Copyright (c) 2010 Metaform Systems
 *
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
package org.fabric3.samples.bigbank.api.event;

import java.io.Serializable;

/**
 * The root event type.
 *
 * @version $Rev$ $Date$
 */
public abstract class ApplicationEvent implements Serializable {
    private static final long serialVersionUID = 7550992408332724548L;
    private long loanId;
    private long timestamp;

    protected ApplicationEvent() {
    }

    public ApplicationEvent(long loanId) {
        this(loanId, System.currentTimeMillis());
    }

    public ApplicationEvent(long loanId, long timestamp) {
        this.loanId = loanId;
        this.timestamp = timestamp;
    }

    public long getLoanId() {
        return loanId;
    }


    public long getTimestamp() {
        return timestamp;
    }

    protected void setLoanId(long loanId) {
        this.loanId = loanId;
    }
}
