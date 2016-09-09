package org.apache.cayenne.joda.access.types;

import org.apache.cayenne.joda.access.types.LocalTimeType;

import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockPreparedStatement;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.sql.PreparedStatement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

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

public class LocalTimeTypeTest extends JodaTestCase {

	private LocalTimeType type;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		type = new LocalTimeType();
	}

	public void testMaterializeObjectTimestamp() throws Exception {
		Object o = type.materializeObject(resultSet(new Timestamp(0)), 1, Types.TIMESTAMP);
		assertEquals(new LocalTime(0), o);
	}

	public void testMaterializeObjectTime() throws Exception {
		Object o = type.materializeObject(resultSet(new Time(0)), 1, Types.TIME);
		assertEquals(new LocalTime(0), o);
	}

	public void testSetJdbcObject() throws Exception {
		PreparedStatement statement = new MockPreparedStatement(new MockConnection(), "update t set c = ?");
		LocalTime date = new LocalTime(0);

		type.setJdbcObject(statement, date, 1, Types.TIME, 0);

        Object object = ((MockPreparedStatement) statement).getParameter(1);
        assertEquals(Time.class, object.getClass());
        assertEquals(new LocalDate(0, DateTimeZone.UTC).toDateTime(date).getMillis(), ((Time) object).getTime());

		type.setJdbcObject(statement, date, 1, Types.TIMESTAMP, 0);

        object = ((MockPreparedStatement) statement).getParameter(1);
        assertEquals(Timestamp.class, object.getClass());
        assertEquals(new LocalDate(0, DateTimeZone.UTC).toDateTime(date).getMillis(), ((Timestamp) object).getTime());

		type.setJdbcObject(statement, null, 1, Types.TIMESTAMP, 0);

        object = ((MockPreparedStatement) statement).getParameter(1);
        assertNull(object);
	}

}