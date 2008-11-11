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

package org.apache.cayenne.access;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.art.BigDecimalEntity;
import org.apache.art.BigIntegerEntity;
import org.apache.art.BitTestEntity;
import org.apache.art.BooleanTestEntity;
import org.apache.art.DecimalPKTest1;
import org.apache.art.DecimalPKTestEntity;
import org.apache.art.LongEntity;
import org.apache.art.SmallintTestEntity;
import org.apache.art.TinyintTestEntity;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class NumericTypesTest extends CayenneCase {

    protected DataContext context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        context = createDataContext();
    }

    public void testLong() throws Exception {

        LongEntity test = context.newObject(LongEntity.class);

        Long i = new Long(Integer.MAX_VALUE + 10l);
        test.setLongField(i);
        context.commitChanges();

        SelectQuery q = new SelectQuery(LongEntity.class);
        LongEntity testRead = (LongEntity) context.performQuery(q).get(0);
        assertNotNull(testRead.getLongField());
        assertEquals(i, testRead.getLongField());

        test.setLongField(null);
        context.commitChanges();
    }

    public void testBigInteger() throws Exception {

        BigIntegerEntity test = context
                .newObject(BigIntegerEntity.class);

        BigInteger i = new BigInteger("1234567890");
        test.setBigIntegerField(i);
        context.commitChanges();

        SelectQuery q = new SelectQuery(BigIntegerEntity.class);
        BigIntegerEntity testRead = (BigIntegerEntity) context.performQuery(q).get(0);
        assertNotNull(testRead.getBigIntegerField());
        assertEquals(i, testRead.getBigIntegerField());

        test.setBigIntegerField(null);
        context.commitChanges();
    }

    public void testBigDecimal() throws Exception {

        BigDecimalEntity test = context
                .newObject(BigDecimalEntity.class);

        BigDecimal i = new BigDecimal("1234567890.44");
        test.setBigDecimalField(i);
        context.commitChanges();

        SelectQuery q = new SelectQuery(BigDecimalEntity.class);
        BigDecimalEntity testRead = (BigDecimalEntity) context.performQuery(q).get(0);
        assertNotNull(testRead.getBigDecimalField());
        assertEquals(i, testRead.getBigDecimalField());

        test.setBigDecimalField(null);
        context.commitChanges();
    }

    public void testShortInQualifier() throws Exception {
        createTestData("testShortInQualifier");

        // test
        Expression qual = ExpressionFactory.matchExp("smallintCol", new Short("9999"));
        List objects = context.performQuery(new SelectQuery(
                SmallintTestEntity.class,
                qual));
        assertEquals(1, objects.size());

        SmallintTestEntity object = (SmallintTestEntity) objects.get(0);
        assertEquals(new Short("9999"), object.getSmallintCol());
    }

    public void testShortInInsert() throws Exception {
        SmallintTestEntity object = (SmallintTestEntity) context
                .newObject("SmallintTestEntity");
        object.setSmallintCol(new Short("1"));
        context.commitChanges();
    }

    public void testTinyintInQualifier() throws Exception {
        createTestData("testTinyintInQualifier");

        // test
        Expression qual = ExpressionFactory.matchExp("tinyintCol", new Byte((byte) 81));
        List objects = context
                .performQuery(new SelectQuery(TinyintTestEntity.class, qual));
        assertEquals(1, objects.size());

        TinyintTestEntity object = (TinyintTestEntity) objects.get(0);
        assertEquals(new Byte((byte) 81), object.getTinyintCol());
    }

    public void testTinyintInInsert() throws Exception {
        TinyintTestEntity object = (TinyintTestEntity) context
                .newObject("TinyintTestEntity");
        object.setTinyintCol(new Byte((byte) 1));
        context.commitChanges();
    }

    public void testBooleanBit() throws Exception {

        // populate (testing insert as well)
        BitTestEntity trueObject = (BitTestEntity) context.newObject("BitTestEntity");
        trueObject.setBitColumn(Boolean.TRUE);
        BitTestEntity falseObject = (BitTestEntity) context.newObject("BitTestEntity");
        falseObject.setBitColumn(Boolean.FALSE);
        context.commitChanges();

        // this will clear cache as a side effect
        context = createDataContext();

        // fetch true...
        Expression trueQ = ExpressionFactory.matchExp("bitColumn", Boolean.TRUE);
        List trueResult = context
                .performQuery(new SelectQuery(BitTestEntity.class, trueQ));
        assertEquals(1, trueResult.size());

        BitTestEntity trueRefetched = (BitTestEntity) trueResult.get(0);
        assertEquals(Boolean.TRUE, trueRefetched.getBitColumn());

        // CAY-320. Simplifying the use of booleans to allow identity comparison.
        assertNotSame(trueRefetched, trueObject);
        assertSame(Boolean.TRUE, trueRefetched.getBitColumn());

        // fetch false
        Expression falseQ = ExpressionFactory.matchExp("bitColumn", Boolean.FALSE);
        List falseResult = context.performQuery(new SelectQuery(
                BitTestEntity.class,
                falseQ));
        assertEquals(1, falseResult.size());

        BitTestEntity falseRefetched = (BitTestEntity) falseResult.get(0);
        assertEquals(Boolean.FALSE, falseRefetched.getBitColumn());

        // CAY-320. Simplifying the use of booleans to allow identity comparison.
        assertNotSame(falseRefetched, falseObject);
        assertSame(Boolean.FALSE, falseRefetched.getBitColumn());
    }

    public void testBooleanBoolean() throws Exception {

        // populate (testing insert as well)
        BooleanTestEntity trueObject = (BooleanTestEntity) context
                .newObject("BooleanTestEntity");
        trueObject.setBooleanColumn(Boolean.TRUE);
        BooleanTestEntity falseObject = (BooleanTestEntity) context
                .newObject("BooleanTestEntity");
        falseObject.setBooleanColumn(Boolean.FALSE);
        context.commitChanges();

        // this will clear cache as a side effect
        context = createDataContext();

        // fetch true...
        Expression trueQ = ExpressionFactory.matchExp("booleanColumn", Boolean.TRUE);
        List trueResult = context.performQuery(new SelectQuery(
                BooleanTestEntity.class,
                trueQ));
        assertEquals(1, trueResult.size());

        BooleanTestEntity trueRefetched = (BooleanTestEntity) trueResult.get(0);
        assertEquals(Boolean.TRUE, trueRefetched.getBooleanColumn());

        // CAY-320. Simplifying the use of booleans to allow identity comparison.
        assertNotSame(trueRefetched, trueObject);
        assertSame(Boolean.TRUE, trueRefetched.getBooleanColumn());

        // fetch false
        Expression falseQ = ExpressionFactory.matchExp("booleanColumn", Boolean.FALSE);
        List falseResult = context.performQuery(new SelectQuery(
                BooleanTestEntity.class,
                falseQ));
        assertEquals(1, falseResult.size());

        BooleanTestEntity falseRefetched = (BooleanTestEntity) falseResult.get(0);
        assertEquals(Boolean.FALSE, falseRefetched.getBooleanColumn());

        // CAY-320. Simplifying the use of booleans to allow identity comparison.
        assertNotSame(falseRefetched, falseObject);
        assertSame(Boolean.FALSE, falseRefetched.getBooleanColumn());
    }

    public void testDecimalPK() throws Exception {

        // populate (testing insert as well)
        DecimalPKTestEntity object = context
                .newObject(DecimalPKTestEntity.class);

        object.setName("o1");
        object.setDecimalPK(new BigDecimal("1.25"));
        context.commitChanges();

        Map map = Collections.singletonMap("DECIMAL_PK", new BigDecimal("1.25"));
        ObjectId syntheticId = new ObjectId("DecimalPKTestEntity", map);
        assertSame(object, context.localObject(syntheticId, null));
    }

    public void testDecimalPK1() throws Exception {

        // populate (testing insert as well)
        DecimalPKTest1 object = context.newObject(DecimalPKTest1.class);

        object.setName("o2");
        object.setDecimalPK(new Double(1.25));
        context.commitChanges();

        Map map = Collections.singletonMap("DECIMAL_PK", new Double(1.25));
        ObjectId syntheticId = new ObjectId("DecimalPKTest1", map);
        assertSame(object, context.localObject(syntheticId, null));
    }
}
