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
package org.apache.cayenne.access.select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.testdo.horizontalinherit.AbstractSuperEntity;
import org.apache.cayenne.testdo.horizontalinherit.SubEntity1;
import org.apache.cayenne.unit.InheritanceCase;

public class EntityTreeSegmentBuilderHorizontalTest extends InheritanceCase {

    public void testBuildSegmentColumnsLeafWithDbEntityLessSuper() {

        SelectQuery query = new SelectQuery(SubEntity1.class);

        EntityResolver resolver = getDomain().getEntityResolver();
        QueryMetadata md = query.getMetaData(resolver);
        ClassDescriptor descriptor = md.getClassDescriptor();
        ExtendedTypeMap converters = getDomain()
                .getDataNodes()
                .iterator()
                .next()
                .getAdapter()
                .getExtendedTypes();

        SelectDescriptor<Object> select = new EntityTreeSegmentBuilder(
                md,
                converters,
                descriptor).buildSegment();

        List<? extends SelectColumn> columns = select.getColumns();

        DbEntity e0 = resolver.getDbEntity("INHERITANCE_SUB_ENTITY1");
        Collection<String> columnNames0 = new ArrayList<String>(Arrays.asList(
                "ID",
                "SUPER_INT_DB_ATTR",
                "SUPER_STRING_DB_ATTR",
                "SUBENTITY_STRING_DB_ATTR"));

        for (SelectColumn column : columns) {
            columnNames0.remove(column.getColumnName(e0, ""));
        }

        assertTrue(
                "Missing columns for 'INHERITANCE_SUB_ENTITY1': " + columnNames0,
                columnNames0.isEmpty());
        assertEquals("Unexpected columns present", 4, columns.size());
    }

    public void testBuildSegmentColumnsSuper() {

        SelectQuery query = new SelectQuery(AbstractSuperEntity.class);

        EntityResolver resolver = getDomain().getEntityResolver();
        QueryMetadata md = query.getMetaData(resolver);
        ClassDescriptor descriptor = md.getClassDescriptor();
        ExtendedTypeMap converters = getDomain()
                .getDataNodes()
                .iterator()
                .next()
                .getAdapter()
                .getExtendedTypes();

        SelectDescriptor<Object> select = new EntityTreeSegmentBuilder(
                md,
                converters,
                descriptor).buildSegment();

        List<? extends SelectColumn> columns = select.getColumns();

        DbEntity e0 = resolver.getDbEntity("INHERITANCE_SUB_ENTITY1");
        Collection<String> columnNames0 = new ArrayList<String>(Arrays.asList(
                "CAYENNE:ENTITY",
                "ID",
                "SUPER_INT_DB_ATTR",
                "SUPER_STRING_DB_ATTR",
                "SUBENTITY_STRING_DB_ATTR",
                "'1'",
                "1"));

        DbEntity e1 = resolver.getDbEntity("INHERITANCE_SUB_ENTITY2");
        Collection<String> columnNames1 = new ArrayList<String>(Arrays.asList(
                "CAYENNE:ENTITY",
                "ID",
                "SUPER_INT_DB_ATTR",
                "OVERRIDDEN_STRING_DB_ATTR",
                "'1'",
                "SUBENTITY_INT_DB_ATTR",
                "1"));

        DbEntity e2 = resolver.getDbEntity("INHERITANCE_SUB_ENTITY3");
        Collection<String> columnNames2 = new ArrayList<String>(Arrays.asList(
                "CAYENNE:ENTITY",
                "ID",
                "OVERRIDDEN_INT_DB_ATTR",
                "OVERRIDDEN_STRING_DB_ATTR",
                "'1'",
                "1",
                "SUBENTITY_BOOL_ATTR"));

        for (SelectColumn column : columns) {
            columnNames0.remove(column.getColumnName(e0, ""));
            columnNames1.remove(column.getColumnName(e1, ""));
            columnNames2.remove(column.getColumnName(e2, ""));
        }

        assertTrue(
                "Missing columns for 'INHERITANCE_SUB_ENTITY1': " + columnNames0,
                columnNames0.isEmpty());
        assertTrue(
                "Missing columns for 'INHERITANCE_SUB_ENTITY2': " + columnNames1,
                columnNames1.isEmpty());
        assertTrue(
                "Missing columns for 'INHERITANCE_SUB_ENTITY3': " + columnNames2,
                columnNames2.isEmpty());
        assertEquals("Unexpected columns present", 7, columns.size());
    }
}
