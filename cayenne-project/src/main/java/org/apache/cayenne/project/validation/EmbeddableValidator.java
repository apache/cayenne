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
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

public class EmbeddableValidator extends ConfigurationNodeValidator<Embeddable> {

    /**
     * @param validationConfig the config defining the behavior of this validator.
     * @since 5.0
     */
    public EmbeddableValidator(ValidationConfig validationConfig) {
        super(validationConfig);
    }

    @Override
    public void validate(Embeddable node, ValidationResult validationResult) {
        on(node, validationResult)
                .performIfEnabled(Inspection.EMBEDDABLE_NO_NAME, this::checkForName)
                .performIfEnabled(Inspection.EMBEDDABLE_NAME_DUPLICATE, this::checkForNameDuplicates);
    }

    private void checkForName(Embeddable embeddable, ValidationResult validationResult) {
        String name = embeddable.getClassName();

        // Must have name
        if (Util.isEmptyString(name)) {
            addFailure(validationResult, embeddable, "Unnamed Embeddable");
        }
    }

    private void checkForNameDuplicates(Embeddable embeddable, ValidationResult validationResult) {
        String name = embeddable.getClassName();
        DataMap map = embeddable.getDataMap();
        if (map == null || Util.isEmptyString(name)) {
            return;
        }

        // check for duplicate names in the parent context
        for (Embeddable otherEmb : map.getEmbeddables()) {
            if (otherEmb == embeddable) {
                continue;
            }
            if (name.equals(otherEmb.getClassName())) {
                addFailure(validationResult, embeddable, "Duplicate Embeddable class name: %s", name);
                break;
            }
        }

        // check for duplicates in other DataMaps
        DataChannelDescriptor domain = map.getDataChannelDescriptor();
        if (domain == null) {
            return;
        }
        for (DataMap nextMap : domain.getDataMaps()) {
            if (nextMap == map) {
                continue;
            }

            // note that lookup below will return the same embeddable due to the
            // shared namespace if not conflicts exist
            Embeddable conflictingEmbeddable = nextMap.getEmbeddable(name);
            if (conflictingEmbeddable != null && conflictingEmbeddable != embeddable) {
                addFailure(validationResult, embeddable, "Duplicate Embeddable name in another DataMap: %s", name);
                break;
            }
        }
    }
}
