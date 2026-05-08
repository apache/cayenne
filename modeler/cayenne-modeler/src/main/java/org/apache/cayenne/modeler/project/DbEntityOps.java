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

package org.apache.cayenne.modeler.project;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

public class DbEntityOps {


    public static boolean isValidDbPath(DbEntity root, String dbPath) {
        if (root == null) {
            return true;
        }

        String[] pathSplit = dbPath.split("\\.");

        int size = pathSplit.length - 1;
        DbEntity next = root;
        for (int j = 0; j < size; j++) {
            DbRelationship relationship = next.getRelationship(pathSplit[j]);
            if (relationship == null) {
                return false;
            }
            next = relationship.getTargetEntity();
        }

        return next.getAttribute(pathSplit[(size)]) != null;
    }
}
