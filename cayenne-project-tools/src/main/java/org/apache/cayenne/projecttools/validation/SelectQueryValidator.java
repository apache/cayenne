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

import org.apache.cayenne.map.NamedSelectQuery;
import org.apache.cayenne.projecttools.SelectQueryInfo;
import org.apache.cayenne.validation.ValidationResult;

class SelectQueryValidator extends BaseQueryValidator<NamedSelectQuery> {

    /**
     * @param configSupplier the config defining the behavior of this validator.
     * @since 5.0
     */
    public SelectQueryValidator(Supplier<ValidationConfig> configSupplier) {
        super(configSupplier);
    }

    @Override
    public void validate(NamedSelectQuery node, ValidationResult validationResult) {
        ProjectNodeValidator<NamedSelectQuery>.Performer<NamedSelectQuery> performer =
                validateQuery(node, validationResult);

        SelectQueryInfo info = SelectQueryInfo.parse(node);
        if (info.isEmpty()) {
            performer.performIfEnabled(Inspection.SELECT_QUERY_NO_ROOT,
                    () -> addFailure(validationResult, node, "Query '%s' is empty", node.getName()));
            return;
        }

        if (!info.isValid()) {
            performer.performIfEnabled(Inspection.SELECT_QUERY_INVALID_QL,
                    () -> addFailure(validationResult, node, "Query '%s' can't be parsed: %s", node.getName(),
                            info.error().getUnlabeledMessage()));
            return;
        }

        List<SelectQueryPaths.Problem> problems = SelectQueryPaths.check(info);
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
}
