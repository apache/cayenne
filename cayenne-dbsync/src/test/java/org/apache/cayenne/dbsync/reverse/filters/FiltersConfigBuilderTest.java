/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.cayenne.dbsync.reverse.filters;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FiltersConfigBuilderTest {

    @Test
    public void testCompact_01() {
        ReverseEngineering engineering = new ReverseEngineering();
        engineering.addIncludeTable(new IncludeTable("table1"));
        engineering.addIncludeTable(new IncludeTable("table2"));
        engineering.addIncludeTable(new IncludeTable("table3"));

        engineering.addIncludeColumn(new IncludeColumn("includeColumn"));

        FiltersConfigBuilder builder = new FiltersConfigBuilder(engineering);
        builder.compact();
        assertEquals(
                "ReverseEngineering: \n" +
                "  Catalog: null\n" +
                "    Schema: null\n" +
                "      IncludeTable: table1\n" +
                "        IncludeColumn: includeColumn\n" +
                "      IncludeTable: table2\n" +
                "        IncludeColumn: includeColumn\n" +
                "      IncludeTable: table3\n" +
                "        IncludeColumn: includeColumn\n\n" +
                "  Use primitives", engineering.toString());
    }

    @Test
    public void testCompact_02() {
        ReverseEngineering engineering = new ReverseEngineering();
        engineering.addCatalog(new Catalog("catalogName"));
        engineering.addSchema(new Schema("schemaName01"));
        engineering.addSchema(new Schema("schemaName02"));

        engineering.addIncludeTable(new IncludeTable("table1"));
        engineering.addExcludeTable(new ExcludeTable("table2"));

        engineering.addIncludeColumn(new IncludeColumn("includeColumn"));

        FiltersConfigBuilder builder = new FiltersConfigBuilder(engineering);
        builder.compact();
        assertEquals(
                "ReverseEngineering: \n" +
                "  Catalog: catalogName\n" +
                "    Schema: schemaName01\n" +
                "      IncludeTable: table1\n" +
                "        IncludeColumn: includeColumn\n" +
                "      ExcludeTable: table2\n" +
                "    Schema: schemaName02\n" +
                "      IncludeTable: table1\n" +
                "        IncludeColumn: includeColumn\n" +
                "      ExcludeTable: table2\n\n"+
                "  Use primitives", engineering.toString());
    }

    @Test
    public void testCompact_03() {
        ReverseEngineering engineering = new ReverseEngineering();
        engineering.addCatalog(new Catalog("APP1"));
        engineering.addCatalog(new Catalog("APP2"));

        engineering.addExcludeTable(new ExcludeTable("SYS_.*"));
        engineering.addExcludeColumn(new ExcludeColumn("calculated_.*"));

        FiltersConfigBuilder builder = new FiltersConfigBuilder(engineering);
        builder.compact();
        assertEquals(
                "ReverseEngineering: \n" +
                "  Catalog: APP1\n" +
                "    Schema: null\n" +
                "      IncludeTable: null\n" +
                "        ExcludeColumn: calculated_.*\n" +
                "      ExcludeTable: SYS_.*\n" +
                "  Catalog: APP2\n" +
                "    Schema: null\n" +
                "      IncludeTable: null\n" +
                "        ExcludeColumn: calculated_.*\n" +
                "      ExcludeTable: SYS_.*\n\n" +
                "  Use primitives", engineering.toString());
    }

    @Test
    public void testCompact_04() {
        ReverseEngineering engineering = new ReverseEngineering();
        engineering.addSchema(new Schema("s"));

        FiltersConfigBuilder builder = new FiltersConfigBuilder(engineering);
        builder.compact();
        assertEquals(
                "ReverseEngineering: \n" +
                "  Catalog: null\n" +
                "    Schema: s\n" +
                "      IncludeTable: null\n\n" +
                "  Use primitives", engineering.toString());
    }

    @Test
    public void testCompact_full() {
        ReverseEngineering engineering = new ReverseEngineering();
        Catalog cat01 = new Catalog("cat_01");

        Schema sch01 = new Schema("sch_01");

        sch01.addIncludeTable(includeTable("t1", "c11", "c12"));
        sch01.addExcludeTable(new ExcludeTable("t2"));
        sch01.addIncludeProcedure(new IncludeProcedure("p1"));
        sch01.addExcludeProcedure(new ExcludeProcedure("p2"));
        sch01.addIncludeColumn(new IncludeColumn("c_x1"));
        sch01.addExcludeColumn(new ExcludeColumn("c_x2"));

        cat01.addSchema(sch01);

        cat01.addIncludeTable(includeTable("t3", "c31", "c32"));
        cat01.addExcludeTable(new ExcludeTable("t4"));
        cat01.addIncludeProcedure(new IncludeProcedure("p3"));
        cat01.addExcludeProcedure(new ExcludeProcedure("p4"));
        cat01.addIncludeColumn(new IncludeColumn("c_xx1"));
        cat01.addExcludeColumn(new ExcludeColumn("c_xx2"));

        engineering.addCatalog(cat01);

        Schema sch02 = new Schema("sch_02");

        sch02.addIncludeTable(includeTable("t5", "c51", "c52"));
        sch02.addExcludeTable(new ExcludeTable("t6"));
        sch02.addIncludeProcedure(new IncludeProcedure("p5"));
        sch02.addExcludeProcedure(new ExcludeProcedure("p6"));
        sch02.addIncludeColumn(new IncludeColumn("c2_x1"));
        sch02.addExcludeColumn(new ExcludeColumn("c2_x2"));

        engineering.addSchema(sch02);

        engineering.addIncludeTable(includeTable("t7", "c71", "c72"));
        engineering.addExcludeTable(new ExcludeTable("t8"));
        engineering.addIncludeProcedure(new IncludeProcedure("p7"));
        engineering.addExcludeProcedure(new ExcludeProcedure("p8"));
        engineering.addIncludeColumn(new IncludeColumn("c_xxx1"));
        engineering.addExcludeColumn(new ExcludeColumn("c_xxx2"));

        FiltersConfigBuilder builder = new FiltersConfigBuilder(engineering);
        assertEquals("Original ReverseEngineering should be",
                "ReverseEngineering: \n" +
                "  Catalog: cat_01\n" +
                "    Schema: sch_01\n" +
                "      IncludeTable: t1\n" +
                "        IncludeColumn: c11\n" +
                "        ExcludeColumn: c12\n" +
                "      ExcludeTable: t2\n" +
                "      IncludeColumn: c_x1\n" +
                "      ExcludeColumn: c_x2\n" +
                "      IncludeProcedure: p1\n" +
                "      ExcludeProcedure: p2\n" +
                "      IncludeTable: t3\n" +
                "        IncludeColumn: c31\n" +
                "        ExcludeColumn: c32\n" +
                "      ExcludeTable: t4\n" +
                "      IncludeColumn: c_xx1\n" +
                "      ExcludeColumn: c_xx2\n" +
                "      IncludeProcedure: p3\n" +
                "      ExcludeProcedure: p4\n" +
                "  Schema: sch_02\n" +
                "    IncludeTable: t5\n" +
                "      IncludeColumn: c51\n" +
                "      ExcludeColumn: c52\n" +
                "    ExcludeTable: t6\n" +
                "    IncludeColumn: c2_x1\n" +
                "    ExcludeColumn: c2_x2\n" +
                "    IncludeProcedure: p5\n" +
                "    ExcludeProcedure: p6\n" +
                "    IncludeTable: t7\n" +
                "      IncludeColumn: c71\n" +
                "      ExcludeColumn: c72\n" +
                "    ExcludeTable: t8\n" +
                "    IncludeColumn: c_xxx1\n" +
                "    ExcludeColumn: c_xxx2\n" +
                "    IncludeProcedure: p7\n" +
                "    ExcludeProcedure: p8\n\n" +
                "  Use primitives", engineering.toString());


        builder.compact();
        assertEquals(
                "ReverseEngineering: \n" +
                        "  Catalog: cat_01\n" +
                        "    Schema: sch_01\n" +
                        "      IncludeTable: t1\n" +
                        "        IncludeColumn: c11\n" +
                        "        IncludeColumn: c_xxx1\n" +
                        "        IncludeColumn: c_xx1\n" +
                        "        IncludeColumn: c_x1\n" +
                        "        ExcludeColumn: c12\n" +
                        "        ExcludeColumn: c_xxx2\n" +
                        "        ExcludeColumn: c_xx2\n" +
                        "        ExcludeColumn: c_x2\n" +
                        "      IncludeTable: t7\n" +
                        "        IncludeColumn: c71\n" +
                        "        IncludeColumn: c_xxx1\n" +
                        "        ExcludeColumn: c72\n" +
                        "        ExcludeColumn: c_xxx2\n" +
                        "      IncludeTable: t3\n" +
                        "        IncludeColumn: c31\n" +
                        "        IncludeColumn: c_xxx1\n" +
                        "        IncludeColumn: c_xx1\n" +
                        "        ExcludeColumn: c32\n" +
                        "        ExcludeColumn: c_xxx2\n" +
                        "        ExcludeColumn: c_xx2\n" +
                        "      ExcludeTable: t2\n" +
                        "      ExcludeTable: t8\n" +
                        "      ExcludeTable: t4\n" +
                        "      IncludeProcedure: p1\n" +
                        "      IncludeProcedure: p7\n" +
                        "      IncludeProcedure: p3\n" +
                        "      ExcludeProcedure: p2\n" +
                        "      ExcludeProcedure: p8\n" +
                        "      ExcludeProcedure: p4\n" +
                        "    Schema: sch_02\n" +
                        "      IncludeTable: t5\n" +
                        "        IncludeColumn: c51\n" +
                        "        IncludeColumn: c_xxx1\n" +
                        "        IncludeColumn: c2_x1\n" +
                        "        ExcludeColumn: c52\n" +
                        "        ExcludeColumn: c_xxx2\n" +
                        "        ExcludeColumn: c2_x2\n" +
                        "      IncludeTable: t7\n" +
                        "        IncludeColumn: c71\n" +
                        "        IncludeColumn: c_xxx1\n" +
                        "        ExcludeColumn: c72\n" +
                        "        ExcludeColumn: c_xxx2\n" +
                        "      ExcludeTable: t6\n" +
                        "      ExcludeTable: t8\n" +
                        "      IncludeProcedure: p5\n" +
                        "      IncludeProcedure: p7\n" +
                        "      ExcludeProcedure: p6\n" +
                        "      ExcludeProcedure: p8\n\n" +
                        "  Use primitives", engineering.toString());
    }

    protected IncludeTable includeTable(String name, String incCol, String excCol) {
        IncludeTable incTable01 = new IncludeTable(name);
        incTable01.addIncludeColumn(new IncludeColumn(incCol));
        incTable01.addExcludeColumn(new ExcludeColumn(excCol));
        return incTable01;
    }
}