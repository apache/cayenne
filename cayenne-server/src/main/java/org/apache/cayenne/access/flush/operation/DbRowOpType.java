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

package org.apache.cayenne.access.flush.operation;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;

/**
 * @since 4.2
 */
public enum DbRowOpType implements Comparable<DbRowOpType> {
    INSERT,
    UPDATE,
    DELETE;

    public static DbRowOpType forObject(Persistent object) {
        switch (object.getPersistenceState()) {
            case PersistenceState.NEW:
                return INSERT;
            case PersistenceState.MODIFIED:
                return UPDATE;
            case PersistenceState.DELETED:
                return DELETE;
        }
        throw new CayenneRuntimeException("Trying to flush object %s in wrong persistence state %s",
                object, PersistenceState.persistenceStateName(object.getPersistenceState()));
    }
}
