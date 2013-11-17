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
package org.apache.cayenne.map.event;

import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;

public class EmbeddableAttributeEvent extends EmbeddableEvent {

    protected EmbeddableAttribute embeddableAttribute;

    public EmbeddableAttributeEvent(Object source, Embeddable embeddable,
            EmbeddableAttribute embeddableAttribute) {
        super(source, embeddable);
        setEmbeddableAttribute(embeddableAttribute);
    }

    public EmbeddableAttributeEvent(Object source, EmbeddableAttribute attrib,
            Embeddable embeddable, int id) {
       
        this(source, embeddable, attrib);
        setId(id);
    }

    @Override
    public String getNewName() {
        return (embeddableAttribute != null) ? embeddableAttribute.getName() : null;
    }

    public EmbeddableAttribute getEmbeddableAttribute() {
        return embeddableAttribute;
    }

    public void setEmbeddableAttribute(EmbeddableAttribute embeddableAttribute) {
        this.embeddableAttribute = embeddableAttribute;
    }

}
