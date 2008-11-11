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


package org.apache.cayenne.dba;

import junit.framework.TestCase;

/**
 */
public class PkRangeTest extends TestCase {

	public void testExhausted1() throws Exception {
		PkRange range = new PkRange(1, 0);
		assertTrue(range.isExhausted());
	}

	public void testExhausted2() throws Exception {
		PkRange range = new PkRange(0, 1);
		assertTrue(!range.isExhausted());
	}

	public void testExhausted3() throws Exception {
		PkRange range = new PkRange(0, 2);
		assertTrue(!range.isExhausted());

		assertEquals(0, range.getNextPrimaryKey().intValue());
		assertEquals(1, range.getNextPrimaryKey().intValue());
		assertEquals(2, range.getNextPrimaryKey().intValue());
		assertTrue(range.isExhausted());
	}
	
	public void testReset() throws Exception {
		PkRange range = new PkRange(1, 0);
		assertTrue(range.isExhausted());
		
		range.reset(0, 2);
		assertTrue(!range.isExhausted());
	}

}
