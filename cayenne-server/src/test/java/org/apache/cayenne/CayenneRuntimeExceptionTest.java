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

package org.apache.cayenne;

import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CayenneRuntimeExceptionTest {

    @Test
    public void testConstructor1() {
        CayenneRuntimeException ex = new CayenneRuntimeException();
        assertNull(ex.getCause());
        assertTrue(ex.getMessage().startsWith(CayenneRuntimeException.getExceptionLabel()));
    }

    @Test
    public void testConstructor2() {
        CayenneRuntimeException ex = new CayenneRuntimeException("abc");
        assertNull(ex.getCause());
        assertEquals(CayenneRuntimeException.getExceptionLabel() + "abc", ex.getMessage());
    }

    @Test
    public void testConstructor3() {
        Throwable cause = new Throwable();
        CayenneRuntimeException ex = new CayenneRuntimeException(cause);
        assertSame(cause, ex.getCause());
        assertEquals(
                CayenneRuntimeException.getExceptionLabel() + cause.toString(),
            ex.getMessage());
    }

    @Test
    public void testConstructor4() {
        Throwable cause = new Throwable();
        CayenneRuntimeException ex = new CayenneRuntimeException("abc", cause);
        assertSame(cause, ex.getCause());
        assertEquals(CayenneRuntimeException.getExceptionLabel() + "abc", ex.getMessage());
    }

    @Test
    public void testConstructorNullMessage() {
        Throwable cause = new Throwable();

        CayenneRuntimeException ex = new CayenneRuntimeException(null, cause);
        assertSame(cause, ex.getCause());
        assertEquals(CayenneRuntimeException.getExceptionLabel() + "(no message)", ex.getMessage());
        assertNull(ex.getUnlabeledMessage());

        CayenneRuntimeException ex2 = new CayenneRuntimeException((String)null);
        assertNull(ex2.getCause());
        assertEquals(CayenneRuntimeException.getExceptionLabel() + "(no message)", ex2.getMessage());
        assertNull(ex2.getUnlabeledMessage());
    }

    @Test
    public void testThrow1() {
        try {
            throw new CayenneRuntimeException();
        }
        catch (CayenneRuntimeException rtex) {
            StringWriter w = new StringWriter();
            rtex.printStackTrace(new PrintWriter(w));
        }
    }

    @Test
    public void testThrow2() {
        try {
            try {
                throw new Throwable("Test Cause");
            }
            catch (Throwable th) {
                throw new CayenneRuntimeException(th);
            }
        }
        catch (CayenneRuntimeException rtex) {
            StringWriter w = new StringWriter();
            rtex.printStackTrace(new PrintWriter(w));
        }
    }

    @Test
    public void testMessageFormatting1() {
        CayenneRuntimeException ex = new CayenneRuntimeException("x%sx%sx", "a", "b");
        assertEquals("xaxbx", ex.getUnlabeledMessage());
    }

    @Test
    public void testMessageFormatting2() {
        Throwable cause = new Throwable();
        CayenneRuntimeException ex = new CayenneRuntimeException("x%sx%sx", cause, "a", "b");
        assertEquals("xaxbx", ex.getUnlabeledMessage());
    }

}
