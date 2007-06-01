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
package org.objectstyle.cayenne.perform.test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.perform.CayennePerformanceTest;

/**
 * @author Andrei Adamchik
 */
public class SmallSelectTest extends CayennePerformanceTest {
	public static final int QUERIES_COUNT = 100;

	/**
	 * Constructor for SmallSelectTest.
	 * @param name
	 */
	public SmallSelectTest(String name) {
		super(name);
	}

	public void prepare() throws Exception {
		super.deleteArtists();
		super.insertArtists();
	}

	/**
	 * @see org.objectstyle.perform.PerformanceTest#runTest()
	 */
	public void runTest() throws Exception {
		Connection con = getConnection();
		try {
			Statement st = con.createStatement();
			try {
				for (int i = 0; i < QUERIES_COUNT; i++) {
					ResultSet rs =
						st.executeQuery(
							"SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST WHERE ARTIST_NAME = 'artist_1000' OR ARTIST_NAME LIKE '%rtist_1000'");
					try {
						List artists = new ArrayList();
						while (rs.next()) {
							Artist artist = new Artist();
							artist.setArtistName(rs.getString(2));
							artists.add(artist);

							// read artist_id to provide closer comparison
							rs.getInt(1);
						}
					} finally {
						rs.close();
					}
				}
			} finally {
				st.close();
			}
		} finally {
			con.close();
		}
	}
}
