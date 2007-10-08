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
package org.objectstyle.cayenne;

import java.util.Arrays;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.testdo.relationship.ToManyFkDep;
import org.objectstyle.cayenne.testdo.relationship.ToManyFkRoot;
import org.objectstyle.cayenne.testdo.relationship.ToManyRoot2;
import org.objectstyle.cayenne.unit.RelationshipTestCase;

// TODO: this mapping scenario is really unsupported ... this is just an attempt at
// partial solution
public class CDOOneToManyFKTst extends RelationshipTestCase {

    public void testReadRelationship() throws Exception {
        deleteTestData();
        DataContext context = createDataContext();

        ToManyRoot2 src2 = (ToManyRoot2) context
                .createAndRegisterNewObject(ToManyRoot2.class);
        ToManyFkRoot src = (ToManyFkRoot) context
                .createAndRegisterNewObject(ToManyFkRoot.class);

        // this should go away when such mapping becomes fully supported
        src.setDepId(new Integer(1));
        ToManyFkDep target = (ToManyFkDep) context
                .createAndRegisterNewObject(ToManyFkDep.class);

        // this should go away when such mapping becomes fully supported
        target.setDepId(new Integer(1));
        target.setRoot2(src2);

        src.addToDeps(target);
        context.commitChanges();

        context.invalidateObjects(Arrays.asList(new Object[] {
                src, target, src2
        }));

        ToManyFkRoot src1 = (ToManyFkRoot) DataObjectUtils.objectForPK(context, src
                .getObjectId());
        assertNotNull(src1.getDeps());
        assertEquals(1, src1.getDeps().size());
        // resolve HOLLOW
        assertSame(src1, ((ToManyFkDep) src1.getDeps().get(0)).getRoot());

        context.invalidateObjects(Arrays.asList(new Object[] {
                src1, src1.getDeps().get(0)
        }));

        ToManyFkDep target2 = (ToManyFkDep) DataObjectUtils.objectForPK(context, target
                .getObjectId());
        assertNotNull(target2.getRoot());

        // resolve HOLLOW
        assertSame(target2, target2.getRoot().getDeps().get(0));
    }

}
