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
package org.apache.cayenne;

import org.apache.art.Continent;
import org.apache.art.Country;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class LightSuperClassTest extends CayenneCase {
    public void testServer() {
        doTest(createDataContext());
    }
    
    private void doTest(ObjectContext context) {
        Continent continent = context.newObject(Continent.class);
        continent.setName("Europe");
        
        Country country = new Country();
        country.setName("Russia");
        context.registerNewObject(country);
        
        country.setContinent(continent);
        assertEquals(continent.getCountries().size(), 1);
        
        context.commitChanges();
        
        context.deleteObject(country);
        assertEquals(continent.getCountries().size(), 0);
        continent.setName("Australia");
        
        context.commitChanges();
        context.performQuery(new RefreshQuery());
        
        assertEquals(context.performQuery(new SelectQuery(Country.class)).size(), 0);
        assertEquals(context.performQuery(new SelectQuery(Continent.class)).size(), 1);
    }
}
