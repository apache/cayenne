/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/


package org.apache.cayenne.event;

import java.util.Collections;
import java.util.EventObject;
import java.util.Map;

/**
 * Common superclass for events passed from the EventManager to Listeners; encapsulates
 * optional event information.
 * 
 */
public class CayenneEvent extends EventObject {

    protected Map info;
    protected transient Object postedBy;
    protected EventSubject subject;

    public CayenneEvent(Object source) {
        this(source, null);
    }

    public CayenneEvent(Object source, Map info) {
        this(source, source, info);
    }

    /**
     * Creates CayenneEvent with possibly different event source and poster. This may be
     * the case when an event is resent by listener.
     * 
     * @since 1.1
     */
    public CayenneEvent(Object source, Object postedBy, Map info) {
        super(source);
        this.postedBy = postedBy;
        this.info = info;
    }

    public Map getInfo() {
        return info != null ? info : Collections.EMPTY_MAP;
    }

    /**
     * @since 1.2
     */
    public EventSubject getSubject() {
        return subject;
    }

    /**
     * @since 1.2
     */
    public void setSubject(EventSubject subject) {
        this.subject = subject;
    }

    /**
     * Used when deserializing remote events.
     */
    void setSource(Object source) {
        super.source = source;
    }

    /**
     * Returns an object that posted this event. It may be different from event source, if
     * event is reposted multiple times.
     */
    public Object getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(Object postedBy) {
        this.postedBy = postedBy;
    }
}
