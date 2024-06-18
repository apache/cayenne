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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.RelationshipQuery;
import org.apache.cayenne.query.UpdateBatchQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.quotemap.QuoteAdress;
import org.apache.cayenne.testdo.quotemap.Quote_Person;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.QUOTED_IDENTIFIERS_PROJECT)
public class QuotedIdentifiersIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    @Before
    public void setUp() throws Exception {
        QuoteAdress quoteAdress = context.newObject(QuoteAdress.class);
        quoteAdress.setCity("city");
        quoteAdress.setGroup("324");

        Quote_Person quote_Person = context.newObject(Quote_Person.class);
        quote_Person.setSalary(10000);
        quote_Person.setName("Arcadi");
        quote_Person.setGroup("107324");
        quote_Person.setAddress_Rel(quoteAdress);

        context.commitChanges();

        QuoteAdress quoteAdress2 = context.newObject(QuoteAdress.class);
        quoteAdress2.setCity("city2");

        Quote_Person quote_Person2 = context.newObject(Quote_Person.class);
        quote_Person2.setSalary(100);
        quote_Person2.setName("Name");
        quote_Person2.setGroup("1111");
        quote_Person2.setDAte(new Date());
        quote_Person2.setAddress_Rel(quoteAdress2);

        context.commitChanges();
    }

    @Test
    public void testDataSetup() {
        List<QuoteAdress> objects = ObjectSelect.query(QuoteAdress.class).select(context);
        assertEquals(2, objects.size());

        List<Quote_Person> objects2 = ObjectSelect.query(Quote_Person.class).select(context);
        assertEquals(2, objects2.size());
    }

    @Test
    public void testInsert() {
        QuoteAdress quoteAdress = context.newObject(QuoteAdress.class);
        quoteAdress.setCity("city");
        quoteAdress.setGroup("324");

        Quote_Person quote_Person = context.newObject(Quote_Person.class);
        quote_Person.setSalary(10000);
        quote_Person.setName("Arcadi");
        quote_Person.setGroup("107324");
        quote_Person.setAddress_Rel(quoteAdress);

        context.commitChanges();

        QuoteAdress quoteAdress2 = context.newObject(QuoteAdress.class);
        quoteAdress2.setCity("city2");

        Quote_Person quote_Person2 = context.newObject(Quote_Person.class);
        quote_Person2.setSalary(100);
        quote_Person2.setName("Name");
        quote_Person2.setGroup("1111");
        quote_Person2.setDAte(new Date());
        quote_Person2.setAddress_Rel(quoteAdress2);

        context.commitChanges();

        List<QuoteAdress> objects = ObjectSelect.query(QuoteAdress.class).select(context);
        assertEquals(4, objects.size());

        List<Quote_Person> objects2 = ObjectSelect.query(Quote_Person.class).select(context);
        assertEquals(4, objects2.size());
    }

    @Test
    public void testPrefetchQuote() {
        DbEntity entity = context.getEntityResolver().getObjEntity(QuoteAdress.class).getDbEntity();
        List<DbAttribute> idAttributes = Collections.singletonList(entity.getAttribute("City"));
        List<DbAttribute> updatedAttributes = Collections.singletonList(entity.getAttribute("City"));

        UpdateBatchQuery updateQuery = new UpdateBatchQuery(entity, idAttributes, updatedAttributes, Collections.emptySet(), 1);

        List objects3 = context.performQuery(updateQuery);
        assertEquals(0, objects3.size());

        List<Quote_Person> objects4 = ObjectSelect.query(Quote_Person.class).select(context);
        assertEquals(2, objects4.size());

        List<Quote_Person> objects5 = ObjectSelect.query(Quote_Person.class, Quote_Person.SALARY.eq(100)).select(context);
        assertEquals(1, objects5.size());

        List<Quote_Person> objects6 =ObjectSelect.query(Quote_Person.class, Quote_Person.GROUP.eq("107324")).select(context);
        assertEquals(1, objects6.size());

        List<QuoteAdress> objects7 = ObjectSelect.query(QuoteAdress.class, QuoteAdress.GROUP.eq("324")).select(context);
        assertEquals(1, objects7.size());

        ObjectIdQuery queryObjectId = new ObjectIdQuery(ObjectId.of("QuoteAdress", QuoteAdress.GROUP.getName(), "324"));

        List objects8 = context.performQuery(queryObjectId);
        assertEquals(1, objects8.size());

        ObjectIdQuery queryObjectId2 = new ObjectIdQuery(ObjectId.of("Quote_Person", "GROUP", "1111"));
        List objects9 = context.performQuery(queryObjectId2);
        assertEquals(1, objects9.size());

        Quote_Person quote_Person2 = ObjectSelect.query(Quote_Person.class, Quote_Person.NAME.eq("Name")).selectOne(context);

        RelationshipQuery relationshipQuery = new RelationshipQuery(quote_Person2.getObjectId(), "address_Rel");
        List objects10 = context.performQuery(relationshipQuery);
        assertEquals(1, objects10.size());
    }

    @Test
    public void testQuotedEJBQLQuery() {
        String ejbql = "select a from QuoteAdress a where a.group = '324'";
        EJBQLQuery queryEJBQL = new EJBQLQuery(ejbql);
        List objects11 = context.performQuery(queryEJBQL);
        assertEquals(1, objects11.size());
    }

    @Test
    public void testQuotedEJBQLQueryWithJoin() {
        String ejbql = "select p from Quote_Person p join p.address_Rel a where p.name = 'Arcadi'";
        EJBQLQuery queryEJBQL = new EJBQLQuery(ejbql);
        List resultList = context.performQuery(queryEJBQL);
        assertEquals(1, resultList.size());
    }

    @Test
    public void testQuotedEJBQLQueryWithOrderBy() {
        EJBQLQuery query = new EJBQLQuery("select p from Quote_Person p order by p.name");

        @SuppressWarnings("unchecked")
        List<Quote_Person> resultList = (List<Quote_Person>) context.performQuery(query);

        assertEquals(2, resultList.size());
        assertEquals("Arcadi", resultList.get(0).getName());
        assertEquals("Name", resultList.get(1).getName());
    }

    @Test
    public void testQuotedEJBQLCountQuery() {
        EJBQLQuery query = new EJBQLQuery("select count(p) from Quote_Person p");
        assertEquals(Collections.singletonList(2L), context.performQuery(query));

        query = new EJBQLQuery("select count(p.fULL_name) from Quote_Person p");
        assertEquals(Collections.singletonList(0L), context.performQuery(query));
    }

}
