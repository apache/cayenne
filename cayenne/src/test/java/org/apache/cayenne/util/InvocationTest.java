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


package org.apache.cayenne.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InvocationTest {

    private String _methodName = "myListenerMethod";

    @Test
	public void equalsReflexive() throws NoSuchMethodException {
		Invocation inv0 = new Invocation(this, _methodName);

		assertEquals(inv0, inv0);
	}

    @Test
	public void equalsSymmetric() throws NoSuchMethodException {
		Invocation inv01 = new Invocation(this, _methodName);
		Invocation inv02 = new Invocation(this, _methodName);

		assertEquals(inv01, inv02);
		assertEquals(inv02, inv01);
	}

    @Test
	public void equalsTransitive() throws NoSuchMethodException {
		Invocation inv01 = new Invocation(this, _methodName);
		Invocation inv02 = new Invocation(this, _methodName);
		Invocation inv03 = new Invocation(this, _methodName);

		assertEquals(inv01, inv02);
		assertEquals(inv02, inv03);
		assertEquals(inv01, inv03);
	}

    @Test
	public void equalsNull() throws NoSuchMethodException {
		Invocation inv0 = new Invocation(this, _methodName);

		assertTrue(inv0.equals(null) == false);
	}

    @Test
	public void equalsDifferentMethods() throws NoSuchMethodException  {
		Invocation inv0 = new Invocation(this, _methodName);
		Invocation inv1 = new Invocation(this, _methodName, new Class[]{Object.class});

		assertTrue(inv0.equals(inv1) == false);
	}

    @Test
	public void equalsNoVsOneArg() throws NoSuchMethodException {
		Invocation inv0 = new Invocation(this, _methodName);
		Invocation inv1 = new Invocation(this, _methodName, new Class[]{Object.class});

		assertTrue(inv0.equals(inv1) == false);
	}

    @Test
	public void addToSet() throws NoSuchMethodException {
		HashSet set = new HashSet();

		Invocation inv0 = new Invocation(this, _methodName);

		set.add(inv0);
		set.add(inv0);

		assertEquals(1, set.size());
	}

    @Test
	public void addTwo() throws NoSuchMethodException {
		Set set = new HashSet();

		Invocation inv01 = new Invocation(this, _methodName);
		Invocation inv02 = new Invocation(this, _methodName);

		set.add(inv01);
		set.add(inv02);

		assertEquals(1, set.size());
	}

    @Test
	public void emptyParamTypes() throws NoSuchMethodException {
		assertThrows(IllegalArgumentException.class,
				() -> new Invocation(this, _methodName, new Class[]{}));
	}

    @Test
	public void nullParamTypes0() throws NoSuchMethodException {
		assertThrows(IllegalArgumentException.class,
				() -> new Invocation(this, _methodName, new Class[]{null}));
	}

    @Test
	public void nullParamTypes1() throws NoSuchMethodException {
		assertThrows(IllegalArgumentException.class,
				() -> new Invocation(this, _methodName, new Class[]{String.class, null}));
	}

    @Test
	public void fireNoArgument() throws NoSuchMethodException {
		Invocation inv0 = new Invocation(this, _methodName);

		assertTrue(inv0.fire());
	}

    @Test
	public void fireOneArgument() throws NoSuchMethodException {
		Invocation inv1 = new Invocation(this, _methodName, new Class[]{Object.class});

		assertTrue(inv1.fire("foo"));
	}

    @Test
	public void fireWrongArgumentCount0() throws Exception {
		Invocation inv0 = new Invocation(this, _methodName);
		assertThrows(IllegalArgumentException.class, () -> inv0.fire("foo"));
	}

    @Test
	public void fireWrongArgumentCount1() throws Exception {
		Invocation inv1 = new Invocation(this, _methodName, new Class[]{Object.class});
		assertThrows(IllegalArgumentException.class, () -> inv1.fire());
	}

    @Test
	public void fireWrongArgumentCount2() throws Exception {
		Invocation inv1 = new Invocation(this, _methodName, new Class[]{Object.class});
		assertThrows(IllegalArgumentException.class,
				() -> inv1.fire(new Object[]{"foo", "bar"}));
	}

    @Test
	public void fireNullArgArray() throws Exception {
		Invocation inv1 = new Invocation(this, _methodName, new Class[]{Object.class});
		assertThrows(IllegalArgumentException.class, () -> inv1.fire(null));
	}

    @Test
	public void garbageCollection() throws NoSuchMethodException {
		// create an invocation with an listener that will be garbage collected
		Invocation inv0 = new Invocation(new String(), "toString");

		// (hopefully) make the listener go away
		System.gc();
		System.gc();

		assertFalse(inv0.fire());
	}


	// these methods exist for the test of Invocation equality
	public void myListenerMethod() {
	}

	public void myListenerMethod(Object o) {
	}

}
