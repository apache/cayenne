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
package org.apache.cayenne.project2.validate;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.util.Util;

class SelectQueryValidator {

    void validate(Object object, ConfigurationValidationVisitor validator) {
        SelectQuery query = (SelectQuery) object;

        validateName(query, validator);

        // Resolve root to Entity for further validation
        Entity root = validateRoot(query, validator);

        // validate path-based parts
        if (root != null) {
            validateQualifier(root, query.getQualifier(), validator);

            for (final Ordering ordering : query.getOrderings()) {
                validateOrdering(root, ordering, validator);
            }

            if (query.getPrefetchTree() != null) {
                for (final PrefetchTreeNode prefetchTreeNode : query
                        .getPrefetchTree()
                        .nonPhantomNodes()) {
                    validatePrefetch(root, prefetchTreeNode.getPath(), validator);
                }
            }
        }
    }

    void validatePrefetch(
            Entity root,
            String path,
            ConfigurationValidationVisitor validator) {
    }

    void validateOrdering(
            Entity root,
            Ordering ordering,
            ConfigurationValidationVisitor validator) {
    }

    void validateQualifier(
            Entity root,
            Expression qualifier,
            ConfigurationValidationVisitor validator) {
    }

    Entity validateRoot(SelectQuery query, ConfigurationValidationVisitor validator) {
        DataMap map = query.getDataMap();
        if (query.getRoot() == null && map != null) {
            validator.registerWarning("Query has no root", query);
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

            DataMap parent = query.getDataMap();

            if (parent != null) {
                return parent.getNamespace().getObjEntity((String) query.getRoot());
            }
        }

        return null;
    }

    void validateName(SelectQuery query, ConfigurationValidationVisitor validator) {
        String name = query.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            validator.registerError("Unnamed SelectQuery.", query);
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
                validator.registerError("Duplicate Query name: " + name + ".", query);
                break;
            }
        }
    }
}
