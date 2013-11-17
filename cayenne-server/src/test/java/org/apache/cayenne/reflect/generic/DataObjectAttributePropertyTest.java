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
package org.apache.cayenne.reflect.generic;

import junit.framework.TestCase;

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.Util;

public class DataObjectAttributePropertyTest extends TestCase {
	
	public void testSerialization() throws Exception {
		ObjEntity e1 = new ObjEntity("objEntityName");
		ObjAttribute a1 = new ObjAttribute("aName", "aType", e1);
		
		DataObjectAttributeProperty p1 = new DataObjectAttributeProperty(a1);
		DataObjectAttributeProperty p2 = Util.cloneViaSerialization(p1);
		
		assertNotNull(p2);
		assertNotNull(p2.getAttribute());
		assertEquals(p1.getAttribute().getName(), p2.getAttribute().getName());
		assertEquals(p1.getName(), p2.getName());
	}

}
