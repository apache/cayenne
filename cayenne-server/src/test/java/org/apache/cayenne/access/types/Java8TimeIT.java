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

package org.apache.cayenne.access.types;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.java8.LocalDateTestEntity;
import org.apache.cayenne.testdo.java8.LocalDateTimeTestEntity;
import org.apache.cayenne.testdo.java8.LocalTimeTestEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@UseServerRuntime(CayenneProjects.JAVA8)
public class Java8TimeIT extends ServerCase {

	@Inject
	private DataContext context;

	@Inject
	private DBHelper dbHelper;

	@Before
	public void before() throws SQLException {
		dbHelper.deleteAll("LOCAL_DATE_TEST");
		dbHelper.deleteAll("LOCAL_DATETIME_TEST");
		dbHelper.deleteAll("LOCAL_TIME_TEST");
	}

	@Test
	public void testJava8LocalDate_Null() {
		LocalDateTestEntity localDateTestEntity = context.newObject(LocalDateTestEntity.class);
		localDateTestEntity.setDate(null);

		context.commitChanges();

		LocalDateTestEntity testRead = ObjectSelect.query(LocalDateTestEntity.class).selectOne(context);

		assertNull(testRead.getDate());
	}

	@Test
	public void testJava8LocalDate() {
		LocalDateTestEntity localDateTestEntity = context.newObject(LocalDateTestEntity.class);
		LocalDate localDate = LocalDate.now();
		localDateTestEntity.setDate(localDate);

		context.commitChanges();

		LocalDateTestEntity testRead = ObjectSelect.query(LocalDateTestEntity.class).selectOne(context);

		assertNotNull(testRead.getDate());
		assertEquals(LocalDate.class, testRead.getDate().getClass());
		assertEquals(localDate, testRead.getDate());
	}

	@Test
	public void testJava8LocalTime() {
		LocalTimeTestEntity localTimeTestEntity = context.newObject(LocalTimeTestEntity.class);
		LocalTime localTime = LocalTime.now();
		localTimeTestEntity.setTime(localTime);

		context.commitChanges();

		LocalTimeTestEntity testRead = ObjectSelect.query(LocalTimeTestEntity.class).selectOne(context);

		assertNotNull(testRead.getTime());
		assertEquals(LocalTime.class, testRead.getTime().getClass());
		assertEquals(localTime.toSecondOfDay(), testRead.getTime().toSecondOfDay());

	}

	@Test
	public void testJava8LocalDateTime() {
		LocalDateTimeTestEntity localDateTimeTestEntity = context.newObject(LocalDateTimeTestEntity.class);
		// round up seconds fraction
		// reason: on MySQL field should be defined as TIMESTAMP(fractionSecondsPrecision) to support it
		LocalDateTime localDateTime = LocalDateTime.now().with(ChronoField.NANO_OF_SECOND, 0);
		localDateTimeTestEntity.setTimestamp(localDateTime);

		context.commitChanges();

		LocalDateTimeTestEntity testRead = ObjectSelect.query(LocalDateTimeTestEntity.class).selectOne(context);

		assertNotNull(testRead.getTimestamp());
		assertEquals(LocalDateTime.class, testRead.getTimestamp().getClass());
		assertEquals(localDateTime, testRead.getTimestamp());

	}

	@Test
	public void columnSelectWithJava8Type() {
		// round up seconds fraction
		// reason: on MySQL field should be defined as TIMESTAMP(fractionSecondsPrecision) to support it
		LocalDateTime localDateTime = LocalDateTime.now().with(ChronoField.NANO_OF_SECOND, 0);

		LocalDateTimeTestEntity localDateTimeTestEntity = context.newObject(LocalDateTimeTestEntity.class);
		localDateTimeTestEntity.setTimestamp(localDateTime);

		context.commitChanges();

		LocalDateTime value = ObjectSelect.query(LocalDateTimeTestEntity.class)
				.column(LocalDateTimeTestEntity.TIMESTAMP)
				.selectOne(context);
		assertEquals(localDateTime, value);

		LocalDateTime value2 = ObjectSelect.query(LocalDateTimeTestEntity.class)
				.min(LocalDateTimeTestEntity.TIMESTAMP)
				.selectOne(context);
		assertEquals(localDateTime, value2);
	}

}
