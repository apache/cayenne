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
package org.apache.cayenne.project.validation;

import java.util.Iterator;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

class SelectQueryValidator extends ConfigurationNodeValidator {

    <T> void validate(SelectQuery<T> query, ValidationResult validationResult) {

        validateName(query, validationResult);

        // Resolve root to Entity for further validation
        Entity root = validateRoot(query, validationResult);

        // validate path-based parts
        if (root != null) {
            validateQualifier(root, query.getQualifier(), validationResult);

            for (final Ordering ordering : query.getOrderings()) {
                validateOrdering(query, root, ordering, validationResult);
            }

            if (query.getPrefetchTree() != null) {
                for (PrefetchTreeNode prefetchTreeNode : query
                        .getPrefetchTree()
                        .nonPhantomNodes()) {
                    validatePrefetch(root, prefetchTreeNode.getPath(), validationResult);
                }
            }
      
        }
    }

    void validatePrefetch(Entity root, String path, ValidationResult validationResult) {
        // TODO: andrus 03/10/2010 - should this be implemented?
    }

    <T> void validateOrdering(
            SelectQuery<T> query,
            Entity root,
            Ordering ordering,
            ValidationResult validationResult) {
       
        // validate paths in ordering
        String path = ordering.getSortSpecString();
        Iterator<CayenneMapEntry> it = root.resolvePathComponents(path);
        while (it.hasNext()) {
            try {
                it.next();
            } catch (ExpressionException e) {
                addFailure(validationResult, query, "Invalid ordering path: '%s'", path);
            }
        }
    }

    void validateQualifier(
            Entity root,
            Expression qualifier,
            ValidationResult validationResult) {
        // TODO: andrus 03/10/2010 - should this be implemented?
    }

    <T> Entity validateRoot(SelectQuery<T> query, ValidationResult validationResult) {
        DataMap map = query.getDataMap();
        if (query.getRoot() == null && map != null) {
            addFailure(validationResult, query, "Query '%s' has no root", query.getName());
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
        if (query.getRoot() instanceof Class<?>) {
            return null;
        }

        // resolve entity
        if (query.getRoot() instanceof String) {

            DataMap parent = query.getDataMap();

            if (parent != null) {
                return parent.getNamespace().getObjEntity((String) query.getRoot());
            }
        }

        return null;
    }

    <T> void validateName(SelectQuery<T> query, ValidationResult validationResult) {
        String name = query.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            addFailure(validationResult, query, "Unnamed SelectQuery");
            return;
        }

        DataMap map = query.getDataMap();
        if (map == null) {
            return;
        }

        // check for duplicate names in the parent context

        for (final Query otherQuery : map.getQueries()) {
            if (otherQuery == query) {
                continue;
            }

            if (name.equals(otherQuery.getName())) {
                addFailure(validationResult, query, "Duplicate query name: %s", name);
                break;
            }
        }
    }
}
