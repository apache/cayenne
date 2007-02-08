/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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

package org.objectstyle.cayenne.util;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

public class InvocationTst extends TestCase {
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
