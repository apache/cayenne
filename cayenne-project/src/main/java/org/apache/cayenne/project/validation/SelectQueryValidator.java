/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.function.Supplier;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.validation.ValidationResult;

public class SelectQueryValidator extends BaseQueryValidator<SelectQueryDescriptor> {

    /**
     * @param configSupplier the config defining the behavior of this validator.
     * @since 5.0
     */
    public SelectQueryValidator(Supplier<ValidationConfig> configSupplier) {
        super(configSupplier);
    }

    @Override
    public void validate(SelectQueryDescriptor node, ValidationResult validationResult) {
        ConfigurationNodeValidator<SelectQueryDescriptor>.Performer<SelectQueryDescriptor> performer =
                validateQuery(node, validationResult);

        performer.performIfEnabled(Inspection.SELECT_QUERY_NO_ROOT, this::checkForRoot);

        // Resolve root to Entity for further validation
        Entity<?, ?, ?> root = getRootForValidation(node);

        // validate path-based parts
        if (root == null) {
            return;
        }
        performer.performIfEnabled(Inspection.SELECT_QUERY_INVALID_QUALIFIER,
                () -> validateQualifier(root, node.getQualifier(), validationResult));
        for (final Ordering ordering : node.getOrderings()) {
            performer.performIfEnabled(Inspection.SELECT_QUERY_INVALID_ORDERING_PATH,
                    () -> validateOrdering(node, root, ordering, validationResult));
        }
        if (node.getPrefetchesMap() == null) {
            return;
        }
        for (String prefetchPath : node.getPrefetchesMap().keySet()) {
            performer.performIfEnabled(Inspection.SELECT_QUERY_INVALID_PREFETCH_PATH,
                    () -> validatePrefetch(root, prefetchPath, validationResult));
        }
    }

    private void validatePrefetch(Entity<?, ?, ?> root, String path, ValidationResult validationResult) {
        // TODO: andrus 03/10/2010 - should this be implemented?
    }

    private void validateOrdering(SelectQueryDescriptor query, Entity<?, ?, ?> root,
                                  Ordering ordering, ValidationResult validationResult) {

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

    private void validateQualifier(Entity<?, ?, ?> root, Expression qualifier, ValidationResult validationResult) {
        // TODO: andrus 03/10/2010 - should this be implemented?
    }

    private void checkForRoot(SelectQueryDescriptor query, ValidationResult validationResult) {
        DataMap map = query.getDataMap();
        if (query.getRoot() == null && map != null) {
            addFailure(validationResult, query, "Query '%s' has no root", query.getName());
        }
    }

    private Entity<?, ?, ?> getRootForValidation(SelectQueryDescriptor query) {
        DataMap map = query.getDataMap();
        if (query.getRoot() == null && map != null) {
            return null;
        }

        if (query.getRoot() == map) {
            // map-level query... everything is clean
            return null;
        }

        if (map == null) {
            // maybe standalone entity, otherwise bail...
            return (query.getRoot() instanceof Entity) ? (Entity<?, ?, ?>) query.getRoot() : null;
        }

        if (query.getRoot() instanceof Entity) {
            return (Entity<?, ?, ?>) query.getRoot();
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
}
