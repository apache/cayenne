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
package org.apache.cayenne.jpa.reflect;

import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyException;
import org.apache.cayenne.reflect.ToManyMapProperty;
import org.apache.cayenne.util.PersistentObjectMap;

class JpaMapProperty extends JpaToManyProperty implements ToManyMapProperty {
    
    private Expression mapKey;

    JpaMapProperty(ClassDescriptor owner, ClassDescriptor targetDescriptor,
            Accessor accessor, String reverseName) {
        super(owner, targetDescriptor, accessor, reverseName);
        this.mapKey = getRelationship().getMapKeyExpression();
    }

    @Override
    protected ValueHolder createValueHolder(Persistent relationshipOwner) {
        return new PersistentObjectMap(relationshipOwner, getName(), getRelationship()
                .getMapKeyExpression());
    }

    public void remapTarget(Object source, Object target) throws PropertyException {
        // TODO: andrus, 9/24/2007 - this is an clone of the remapTarget method in
        // DataObjectToManyMapProperty that is a part of a different inheritance
        // hierarchy... need to reconcile that somehow...

        if (target == null) {
            throw new NullPointerException("Null target");
        }

        Map map = (Map) readProperty(source);

        Object newKey = mapKey.evaluate(target);
        Object currentValue = map.get(newKey);

        if (currentValue == target) {
            // nothing to do
            return;
        }
        // else - do not check for conflicts here (i.e. another object mapped for the same
        // key), as we have no control of the order in which this method is called, so
        // another object may be remapped later by the caller

        // must do a slow map scan to ensure the object is not mapped under a different
        // key...
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            if (e.getValue() == target) {
                it.remove();
                break;
            }
        }

        map.put(newKey, target);
    }
}
