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
package org.apache.cayenne.access;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.util.Util;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class IncrementalFaultListIT extends RuntimeCase {

	@Inject
	protected DataContext context;

    @Test
	public void testSerialization() throws Exception {
		ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
				.pageSize(10);

		IncrementalFaultList<Artist> i1 = new IncrementalFaultList<Artist>(context, query, 10, List.of());
		IncrementalFaultList<Artist> i2 = Util.cloneViaSerialization(i1);

		assertNotNull(i2);
		assertEquals(i1.getMaxFetchSize(), i2.getMaxFetchSize());
		assertEquals(i1.getClass(), i2.getClass());
	}

}
