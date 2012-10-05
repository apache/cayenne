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
