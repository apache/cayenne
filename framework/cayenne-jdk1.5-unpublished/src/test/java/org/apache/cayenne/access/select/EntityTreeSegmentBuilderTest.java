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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.testdo.inherit.AbstractPerson;
import org.apache.cayenne.testdo.inherit.CustomerRepresentative;
import org.apache.cayenne.unit.PeopleCase;

import com.mockrunner.mock.jdbc.MockResultSet;

public class EntityTreeSegmentBuilderTest extends PeopleCase {

    public void testBuildSegmentColumnsLeaf() {

        SelectQuery query = new SelectQuery(CustomerRepresentative.class);

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

        Collection<String> columnNames = new ArrayList<String>(Arrays.asList(
                "CLIENT_COMPANY_ID",
                "CLIENT_CONTACT_TYPE",
                "NAME",
                "PERSON_ID",
                "PERSON_TYPE"));

        for (SelectColumn column : columns) {
            columnNames.remove(column.getDataRowKey());
        }

        assertTrue("Missing columns: " + columnNames, columnNames.isEmpty());
        assertEquals("Unexpected columns present", 5, columns.size());
    }

    public void testBuildSegmentRowReaderLeaf() throws Exception {

        SelectQuery query = new SelectQuery(CustomerRepresentative.class);

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

        Map<String, Object> crRowMap = new HashMap<String, Object>();
        crRowMap.put("PERSON_ID", 3);
        crRowMap.put("PERSON_TYPE", "C");
        crRowMap.put("NAME", "E2");
        crRowMap.put("CLIENT_CONTACT_TYPE", "XX");
        crRowMap.put("CLIENT_COMPANY_ID", 3);

        List<Object> crRow = new ArrayList<Object>();

        MockResultSet rs = new MockResultSet("test");
        for (SelectColumn column : columns) {
            rs.addColumn(column.getColumnName(md.getDbEntity(), null));
            crRow.add(crRowMap.get(column.getDataRowKey()));
        }

        rs.addRow(crRow);

        RowReader<Object> reader = select.getRowReader(rs);

        rs.next();
        DataRow crRowRead = (DataRow) reader.readRow(rs);
        assertEquals("CustomerRepresentative", crRowRead.getEntityName());
        assertEquals("Invalid row read: " + crRowRead, crRowMap, crRowRead);
    }

    public void testBuildSegmentColumnsSuper() {

        SelectQuery query = new SelectQuery(AbstractPerson.class);

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

        Collection<String> columnNames = new ArrayList<String>(Arrays.asList(
                "CLIENT_COMPANY_ID",
                "CLIENT_CONTACT_TYPE",
                "DEPARTMENT_ID",
                "NAME",
                "PERSON_ID",
                "PERSON_TYPE",
                "SALARY"));

        for (SelectColumn column : columns) {
            columnNames.remove(column.getDataRowKey());
        }

        assertTrue("Missing columns: " + columnNames, columnNames.isEmpty());
        assertEquals("Unexpected columns present", 7, columns.size());
    }

    public void testBuildSegmentRowReaderSuper() throws Exception {
        SelectQuery query = new SelectQuery(AbstractPerson.class);

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

        Map<String, Object> employeeRowMap = new HashMap<String, Object>();
        employeeRowMap.put("PERSON_ID", 1);
        employeeRowMap.put("PERSON_TYPE", "EE");
        employeeRowMap.put("NAME", "E1");
        employeeRowMap.put("SALARY", new Float(1.0));
        employeeRowMap.put("DEPARTMENT_ID", 1);

        Map<String, Object> managerRowMap = new HashMap<String, Object>();
        managerRowMap.put("PERSON_ID", 2);
        managerRowMap.put("PERSON_TYPE", "EM");
        managerRowMap.put("NAME", "E2");
        managerRowMap.put("SALARY", new Float(2.0));
        managerRowMap.put("DEPARTMENT_ID", 2);

        Map<String, Object> crRowMap = new HashMap<String, Object>();
        crRowMap.put("PERSON_ID", 3);
        crRowMap.put("PERSON_TYPE", "C");
        crRowMap.put("NAME", "E2");
        crRowMap.put("CLIENT_CONTACT_TYPE", "XX");
        crRowMap.put("CLIENT_COMPANY_ID", 3);

        List<Object> employeeRow = new ArrayList<Object>();
        List<Object> managerRow = new ArrayList<Object>();
        List<Object> crRow = new ArrayList<Object>();

        MockResultSet rs = new MockResultSet("test");
        for (SelectColumn column : columns) {
            rs.addColumn(column.getColumnName(md.getDbEntity(), null));

            employeeRow.add(employeeRowMap.get(column.getDataRowKey()));
            managerRow.add(managerRowMap.get(column.getDataRowKey()));
            crRow.add(crRowMap.get(column.getDataRowKey()));
        }

        rs.addRow(employeeRow);
        rs.addRow(managerRow);
        rs.addRow(crRow);

        RowReader<Object> reader = select.getRowReader(rs);

        rs.next();
        DataRow employeeRowRead = (DataRow) reader.readRow(rs);
        assertEquals("Employee", employeeRowRead.getEntityName());
        assertEquals(
                "Invalid row read: " + employeeRowRead,
                employeeRowMap,
                employeeRowRead);

        rs.next();
        DataRow managerRowRead = (DataRow) reader.readRow(rs);
        assertEquals("Manager", managerRowRead.getEntityName());
        assertEquals("Invalid row read: " + managerRowRead, managerRowMap, managerRowRead);

        rs.next();
        DataRow crRowRead = (DataRow) reader.readRow(rs);
        assertEquals("CustomerRepresentative", crRowRead.getEntityName());
        assertEquals("Invalid row read: " + crRowRead, crRowMap, crRowRead);
    }
}
