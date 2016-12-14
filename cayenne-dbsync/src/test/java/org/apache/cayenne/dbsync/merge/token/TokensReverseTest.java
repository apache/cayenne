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

import org.apache.cayenne.dbsync.merge.factory.HSQLMergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

import static org.apache.cayenne.dbsync.merge.builders.ObjectMother.dbAttr;
import static org.apache.cayenne.dbsync.merge.builders.ObjectMother.dbEntity;

/**
 * @since 4.0.
 */
public class TokensReverseTest {

    @Test
    public void testReverses() {
        DbAttribute attr = dbAttr().build();
        DbEntity entity = dbEntity().attributes(attr).build();
        DbRelationship rel = new DbRelationship("rel");
        rel.setSourceEntity(entity);
        rel.addJoin(new DbJoin(rel, attr.getName(), "dontKnow"));

        testOneToOneReverse(factory().createAddColumnToDb(entity, attr));
        testOneToOneReverse(factory().createAddColumnToModel(entity, attr));
        testOneToOneReverse(factory().createDropColumnToDb(entity, attr));
        testOneToOneReverse(factory().createDropColumnToModel(entity, attr));

        testOneToOneReverse(factory().createAddRelationshipToDb(entity, rel));
        testOneToOneReverse(factory().createAddRelationshipToModel(entity, rel));
        testOneToOneReverse(factory().createDropRelationshipToDb(entity, rel));
        testOneToOneReverse(factory().createDropRelationshipToModel(entity, rel));

        testOneToOneReverse(factory().createCreateTableToDb(entity));
        testOneToOneReverse(factory().createCreateTableToModel(entity));
        testOneToOneReverse(factory().createDropTableToDb(entity));
        testOneToOneReverse(factory().createDropTableToModel(entity));

        testOneToOneReverse(factory().createSetAllowNullToDb(entity, attr));
        testOneToOneReverse(factory().createSetAllowNullToModel(entity, attr));
        testOneToOneReverse(factory().createSetNotNullToDb(entity, attr));
        testOneToOneReverse(factory().createSetNotNullToModel(entity, attr));

        DbAttribute attr2 = dbAttr().build();
        testOneToOneReverse(factory().createSetColumnTypeToDb(entity, attr, attr2));
        testOneToOneReverse(factory().createSetColumnTypeToModel(entity, attr, attr2));

        testOneToOneReverse(factory().createSetPrimaryKeyToDb(entity, Collections.singleton(attr), Collections.singleton(attr2), "PK"));
        testOneToOneReverse(factory().createSetPrimaryKeyToModel(entity, Collections.singleton(attr), Collections.singleton(attr2), "PK"));

        testOneToOneReverse(factory().createSetValueForNullToDb(entity, attr, new DefaultValueForNullProvider()));
    }

    private void testOneToOneReverse(MergerToken token) {
        MergerToken token2 = token.createReverse(factory()).createReverse(factory());

        Assert.assertEquals(token.getTokenName(), token2.getTokenName());
        Assert.assertEquals(token.getTokenValue(), token2.getTokenValue());
        Assert.assertEquals(token.getDirection(), token2.getDirection());
    }

    private MergerTokenFactory factory() {
        return new HSQLMergerTokenFactory();
    }
}
