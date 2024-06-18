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

import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.Entity;

/** 
 * Represents events resulted from Attribute changes 
 * in CayenneModeler. This event is used for both ObjAttributes
 * and DbAttributes.
 * 
 */
public class AttributeEvent extends EntityEvent {
    protected Attribute<?,?,?> attribute;

    /** Creates a Attribute change event. */
    public AttributeEvent(Object src, Attribute<?,?,?> attr, Entity<?,?,?> entity) {
        super(src, entity);
        setAttribute(attr);
    }

    /** Creates a Attribute event of a specified type. */
    public AttributeEvent(Object src, Attribute<?,?,?> attr, Entity<?,?,?> entity, int id) {
        this(src, attr, entity);
        setId(id);
    }

    /** Creates a Attribute name change event.*/
    public AttributeEvent(Object src, Attribute<?,?,?> attr, Entity<?,?,?> entity, String oldName) {

        this(src, attr, entity);
        setOldName(oldName);
    }

    /** Get attribute (obj or db). */
    public Attribute<?,?,?> getAttribute() {
        return attribute;
    }

    /**
     * Sets the attribute.
     * @param attribute The attribute to set
     */
    public void setAttribute(Attribute<?,?,?> attribute) {
        this.attribute = attribute;
    }

    @Override
    public String getNewName() {
        return (attribute != null) ? attribute.getName() : null;
    }
}
