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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;

/**
 * Locates parents by checking for the child PK in the parent DataRow under FK.
 */
class ResultScanParentAttachmentStrategy implements ParentAttachmentStrategy {

    private PrefetchProcessorNode parentNode;
    private DbJoin[] joins;
    private PrefetchProcessorNode node;

    // TODO: the ivar below makes this strategy STATEFUL and non-reusable. If we need a
    // stateless version down the line, will need to move this to the
    // PrefetchProcessorNode.
    private Map<Object, List<Persistent>> partitionByChild;

    ResultScanParentAttachmentStrategy(PrefetchProcessorNode node) {

        if (node.getParent() == null) {
            throw new IllegalArgumentException(
                    "ResultScanParentAttachmentStrategy works only for non-root nodes");
        }

        this.node = node;
        parentNode = (PrefetchProcessorNode) node.getParent();

        ObjRelationship relationship = node.getIncoming().getRelationship();

        List<DbRelationship> dbRelationships = relationship.getDbRelationships();
        if (dbRelationships.size() > 1) {
            throw new IllegalArgumentException(
                    "ResultScanParentAttachmentStrategy does not work for flattened relationships");
        }

        joins = dbRelationships.get(0).getJoins().toArray(
                new DbJoin[dbRelationships.get(0).getJoins().size()]);

    }

    public void linkToParent(DataRow row, Persistent object) {

        if (partitionByChild == null) {
            indexParents();
        }

        Object key;

        if (joins.length > 1) {

            List<Object> values = new ArrayList<Object>(joins.length);

            for (int j = 0; j < joins.length; j++) {
                values.add(row.get(joins[j].getTargetName()));
            }

            key = values;
        }
        else {
            key = row.get(joins[0].getTargetName());
        }

        List<Persistent> parents = (List<Persistent>) partitionByChild.get(key);
        if (parents != null) {
            for (Persistent parent : parents) {
                node.linkToParent(object, parent);
            }
        }
    }

    private void indexParents() {
        partitionByChild = new HashMap<Object, List<Persistent>>();

        List<Persistent> objects = parentNode.getObjects();
        
        // null is possible if the parent node matched no objects...
        if(objects == null) {
            return;
        }
        
        List<DataRow> rows = parentNode.getDataRows();
        int size = objects.size();
        for (int i = 0; i < size; i++) {

            DataRow row = rows.get(i);

            Object key;

            if (joins.length > 1) {

                List<Object> values = new ArrayList<Object>(joins.length);

                for (int j = 0; j < joins.length; j++) {
                    values.add(row.get(joins[j].getSourceName()));
                }

                key = values;
            }
            else {
                key = row.get(joins[0].getSourceName());
            }

            List<Persistent> parents = partitionByChild.get(key);
            if (parents == null) {
                parents = new ArrayList<Persistent>();
                partitionByChild.put(key, parents);
            }

            parents.add(objects.get(i));
        }
    }
}
