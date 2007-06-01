/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DerivedDbEntityTst extends CayenneTestCase {
    protected DerivedDbEntity ent;
    protected DataMap map;

    public void setUp() throws Exception {
        ent = new DerivedDbEntity("abc");
        map = new DataMap();
        map.addDbEntity(ent);
    }

    public void testParentEntity() throws Exception {
    	assertNull(ent.getParentEntity());
    	map.addDbEntity(ent);
    	DbEntity parent = new DbEntity("xyz");
    	map.addDbEntity(parent);
    	
    	ent.setParentEntity(parent);
    	
    	assertSame(parent, ent.getParentEntity());
    }
    
    public void testGroupByAttributes() throws Exception {
    	DerivedDbAttribute at = new DerivedDbAttribute();
    	at.setName("abc");
        ent.addAttribute(at);
    	assertEquals(0, ent.getGroupByAttributes().size());
    	
    	at.setGroupBy(true);
    	assertEquals(1, ent.getGroupByAttributes().size());
    }
    
    public void testFullyQualifiedName1() throws Exception {
    	assignParent();
    	
    	ent.setName("derived");
    	ent.getParentEntity().setName("parent");
    	assertEquals(ent.getParentEntity().getFullyQualifiedName(), ent.getFullyQualifiedName());
    }
    
    public void testFullyQualifiedName2() throws Exception {
    	assignParent();
    	
    	ent.setName("derived");
    	ent.getParentEntity().setName("parent");
    	ent.getParentEntity().setSchema("parent_schema");
    	assertEquals(ent.getParentEntity().getFullyQualifiedName(), ent.getFullyQualifiedName());
    }
    
    public void testResetToParentView1() throws Exception {
    	assignParent();
    	
    	DerivedDbAttribute at = new DerivedDbAttribute();
    	at.setName("abc");
    	
    	DbRelationship rel = new DbRelationship();
    	rel.setName("abc");
    	
    	ent.addAttribute(at);
    	ent.addRelationship(rel);
    	
    	ent.resetToParentView();
    	
    	assertEquals(0, ent.getAttributes().size());
    	assertEquals(0, ent.getRelationships().size());
    }
    
    public void testResetToParentView2() throws Exception {
    	assignParent();
    	
    	DbAttribute at = new DbAttribute();
    	at.setName("abc");
    	
    	DbRelationship rel = new DbRelationship();
    	rel.setName("abc");
    	
    	ent.getParentEntity().addAttribute(at);
    	ent.getParentEntity().addRelationship(rel);
    	
    	ent.resetToParentView();
    	
    	assertEquals(1, ent.getAttributes().size());
    	assertEquals(1, ent.getRelationships().size());
    }
    
    protected void assignParent() {
    	DbEntity parent = new DbEntity("qwerty");
    	map.addDbEntity(parent);
    	ent.setParentEntity(parent);
    }
}

