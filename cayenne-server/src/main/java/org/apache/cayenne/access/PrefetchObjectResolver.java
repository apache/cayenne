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
package org.apache.cayenne.access;

import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 3.2
 */
class PrefetchObjectResolver extends ObjectResolver {

    private Map<ObjectId, Persistent> seen;

    PrefetchObjectResolver(DataContext context, ClassDescriptor descriptor, boolean refresh,
            Map<ObjectId, Persistent> seen) {
        super(context, descriptor, refresh);
        this.seen = seen;
    }

    @Override
    Persistent objectFromDataRow(DataRow row, ObjectId anId, ClassDescriptor classDescriptor) {
        // skip processing of objects that were already processed in this
        // transaction, either by this node or by some other node...
        // added per CAY-1695 ..

        Persistent object = seen.get(anId);
        if (object == null) {
            object = super.objectFromDataRow(row, anId, classDescriptor);
            seen.put(anId, object);
        }

        return object;
    }

}
