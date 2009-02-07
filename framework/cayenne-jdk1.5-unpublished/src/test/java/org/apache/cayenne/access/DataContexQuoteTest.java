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

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.UpdateBatchQuery;
import org.apache.cayenne.testdo.quotemap.QuoteAdress;
import org.apache.cayenne.testdo.quotemap.Quote_Person;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;



public class DataContexQuoteTest extends CayenneCase{
    private DataContext context;

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(QUOTEMAP_ACCESS_STACK);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
        context = createDataContext();
    }
    
    public void testPrefetchQuote() throws Exception {
        
        // work with tables QuoteAdress and Quote_Person. 
        // In this table parameter quoteSqlIdentifiers = true. 
        
        QuoteAdress quoteAdress = (QuoteAdress) context.newObject("QuoteAdress");
        quoteAdress.setCity("city");
        
        Quote_Person quote_Person = (Quote_Person) context.newObject("Quote_Person");
        quote_Person.setSalary(10000);
        quote_Person.setName("Arcadi");
        
        context.commitChanges();
        
        SelectQuery q = new SelectQuery(QuoteAdress.class);
        List objects = context.performQuery(q);
        assertEquals(1, objects.size());
        
        
        SelectQuery qQuote_Person = new SelectQuery(Quote_Person.class);
        List objects2 = context.performQuery(qQuote_Person);
        assertEquals(1, objects2.size());
        
        QuoteAdress quoteAdress2 = (QuoteAdress) context.newObject("QuoteAdress");
        quoteAdress2.setCity("city2");
        
        Quote_Person quote_Person2 = (Quote_Person) context.newObject("Quote_Person");
        quote_Person2.setSalary(100);
        quote_Person2.setName("Name");
        quote_Person2.setDAte(new Date());
        
        context.commitChanges();
        
        DbEntity entity = getDomain().getEntityResolver().lookupObjEntity(
                QuoteAdress.class).getDbEntity();
        List idAttributes = Collections.singletonList(entity
                    .getAttribute("City"));
        List updatedAttributes = Collections.singletonList(entity
                    .getAttribute("City"));

        UpdateBatchQuery updateQuery = new UpdateBatchQuery(
                    entity,
                    idAttributes,
                    updatedAttributes,
                    null,
                    1);
        
        List objects3 = context.performQuery(updateQuery);
        assertEquals(0, objects3.size());
  
        SelectQuery qQuote_Person2 = new SelectQuery(Quote_Person.class);
        List objects4 = context.performQuery(qQuote_Person);
        assertEquals(2, objects4.size());
 
        
    }

}
