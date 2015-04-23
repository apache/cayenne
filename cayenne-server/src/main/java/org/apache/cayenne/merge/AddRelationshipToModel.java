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

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;

import static org.apache.cayenne.util.Util.join;

public class AddRelationshipToModel extends AbstractToModelToken.Entity {

    public static final String COMMA_SEPARATOR = ", ";
    public static final int COMMA_SEPARATOR_LENGTH = COMMA_SEPARATOR.length();
    private DbRelationship rel;

    public AddRelationshipToModel(DbEntity entity, DbRelationship rel) {
        super("Add Relationship", entity);
        this.rel = rel;
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createDropRelationshipToDb(getEntity(), rel);
    }

    public void execute(MergerContext mergerContext) {
        getEntity().addRelationship(rel);
        // TODO: add reverse relationship as well if it does not exist
        synchronizeWithObjEntity(getEntity());
        mergerContext.getModelMergeDelegate().dbRelationshipAdded(rel);
    }

    @Override
    public String getTokenValue() {
        String attributes = "";
        if (rel.getJoins().size() == 1) {
            attributes = rel.getJoins().get(0).getTargetName();
        } else {
            for (DbJoin dbJoin : rel.getJoins()) {
                attributes += dbJoin.getTargetName() + COMMA_SEPARATOR;
            }

            attributes = "{" + attributes.substring(0, attributes.length() - COMMA_SEPARATOR_LENGTH) + "}";
        }

        return rel.getName() + " " + rel.getSourceEntity().getName() + "->" + rel.getTargetEntityName() + "." + attributes;
    }


    public static String getTokenValue(DbRelationship rel) {
        String attributes = "";
        if (rel.getJoins().size() == 1) {
            attributes = rel.getJoins().get(0).getTargetName();
        } else {
            for (DbJoin dbJoin : rel.getJoins()) {
                attributes += dbJoin.getTargetName() + COMMA_SEPARATOR;
            }

            attributes = "{" + attributes.substring(0, attributes.length() - COMMA_SEPARATOR_LENGTH) + "}";
        }

        return rel.getName() + " " + rel.getSourceEntity().getName() + "->" + rel.getTargetEntityName() + "." + attributes;
    }

    public DbRelationship getRelationship() {
        return rel;
    }



}
