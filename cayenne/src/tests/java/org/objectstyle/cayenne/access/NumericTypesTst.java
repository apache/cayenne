/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.BitTest;
import org.objectstyle.art.DecimalPKTest;
import org.objectstyle.art.DecimalPKTest1;
import org.objectstyle.art.SmallintTest;
import org.objectstyle.art.TinyintTest;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

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
        SmallintTest object =
            (SmallintTest) context.createAndRegisterNewObject("SmallintTest");
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
        TinyintTest object =
            (TinyintTest) context.createAndRegisterNewObject("TinyintTest");
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

        Expression qual = ExpressionFactory.matchExp("bitColumn", Boolean.TRUE);
        List objects = context.performQuery(new SelectQuery(BitTest.class, qual));
        assertEquals(1, objects.size());

        BitTest object = (BitTest) objects.get(0);
        assertEquals(Boolean.TRUE, object.getBitColumn());
    }

    // mapping bit as an integer doesn't work on most databases (except for MySQL, as always),
    // this test case is commented out just in case we need to do more testing with it
    /*
    
    public void testNumericBit() throws Exception {
    
        // populate (testing insert as well)
        BitNumberTest trueObject = (BitNumberTest) context.createAndRegisterNewObject("BitNumberTest");
        trueObject.setBitColumn(new Integer(1));
        BitNumberTest falseObject = (BitNumberTest) context.createAndRegisterNewObject("BitNumberTest");
        falseObject.setBitColumn(new Integer(0));
        context.commitChanges();
    
        // this will clear cache as a side effect
        context = createDataContext();
    
        Expression qual = ExpressionFactory.matchExp("bitColumn", new Integer(1));
        List objects = context.performQuery(new SelectQuery(BitNumberTest.class, qual));
        assertEquals(1, objects.size());
    
        BitNumberTest object = (BitNumberTest) objects.get(0);
        assertEquals(new Integer(1), object.getBitColumn());
    } */

    public void testDecimalPK() throws Exception {

        // populate (testing insert as well)
        DecimalPKTest object =
            (DecimalPKTest) context.createAndRegisterNewObject(DecimalPKTest.class);

        object.setName("o1");
        object.setDecimalPK(new BigDecimal("1.25"));
        context.commitChanges();

        Map map = Collections.singletonMap("DECIMAL_PK", new BigDecimal("1.25"));
        ObjectId syntheticId = new ObjectId(DecimalPKTest.class, map);
        assertSame(object, context.registeredObject(syntheticId));
    }

    public void testDecimalPK1() throws Exception {

        // populate (testing insert as well)
        DecimalPKTest1 object =
            (DecimalPKTest1) context.createAndRegisterNewObject(DecimalPKTest1.class);

        object.setName("o2");
        object.setDecimalPK(new Double(1.25));
        context.commitChanges();

        Map map = Collections.singletonMap("DECIMAL_PK", new Double(1.25));
        ObjectId syntheticId = new ObjectId(DecimalPKTest1.class, map);
        assertSame(object, context.registeredObject(syntheticId));
    }
}
