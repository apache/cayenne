/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.art.oneway;

import org.apache.cayenne.access.event.DataContextEvent;
import org.apache.cayenne.access.event.DataObjectTransactionEventListener;

public class Artist extends org.apache.art.oneway.auto._Artist implements DataObjectTransactionEventListener {
    private boolean _receivedWillCommit = false;
    private boolean _receivedDidCommit = false;

    protected String someOtherProperty;
    protected Object someOtherObjectProperty;

    public Artist() {
        super();
    }

    public void didCommit(DataContextEvent event) {
        _receivedDidCommit = true;
    }

    public void willCommit(DataContextEvent event) {
        _receivedWillCommit = true;
    }

    public boolean receivedDidCommit() {
        return _receivedDidCommit;
    }

    public boolean receivedWillCommit() {
        return _receivedWillCommit;
    }

    public void resetEvents() {
        _receivedWillCommit = false;
        _receivedDidCommit = false;
    }

    public String getSomeOtherProperty() {
        return someOtherProperty;
    }

    public void setSomeOtherProperty(String string) {
        someOtherProperty = string;
    }

    public Object getSomeOtherObjectProperty() {
        return someOtherObjectProperty;
    }

    public void setSomeOtherObjectProperty(Object object) {
        someOtherObjectProperty = object;
    }

}
