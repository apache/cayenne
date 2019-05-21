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

package org.apache.cayenne.unit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Defines Java stored procedures loaded to HSQLDB.
 */
public class HSQLProcedures {

	public static void cayenne_tst_upd_proc(Connection c, int paintingPrice) throws SQLException {

		try (PreparedStatement st = c.prepareStatement("UPDATE PAINTING SET ESTIMATED_PRICE = ESTIMATED_PRICE * 2 "
				+ "WHERE ESTIMATED_PRICE < ?");) {
			st.setInt(1, paintingPrice);
			st.execute();
		}
	}

	public static void cayenne_tst_select_proc(Connection c, String name, int paintingPrice) throws SQLException {

		try (PreparedStatement st = c.prepareStatement("UPDATE PAINTING SET ESTIMATED_PRICE = ESTIMATED_PRICE * 2 "
				+ "WHERE ESTIMATED_PRICE < ?");) {

			st.setInt(1, paintingPrice);
			st.execute();
		}

		try (PreparedStatement select = c
				.prepareStatement("SELECT DISTINCT A.ARTIST_ID, A.ARTIST_NAME, A.DATE_OF_BIRTH"
						+ " FROM ARTIST A, PAINTING P" + " WHERE A.ARTIST_ID = P.ARTIST_ID AND" + " A.ARTIST_NAME = ?"
						+ " ORDER BY A.ARTIST_ID");) {
			select.setString(1, name);
			select.executeQuery();
		}
	}
}
