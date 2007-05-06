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
package org.objectstyle.cayenne.map;

import junit.framework.TestCase;

import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.property.BaseClassDescriptor;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1;
import org.objectstyle.cayenne.testdo.mt.MtTable1;

public class EntityDescriptorTst extends TestCase {

    public void testConstructor() {
        ObjEntity e1 = new ObjEntity("TestEntity");
        e1.setClassName(String.class.getName());

        EntityDescriptor d1 = new EntityDescriptor(e1, null);
        assertNull(d1.getSuperclassDescriptor());

        BaseClassDescriptor mockSuper = new BaseClassDescriptor(null) {
        };
        EntityDescriptor d2 = new EntityDescriptor(e1, mockSuper);
        assertSame(mockSuper, d2.getSuperclassDescriptor());
    }

    public void testCompile() {
        ObjEntity e1 = new ObjEntity("TestEntity");
        e1.setClassName(CayenneDataObject.class.getName());

        // compilation must be done in constructor...
        EntityDescriptor d1 = new EntityDescriptor(e1, null);
        d1.compile(new EntityResolver());
        assertTrue(d1.isValid());
    }

    public void testProperties() {
        DataMap map = new DataMap();

        ObjEntity e1 = new ObjEntity("TestEntity");
        map.addObjEntity(e1);
        e1.setClassName(ClientMtTable1.class.getName());
        e1.addAttribute(new ObjAttribute(
                ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY,
                String.class.getName(),
                e1));

        ObjRelationship toMany = new ObjRelationship(MtTable1.TABLE2ARRAY_PROPERTY) {

            public boolean isToMany() {
                return true;
            }
        };
        toMany.setTargetEntityName(e1.getName());

        e1.addRelationship(toMany);

        EntityDescriptor d1 = new EntityDescriptor(e1, null);
        d1.compile(new EntityResolver());

        assertSame(e1, d1.getEntity());
        assertNotNull(d1.getObjectClass());
        assertEquals(ClientMtTable1.class.getName(), d1.getObjectClass().getName());

        // properties that exist in the entity must be included
        assertNotNull(d1.getDeclaredProperty(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY));

        // properties not described in the entity must not be included
        assertNull(d1.getDeclaredProperty(ClientMtTable1.SERVER_ATTRIBUTE1_PROPERTY));

        // collection properties must be returned just as simple properties do...
        assertNotNull(d1.getDeclaredProperty(ClientMtTable1.TABLE2ARRAY_PROPERTY));
    }
}
