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
package org.apache.cayenne.map;

import junit.framework.TestCase;

public class EmbeddableTest extends TestCase {

    public void testClassName() {
        Embeddable e1 = new Embeddable();
        assertNull(e1.getClassName());

        e1.setClassName("XYZ");
        assertEquals("XYZ", e1.getClassName());

        Embeddable e2 = new Embeddable("ABC");
        assertEquals("ABC", e2.getClassName());
    }

    public void testAddAttribute() {
        Embeddable e1 = new Embeddable();

        EmbeddableAttribute a1 = new EmbeddableAttribute("a1");
        EmbeddableAttribute a2 = new EmbeddableAttribute("a2");

        assertEquals(0, e1.getAttributeMap().size());
        e1.addAttribute(a1);
        assertEquals(1, e1.getAttributeMap().size());
        assertSame(e1, a1.getEmbeddable());
        e1.addAttribute(a2);
        assertEquals(2, e1.getAttributeMap().size());
        assertSame(e1, a2.getEmbeddable());

        assertTrue(e1.getAttributes().contains(a1));
        assertTrue(e1.getAttributes().contains(a2));
    }

    public void testRemoveAttribute() {
        Embeddable e1 = new Embeddable();

        EmbeddableAttribute a1 = new EmbeddableAttribute("a1");
        EmbeddableAttribute a2 = new EmbeddableAttribute("a2");

        e1.addAttribute(a1);
        e1.addAttribute(a2);

        e1.removeAttribute("a1");
        e1.removeAttribute("a2");
        assertEquals(0, e1.getAttributeMap().size());

        assertFalse(e1.getAttributes().contains(a1));
        assertFalse(e1.getAttributes().contains(a2));
    }
}
