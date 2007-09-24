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

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.Persistent;

/**
 * A stateless read-only accessor of the map key value that is based on the Persistent
 * object single-column id.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public final class SingleColumnIdMapKeyAccessor implements Accessor {

    public static final Accessor SHARED_ACCESSOR = new SingleColumnIdMapKeyAccessor();

    public String getName() {
        return "SingleColumnIdMapKeyAccessor";
    }

    public Object getValue(Object object) throws PropertyException {
        if (object instanceof Persistent) {
            return DataObjectUtils.pkForObject((Persistent) object);
        }
        else {
            throw new IllegalArgumentException("Object must be Persistent: " + object);
        }
    }

    public void setValue(Object object, Object newValue) throws PropertyException {
        throw new UnsupportedOperationException("Setting map key is not supported");
    }
}
