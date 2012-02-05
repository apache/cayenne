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
package org.apache.cayenne.lifecycle.audit;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.lifecycle.changeset.ChangeSet;
import org.apache.cayenne.lifecycle.changeset.ChangeSetFilter;
import org.apache.cayenne.lifecycle.changeset.PropertyChange;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

class AuditableEntityDescriptor {

    private Collection<String> ignoredProperties;

    AuditableEntityDescriptor(ObjEntity auditableEntity, String[] ignoredProperties) {

        this.ignoredProperties = new HashSet<String>();

        // ignore runtime relationships
        for (ObjRelationship relationship : auditableEntity.getRelationships()) {
            if (relationship.isRuntime()) {
                this.ignoredProperties.add(relationship.getName());
            }
        }

        // ignore explicitly specified properties
        if (ignoredProperties != null) {
            for (String property : ignoredProperties) {
                this.ignoredProperties.add(property);
            }
        }
    }

    boolean auditableChange(Persistent object) {
        if (ignoredProperties.isEmpty()) {
            return true;
        }

        ChangeSet changeSet = ChangeSetFilter.preCommitChangeSet();
        if (changeSet == null) {
            throw new CayenneRuntimeException(
                    "Required ChangeSetFilter is not installed, or is in the wrong place in the filter chain.");
        }

        Map<String, PropertyChange> changes = changeSet.getChanges(object);

        if (changes.size() > ignoredProperties.size()) {
            return true;
        }

        for (String key : changes.keySet()) {
            if (!ignoredProperties.contains(key)) {
                return true;
            }
        }

        return false;
    }
}
