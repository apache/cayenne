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

package org.apache.cayenne.access;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.numeric_types.BigDecimalEntity;
import org.apache.cayenne.testdo.numeric_types.BigIntegerEntity;
import org.apache.cayenne.testdo.numeric_types.BitTestEntity;
import org.apache.cayenne.testdo.numeric_types.BooleanTestEntity;
import org.apache.cayenne.testdo.numeric_types.DecimalPKTest1;
import org.apache.cayenne.testdo.numeric_types.DecimalPKTestEntity;
import org.apache.cayenne.testdo.numeric_types.LongEntity;
import org.apache.cayenne.testdo.numeric_types.SmallintTestEntity;
import org.apache.cayenne.testdo.numeric_types.TinyintTestEntity;
import org.apache.cayenne.unit.di.CommitStats;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
@UseCayenneRuntime(CayenneProjects.NUMERIC_TYPES_PROJECT)
public class NumericTypesIT extends RuntimeCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DataContext context1;

    @Inject
    protected CayenneRuntime runtime;

    @Inject
    protected DBHelper dbHelper;

    private final CommitStats commitStats = new CommitStats(() -> runtime.getDataDomain());

    protected TableHelper tSmallintTest;
    protected TableHelper tTinyintTest;

    @Before
    public void before() {
        commitStats.before();

        tSmallintTest = new TableHelper(dbHelper, "SMALLINT_TEST");
        tSmallintTest.setColumns("ID", "SMALLINT_COL");

        tTinyintTest = new TableHelper(dbHelper, "TINYINT_TEST");
        tTinyintTest.setColumns("ID", "TINYINT_COL");
    }

    @After
    public void after() {
        commitStats.after();
    }

    protected void createShortDataSet() throws Exception {
        tSmallintTest.insert(1, 9999);
        tSmallintTest.insert(2, 3333);
    }

    protected void createTinyintDataSet() throws Exception {
        tTinyintTest.insert(1, 81);
        tTinyintTest.insert(2, 50);
    }

    @Test
    public void testLong() throws Exception {

        LongEntity test = context.newObject(LongEntity.class);

        Long i = Integer.MAX_VALUE + 10L;
        test.setLongField(i);
        context.commitChanges();

        LongEntity testRead = ObjectSelect.query(LongEntity.class)
                .selectFirst(context);
        assertNotNull(testRead.getLongField());
        assertEquals(i, testRead.getLongField());

        test.setLongField(null);
        context.commitChanges();
    }

    @Test
    public void testBigInteger() throws Exception {

        BigIntegerEntity test = context.newObject(BigIntegerEntity.class);

        BigInteger i = new BigInteger("1234567890");
        test.setBigIntegerField(i);
        context.commitChanges();

        BigIntegerEntity testRead = ObjectSelect.query(BigIntegerEntity.class)
                .selectFirst(context);
        assertNotNull(testRead.getBigIntegerField());
        assertEquals(i, testRead.getBigIntegerField());

        test.setBigIntegerField(null);
        context.commitChanges();
    }

    @Test
    public void testBigDecimal_Decimal() {

        // this matches the column scale exactly
        BigDecimal v1 = new BigDecimal("7890.123456");
        // this has lower scale than the column
        BigDecimal v2 = new BigDecimal("7890.1");
        BigDecimal v2_padded = new BigDecimal("7890.100000");

        BigDecimalEntity o = context.newObject(BigDecimalEntity.class);
        o.setBigDecimalDecimal(v1);
        o.getObjectContext().commitChanges();
        assertEquals(1, commitStats.getCommitCount());
        BigDecimalEntity o1 = ObjectSelect.query(BigDecimalEntity.class).selectFirst(runtime.newContext());
        assertEquals(0, v1.compareTo(o1.getBigDecimalDecimal()));

        o.setBigDecimalDecimal(v2);
        o.getObjectContext().commitChanges();
        BigDecimalEntity o2 = ObjectSelect.query(BigDecimalEntity.class).selectFirst(runtime.newContext());
        assertEquals(0, v2_padded.compareTo(o2.getBigDecimalDecimal()));
        assertEquals(2, commitStats.getCommitCount());

        o2.setBigDecimalDecimal(v2);
        o2.getObjectContext().commitChanges();
        assertEquals("Commit was not expected. The difference is purely in value padding", 2, commitStats.getCommitCount());
        BigDecimalEntity o3 = ObjectSelect.query(BigDecimalEntity.class).selectFirst(runtime.newContext());
        assertEquals(0, v2_padded.compareTo(o3.getBigDecimalDecimal()));

        o3.setBigDecimalDecimal(null);
        o3.getObjectContext().commitChanges();
        assertEquals(3, commitStats.getCommitCount());
        BigDecimalEntity o4 = ObjectSelect.query(BigDecimalEntity.class).selectFirst(runtime.newContext());
        assertNull(o4.getBigDecimalDecimal());
    }

    @Test
    public void testBigDecimal_Numeric() {

        BigDecimal v1 = new BigDecimal("1234567890.44");
        BigDecimal v2 = new BigDecimal("1234567890.4");
        BigDecimal v2_padded = new BigDecimal("1234567890.40");

        BigDecimalEntity o = context.newObject(BigDecimalEntity.class);
        o.setBigDecimalNumeric(v1);
        o.getObjectContext().commitChanges();
        assertEquals(1, commitStats.getCommitCount());
        BigDecimalEntity o1 = ObjectSelect.query(BigDecimalEntity.class).selectFirst(runtime.newContext());
        assertEquals(0, v1.compareTo(o1.getBigDecimalNumeric()));

        o1.setBigDecimalNumeric(v2);
        o1.getObjectContext().commitChanges();
        assertEquals(2, commitStats.getCommitCount());
        BigDecimalEntity o2 = ObjectSelect.query(BigDecimalEntity.class).selectFirst(runtime.newContext());
        assertEquals(0, v2_padded.compareTo(o2.getBigDecimalNumeric()));

        o2.setBigDecimalNumeric(v2);
        assertEquals("Commit was not expected. The difference is purely in value padding", 2, commitStats.getCommitCount());
        BigDecimalEntity o3 = ObjectSelect.query(BigDecimalEntity.class).selectFirst(runtime.newContext());
        assertEquals(0, v2_padded.compareTo(o3.getBigDecimalNumeric()));

        o3.setBigDecimalNumeric(null);
        o3.getObjectContext().commitChanges();
        assertEquals(3, commitStats.getCommitCount());
        BigDecimalEntity o4 = ObjectSelect.query(BigDecimalEntity.class).selectFirst(runtime.newContext());
        assertNull(o4.getBigDecimalNumeric());
    }

    @Test
    public void testShortInQualifier() throws Exception {
        createShortDataSet();

        // test
        List<SmallintTestEntity> objects = ObjectSelect.query(SmallintTestEntity.class)
                .where(SmallintTestEntity.SMALLINT_COL.eq(Short.valueOf("9999")))
                .select(context);
        assertEquals(1, objects.size());

        SmallintTestEntity object = objects.get(0);
        assertEquals(Short.valueOf("9999"), object.getSmallintCol());
    }

    @Test
    public void testShortInInsert() throws Exception {
        SmallintTestEntity object = (SmallintTestEntity) (context)
                .newObject("SmallintTestEntity");
        object.setSmallintCol(Short.valueOf("1"));
        context.commitChanges();
    }

    @Test
    public void testTinyintInQualifier() throws Exception {
        createTinyintDataSet();

        // test
        List<?> objects = ObjectSelect.query(TinyintTestEntity.class)
                .where(TinyintTestEntity.TINYINT_COL.eq((byte) 81))
                .select(context);
        assertEquals(1, objects.size());

        TinyintTestEntity object = (TinyintTestEntity) objects.get(0);
        assertEquals(Byte.valueOf((byte) 81), object.getTinyintCol());
    }

    @Test
    public void testTinyintInInsert() throws Exception {
        TinyintTestEntity object = (TinyintTestEntity) (context)
                .newObject("TinyintTestEntity");
        object.setTinyintCol((byte) 1);
        context.commitChanges();
    }

    @Test
    public void testBooleanBit() throws Exception {

        BitTestEntity trueObject = (BitTestEntity) context.newObject("BitTestEntity");
        trueObject.setBitColumn(Boolean.TRUE);
        BitTestEntity falseObject = (BitTestEntity) context.newObject("BitTestEntity");
        falseObject.setBitColumn(Boolean.FALSE);
        context.commitChanges();
        context.invalidateObjects(trueObject, falseObject);

        // fetch true...
        List<?> trueResult = ObjectSelect.query(BitTestEntity.class)
                .where(BitTestEntity.BIT_COLUMN.eq(Boolean.TRUE))
                .select(context1);
        assertEquals(1, trueResult.size());

        BitTestEntity trueRefetched = (BitTestEntity) trueResult.get(0);
        assertEquals(Boolean.TRUE, trueRefetched.getBitColumn());

        // CAY-320. Simplifying the use of booleans to allow identity comparison.
        assertNotSame(trueRefetched, trueObject);
        assertSame(Boolean.TRUE, trueRefetched.getBitColumn());

        // fetch false
        List<?> falseResult = ObjectSelect.query(BitTestEntity.class)
                .where(BitTestEntity.BIT_COLUMN.eq(Boolean.FALSE))
                .select(context1);
        assertEquals(1, falseResult.size());

        BitTestEntity falseRefetched = (BitTestEntity) falseResult.get(0);
        assertEquals(Boolean.FALSE, falseRefetched.getBitColumn());

        // CAY-320. Simplifying the use of booleans to allow identity comparison.
        assertNotSame(falseRefetched, falseObject);
        assertSame(Boolean.FALSE, falseRefetched.getBitColumn());
    }

    @Test
    public void testBooleanBoolean() throws Exception {

        // populate (testing insert as well)
        BooleanTestEntity trueObject = (BooleanTestEntity) context
                .newObject("BooleanTestEntity");
        trueObject.setBooleanColumn(Boolean.TRUE);
        BooleanTestEntity falseObject = (BooleanTestEntity) context
                .newObject("BooleanTestEntity");
        falseObject.setBooleanColumn(Boolean.FALSE);
        context.commitChanges();

        context.invalidateObjects(trueObject, falseObject);

        // fetch true...
        List<?> trueResult = ObjectSelect.query(BooleanTestEntity.class)
                .where(BooleanTestEntity.BOOLEAN_COLUMN.eq(Boolean.TRUE))
                .select(context1);
        assertEquals(1, trueResult.size());

        BooleanTestEntity trueRefetched = (BooleanTestEntity) trueResult.get(0);
        assertEquals(Boolean.TRUE, trueRefetched.getBooleanColumn());

        // CAY-320. Simplifying the use of booleans to allow identity comparison.
        assertNotSame(trueRefetched, trueObject);
        assertSame(Boolean.TRUE, trueRefetched.getBooleanColumn());

        // fetch false
        List<?> falseResult = ObjectSelect.query(BooleanTestEntity.class)
                .where(BooleanTestEntity.BOOLEAN_COLUMN.eq(Boolean.FALSE))
                .select(context1);
        assertEquals(1, falseResult.size());

        BooleanTestEntity falseRefetched = (BooleanTestEntity) falseResult.get(0);
        assertEquals(Boolean.FALSE, falseRefetched.getBooleanColumn());

        // CAY-320. Simplifying the use of booleans to allow identity comparison.
        assertNotSame(falseRefetched, falseObject);
        assertSame(Boolean.FALSE, falseRefetched.getBooleanColumn());
    }

    @Test
    public void testDecimalPK() throws Exception {

        // populate (testing insert as well)
        DecimalPKTestEntity object = context.newObject(DecimalPKTestEntity.class);

        object.setName("o1");
        object.setDecimalPK(new BigDecimal("1.25"));
        context.commitChanges();

        ObjectId syntheticId = ObjectId.of(
                "DecimalPKTestEntity",
                "DECIMAL_PK",
                new BigDecimal("1.25"));
        assertSame(object, context.getGraphManager().getNode(syntheticId));

        context.deleteObjects(object);
        context.commitChanges();
    }

    @Test
    public void testDecimalPK1() throws Exception {

        // populate (testing insert as well)
        DecimalPKTest1 object = context.newObject(DecimalPKTest1.class);

        object.setName("o2");
        object.setDecimalPK(1.25);
        context.commitChanges();

        ObjectId syntheticId = ObjectId.of("DecimalPKTest1", "DECIMAL_PK", 1.25);
        assertSame(object, context.getGraphManager().getNode(syntheticId));
    }

    @Test
    public void testBigIntegerColumnSelect() {
        BigIntegerEntity test = context.newObject(BigIntegerEntity.class);
        BigInteger i = new BigInteger("1234567890");
        test.setBigIntegerField(i);
        context.commitChanges();

        BigInteger readValue = ObjectSelect.query(BigIntegerEntity.class)
                .column(BigIntegerEntity.BIG_INTEGER_FIELD).selectOne(context);

        assertEquals(i, readValue);

        NumericProperty<BigInteger> calculated =
                PropertyFactory.createNumeric(ExpressionFactory.exp("bigIntegerField + 1"), BigInteger.class);

        BigInteger readValue2 = ObjectSelect.query(BigIntegerEntity.class)
                .column(calculated).selectOne(context);
        assertEquals(i.add(BigInteger.ONE), readValue2);
    }
}
