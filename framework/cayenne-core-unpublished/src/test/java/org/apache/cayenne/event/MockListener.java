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

package org.apache.cayenne.event;

import java.util.EventObject;

/**
 * A test event listener that reacts to an event by registering another listener.
 * 
 */
public class MockListener {

    public static final EventSubject mockSubject = EventSubject.getSubject(
            MockListener.class,
            "mock");
    
    protected EventManager manager;
    protected Object sender;

    public MockListener(EventManager manager) {
        this(manager, null);
    }
    
    public MockListener(EventManager manager, Object sender) {
        this.manager = manager;
        this.sender = sender;
    }

    public void processEvent(EventObject object) {
        manager.addListener(
                new MockListener(manager),
                "processEvent",
                EventObject.class,
                mockSubject, sender);
    }
}
