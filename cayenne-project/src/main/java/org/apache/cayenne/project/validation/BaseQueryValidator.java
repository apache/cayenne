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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;


/**
 * Base validation for all query types
 */
class BaseQueryValidator extends ConfigurationNodeValidator {

    void validateCacheGroup(QueryDescriptor query, ValidationResult validationResult) {
        String cacheGroup = query.getProperty(QueryMetadata.CACHE_GROUPS_PROPERTY);
        if(cacheGroup != null && cacheGroup.contains(",")) {
            addFailure(validationResult, query, "Invalid cache group \"%s\", " +
                    "multiple groups are deprecated", cacheGroup);
        }
    }

    void validateName(QueryDescriptor query, ValidationResult validationResult) {
        final String name = query.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            addFailure(validationResult, query, "Unnamed " + query.getType());
            return;
        }

        DataMap map = query.getDataMap();
        if (map == null) {
            return;
        }

        // check for duplicate names in the parent context
        if(hasDuplicateQueryDescriptorInDataMap(query, map)) {
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
                addFailure(validationResult, query,
                        "Duplicate %s name in another DataMap: %s",
                        query.getType(), name);
                return;
            }
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
