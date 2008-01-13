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


package org.apache.cayenne.access.trans;

import java.sql.Types;

import junit.framework.TestCase;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;

/**
 * @author Andrus Adamchik
 */
public class BatchQueryBuilderTest extends TestCase {

	public void testConstructor() throws Exception {
		DbAdapter adapter = new JdbcAdapter();
		BatchQueryBuilder builder =
			new BatchQueryBuilder(adapter) {
			@Override
            public String createSqlString(BatchQuery batch) {
				return null;
			}
		};

		assertSame(adapter, builder.getAdapter());
	}

	public void testAppendDbAttribute1() throws Exception {
		DbAdapter adapter = new JdbcAdapter();
		String trimFunction = "testTrim";

		BatchQueryBuilder builder =
			new BatchQueryBuilder(adapter) {
			@Override
            public String createSqlString(BatchQuery batch) {
				return null;
			}
		};
		
		builder.setTrimFunction(trimFunction);

		StringBuffer buf = new StringBuffer();
		DbAttribute attr = new DbAttribute("testAttr", Types.CHAR, null);
		builder.appendDbAttribute(buf, attr);
		assertEquals("testTrim(testAttr)", buf.toString());

		buf = new StringBuffer();
		attr = new DbAttribute("testAttr", Types.VARCHAR, null);
		builder.appendDbAttribute(buf, attr);
		assertEquals("testAttr", buf.toString());
	}

	public void testAppendDbAttribute2() throws Exception {
		DbAdapter adapter = new JdbcAdapter();

		BatchQueryBuilder builder = new BatchQueryBuilder(adapter) {
			@Override
            public String createSqlString(BatchQuery batch) {
				return null;
			}
		};

		StringBuffer buf = new StringBuffer();
		DbAttribute attr = new DbAttribute("testAttr", Types.CHAR, null);
		builder.appendDbAttribute(buf, attr);
		assertEquals("testAttr", buf.toString());

		buf = new StringBuffer();
		attr = new DbAttribute("testAttr", Types.VARCHAR, null);
		builder.appendDbAttribute(buf, attr);
		assertEquals("testAttr", buf.toString());
	}
}
