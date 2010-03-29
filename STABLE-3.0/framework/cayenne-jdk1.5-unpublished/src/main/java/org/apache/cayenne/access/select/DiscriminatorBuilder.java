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
package org.apache.cayenne.access.select;

import java.util.List;

import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.TraversalHelper;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 3.0
 */
class DiscriminatorBuilder extends MappedColumnBuilder {

    private EntityInheritanceTree node;

    DiscriminatorBuilder(ExtendedTypeMap extendedTypes, EntityInheritanceTree node) {
        super(extendedTypes);
        this.node = node;
    }

    List<EntitySelectColumn> buildColumns() {
        appendColumns(node);
        return columns;
    }

    private void appendColumns(EntityInheritanceTree node) {

        ObjEntity entity = node.getEntity();
        if (!entity.isAbstract()) {

            Expression qualifier = entity.getDeclaredQualifier();
            if (qualifier != null) {
                appendColumns(qualifier);
            }
        }

        for (EntityInheritanceTree childNode : node.getChildren()) {
            appendColumns(childNode);
        }
    }

    private void appendColumns(Expression expression) {

        final ObjEntity entity = node.getEntity();
        final DbEntity dbEntity = entity.getDbEntity();

        // find and register discriminator columns
        expression.traverse(new TraversalHelper() {

            @Override
            public void startNode(Expression node, Expression parentNode) {
                if (node.getType() == Expression.DB_PATH) {
                    append(dbEntity, node);
                }
                else if (node.getType() == Expression.OBJ_PATH) {
                    append(entity, node);
                }
            }
        });
    }
}
