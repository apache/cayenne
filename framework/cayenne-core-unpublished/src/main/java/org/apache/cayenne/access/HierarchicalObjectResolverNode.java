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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.reflect.ClassDescriptor;

class HierarchicalObjectResolverNode extends PrefetchObjectResolver {

    private PrefetchProcessorNode node;

    HierarchicalObjectResolverNode(PrefetchProcessorNode node, DataContext context, ClassDescriptor descriptor,
            boolean refresh, Map<ObjectId, Persistent> seen) {
        super(context, descriptor, refresh, seen);
        this.node = node;
    }

    @Override
    List<Persistent> objectsFromDataRows(List<? extends DataRow> rows) {

        if (rows == null || rows.size() == 0) {
            return new ArrayList<Persistent>(1);
        }

        List<Persistent> results = new ArrayList<Persistent>(rows.size());

        for (DataRow row : rows) {

            // determine entity to use
            ClassDescriptor classDescriptor = descriptorResolutionStrategy.descriptorForRow(row);

            // not using DataRow.createObjectId for performance reasons -
            // ObjectResolver
            // has all needed metadata already cached.
            ObjectId anId = createObjectId(row, classDescriptor.getEntity(), null);

            Persistent object = objectFromDataRow(row, anId, classDescriptor);
            if (object == null) {
                throw new CayenneRuntimeException("Can't build Object from row: " + row);
            }

            // keep the dupe objects (and data rows) around, as there maybe an
            // attached
            // joint prefetch...
            results.add(object);

            node.getParentAttachmentStrategy().linkToParent(row, object);
        }

        // now deal with snapshots

        // TODO: refactoring: dupes will clutter the lists and cause extra
        // processing...
        // removal of dupes happens only downstream, as we need the objects
        // matching
        // fetched rows for joint prefetch resolving... maybe pushback unique
        // and
        // non-unique lists to the "node", instead of returning a single list
        // from this
        // method
        cache.snapshotsUpdatedForObjects(results, rows, refreshObjects);

        return results;
    }
}
