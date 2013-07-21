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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.RelationshipQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.UpdateBatchQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.quotemap.QuoteAdress;
import org.apache.cayenne.testdo.quotemap.Quote_Person;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.QUOTED_IDENTIFIERS_PROJECT)
public class QuotedIdentifiersTest extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("quote Person");
        dbHelper.deleteAll("QUOTED_ADDRESS");

        QuoteAdress quoteAdress = context.newObject(QuoteAdress.class);
        quoteAdress.setCity("city");
        quoteAdress.setGroup("324");

        Quote_Person quote_Person = context.newObject(Quote_Person.class);
        quote_Person.setSalary(10000);
        quote_Person.setName("Arcadi");
        quote_Person.setGroup("107324");
        quote_Person.setAddress_Rel(quoteAdress);

        context.commitChanges();

        SelectQuery q = new SelectQuery(QuoteAdress.class);
        List objects = context.performQuery(q);
        assertEquals(1, objects.size());

        SelectQuery qQuote_Person = new SelectQuery(Quote_Person.class);
        List objects2 = context.performQuery(qQuote_Person);
        assertEquals(1, objects2.size());

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

    public void testPrefetchQuote() throws Exception {
        DbEntity entity = context
                .getEntityResolver()
                .getObjEntity(QuoteAdress.class)
                .getDbEntity();
        List idAttributes = Collections.singletonList(entity.getAttribute("City"));
        List updatedAttributes = Collections.singletonList(entity.getAttribute("City"));

        UpdateBatchQuery updateQuery = new UpdateBatchQuery(
                entity,
                idAttributes,
                updatedAttributes,
                null,
                1);

        List objects3 = context.performQuery(updateQuery);
        assertEquals(0, objects3.size());

        SelectQuery qQuote_Person2 = new SelectQuery(Quote_Person.class);
        List objects4 = context.performQuery(qQuote_Person2);
        assertEquals(2, objects4.size());
        
        SelectQuery qQuote_Person3 = new SelectQuery(Quote_Person.class, ExpressionFactory.matchExp(
                "salary",100));
        List objects5 = context.performQuery(qQuote_Person3);
        assertEquals(1, objects5.size());
        
        SelectQuery qQuote_Person4 = new SelectQuery(Quote_Person.class, ExpressionFactory.matchExp(
                "group","107324"));
        List objects6 = context.performQuery(qQuote_Person4);
        assertEquals(1, objects6.size());
        
        SelectQuery quoteAdress1 = new SelectQuery(QuoteAdress.class, ExpressionFactory.matchExp(
                "group","324"));
        List objects7 = context.performQuery(quoteAdress1);
        assertEquals(1, objects7.size());
        
        ObjectIdQuery queryObjectId = new ObjectIdQuery(new ObjectId(
                "QuoteAdress",
                QuoteAdress.GROUP_PROPERTY,
                "324"));
        
        List objects8 = context.performQuery(queryObjectId);
        assertEquals(1, objects8.size());
        
        ObjectIdQuery queryObjectId2 = new ObjectIdQuery(new ObjectId(
                "Quote_Person", "GROUP", "1111"));
        List objects9 = context.performQuery(queryObjectId2);
        assertEquals(1, objects9.size());

        SelectQuery person2Query = new SelectQuery(Quote_Person.class, ExpressionFactory.matchExp("name", "Name"));
        Quote_Person quote_Person2 = (Quote_Person) context.performQuery(person2Query).get(0);

        RelationshipQuery relationshipQuery = new RelationshipQuery(quote_Person2.getObjectId(), "address_Rel");
        List objects10 = context.performQuery(relationshipQuery);
        assertEquals(1, objects10.size());
    }

    public void testQuotedEJBQLQuery() throws Exception {
        String ejbql = "select a from QuoteAdress a where a.group = '324'";
        EJBQLQuery queryEJBQL = new EJBQLQuery(ejbql);
        List objects11 = context.performQuery(queryEJBQL);
        assertEquals(1, objects11.size());
    }

    public void testQuotedEJBQLQueryWithJoin() throws Exception {
        String ejbql = "select p from Quote_Person p join p.address_Rel a where p.name = 'Arcadi'";
        EJBQLQuery queryEJBQL = new EJBQLQuery(ejbql);
        List resultList = context.performQuery(queryEJBQL);
        assertEquals(1, resultList.size());
    }

    public void testQuotedEJBQLQueryWithOrderBy() throws Exception {
        EJBQLQuery query = new EJBQLQuery("select p from Quote_Person p order by p.name");

        List<Quote_Person> resultList = (List<Quote_Person>) context.performQuery(query);

        assertEquals(2, resultList.size());
        assertEquals("Arcadi", resultList.get(0).getName());
        assertEquals("Name", resultList.get(1).getName());
    }

}
