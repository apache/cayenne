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
package org.apache.cayenne.reflect;

import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;

/**
 * A stateless read-only accessor of the map key value that is based on the Persistent
 * object id. For single-column ID's the accessor returns a single value (e.g. an
 * Integer). For multi-column ID's it returns the ObjectId.
 * 
 * @since 3.0
 */
public class IdMapKeyAccessor implements Accessor {

    public static final Accessor SHARED_ACCESSOR = new IdMapKeyAccessor();

    public String getName() {
        return "IdMapKeyAccessor";
    }

    public Object getValue(Object object) throws PropertyException {
        if (object instanceof Persistent) {
            ObjectId id = ((Persistent) object).getObjectId();

            if (id.isTemporary()) {
                return id;
            }

            Map<?, ?> map = id.getIdSnapshot();
            if (map.size() == 1) {
                Map.Entry<?, ?> pkEntry = map.entrySet().iterator().next();
                return pkEntry.getValue();
            }

            return id;
        }
        else {
            throw new IllegalArgumentException("Object must be Persistent: " + object);
        }
    }

    public void setValue(Object object, Object newValue) throws PropertyException {
        throw new UnsupportedOperationException("Setting map key is not supported");
    }
}
