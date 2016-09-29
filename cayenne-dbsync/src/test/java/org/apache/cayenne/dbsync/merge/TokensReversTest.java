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
package org.apache.cayenne.dbsync.merge;

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
public class TokensReversTest {

    @Test
    public void testReverses() {
        DbAttribute attr = dbAttr().build();
        DbEntity entity = dbEntity().attributes(attr).build();
        DbRelationship rel = new DbRelationship("rel");
        rel.setSourceEntity(entity);
        rel.addJoin(new DbJoin(rel, attr.getName(), "dontKnow"));

        test(factory().createAddColumnToDb(entity, attr));
        test(factory().createAddColumnToModel(entity, attr));
        test(factory().createDropColumnToDb(entity, attr));
        test(factory().createDropColumnToModel(entity, attr));

        test(factory().createAddRelationshipToDb(entity, rel));
        test(factory().createAddRelationshipToModel(entity, rel));
        test(factory().createDropRelationshipToDb(entity, rel));
        test(factory().createDropRelationshipToModel(entity, rel));

        test(factory().createCreateTableToDb(entity));
        test(factory().createCreateTableToModel(entity));
        test(factory().createDropTableToDb(entity));
        test(factory().createDropTableToModel(entity));

        test(factory().createSetAllowNullToDb(entity, attr));
        test(factory().createSetAllowNullToModel(entity, attr));
        test(factory().createSetNotNullToDb(entity, attr));
        test(factory().createSetNotNullToModel(entity, attr));

        DbAttribute attr2 = dbAttr().build();
        test(factory().createSetColumnTypeToDb(entity, attr, attr2));
        test(factory().createSetColumnTypeToModel(entity, attr, attr2));

        test(factory().createSetPrimaryKeyToDb(entity, Collections.singleton(attr), Collections.singleton(attr2), "PK"));
        test(factory().createSetPrimaryKeyToModel(entity, Collections.singleton(attr), Collections.singleton(attr2), "PK"));

        test(factory().createSetValueForNullToDb(entity, attr, new DefaultValueForNullProvider()));
    }

    private void test(MergerToken token1) {
        MergerToken token2 = token1.createReverse(factory()).createReverse(factory());

        Assert.assertEquals(token1.getTokenName(), token2.getTokenName());
        Assert.assertEquals(token1.getTokenValue(), token2.getTokenValue());
        Assert.assertEquals(token1.getDirection(), token2.getDirection());
    }

    private MergerTokenFactory factory() {
        return new HSQLMergerTokenFactory();
    }
}
