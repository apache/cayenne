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

package org.apache.cayenne.joda.access.types;

import org.apache.cayenne.access.types.ExtendedType;
import org.joda.time.DateTime;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * Handles <code>org.joda.time.DateTime</code> type mapping.
 * 
 * @since 4.0
 */
public class DateTimeType implements ExtendedType {

	@Override
	public String getClassName() {
		return DateTime.class.getName();
	}

	@Override
	public DateTime materializeObject(ResultSet rs, int index, int type) throws Exception {
		if (rs.getTimestamp(index) != null) {
			return new DateTime(rs.getTimestamp(index).getTime());
		} else {
			return null;
		}
	}

	@Override
	public DateTime materializeObject(CallableStatement rs, int index, int type) throws Exception {
		if (rs.getTimestamp(index) != null) {
			return new DateTime(rs.getTimestamp(index).getTime());
		} else {
			return null;
		}
	}

	@Override
	public void setJdbcObject(PreparedStatement statement, Object value, int pos, int type, int scale) throws Exception {

		if (value == null) {
			statement.setNull(pos, type);
		} else {
			Timestamp ts = new Timestamp(getMillis(value));
			statement.setTimestamp(pos, ts);
		}
	}

	protected long getMillis(Object value) {
		return ((DateTime) value).getMillis();
	}

}
