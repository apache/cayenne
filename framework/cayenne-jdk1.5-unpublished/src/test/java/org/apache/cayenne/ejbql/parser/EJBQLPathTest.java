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
package org.apache.cayenne.ejbql.parser;

import junit.framework.TestCase;

public class EJBQLPathTest extends TestCase {

    public void testGetAbsolutePath() {
        EJBQLPath path = new EJBQLPath(-1);
        EJBQLIdentifier id = new EJBQLIdentifier(-1);
        id.setText("x");

        EJBQLIdentificationVariable c1 = new EJBQLIdentificationVariable(-1);
        c1.setText("y");
        
        path.jjtAddChild(id, 0);
        path.jjtAddChild(c1, 1);
        assertEquals("x.y", path.getAbsolutePath());
    }
}
