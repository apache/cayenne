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
package org.apache.cayenne.exp.parser;

import static org.junit.Assert.assertTrue;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ASTObjPathIT extends RuntimeCase {

	@Inject
	private ObjectContext context;

	@Test
	public void testEvaluate_ObjEntity() {
		ASTObjPath node = new ASTObjPath("paintingArray.paintingTitle");

		ObjEntity ae = context.getEntityResolver().getObjEntity(Artist.class);

		Object target = node.evaluate(ae);
		assertTrue(target instanceof ObjAttribute);
	}

	@Test
	public void testEvaluate_ObjEntity_Outer() {
		ASTObjPath node = new ASTObjPath("paintingArray+.paintingTitle");

		ObjEntity ae = context.getEntityResolver().getObjEntity(Artist.class);

		Object target = node.evaluate(ae);
		assertTrue(target instanceof ObjAttribute);
	}

}
