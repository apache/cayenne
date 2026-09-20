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
package org.apache.cayenne.docs;

import org.apache.cayenne.QueryResult;
import org.apache.cayenne.docs.customquery.MyDelegatingQuery;
import org.apache.cayenne.docs.customquery.MyQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class CustomQueryTest extends BaseTest {

    @Test
    public void myQuery() {
        assertEquals(0, context.execute(new MyQuery()).size());
    }

    @Test
    public void myDelegatingQuery() {
        createArtistsDataSet();

        List<QueryResult> result = context.execute(new MyDelegatingQuery());
        QueryResult.Select<?> select = assertInstanceOf(QueryResult.Select.class, result.getFirst());
        assertEquals(3, select.objects().size());
    }
}
