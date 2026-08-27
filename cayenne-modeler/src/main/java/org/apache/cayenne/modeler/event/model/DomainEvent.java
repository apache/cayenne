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

package org.apache.cayenne.modeler.event.model;

import org.apache.cayenne.configuration.DataChannelDescriptor;

/**
 * Represents events resulted from DataDomain changes in CayenneModeler.
 */
public class DomainEvent extends ModelEvent {

    private final DataChannelDescriptor domain;

    public static DomainEvent ofAdd(Object src, DataChannelDescriptor domain) {
        return new DomainEvent(src, domain, Type.ADD, null);
    }

    public static DomainEvent ofChange(Object src, DataChannelDescriptor domain) {
        return new DomainEvent(src, domain, Type.CHANGE, null);
    }

    public static DomainEvent ofChange(Object src, DataChannelDescriptor domain, String oldName) {
        return new DomainEvent(src, domain, Type.CHANGE, oldName);
    }

    public static DomainEvent ofRemove(Object src, DataChannelDescriptor domain) {
        return new DomainEvent(src, domain, Type.REMOVE, null);
    }

    private DomainEvent(Object src, DataChannelDescriptor domain, Type type, String oldName) {
        super(src, type, oldName);
        this.domain = domain;
    }

    public DataChannelDescriptor getDomain() {
        return domain;
    }

    @Override
    public String getNewName() {
        return (domain != null) ? domain.getName() : null;
    }
}
