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

package org.apache.cayenne.tools.model;

import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.0
 */
public class PatternParamTest {

    private PatternParam param;

    @Before
    public void createNewPatternParam() {
        param = new PatternParam("test");
    }

    @Test
    public void toExcludeTable() throws Exception {
        ExcludeTable table = param.toExcludeTable();
        assertNotNull(table);
        assertEquals("test", table.getPattern());
    }

    @Test
    public void toIncludeColumn() throws Exception {
        IncludeColumn table = param.toIncludeColumn();
        assertNotNull(table);
        assertEquals("test", table.getPattern());
    }

    @Test
    public void toExcludeColumn() throws Exception {
        ExcludeColumn table = param.toExcludeColumn();
        assertNotNull(table);
        assertEquals("test", table.getPattern());
    }

    @Test
    public void toIncludeProcedure() throws Exception {
        IncludeProcedure table = param.toIncludeProcedure();
        assertNotNull(table);
        assertEquals("test", table.getPattern());
    }

    @Test
    public void toExcludeProcedure() throws Exception {
        ExcludeProcedure table = param.toExcludeProcedure();
        assertNotNull(table);
        assertEquals("test", table.getPattern());
    }

}
