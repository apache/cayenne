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

package org.apache.cayenne.access;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.many_to_many_joinTable_objEntity.Enrollments;
import org.apache.cayenne.testdo.many_to_many_joinTable_objEntity.Student;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@UseServerRuntime(CayenneProjects.MANY_TO_MANY_JOIN_TABLE_OBJ_ENTITY_PROJECT)
public class ManyToManyJoinTableWithObjEntityIT extends ServerCase {

    @Inject
    protected DataContext context;
    @Inject
    protected DBHelper dbHelper;
    protected TableHelper tStudent;
    protected TableHelper tCourse;
    protected TableHelper tEnrollments;

    @Before
    public void setUp() throws Exception {
        tStudent = new TableHelper(dbHelper, "student");
        tStudent.setColumns("id");

        tCourse = new TableHelper(dbHelper, "course");
        tCourse.setColumns("id");

        tEnrollments = new TableHelper(dbHelper, "enrollments");
        tEnrollments.setColumns("student_id","course_id");

    }

    protected void createObjectsDataSet() throws Exception {
        tStudent.insert(1 );

        tCourse.insert(1);
        tCourse.insert(2);
        tCourse.insert(3);

        tEnrollments.insert(1,1);
        tEnrollments.insert(1,2);
        tEnrollments.insert(1,3);

    }


    @Test
    public void testDeleteObjects() throws Exception {
        createObjectsDataSet();

        assertEquals(1, tStudent.getRowCount());
        assertEquals(1, tStudent.selectAll().size());
        assertEquals(3, tEnrollments.getRowCount());
        assertEquals(3, tEnrollments.selectAll().size());

        Student student = Cayenne.objectForPK(context, Student.class, 1);

        List<Enrollments> select = ObjectSelect.query(Enrollments.class).select(context);
        assertEquals(3,select.size());

        assertEquals(PersistenceState.COMMITTED, student.getPersistenceState());
        context.deleteObject(student);

        assertEquals(PersistenceState.DELETED, student.getPersistenceState());
        context.commitChanges();

        for (Enrollments enrollments : select) {
            assertEquals(PersistenceState.TRANSIENT, enrollments.getPersistenceState());
            assertNull(enrollments.getObjectContext());
        }

        assertEquals(PersistenceState.TRANSIENT, student.getPersistenceState());
        assertNull(student.getObjectContext());

        assertEquals(0, tStudent.getRowCount());
        assertEquals(0, tStudent.selectAll().size());
        assertEquals(0, tEnrollments.getRowCount());
        assertEquals(0, tEnrollments.selectAll().size());
    }

}
