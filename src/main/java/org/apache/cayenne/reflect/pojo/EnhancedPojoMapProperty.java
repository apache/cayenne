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
package org.apache.cayenne.reflect.pojo;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyException;
import org.apache.cayenne.reflect.ToManyMapProperty;
import org.apache.cayenne.util.PersistentObjectMap;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
class EnhancedPojoMapProperty extends EnhancedPojoToManyProperty implements
        ToManyMapProperty {

    private Accessor mapKeyAccessor;

    EnhancedPojoMapProperty(ClassDescriptor owner, ClassDescriptor targetDescriptor,
            Accessor accessor, String reverseName, Accessor mapKeyAccessor) {
        super(owner, targetDescriptor, accessor, reverseName);
        this.mapKeyAccessor = mapKeyAccessor;
    }

    protected ValueHolder createValueHolder(Persistent relationshipOwner) {
        return new PersistentObjectMap(relationshipOwner, getName(), mapKeyAccessor);
    }

    public Object getMapKey(Object target) throws PropertyException {
        return mapKeyAccessor.getValue(target);
    }
}
