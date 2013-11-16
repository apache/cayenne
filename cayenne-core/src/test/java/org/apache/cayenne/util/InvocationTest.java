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


package org.apache.cayenne.util;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

public class InvocationTest extends TestCase {
	private String _methodName = "myListenerMethod";

	public void testEqualsReflexive() throws NoSuchMethodException {
		Invocation inv0 = new Invocation(this, _methodName);		

		Assert.assertEquals(inv0, inv0);
	}

	public void testEqualsSymmetric() throws NoSuchMethodException {
		Invocation inv01 = new Invocation(this, _methodName);
		Invocation inv02 = new Invocation(this, _methodName);
		
		Assert.assertEquals(inv01, inv02);
		Assert.assertEquals(inv02, inv01);
	}

	public void testEqualsTransitive() throws NoSuchMethodException {
		Invocation inv01 = new Invocation(this, _methodName);
		Invocation inv02 = new Invocation(this, _methodName);
		Invocation inv03 = new Invocation(this, _methodName);
		
		Assert.assertEquals(inv01, inv02);
		Assert.assertEquals(inv02, inv03);
		Assert.assertEquals(inv01, inv03);
	}

	public void testEqualsNull() throws NoSuchMethodException {
		Invocation inv0 = new Invocation(this, _methodName);

		Assert.assertTrue(inv0.equals(null) == false);
	}

	public void testEqualsDifferentMethods() throws NoSuchMethodException  {
		Invocation inv0 = new Invocation(this, _methodName);
		Invocation inv1 = new Invocation(this, _methodName, new Class[]{Object.class});

		Assert.assertTrue(inv0.equals(inv1) == false);
	}

	public void testEqualsNoVsOneArg() throws NoSuchMethodException {
		Invocation inv0 = new Invocation(this, _methodName);
		Invocation inv1 = new Invocation(this, _methodName, new Class[]{Object.class});

		Assert.assertTrue(inv0.equals(inv1) == false);
	}

	public void testAddToSet() throws NoSuchMethodException {
		HashSet set = new HashSet();
		
		Invocation inv0 = new Invocation(this, _methodName);

		set.add(inv0);
		set.add(inv0);

		Assert.assertEquals(1, set.size());
	}

	public void testAddTwo() throws NoSuchMethodException {
		Set set = new HashSet();
		
		Invocation inv01 = new Invocation(this, _methodName);
		Invocation inv02 = new Invocation(this, _methodName);
		
		set.add(inv01);
		set.add(inv02);

		Assert.assertEquals(1, set.size());
	}

	public void testEmptyParamTypes() throws NoSuchMethodException {
		try {
			new Invocation(this, _methodName, new Class[]{});
			Assert.fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testNullParamTypes0() throws NoSuchMethodException {
		try {
			new Invocation(this, _methodName, new Class[]{null});
			Assert.fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testNullParamTypes1() throws NoSuchMethodException {
		try {
			new Invocation(this, _methodName, new Class[]{String.class, null});
			Assert.fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testFireNoArgument() throws NoSuchMethodException {
		Invocation inv0 = new Invocation(this, _methodName);

		Assert.assertTrue(inv0.fire());
	}

	public void testFireOneArgument() throws NoSuchMethodException {
		Invocation inv1 = new Invocation(this, _methodName, new Class[]{Object.class});

		Assert.assertTrue(inv1.fire("foo"));
	}

	public void testFireWrongArgumentCount0() throws Exception {
		Invocation inv0 = new Invocation(this, _methodName);

		try {
			inv0.fire("foo");
			Assert.fail();
		}

		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testFireWrongArgumentCount1() throws Exception {
		Invocation inv1 = new Invocation(this, _methodName, new Class[]{Object.class});

		try {
			inv1.fire();
			Assert.fail();
		}

		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testFireWrongArgumentCount2() throws Exception {
		Invocation inv1 = new Invocation(this, _methodName, new Class[]{Object.class});

		try {
			inv1.fire(new Object[]{"foo", "bar"});
			Assert.fail();
		}

		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testFireNullArgArray() throws Exception {
		Invocation inv1 = new Invocation(this, _methodName, new Class[]{Object.class});

		try {
			inv1.fire(null);
			Assert.fail();
		}

		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testGarbageCollection() throws NoSuchMethodException {
		// create an invocation with an listener that will be garbage collected
		Invocation inv0 = new Invocation(new String(), "toString");

		// (hopefully) make the listener go away
		System.gc();
		System.gc();

		Assert.assertFalse(inv0.fire());
	}

	
	// these methods exist for the test of Invocation equality
	public void myListenerMethod() {
	}

	public void myListenerMethod(Object o) {
	}

}
