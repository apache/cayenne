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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

/**
 * Base validation for all query types
 */
public abstract class BaseQueryValidator<T extends QueryDescriptor> extends ConfigurationNodeValidator<T> {

    /**
     * @param validationConfig the config defining the behavior of this validator.
     * @since 5.0
     */
    public BaseQueryValidator(ValidationConfig validationConfig) {
        super(validationConfig);
    }

    @Override
    public void validate(T node, ValidationResult validationResult) {
        validateQuery(node, validationResult);
    }

    protected Performer<T> validateQuery(T query, ValidationResult validationResult) {
        return on(query, validationResult)
                .performIfEnabled(Inspection.QUERY_NO_NAME, this::checkForName)
                .performIfEnabled(Inspection.QUERY_NAME_DUPLICATE, this::checkForNameDuplicates)
                .performIfEnabled(Inspection.QUERY_MULTI_CACHE_GROUP, this::checkForMultiCacheGroup);
    }

    void checkForName(T query, ValidationResult validationResult) {

        // Must have name
        String name = query.getName();
        if (Util.isEmptyString(name)) {
            addFailure(validationResult, query, "Unnamed " + query.getType());
        }
    }

    void checkForNameDuplicates(T query, ValidationResult validationResult) {
        String name = query.getName();
        DataMap map = query.getDataMap();
        if (map == null || Util.isEmptyString(name)) {
            return;
        }

        // check for duplicate names in the parent context
        if (hasDuplicateQueryDescriptorInDataMap(query, map)) {
            addFailure(validationResult, query, "Duplicate query name: %s", name);
            return;
        }

        DataChannelDescriptor domain = query.getDataMap().getDataChannelDescriptor();
        if (domain == null) {
            return;
        }

        // check for duplicate names in sibling contexts
        for (DataMap nextMap : domain.getDataMaps()) {
            if (nextMap == map) {
                continue;
            }

            if (hasDuplicateQueryDescriptorInDataMap(query, nextMap)) {
                addFailure(validationResult, query, "Duplicate %s name in another DataMap: %s",
                        query.getType(), name);
                return;
            }
        }
    }

    void checkForMultiCacheGroup(T query, ValidationResult validationResult) {
        String cacheGroup = query.getProperty(QueryMetadata.CACHE_GROUPS_PROPERTY);
        if (cacheGroup != null && cacheGroup.contains(",")) {
            addFailure(validationResult, query, "Invalid cache group '%s', multiple groups are deprecated",
                    cacheGroup);
        }
    }

    private boolean hasDuplicateQueryDescriptorInDataMap(QueryDescriptor queryDescriptor, DataMap dataMap) {
        for (final QueryDescriptor otherQuery : dataMap.getQueryDescriptors()) {
            if (otherQuery == queryDescriptor) {
                continue;
            }

            if (queryDescriptor.getName().equals(otherQuery.getName())) {
                return true;
            }
        }

        return false;
    }
}
