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

package org.apache.cayenne;

/**
 * Defines a set of object states from the point of view of persistence. I.e.
 * PersistenceState is the state of data stored in an object relative to the external
 * persistence store. If an object's state matches the state of the persistence store, the
 * object is COMMITTED. If object is not intended to be persistent or is not explicitly
 * made persistent, the state is TRANSIENT, and so on.
 * <p>
 * Object persistence states should not be modified directly. Rather it is a
 * responsibility of a ObjectContext/DataContext to maintain correct state of the managed
 * objects.
 * 
 */
public class PersistenceState {

    /**
     * Returns String label for persistence state. Used for debugging.
     */
    public static String persistenceStateName(int persistenceState) {
        switch (persistenceState) {
            case PersistenceState.TRANSIENT:
                return "transient";
            case PersistenceState.NEW:
                return "new";
            case PersistenceState.MODIFIED:
                return "modified";
            case PersistenceState.COMMITTED:
                return "committed";
            case PersistenceState.HOLLOW:
                return "hollow";
            case PersistenceState.DELETED:
                return "deleted";
            default:
                return "unknown";
        }
    }

    /**
     * Describes a state of an object not registered with DataContext/ObjectContext, and
     * therefore having no persistence features.
     */
    public static final int TRANSIENT = 1;

    /**
     * Describes a state of an object freshly registered with DataContext/ObjectContext,
     * but not committed to the database yet. So there is no corresponding database record
     * for this object just yet.
     */
    public static final int NEW = 2;

    /**
     * Describes a state of an object registered with DataContext/ObjectContext, whose
     * fields exactly match the state of a corresponding database row. This state is not
     * fully "clean", since database record may have been externally modified.
     */
    public static final int COMMITTED = 3;

    /**
     * Describes a state of an object registered with DataContext/ObjectContext, and
     * having a corresponding database row. This object state is known to be locally
     * modified and different from the database state.
     */
    public static final int MODIFIED = 4;

    /**
     * Describes a state of an object registered with DataContext/ObjectContext, and
     * having a corresponding database row. This object does not store any fields except
     * for its id (it is "hollow"), so next time it is accessed, it will be populated from
     * the database by the context. In this respect this is a real "clean" object.
     */
    public static final int HOLLOW = 5;

    /**
     * Describes a state of an object registered with DataContext/ObjectContext, that will
     * be deleted from the database on the next commit.
     */
    public static final int DELETED = 6;
}
