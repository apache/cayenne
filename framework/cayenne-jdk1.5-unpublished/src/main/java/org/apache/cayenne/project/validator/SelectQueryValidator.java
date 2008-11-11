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

package org.apache.cayenne.project.validator;

import java.util.Iterator;

import org.apache.cayenne.access.QueryEngine;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.TraversalHelper;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.util.Util;

/**
 * Validator for SelectQueries.
 * 
 * @since 1.1
 */
public class SelectQueryValidator extends TreeNodeValidator {

    @Override
    public void validateObject(ProjectPath treeNodePath, Validator validator) {
        SelectQuery query = (SelectQuery) treeNodePath.getObject();

        validateName(query, treeNodePath, validator);

        // Resolve root to Entity for further validation
        Entity root = validateRoot(query, treeNodePath, validator);

        // validate path-based parts
        if (root != null) {
            validateQualifier(root, query.getQualifier(), treeNodePath, validator);

            for (final Ordering ordering : query.getOrderings()) {
                validateOrdering(
                        root,
                        ordering,
                        treeNodePath,
                        validator);
            }

            if (query.getPrefetchTree() != null) {
                for (final PrefetchTreeNode prefetchTreeNode : query.getPrefetchTree().nonPhantomNodes()) {
                    validatePrefetch(root, prefetchTreeNode.getPath(), treeNodePath, validator);
                }
            }
        }
    }

    protected Entity validateRoot(SelectQuery query, ProjectPath path, Validator validator) {
        DataMap map = path.firstInstanceOf(DataMap.class);
        if (query.getRoot() == null && map != null) {
            validator.registerWarning("Query has no root", path);
            return null;
        }

        if (query.getRoot() == map) {
            // map-level query... everything is clean
            return null;
        }

        if (map == null) {
            // maybe standalone entity, otherwise bail...
            return (query.getRoot() instanceof Entity) ? (Entity) query.getRoot() : null;
        }

        if (query.getRoot() instanceof Entity) {
            return (Entity) query.getRoot();
        }

        // can't validate Class root - it is likely not accessible from here...
        if (query.getRoot() instanceof Class) {
            return null;
        }

        // resolve entity
        if (query.getRoot() instanceof String) {

            QueryEngine parent = path.firstInstanceOf(QueryEngine.class);

            if (parent != null) {
                return parent.getEntityResolver().getObjEntity((String) query.getRoot());
            }
        }

        return null;
    }

    protected void validateName(Query query, ProjectPath path, Validator validator) {
        String name = query.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            validator.registerError("Unnamed SelectQuery.", path);
            return;
        }

        DataMap map = (DataMap) path.getObjectParent();
        if (map == null) {
            return;
        }

        // check for duplicate names in the parent context

        for (final Query otherQuery : map.getQueries()) {
            if (otherQuery == query) {
                continue;
            }

            if (name.equals(otherQuery.getName())) {
                validator.registerError("Duplicate Query name: " + name + ".", path);
                break;
            }
        }
    }

    protected void validateQualifier(
            Entity entity,
            Expression qualifier,
            ProjectPath path,
            Validator validator) {

        try {
            testExpression(entity, qualifier);
        }
        catch (ExpressionException e) {
            validator.registerWarning(buildValidationMessage(
                    e,
                    "Invalid path in qualifier"), path);
        }
    }

    protected void validateOrdering(
            Entity entity,
            Ordering ordering,
            ProjectPath path,
            Validator validator) {

        if (ordering == null) {
            return;
        }

        try {
            testExpression(entity, ordering.getSortSpec());
        }
        catch (ExpressionException e) {
            validator
                    .registerWarning(buildValidationMessage(e, "Invalid ordering"), path);
        }
    }

    protected void validatePrefetch(
            Entity entity,
            String prefetch,
            ProjectPath path,
            Validator validator) {

        if (prefetch == null) {
            return;
        }

        try {
            testExpression(entity, Expression.fromString(prefetch));
        }
        catch (ExpressionException e) {
            validator
                    .registerWarning(buildValidationMessage(e, "Invalid prefetch"), path);
        }
    }

    private void testExpression(Entity rootEntity, Expression exp)
            throws ExpressionException {

        if (exp != null) {
            exp.traverse(new EntityExpressionValidator(rootEntity));
        }
    }

    private String buildValidationMessage(ExpressionException e, String prefix) {
        StringBuilder buffer = new StringBuilder(prefix);
        if (e.getExpressionString() != null) {
            buffer.append(": '").append(e.getExpressionString()).append("'");
        }

        buffer.append(".");
        return buffer.toString();
    }

    final class EntityExpressionValidator extends TraversalHelper {

        Entity rootEntity;

        EntityExpressionValidator(Entity rootEntity) {
            this.rootEntity = rootEntity;
        }

        @Override
        public void startNode(Expression node, Expression parentNode) {
            // check if path nodes are compatibe with root entity
            if (node.getType() == Expression.OBJ_PATH
                    || node.getType() == Expression.DB_PATH) {
                // this will throw an exception if the path is invalid
                node.evaluate(rootEntity);
            }
        }
    }
}
