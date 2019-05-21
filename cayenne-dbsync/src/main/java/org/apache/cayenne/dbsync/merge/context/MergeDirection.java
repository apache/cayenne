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

package org.apache.cayenne.dbsync.merge.context;

/**
 * Represent a merge direction that can be either from the model to the db or from the db to the model.
 */
public enum MergeDirection {

    /**
     * TO_DB Token means that changes was made in object model and should be reflected at DB
     */
    TO_DB("To DB"),

    /**
     * TO_MODEL Token represent database changes that should be allayed to object model
     */
    TO_MODEL("To Model");

    private String name;

    MergeDirection(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isToDb() {
        return (this == TO_DB);
    }

    public boolean isToModel() {
        return (this == TO_MODEL);
    }

    @Override
    public String toString() {
        return getName();
    }

    public MergeDirection reverseDirection() {
        switch (this) {
            case TO_DB:
                return TO_MODEL;
            case TO_MODEL:
                return TO_DB;
            default:
                throw new IllegalStateException("Invalid direction: " + this);
        }
    }

}
