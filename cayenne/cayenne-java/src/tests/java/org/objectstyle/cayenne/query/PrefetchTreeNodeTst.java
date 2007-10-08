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
package org.objectstyle.cayenne.query;

import junit.framework.TestCase;

import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.remote.hessian.service.HessianUtil;
import org.objectstyle.cayenne.util.Util;

public class PrefetchTreeNodeTst extends TestCase {

    public void testAddPath() {
        PrefetchTreeNode tree = new PrefetchTreeNode();
        tree.addPath("abc");
        tree.addPath("abc.def.mnk");
        tree.addPath("xyz");

        assertTrue(tree.isPhantom());

        PrefetchTreeNode n1 = tree.getNode("abc");
        assertNotNull(n1);
        assertTrue(n1.isPhantom());
        assertEquals("abc", n1.getName());

        PrefetchTreeNode n2 = tree.getNode("abc.def");
        assertNotNull(n2);
        assertTrue(n2.isPhantom());
        assertEquals("def", n2.getName());

        PrefetchTreeNode n3 = tree.getNode("abc.def.mnk");
        assertNotNull(n3);
        assertTrue(n3.isPhantom());
        assertEquals("mnk", n3.getName());

        PrefetchTreeNode n4 = tree.getNode("xyz");
        assertNotNull(n4);
        assertTrue(n4.isPhantom());
        assertEquals("xyz", n4.getName());
    }

    public void testGetPath() {
        PrefetchTreeNode tree = new PrefetchTreeNode();
        tree.addPath("abc");
        tree.addPath("abc.def.mnk");
        tree.addPath("xyz");

        assertEquals("", tree.getPath());

        PrefetchTreeNode n1 = tree.getNode("abc");
        assertEquals("abc", n1.getPath());

        PrefetchTreeNode n2 = tree.getNode("abc.def");
        assertEquals("abc.def", n2.getPath());

        PrefetchTreeNode n3 = tree.getNode("abc.def.mnk");
        assertEquals("abc.def.mnk", n3.getPath());

        PrefetchTreeNode n4 = tree.getNode("xyz");
        assertEquals("xyz", n4.getPath());
    }

    public void testTreeSerializationWithHessian() throws Exception {
        PrefetchTreeNode n1 = new PrefetchTreeNode();
        PrefetchTreeNode n2 = n1.addPath("abc");

        PrefetchTreeNode nc1 = (PrefetchTreeNode) HessianUtil
                .cloneViaClientServerSerialization(n1, new EntityResolver());
        assertNotNull(nc1);

        PrefetchTreeNode nc2 = nc1.getNode("abc");
        assertNotNull(nc2);
        assertNotSame(nc2, n2);
        assertSame(nc1, nc2.getParent());
        assertEquals("abc", nc2.getName());
    }

    public void testSubtreeSerializationWithHessian() throws Exception {
        PrefetchTreeNode n1 = new PrefetchTreeNode();
        PrefetchTreeNode n2 = n1.addPath("abc");
        PrefetchTreeNode n3 = n2.addPath("xyz");

        // test that substree was serialized as independent tree, instead of sucking
        PrefetchTreeNode nc2 = (PrefetchTreeNode) HessianUtil
                .cloneViaClientServerSerialization(n2,new EntityResolver());
        assertNotNull(nc2);
        assertNull(nc2.getParent());

        PrefetchTreeNode nc3 = nc2.getNode("xyz");
        assertNotNull(nc3);
        assertNotSame(nc3, n3);
        assertSame(nc2, nc3.getParent());
        assertEquals("xyz", nc3.getName());
    }

    public void testTreeSerialization() throws Exception {
        PrefetchTreeNode n1 = new PrefetchTreeNode();
        PrefetchTreeNode n2 = n1.addPath("abc");

        PrefetchTreeNode nc1 = (PrefetchTreeNode) Util.cloneViaSerialization(n1);
        assertNotNull(nc1);

        PrefetchTreeNode nc2 = nc1.getNode("abc");
        assertNotNull(nc2);
        assertNotSame(nc2, n2);
        assertSame(nc1, nc2.getParent());
        assertEquals("abc", nc2.getName());
    }

    public void testSubtreeSerialization() throws Exception {
        PrefetchTreeNode n1 = new PrefetchTreeNode();
        PrefetchTreeNode n2 = n1.addPath("abc");
        PrefetchTreeNode n3 = n2.addPath("xyz");

        // test that substree was serialized as independent tree, instead of sucking
        PrefetchTreeNode nc2 = (PrefetchTreeNode) Util.cloneViaSerialization(n2);
        assertNotNull(nc2);
        assertNull(nc2.getParent());

        PrefetchTreeNode nc3 = nc2.getNode("xyz");
        assertNotNull(nc3);
        assertNotSame(nc3, n3);
        assertSame(nc2, nc3.getParent());
        assertEquals("xyz", nc3.getName());
    }
}
