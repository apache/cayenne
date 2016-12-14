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

package org.apache.cayenne.dbsync.merge.token;

import java.util.Comparator;

import org.apache.cayenne.dbsync.merge.token.db.AbstractToDbToken;
import org.apache.cayenne.dbsync.merge.token.db.AddRelationshipToDb;
import org.apache.cayenne.dbsync.merge.token.model.AddRelationshipToModel;

/**
 * Simple sort of merge tokens.
 * Just move all relationships creation tokens to the end of the list.
 */
public class TokenComparator implements Comparator<MergerToken> {

    @Override
    public int compare(MergerToken o1, MergerToken o2) {
        if (o1 instanceof AbstractToDbToken && o2 instanceof AbstractToDbToken) {
            if (o1 instanceof AddRelationshipToDb && o2 instanceof AddRelationshipToDb) {
                return 0;
            }

            if (!(o1 instanceof AddRelationshipToDb || o2 instanceof AddRelationshipToDb)) {
                return 0;
            }

            return o1 instanceof AddRelationshipToDb ? 1 : -1;
        }

        if (o1 instanceof AddRelationshipToModel && o2 instanceof AddRelationshipToModel) {
            return 0;
        }

        if (!(o1 instanceof AddRelationshipToModel || o2 instanceof AddRelationshipToModel)) {
            return o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
        }

        return o1 instanceof AddRelationshipToModel ? 1 : -1;
    }
}
