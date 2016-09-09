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

package org.apache.cayenne.java8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.java8.db.LocalDateTestEntity;
import org.apache.cayenne.java8.db.LocalDateTimeTestEntity;
import org.apache.cayenne.java8.db.LocalTimeTestEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.Test;

public class Java8TimeIT extends RuntimeBase {

	@Test
	public void testJava8LocalDate() {
		ObjectContext context = runtime.newContext();

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
		ObjectContext context = runtime.newContext();

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
		ObjectContext context = runtime.newContext();

		LocalDateTimeTestEntity localDateTimeTestEntity = context.newObject(LocalDateTimeTestEntity.class);
		LocalDateTime localDateTime = LocalDateTime.now();
		localDateTimeTestEntity.setTimestamp(localDateTime);

		context.commitChanges();

		LocalDateTimeTestEntity testRead = ObjectSelect.query(LocalDateTimeTestEntity.class).selectOne(context);

		assertNotNull(testRead.getTimestamp());
		assertEquals(LocalDateTime.class, testRead.getTimestamp().getClass());
		assertEquals(localDateTime, testRead.getTimestamp());

	}

}
