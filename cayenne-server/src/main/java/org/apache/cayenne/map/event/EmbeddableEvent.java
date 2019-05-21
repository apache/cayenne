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
package org.apache.cayenne.map.event;

import org.apache.cayenne.map.Embeddable;


public class EmbeddableEvent extends MapEvent {

    protected Embeddable embeddable;
    
    public EmbeddableEvent(Object source, Embeddable embeddable) {
        super(source);
        setEmbeddable(embeddable);
    }

     public EmbeddableEvent(Object src, Embeddable embeddable2, int id) {
         this(src, embeddable2);
         setId(id);
    }

     public EmbeddableEvent(Object src, Embeddable embeddable2, String oldClassName) {
         this(src, embeddable2);
         setOldName(oldClassName);
    }

     
    public void setEmbeddable(Embeddable embeddable) {
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
