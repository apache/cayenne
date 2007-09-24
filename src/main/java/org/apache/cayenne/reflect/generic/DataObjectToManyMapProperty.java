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
package org.apache.cayenne.reflect.generic;

import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.Fault;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyException;
import org.apache.cayenne.reflect.ToManyMapProperty;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
class DataObjectToManyMapProperty extends DataObjectToManyProperty implements
        ToManyMapProperty {

    private Expression mapKey;

    DataObjectToManyMapProperty(ObjRelationship relationship,
            ClassDescriptor targetDescriptor, Fault fault, Expression mapKey) {
        super(relationship, targetDescriptor, fault);
        this.mapKey = mapKey;
    }

    public void remapTarget(Object source, Object target) throws PropertyException {

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
