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

import org.apache.cayenne.access.QueryEngine;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.util.Util;


public class SelectQueryValidator  implements Validator{

    public void validate(Object object, ConfigurationValidationVisitor validator) {
        SelectQuery query = (SelectQuery) object;
        
        ProjectPath path = new ProjectPath(new Object[] {
                (DataChannelDescriptor) validator.getProject().getRootNode(),
                query.getDataMap(), query
        });

        validateName(query, path, validator);

        // Resolve root to Entity for further validation
        Entity root = validateRoot(query, path, validator);

        // validate path-based parts
        if (root != null) {
            validateQualifier(root, query.getQualifier(), path, validator);

            for (final Ordering ordering : query.getOrderings()) {
                validateOrdering(
                        root,
                        ordering,
                        path,
                        validator);
            }

            if (query.getPrefetchTree() != null) {
                for (final PrefetchTreeNode prefetchTreeNode : query.getPrefetchTree().nonPhantomNodes()) {
                    validatePrefetch(root, prefetchTreeNode.getPath(), path, validator);
                }
            }
        }
    }

    private void validatePrefetch(
            Entity root,
            String path,
            ProjectPath path2,
            ConfigurationValidationVisitor validator) {
    }

    private void validateOrdering(
            Entity root,
            Ordering ordering,
            ProjectPath path,
            ConfigurationValidationVisitor validator) {
    }

    private void validateQualifier(
            Entity root,
            Expression qualifier,
            ProjectPath path,
            ConfigurationValidationVisitor validator) {
    }

    private Entity validateRoot(
            SelectQuery query,
            ProjectPath path,
            ConfigurationValidationVisitor validator) {
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

    private void validateName(
            SelectQuery query,
            ProjectPath path,
            ConfigurationValidationVisitor validator) {
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

}
