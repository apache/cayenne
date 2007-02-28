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
package org.apache.cayenne.query;

import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLParser;
import org.apache.cayenne.ejbql.EJBQLParserFactory;
import org.apache.cayenne.unit.CayenneCase;

public class EJBQLEntityMapperTest extends CayenneCase {

    public void testGetMappedEntity() {
        EJBQLEntityMapper mapper = new EJBQLEntityMapper(getDomain().getEntityResolver());
        assertNull(mapper.getMappedEntity("a"));

        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLExpression select = parser.parse("select a from Artist a");
        select.visit(mapper);

        assertNotNull(mapper.getMappedEntity("a"));
        assertSame(getDomain().getEntityResolver().getObjEntity("Artist"), mapper
                .getMappedEntity("a"));
    }

    public void testGetMappedEntityCaseSensitivity() {
        EJBQLParser parser = EJBQLParserFactory.getParser();

        EJBQLExpression select1 = parser.parse("select a from Artist a");
        EJBQLEntityMapper mapper1 = new EJBQLEntityMapper(getDomain().getEntityResolver());
        select1.visit(mapper1);
        assertNotNull(mapper1.getMappedEntity("a"));
        assertNotNull(mapper1.getMappedEntity("A"));

        EJBQLExpression select2 = parser.parse("select A from Artist A");
        EJBQLEntityMapper mapper2 = new EJBQLEntityMapper(getDomain().getEntityResolver());
        select2.visit(mapper2);
        assertNotNull(mapper2.getMappedEntity("a"));
        assertNotNull(mapper2.getMappedEntity("A"));
        
        EJBQLExpression select3 = parser.parse("select a from Artist A");
        EJBQLEntityMapper mapper3 = new EJBQLEntityMapper(getDomain().getEntityResolver());
        select3.visit(mapper3);
        assertNotNull(mapper3.getMappedEntity("a"));
        assertNotNull(mapper3.getMappedEntity("A"));
    }
}
