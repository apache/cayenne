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

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationships_qualifier.Contact;
import org.apache.cayenne.testdo.relationships_qualifier.ContactRuleRelation;
import org.apache.cayenne.testdo.relationships_qualifier.Course;
import org.apache.cayenne.testdo.relationships_qualifier.CourseRuleRelation;
import org.apache.cayenne.testdo.relationships_qualifier.Rule;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(CayenneProjects.RELATIONSHIPS_QUALIFIER_PROJECT)
public class RelationshipQualifierTranslatorIT extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tContact;
    private TableHelper tCourse;
    private TableHelper tRelations;
    private TableHelper tRule;
    private TableHelper tRuleStatus;

    @Before
    public void setUp() throws Exception {
        tContact = new TableHelper(dbHelper, "CONTACT");
        tContact.setColumns("ID", "NAME");

        tCourse = new TableHelper(dbHelper, "COURSE");
        tCourse.setColumns("ID", "NAME");

        tRule = new TableHelper(dbHelper, "RULE");
        tRule.setColumns("ID", "NAME", "STATUS_ID");

        tRuleStatus = new TableHelper(dbHelper, "RULE_STATUS");
        tRuleStatus.setColumns("ID", "STATUS", "APPROVED");

        tRelations = new TableHelper(dbHelper, "RELATION");
        tRelations.setColumns("ID", "entityId", "entityIdentifier", "ruleId");
    }

    private void insertData() throws Exception {
        tContact.insert(1, "Contact1");
        tCourse.insert(1, "Course1");
        tContact.insert(2, "Contact2");
        tCourse.insert(2, "Course2");
        tContact.insert(3, "Contact3");
        tCourse.insert(3, "Course3");
        tRuleStatus.insert(1, "enable", true);
        tRuleStatus.insert(2, "disable", true);
        tRuleStatus.insert(3, "enable", false);
        tRule.insert(1, "Rule", 1);
        tRule.insert(2, "Rule", 2);
        tRule.insert(3, "Rule", 3);
        tRelations.insert(1, 1, "Course", 1);
        tRelations.insert(2, 2, "Course", 2);
        tRelations.insert(3, 3, "Course", 3);
    }

    @Test
    public void testSelectQueryWithRelationshipQualifier() throws Exception {
        insertData();

        SelectQuery selectContacts = new SelectQuery(Contact.class, Contact.RELATIONS.dot(ContactRuleRelation.RULE).dot(Rule.NAME).eq("Rule"));
        List contacts = context.select(selectContacts);
        assertEquals(0, contacts.size());

        SelectQuery selectCourses = new SelectQuery(Course.class, Course.RELATIONS.dot(CourseRuleRelation.RULE).dot(Rule.NAME).eq("Rule"));
        List courses = context.select(selectCourses);
        assertEquals(1, courses.size());
    }

    @Test
    public void testSelectQueryWithPrefetchRelationshipQualifier() throws Exception {
        insertData();

        SelectQuery selectContacts = new SelectQuery(Contact.class, Contact.ID.eq(1));
        selectContacts.addPrefetch("relations.rule.ruleStatus");
        List<Contact> contacts = context.performQuery(selectContacts);
        assertEquals(1, contacts.size());
        assertEquals(0, contacts.get(0).getRelations().size());

        SelectQuery selectCourses = new SelectQuery(Course.class, Course.ID.eq(1));
        selectCourses.addPrefetch("relations.rule.ruleStatus");
        List<Course> courses = context.performQuery(selectCourses);
        assertEquals(1, courses.size());
        assertEquals(1, courses.get(0).getRelations().size());
    }

    @Test
    public void testSelectComplexQueryWithRelationshipQualifier() throws Exception {
        insertData();

        SelectQuery selectContacts = new SelectQuery(Contact.class, Contact.RELATIONS.dot(ContactRuleRelation.RULE).dot(Rule.NAME).eq("Rule").andExp(Contact.RELATIONS.dot(ContactRuleRelation.RULE).dot(Rule.STATUS_ID).eq(1)));
        List contacts = context.select(selectContacts);
        assertEquals(0, contacts.size());

        SelectQuery selectCourses = new SelectQuery(Course.class, Course.RELATIONS.dot(CourseRuleRelation.RULE).dot(Rule.NAME).eq("Rule").andExp(Contact.RELATIONS.dot(ContactRuleRelation.RULE).dot(Rule.STATUS_ID).eq(1)));
        List courses = context.select(selectCourses);
        assertEquals(1, courses.size());
    }

}
