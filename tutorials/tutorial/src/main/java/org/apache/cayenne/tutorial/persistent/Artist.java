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
package org.apache.cayenne.tutorial.persistent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.cayenne.tutorial.persistent.auto._Artist;

public class Artist extends _Artist {

	static final String DEFAULT_DATE_FORMAT = "yyyyMMdd";

	/**
	 * Sets date of birth using a string in format yyyyMMdd.
	 */
	public void setDateOfBirthString(String yearMonthDay) {
		if (yearMonthDay == null) {
			setDateOfBirth(null);
		} else {

			Date date;
			try {
				date = new SimpleDateFormat(DEFAULT_DATE_FORMAT)
						.parse(yearMonthDay);
			} catch (ParseException e) {
				throw new IllegalArgumentException(
						"A date argument must be in format '"
								+ DEFAULT_DATE_FORMAT + "': " + yearMonthDay);
			}

			setDateOfBirth(date);
		}
	}
}
