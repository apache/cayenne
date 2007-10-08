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
package org.objectstyle.cayenne.modeler.util;

import junit.framework.TestCase;

public class CircularArrayTst extends TestCase {

    public void testArray() {

        String a = "A";
        String b = "B";
        String c = "C";
        String d = "D";
        String e = "E";
        String f = "F";
        String g = "G";
        String h = "H";

        String s = null;

        CircularArray q = new CircularArray(5);

        assertAdd(q, a, "[A, null, null, null, null]");
        assertRemove(q, a, "[null, null, null, null, null]");
        assertAdd(q, a, "[A, null, null, null, null]");

        assertAdd(q, b, "[A, B, null, null, null]");
        assertRemove(q, b, "[A, null, null, null, null]");
        assertAdd(q, b, "[A, B, null, null, null]");

        assertAdd(q, c, "[A, B, C, null, null]");
        assertRemove(q, c, "[A, B, null, null, null]");
        assertAdd(q, c, "[A, B, C, null, null]");

        assertAdd(q, d, "[A, B, C, D, null]");
        assertRemove(q, d, "[A, B, C, null, null]");
        assertAdd(q, d, "[A, B, C, D, null]");

        assertAdd(q, e, "[A, B, C, D, E]");
        assertRemove(q, e, "[A, B, C, D, null]");
        assertAdd(q, e, "[A, B, C, D, E]");

        assertAdd(q, f, "[B, C, D, E, F]");
        assertRemove(q, f, "[B, C, D, E, null]");
        assertAdd(q, f, "[B, C, D, E, F]");

        assertAdd(q, g, "[C, D, E, F, G]");

        assertRemove(q, e, "[C, D, F, G, null]");

        assertAdd(q, h, "[C, D, F, G, H]");

        assertRemove(q, c, "[D, F, G, H, null]");

        assertRemove(q, h, "[D, F, G, null, null]");

        assertRemove(q, f, "[D, G, null, null, null]");

        assertRemove(q, g, "[D, null, null, null, null]");

        assertRemove(q, d, "[null, null, null, null, null]");

        q = new CircularArray(3);
        q.add(a);
        int i = q.indexOf(a);
        if (i != 0) {
            System.out.println("indexOf(a) should be zero instead got ["
                    + String.valueOf(i)
                    + "]");
        }
        s = (String) q.get(0);
        if (s != a) {
            System.out.println("get(0) should be 'a' instead got [" + s + "]");
        }
        i = q.size();
        if (i != 1) {
            System.out.println("size() should be 1 instead got ["
                    + String.valueOf(i)
                    + "]");
        }

        q.add(b);
        i = q.indexOf(b);
        if (i != 1) {
            System.out.println("indexOf(b) should be 1 instead got ["
                    + String.valueOf(i)
                    + "]");
        }
        s = (String) q.get(0);
        if (s != a) {
            System.out.println("get(0) should be 'a' instead got [" + s + "]");
        }
        s = (String) q.get(1);
        if (s != b) {
            System.out.println("get(1) should be 'b' instead got [" + s + "]");
        }

        i = q.size();
        if (i != 2) {
            System.out.println("size() should be 2 instead got ["
                    + String.valueOf(i)
                    + "]");
        }

        q.add(c);
        i = q.indexOf(c);
        if (i != 2) {
            System.out.println("indexOf(c) should be 2 instead got ["
                    + String.valueOf(i)
                    + "]");
        }
        s = (String) q.get(0);
        if (s != a) {
            System.out.println("get(0) should be 'a' instead got [" + s + "]");
        }
        s = (String) q.get(1);
        if (s != b) {
            System.out.println("get(1) should be 'b' instead got [" + s + "]");
        }
        s = (String) q.get(2);
        if (s != c) {
            System.out.println("get(1) should be 'c' instead got [" + s + "]");
        }
        i = q.size();
        if (i != 3) {
            System.out.println("size() should be 3 instead got ["
                    + String.valueOf(i)
                    + "]");
        }

        q.add(d);
        i = q.size();
        if (i != 3) {
            System.out.println("size() should be 3 instead got ["
                    + String.valueOf(i)
                    + "]");
        }

        q.add(e);
        i = q.size();
        if (i != 3) {
            System.out.println("size() should be 3 instead got ["
                    + String.valueOf(i)
                    + "]");
        }

        if (q.contains(a)) {
            System.out.println("A should not be in the q");
        }

        i = q.indexOf(c);
        if (i != 0) {
            System.out.println("indexOf(c) should be zero instead got ["
                    + String.valueOf(i)
                    + "]");
        }
        s = (String) q.get(0);
        if (s != c) {
            System.out.println("get(0) should be 'c' instead got [" + s + "]");
        }

        i = q.indexOf(d);
        if (i != 1) {
            System.out.println("indexOf(d) should be 1 instead got ["
                    + String.valueOf(i)
                    + "]");
        }
        s = (String) q.get(1);
        if (s != d) {
            System.out.println("get(1) should be 'd' instead got [" + s + "]");
        }

        i = q.indexOf(e);
        if (i != 2) {
            System.out.println("indexOf(e) should be 2 instead got ["
                    + String.valueOf(i)
                    + "]");
        }
        s = (String) q.get(2);
        if (s != e) {
            System.out.println("get(2) should be 'e' instead got [" + s + "]");
        }

        q.resize(5);
        i = q.capacity();
        if (i != 5) {
            System.out.println("size() should be 5 after resizing to 5 instead got ["
                    + String.valueOf(i)
                    + "]");
        }

        // should be the same after resizing
        i = q.size();
        if (i != 3) {
            System.out.println("size() should be 3 instead got ["
                    + String.valueOf(i)
                    + "]");
        }

        i = q.indexOf(e);
        if (i != 2) {
            System.out.println("indexOf(e) should be 2 instead got ["
                    + String.valueOf(i)
                    + "]");
        }
        s = (String) q.get(2);
        if (s != e) {
            System.out.println("get(2) should be 'e' instead got [" + s + "]");
        }

        q.resize(2);
        i = q.capacity();
        if (i != 2) {
            System.out.println("size() should be 2 after resizing to 2 instead got ["
                    + String.valueOf(i)
                    + "]");
        }
    }

    public void testToString() {
        CircularArray a = new CircularArray(5);
        assertEquals("[null, null, null, null, null]", a.toString());
    }

    public void assertAdd(CircularArray a, Object obj, String expected) {
        a.add(obj);
        assertEquals(expected, a.toString());
    }

    public void assertRemove(CircularArray a, Object obj, String expected) {
        int i = a.indexOf(obj);
        i = a.convert(i);
        a.remove(obj);
        assertEquals(expected, a.toString());
    }
}
