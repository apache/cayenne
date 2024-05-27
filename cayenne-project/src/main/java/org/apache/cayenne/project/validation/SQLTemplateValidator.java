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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

import java.util.Map;

public class SQLTemplateValidator extends BaseQueryValidator<SQLTemplateDescriptor> {

    /**
     * @param validationConfig the config defining the behavior of this validator.
     * @since 5.0
     */
    public SQLTemplateValidator(ValidationConfig validationConfig) {
        super(validationConfig);
    }

    @Override
    protected ConfigurationNodeValidator<SQLTemplateDescriptor>.Performer<SQLTemplateDescriptor> validateQuery(
            SQLTemplateDescriptor query, ValidationResult validationResult) {
        return super.validateQuery(query, validationResult)
                .performIfEnabled(Inspection.SQL_TEMPLATE_NO_ROOT, this::checkForRoot)
                .performIfEnabled(Inspection.SQL_TEMPLATE_NO_DEFAULT_SQL, this::checkForDefaultSQL);
    }

    private void checkForRoot(SQLTemplateDescriptor query, ValidationResult validationResult) {
        DataMap map = query.getDataMap();
        if (query.getRoot() == null && map != null) {
            addFailure(validationResult, query, "SQLTemplate query '%s' has no root", query.getName());
        }
    }

    private void checkForDefaultSQL(SQLTemplateDescriptor query, ValidationResult validationResult) {
        if (!Util.isEmptyString(query.getSql())) {
            return;
        }
        // see if there is at least one adapter-specific template...
        for (Map.Entry<String, String> entry : query.getAdapterSql().entrySet()) {
            if (!Util.isEmptyString(entry.getValue())) {
                return;
            }
        }
        addFailure(validationResult, query, "SQLTemplate query '%s' has no default SQL template", query.getName());
    }
}
