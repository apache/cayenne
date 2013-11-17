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
package org.apache.cayenne.access.jdbc;

import java.sql.ResultSet;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.EntityResultSegment;

/**
 * @since 3.0
 */
class InheritanceAwareEntityRowReader extends EntityRowReader {

    private EntityInheritanceTree entityInheritanceTree;

    public InheritanceAwareEntityRowReader(RowDescriptor descriptor,
            EntityResultSegment segmentMetadata) {
        super(descriptor, segmentMetadata);

        entityInheritanceTree = segmentMetadata
                .getClassDescriptor()
                .getEntityInheritanceTree();
    }

    @Override
    void postprocessRow(ResultSet resultSet, DataRow dataRow) throws Exception {
        if (postProcessor != null) {
            postProcessor.postprocessRow(resultSet, dataRow);
        }

        ObjEntity entity = entityInheritanceTree.entityMatchingRow(dataRow);
        dataRow.setEntityName(entity != null ? entity.getName() : entityName);
    }
}
