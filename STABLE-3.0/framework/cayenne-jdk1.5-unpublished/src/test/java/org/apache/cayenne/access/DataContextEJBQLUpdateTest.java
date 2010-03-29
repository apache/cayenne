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

import java.util.HashMap;
import java.util.Map;

import org.apache.art.Artist;
import org.apache.art.BooleanTestEntity;
import org.apache.art.CompoundPkTestEntity;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextEJBQLUpdateTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        deleteTestData();
    }

    public void testUpdateQualifier() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
                + "WHERE p.paintingTitle is NULL or p.paintingTitle <> 'XX'");

        Object notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(2l), notUpdated);

        String ejbql = "UPDATE Painting AS p SET p.paintingTitle = 'XX' WHERE p.paintingTitle = 'P1'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(1, count[0]);

        notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(1l), notUpdated);
    }

    public void testUpdateNoQualifierString() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
                + "WHERE p.paintingTitle is NULL or p.paintingTitle <> 'XX'");

        Object notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(2l), notUpdated);

        String ejbql = "UPDATE Painting AS p SET p.paintingTitle = 'XX'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(0l), notUpdated);
    }

    public void testUpdateNoQualifierNull() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
                + "WHERE p.estimatedPrice is not null");

        Object notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(2l), notUpdated);

        String ejbql = "UPDATE Painting AS p SET p.estimatedPrice = NULL";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(0l), notUpdated);
    }

    // This fails until we implement arithmetic exps
    
//    public void testUpdateNoQualifierArithmeticExpression() throws Exception {
//        createTestData("prepare");
//
//        ObjectContext context = createDataContext();
//
//        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
//                + "WHERE p.paintingTitle is NULL or p.estimatedPrice <= 5000");
//
//        Object notUpdated = DataObjectUtils.objectForQuery(context, check);
//        assertEquals(new Long(2l), notUpdated);
//
//        String ejbql = "UPDATE Painting AS p SET p.estimatedPrice = p.estimatedPrice * 2";
//        EJBQLQuery query = new EJBQLQuery(ejbql);
//
//        QueryResponse result = context.performGenericQuery(query);
//
//        int[] count = result.firstUpdateCount();
//        assertNotNull(count);
//        assertEquals(1, count.length);
//        assertEquals(2, count[0]);
//
//        notUpdated = DataObjectUtils.objectForQuery(context, check);
//        assertEquals(new Long(0l), notUpdated);
//    }

    public void testUpdateNoQualifierMultipleItems() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
                + "WHERE p.estimatedPrice is NULL or p.estimatedPrice <> 1");

        Object notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(2l), notUpdated);

        String ejbql = "UPDATE Painting AS p SET p.paintingTitle = 'XX', p.estimatedPrice = 1";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(0l), notUpdated);
    }

    public void testUpdateNoQualifierDecimal() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
                + "WHERE p.estimatedPrice is NULL or p.estimatedPrice <> 1.1");

        Object notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(2l), notUpdated);

        String ejbql = "UPDATE Painting AS p SET p.estimatedPrice = 1.1";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(0l), notUpdated);
    }

    public void testUpdateNoQualifierBoolean() throws Exception {

        ObjectContext context = createDataContext();
        BooleanTestEntity o1 = context
                .newObject(BooleanTestEntity.class);
        o1.setBooleanColumn(Boolean.TRUE);

        BooleanTestEntity o2 = context
                .newObject(BooleanTestEntity.class);
        o2.setBooleanColumn(Boolean.FALSE);

        BooleanTestEntity o3 = context
                .newObject(BooleanTestEntity.class);
        o3.setBooleanColumn(Boolean.FALSE);

        context.commitChanges();

        EJBQLQuery check = new EJBQLQuery("select count(p) from BooleanTestEntity p "
                + "WHERE p.booleanColumn = true");

        Object notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(1l), notUpdated);

        String ejbql = "UPDATE BooleanTestEntity AS p SET p.booleanColumn = true";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(3, count[0]);

        notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(3l), notUpdated);
    }

    public void testUpdateNoQualifierToOne() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();
        Artist object = DataObjectUtils
                .objectForPK(context, Artist.class, 33003);

        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
                + "WHERE p.toArtist <> :artist");
        check.setParameter("artist", object);

        Object notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(2l), notUpdated);

        String ejbql = "UPDATE Painting AS p SET p.toArtist = :artist";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("artist", object);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(0l), notUpdated);
    }

    public void testUpdateNoQualifierToOneCompoundPK() throws Exception {
        createTestData("prepareCompound");

        ObjectContext context = createDataContext();
        Map<String, String> key1 = new HashMap<String, String>();
        key1.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "b1");
        key1.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "b2");
        CompoundPkTestEntity object = DataObjectUtils.objectForPK(
                context,
                CompoundPkTestEntity.class,
                key1);

        EJBQLQuery check = new EJBQLQuery(
                "select count(e) from CompoundFkTestEntity e WHERE e.toCompoundPk <> :param");
        check.setParameter("param", object);

        Object notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(1l), notUpdated);

        String ejbql = "UPDATE CompoundFkTestEntity e SET e.toCompoundPk = :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", object);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(0l), notUpdated);
    }

}
