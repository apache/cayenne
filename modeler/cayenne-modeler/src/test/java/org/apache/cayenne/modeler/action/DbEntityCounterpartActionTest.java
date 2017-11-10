package org.apache.cayenne.modeler.action;

import static org.junit.Assert.*;

import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.junit.Test;

public class DbEntityCounterpartActionTest {

	DbEntityCounterpartAction target;
	
	@Test
	public void testDbEntityCounterpartAction() {
		target = new DbEntityCounterpartAction(null);
		assertNotNull(target);
		target = new DbEntityCounterpartAction(Application.getInstance());
		assertNotNull(target);
	}

	@Test
	public void testViewCounterpartEntity() {
		target = new DbEntityCounterpartAction(null);
		assertNotNull(target);
		try{
			target.viewCounterpartEntity();
			fail("Expected NullPointerException");
		}catch(NullPointerException npo){}
	}

	@Test
	public void testViewCounterpartEntityObjEntity() {
		target = new DbEntityCounterpartAction(null);
		assertNotNull(target);
		try{
			target.viewCounterpartEntity(null);
			fail("Expected NullPointerException");
		}catch(NullPointerException npo){}
		try{
			Application.setInstance(new Application());
			ObjEntity oe = new ObjEntity("NAME");
			target.viewCounterpartEntity(oe);
			fail("Expected NullPointerException");
		}catch(NullPointerException npo){}
	}

}
