/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.Artist;

/**
 * @deprecated Since 1.1 corresponding class is deprecated.
 */
public class SqlSelectQueryInContextTst extends SelectQueryBase {
	private static final int _artistCount = 10;

	protected SqlSelectQuery q;

	public void setUp() throws Exception {
		super.setUp();
		q = new SqlSelectQuery();
	}

	protected Query getQuery() {
		return q;
	}

	public void testSelect1() throws Exception {

        q.setRoot(Artist.class);
		q.setSqlString("select count(*)  from ARTIST");
		performQuery();

		// check query results
		List objects = opObserver.rowsForQuery(q);
		assertNotNull(objects);
		assertEquals(1, objects.size());
		Map countMap = (Map) objects.get(0);
		Object count = countMap.values().iterator().next();
		assertEquals(_artistCount, ((Number) count).intValue());
	}

	public void testSelect2() throws java.lang.Exception {
		// use fetch limit
        q.setRoot(Artist.class);
		q.setSqlString("select ARTIST_NAME from ARTIST");
		q.setFetchLimit(5);
		performQuery();

		// check query results
		List objects = opObserver.rowsForQuery(q);
		assertNotNull(objects);
		assertEquals(5, objects.size());
	}
	

	protected void populateTables() throws java.lang.Exception {
		String insertArtist =
			"INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) VALUES (?,?,?)";
		Connection conn = getConnection();

		try {
			conn.setAutoCommit(false);

			PreparedStatement stmt = conn.prepareStatement(insertArtist);
			long dateBase = System.currentTimeMillis();

			for (int i = 1; i <= _artistCount; i++) {
				stmt.setInt(1, i);
				stmt.setString(2, "artist" + i);
				stmt.setDate(
					3,
					new java.sql.Date(dateBase + 1000 * 60 * 60 * 24 * i));
				stmt.executeUpdate();
			}

			stmt.close();
			conn.commit();
		} finally {
			conn.close();
		}
	}
}