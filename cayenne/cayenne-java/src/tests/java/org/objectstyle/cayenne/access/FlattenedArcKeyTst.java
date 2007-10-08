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
package org.objectstyle.cayenne.access;

import junit.framework.TestCase;

import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.map.MockObjRelationship;

/**
 * @author Andrei Adamchik
 */
public class FlattenedArcKeyTst extends TestCase {

    public void testAttributes() {
        ObjectId src = new ObjectId("X");
        ObjectId target = new ObjectId("Y");
        MockObjRelationship r1 = new MockObjRelationship("r1");
        r1.setReverseRelationship(new MockObjRelationship("r2"));

        FlattenedArcKey update = new FlattenedArcKey(src, target, r1);

        assertSame(src, update.sourceId);
        assertSame(target, update.destinationId);
        assertSame(r1, update.relationship);
        assertSame(r1.getReverseRelationship(), update.reverseRelationship);
        assertTrue(update.isBidirectional());
    }

    public void testEquals() {
        ObjectId src = new ObjectId("X");
        ObjectId target = new ObjectId("Y");
        MockObjRelationship r1 = new MockObjRelationship("r1");
        r1.setReverseRelationship(new MockObjRelationship("r2"));

        FlattenedArcKey update = new FlattenedArcKey(src, target, r1);
        FlattenedArcKey update1 = new FlattenedArcKey(target, src, r1
                .getReverseRelationship());

        FlattenedArcKey update2 = new FlattenedArcKey(
                target,
                src,
                new MockObjRelationship("r3"));

        assertTrue(update.equals(update1));
        assertFalse(update.equals(update2));
    }
}