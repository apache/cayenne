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
package org.apache.cayenne.merge;

public class MergeDirection {

    public static final int TO_DB_ID = 1;
    public static final int TO_MODEL_ID = 2;

    public static final MergeDirection TO_DB = new MergeDirection(TO_DB_ID, "To DB");
    public static final MergeDirection TO_MODEL = new MergeDirection(
            TO_MODEL_ID,
            "To Model");

    private int id;
    private String name;

    private MergeDirection(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public boolean equals(Object obj) {
        return (obj == this);
    }

    public int hashCode() {
        return id * 17;
    }

    public String toString() {
        return getName();
    }

    public MergeDirection reverseDirection() {
        switch (id) {
            case TO_DB_ID:
                return TO_MODEL;
            case TO_MODEL_ID:
                return TO_DB;
            default:
                throw new IllegalStateException("Invalid direction id: " + id);
        }
    }
}
