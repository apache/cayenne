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

import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.util.MockMappingNamespace;

public class DbRelationshipTst extends CayenneTestCase {
    protected DbEntity artistEnt;
    protected DbEntity paintingEnt;
    protected DbEntity galleryEnt;
    
    public void setUp() throws Exception {
        artistEnt = getDbEntity("ARTIST");
        paintingEnt = getDbEntity("PAINTING");
        galleryEnt = getDbEntity("GALLERY");
    }
    
    public void testSrcFkSnapshotWithTargetSnapshot() throws Exception {
        Map map = new HashMap();
        Integer id = new Integer(44);
        map.put("GALLERY_ID", id);
        
        DbRelationship dbRel = (DbRelationship)galleryEnt.getRelationship("paintingArray");
        Map targetMap = dbRel.getReverseRelationship().srcFkSnapshotWithTargetSnapshot(map);
        assertEquals(id, targetMap.get("GALLERY_ID"));
    }
    
    public void testGetReverseRelationship1() throws Exception {
        // start with "to many"
        DbRelationship r1 = (DbRelationship)artistEnt.getRelationship("paintingArray");
        DbRelationship r2 = r1.getReverseRelationship();
        
        assertNotNull(r2);
        assertSame(paintingEnt.getRelationship("toArtist"), r2);
    }
    
    public void testGetReverseRelationship2() throws Exception {
        // start with "to one"
        DbRelationship r1 = (DbRelationship)paintingEnt.getRelationship("toArtist");
        DbRelationship r2 = r1.getReverseRelationship();
        
        assertNotNull(r2);
        assertSame(artistEnt.getRelationship("paintingArray"), r2);
    }
    
    public void testGetReverseRelationshipToSelf() throws Exception {
        
        // assemble mockup entity
        MockMappingNamespace namespace = new MockMappingNamespace();
        DbEntity e = new DbEntity("test");
        namespace.addDbEntity(e);
        DbRelationship rforward = new DbRelationship("rforward");
        e.addRelationship(rforward);
        rforward.setSourceEntity(e);
        rforward.setTargetEntity(e);
        
        assertNull(rforward.getReverseRelationship());
        
        // add a joins
        e.addAttribute(new DbAttribute("a1"));
        e.addAttribute(new DbAttribute("a2"));
        rforward.addJoin(new DbJoin(rforward, "a1", "a2"));
        
        assertNull(rforward.getReverseRelationship());
        
        // create reverse
        
        DbRelationship rback = new DbRelationship("rback");
        e.addRelationship(rback);
        rback.setSourceEntity(e);
        rback.setTargetEntity(e);
        
        assertNull(rforward.getReverseRelationship());
        
        // create reverse join
        rback.addJoin(new DbJoin(rback, "a2", "a1"));
        
        assertSame(rback, rforward.getReverseRelationship());
        assertSame(rforward, rback.getReverseRelationship());
    }
}
