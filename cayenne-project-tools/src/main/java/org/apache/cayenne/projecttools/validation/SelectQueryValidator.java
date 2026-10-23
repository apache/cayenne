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
package org.apache.cayenne.projecttools.validation;

import java.util.List;
import java.util.function.Supplier;

import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.validation.ValidationResult;

class SelectQueryValidator extends BaseQueryValidator<SelectQueryDescriptor> {

    /**
     * @param configSupplier the config defining the behavior of this validator.
     * @since 5.0
     */
    public SelectQueryValidator(Supplier<ValidationConfig> configSupplier) {
        super(configSupplier);
    }

    @Override
    public void validate(SelectQueryDescriptor node, ValidationResult validationResult) {
        ProjectNodeValidator<SelectQueryDescriptor>.Performer<SelectQueryDescriptor> performer =
                validateQuery(node, validationResult);

        if (node.getQl() == null || node.getQl().isBlank()) {
            performer.performIfEnabled(Inspection.SELECT_QUERY_NO_ROOT,
                    () -> addFailure(validationResult, node, "Query '%s' is empty", node.getName()));
            return;
        }

        if (!node.isValid()) {
            performer.performIfEnabled(Inspection.SELECT_QUERY_INVALID_QL, () -> checkSyntax(node, validationResult));
            return;
        }

        List<SelectQueryPaths.Problem> problems = SelectQueryPaths.check(node);
        for (SelectQueryPaths.Problem problem : problems) {
            Inspection inspection = switch (problem.clause()) {
                case FROM -> Inspection.SELECT_QUERY_NO_ROOT;
                case SELECT, WHERE, HAVING -> Inspection.SELECT_QUERY_INVALID_QUALIFIER;
                case ORDER_BY -> Inspection.SELECT_QUERY_INVALID_ORDERING_PATH;
                case PREFETCH -> Inspection.SELECT_QUERY_INVALID_PREFETCH_PATH;
            };
            performer.performIfEnabled(inspection, () -> addFailure(validationResult, node, "Query '%s': %s",
                    node.getName(), problem.message()));
        }
    }

    private void checkSyntax(SelectQueryDescriptor query, ValidationResult validationResult) {
        try {
            query.getParsedQuery();
        } catch (ExpressionException e) {
            addFailure(validationResult, query, "Query '%s' can't be parsed: %s", query.getName(),
                    e.getUnlabeledMessage());
        }
    }
}
