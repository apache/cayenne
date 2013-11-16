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

import org.apache.cayenne.DataObject;
import org.apache.cayenne.reflect.PersistentDescriptor;
import org.apache.cayenne.reflect.PropertyException;

/**
 * A ClassDescriptor for "generic" persistent classes implementing {@link DataObject}
 * interface.
 * 
 * @since 3.0
 */
// non-public as the only difference with the superclass is version handling on merge -
// this is something we need to solved in a more generic fashion (e.g. as via enhancer)
// for other object types.
class DataObjectDescriptor extends PersistentDescriptor {

    @Override
    public void shallowMerge(Object from, Object to) throws PropertyException {
        
        injectValueHolders(to);
        
        super.shallowMerge(from, to);

        if (from instanceof DataObject && to instanceof DataObject) {
            ((DataObject) to)
                    .setSnapshotVersion(((DataObject) from).getSnapshotVersion());
        }
    }
}
