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
package org.objectstyle.cayenne.access;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/** Test cases for DbGenerator.
  *
  * @author Andrei Adamchik
  */
public class DbGeneratorTst extends CayenneTestCase {
    protected DbGenerator gen;

    public void setUp() throws Exception {
        super.setUp();

        gen = new DbGenerator(getNode().getAdapter(), getDomain().getMap("testmap"));
    }

    public void testAdapter() throws Exception {
        assertSame(getNode().getAdapter(), gen.getAdapter());
    }

    public void testPkFilteringLogic() throws Exception {
        DataMap map = getDomain().getMap("testmap");
        DbEntity artistExhibit = map.getDbEntity("ARTIST_EXHIBIT");
        DbEntity exhibit = map.getDbEntity("EXHIBIT");

        // sanity check
        assertNotNull(artistExhibit);
        assertNotNull(exhibit);
        assertNotNull(gen.dbEntitiesRequiringAutoPK);

        // real test
        assertTrue(gen.dbEntitiesRequiringAutoPK.contains(exhibit));
        assertFalse(gen.dbEntitiesRequiringAutoPK.contains(artistExhibit));
    }

    public void testCreatePkSupport() throws Exception {
        assertTrue(gen.shouldCreatePKSupport());
        gen.setShouldCreatePKSupport(false);
        assertFalse(gen.shouldCreatePKSupport());

    }

    public void testShouldCreateTables() throws Exception {
        assertTrue(gen.shouldCreateTables());
        gen.setShouldCreateTables(false);
        assertFalse(gen.shouldCreateTables());
    }

    public void testDropPkSupport() throws Exception {

        assertFalse(gen.shouldDropPKSupport());
        gen.setShouldDropPKSupport(true);
        assertTrue(gen.shouldDropPKSupport());
    }

    public void testShouldDropTables() throws Exception {
        assertFalse(gen.shouldDropTables());
        gen.setShouldDropTables(true);
        assertTrue(gen.shouldDropTables());
    }
}