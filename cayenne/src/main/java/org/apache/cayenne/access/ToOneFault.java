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
package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Fault;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.RelationshipQuery;

/**
 * @since 3.0
 */
public class ToOneFault extends Fault {

    /**
     * Resolves this fault to a Persistent.
     */
    @Override
    public Object resolveFault(Persistent sourceObject, String relationshipName) {

        if (sourceObject.getObjectContext() == null) {
            throw new IllegalStateException(
                    "Null ObjectContext. Can't read a to-one relationship '"
                            + relationshipName
                            + "' for an object with ID: "
                            + sourceObject.getObjectId());
        }

        int state = sourceObject.getPersistenceState();
        if (state == PersistenceState.NEW) {
            return null;
        }

        Object target = doResolveFault(sourceObject, relationshipName);

        // must update the diff for the object

        ObjectContext context = sourceObject.getObjectContext();
        if ((state == PersistenceState.MODIFIED || state == PersistenceState.DELETED)
                && context instanceof DataContext) {

            ObjectDiff diff = ((DataContext) context)
                    .getObjectStore()
                    .getChangesByObjectId()
                    .get(sourceObject.getObjectId());

            if (diff != null) {
                diff.updateArcSnapshot(relationshipName, (Persistent) target);
            }
        }

        return target;
    }

    Object doResolveFault(Persistent sourceObject, String relationshipName) {
        RelationshipQuery query = new RelationshipQuery(
                sourceObject.getObjectId(),
                relationshipName,
                false);

        List objects = sourceObject.getObjectContext().performQuery(query);

        if (objects.isEmpty()) {
            return null;
        } else if (objects.size() == 1) {
            return objects.get(0);
        } else {
            throw new CayenneRuntimeException("Error resolving to-one fault. "
                    + "More than one object found. Source Id: %s, relationship: %s"
                    , sourceObject.getObjectId(), relationshipName);
        }
    }
}
