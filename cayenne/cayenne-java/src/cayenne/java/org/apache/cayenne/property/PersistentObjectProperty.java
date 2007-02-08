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

package org.apache.cayenne.property;

import org.apache.cayenne.Fault;
import org.apache.cayenne.Persistent;

/**
 * An ArcProperty for accessing to-one relationships.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class PersistentObjectProperty extends AbstractSingleObjectArcProperty {

    public PersistentObjectProperty(ClassDescriptor owner,
            ClassDescriptor targetDescriptor, PropertyAccessor accessor,
            String reverseName) {
        super(owner, targetDescriptor, accessor, reverseName);
    }

    public boolean isFault(Object object) {
        Object target = accessor.readPropertyDirectly(object);
        return target instanceof Fault;
    }

    public Object readProperty(Object object) throws PropertyAccessException {
        Object value = super.readProperty(object);

        if (value instanceof Fault) {
            Object resolved = ((Fault) value)
                    .resolveFault((Persistent) object, getName());
            writePropertyDirectly(object, value, resolved);
            value = resolved;
        }

        return value;
    }

    /**
     * Copies a property value that is itself a persistent object from one object to
     * another. If the new value is fault, fault will be copied to the target.
     */
    public void shallowMerge(Object from, Object to) throws PropertyAccessException {

        Object fromValue = accessor.readPropertyDirectly(from);

        if (fromValue == null) {
            writePropertyDirectly(to, accessor.readPropertyDirectly(to), null);
        }
        else {
            writePropertyDirectly(to, accessor.readPropertyDirectly(to), Fault
                    .getToOneFault());
        }
    }
}
