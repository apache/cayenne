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

package org.apache.cayenne.dba.oracle;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;

import org.apache.cayenne.access.types.UtilDateType;

/**
 */
public class OracleUtilDateType extends UtilDateType {

	@Override
    public Date materializeObject(CallableStatement cs, int index, int type)
		throws Exception {
			
	    Date date = super.materializeObject(cs, index, type);
		if (date == null || type != Types.TIME) {
			return date;
		} else {
			return normalizeDate(date);
		}
	}

	@Override
    public Date materializeObject(ResultSet rs, int index, int type)
		throws Exception {

	    Date date = super.materializeObject(rs, index, type);
		if (date == null || type != Types.TIME) {
			return date;
		} else {
			return normalizeDate(date);
		}
	}

	/**
	 * Offsets date component to be January 1, 1970,
	 * since Oracle adapter returns time based on January 1, 1900.
	 */
	protected Date normalizeDate(Date time) {
		// this may need serious optimization - Calendar is slow...
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(time);
		calendar.set(Calendar.YEAR, 1970);
		calendar.set(Calendar.MONTH, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 1);

		return calendar.getTime();
	}
}
