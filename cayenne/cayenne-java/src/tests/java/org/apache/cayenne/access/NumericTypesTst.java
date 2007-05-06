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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.art.BitTest;
import org.apache.art.BooleanTest;
import org.apache.art.DecimalPKTest;
import org.apache.art.DecimalPKTest1;
import org.apache.art.SmallintTest;
import org.apache.art.TinyintTest;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class NumericTypesTst extends CayenneTestCase {

    protected DataContext context;

    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        context = createDataContext();
    }

    public void testShortInQualifier() throws Exception {
        createTestData("testShortInQualifier");

        // test
        Expression qual = ExpressionFactory.matchExp("smallintCol", new Short("9999"));
        List objects = context.performQuery(new SelectQuery(SmallintTest.class, qual));
        assertEquals(1, objects.size());

        SmallintTest object = (SmallintTest) objects.get(0);
        assertEquals(new Short("9999"), object.getSmallintCol());
    }

    public void testShortInInsert() throws Exception {
        SmallintTest object = (SmallintTest) context
                .createAndRegisterNewObject("SmallintTest");
        object.setSmallintCol(new Short("1"));
        context.commitChanges();
    }

    public void testTinyintInQualifier() throws Exception {
        createTestData("testTinyintInQualifier");

        // test
        Expression qual = ExpressionFactory.matchExp("tinyintCol", new Byte((byte) 81));
        List objects = context.performQuery(new SelectQuery(TinyintTest.class, qual));
        assertEquals(1, objects.size());

        TinyintTest object = (TinyintTest) objects.get(0);
        assertEquals(new Byte((byte) 81), object.getTinyintCol());
    }

    public void testTinyintInInsert() throws Exception {
        TinyintTest object = (TinyintTest) context
                .createAndRegisterNewObject("TinyintTest");
        object.setTinyintCol(new Byte((byte) 1));
        context.commitChanges();
    }

    public void testBooleanBit() throws Exception {

        // populate (testing insert as well)
        BitTest trueObject = (BitTest) context.createAndRegisterNewObject("BitTest");
        trueObject.setBitColumn(Boolean.TRUE);
        BitTest falseObject = (BitTest) context.createAndRegisterNewObject("BitTest");
        falseObject.setBitColumn(Boolean.FALSE);
        context.commitChanges();

        // this will clear cache as a side effect
        context = createDataContext();

        // fetch true...
        Expression trueQ = ExpressionFactory.matchExp("bitColumn", Boolean.TRUE);
        List trueResult = context.performQuery(new SelectQuery(BitTest.class, trueQ));
        assertEquals(1, trueResult.size());

        BitTest trueRefetched = (BitTest) trueResult.get(0);
        assertEquals(Boolean.TRUE, trueRefetched.getBitColumn());

        // CAY-320. Simplifying the use of booleans to allow identity comparison.
        assertNotSame(trueRefetched, trueObject);
        assertSame(Boolean.TRUE, trueRefetched.getBitColumn());

        // fetch false
        Expression falseQ = ExpressionFactory.matchExp("bitColumn", Boolean.FALSE);
        List falseResult = context.performQuery(new SelectQuery(BitTest.class, falseQ));
        assertEquals(1, falseResult.size());

        BitTest falseRefetched = (BitTest) falseResult.get(0);
        assertEquals(Boolean.FALSE, falseRefetched.getBitColumn());

        // CAY-320. Simplifying the use of booleans to allow identity comparison.
        assertNotSame(falseRefetched, falseObject);
        assertSame(Boolean.FALSE, falseRefetched.getBitColumn());
    }

    public void testBooleanBoolean() throws Exception {

        // populate (testing insert as well)
        BooleanTest trueObject = (BooleanTest) context
                .createAndRegisterNewObject("BooleanTest");
        trueObject.setBooleanColumn(Boolean.TRUE);
        BooleanTest falseObject = (BooleanTest) context
                .createAndRegisterNewObject("BooleanTest");
        falseObject.setBooleanColumn(Boolean.FALSE);
        context.commitChanges();

        // this will clear cache as a side effect
        context = createDataContext();

        // fetch true...
        Expression trueQ = ExpressionFactory.matchExp("booleanColumn", Boolean.TRUE);
        List trueResult = context.performQuery(new SelectQuery(BooleanTest.class, trueQ));
        assertEquals(1, trueResult.size());

        BooleanTest trueRefetched = (BooleanTest) trueResult.get(0);
        assertEquals(Boolean.TRUE, trueRefetched.getBooleanColumn());

        // CAY-320. Simplifying the use of booleans to allow identity comparison.
        assertNotSame(trueRefetched, trueObject);
        assertSame(Boolean.TRUE, trueRefetched.getBooleanColumn());

        // fetch false
        Expression falseQ = ExpressionFactory.matchExp("booleanColumn", Boolean.FALSE);
        List falseResult = context
                .performQuery(new SelectQuery(BooleanTest.class, falseQ));
        assertEquals(1, falseResult.size());

        BooleanTest falseRefetched = (BooleanTest) falseResult.get(0);
        assertEquals(Boolean.FALSE, falseRefetched.getBooleanColumn());

        // CAY-320. Simplifying the use of booleans to allow identity comparison.
        assertNotSame(falseRefetched, falseObject);
        assertSame(Boolean.FALSE, falseRefetched.getBooleanColumn());
    }

    public void testDecimalPK() throws Exception {

        // populate (testing insert as well)
        DecimalPKTest object = (DecimalPKTest) context
                .createAndRegisterNewObject(DecimalPKTest.class);

        object.setName("o1");
        object.setDecimalPK(new BigDecimal("1.25"));
        context.commitChanges();

        Map map = Collections.singletonMap("DECIMAL_PK", new BigDecimal("1.25"));
        ObjectId syntheticId = new ObjectId("DecimalPKTest", map);
        assertSame(object, context.localObject(syntheticId, null));
    }

    public void testDecimalPK1() throws Exception {

        // populate (testing insert as well)
        DecimalPKTest1 object = (DecimalPKTest1) context
                .createAndRegisterNewObject(DecimalPKTest1.class);

        object.setName("o2");
        object.setDecimalPK(new Double(1.25));
        context.commitChanges();

        Map map = Collections.singletonMap("DECIMAL_PK", new Double(1.25));
        ObjectId syntheticId = new ObjectId("DecimalPKTest1", map);
        assertSame(object, context.localObject(syntheticId, null));
    }
}
