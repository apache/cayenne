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
package org.apache.cayenne.exp;

import junit.framework.TestCase;

import org.apache.cayenne.testdo.testmap.BooleanTestEntity;

public class BooleanExpressionTest extends TestCase {
    public void testCAY1185() {
        Expression expTrue = Expression.fromString("booleanColumn = true");
        Expression expFalse = Expression.fromString("booleanColumn = false");

        BooleanTestEntity entity = new BooleanTestEntity();
        entity.setBooleanColumn(true);
        assertTrue(expTrue.match(entity));
        assertFalse(expFalse.match(entity));

        entity.setBooleanColumn(false);
        assertFalse(expTrue.match(entity));
        assertTrue(expFalse.match(entity));
    }
}
