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

import org.apache.cayenne.map.Embeddable;

public class EmbeddableEvent extends ModelEvent {

    private final Embeddable embeddable;

    public static EmbeddableEvent ofAdd(Object source, Embeddable embeddable) {
        return new EmbeddableEvent(source, embeddable, Type.ADD, null);
    }

    public static EmbeddableEvent ofChange(Object source, Embeddable embeddable) {
        return new EmbeddableEvent(source, embeddable, Type.CHANGE, null);
    }

    public static EmbeddableEvent ofChange(Object source, Embeddable embeddable, String oldClassName) {
        return new EmbeddableEvent(source, embeddable, Type.CHANGE, oldClassName);
    }

    public static EmbeddableEvent ofRemove(Object source, Embeddable embeddable) {
        return new EmbeddableEvent(source, embeddable, Type.REMOVE, null);
    }

    private EmbeddableEvent(Object source, Embeddable embeddable, Type type, String oldName) {
        super(source, type, oldName);
        this.embeddable = embeddable;
    }

    public Embeddable getEmbeddable() {
        return embeddable;
    }

    @Override
    public String getNewName() {
        return (embeddable != null) ? embeddable.getClassName() : null;
    }
}
