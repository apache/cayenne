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
package org.apache.cayenne.ejbql;

import junit.framework.TestCase;

public class EJBQLExpressionTest extends TestCase {

    public void testDbPath() {

        EJBQLParser parser = EJBQLParserFactory.getParser();

        EJBQLExpression select = parser
                .parse("select p from Painting p WHERE db:p.toArtist.ARTIST_NAME = 'a'");
        assertNotNull(select);
    }

    public void testEnumPath() {
        EJBQLParser parser = EJBQLParserFactory.getParser();

        EJBQLExpression select = parser
                .parse("select p from Painting p WHERE p.toArtist.ARTIST_NAME = enum:org.apache.cayenne.ejbql.EJBQLEnum1.X");
        assertNotNull(select);
    }
}
