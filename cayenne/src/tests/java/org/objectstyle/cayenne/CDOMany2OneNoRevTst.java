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
package org.objectstyle.cayenne;

import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting1;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * Tests DataObjects with no reverse relationships.
 * 
 * @author Andrei Adamchik
 */
public class CDOMany2OneNoRevTst extends CayenneDOTestBase {

    /**
     * @see org.objectstyle.cayenne.CayenneDOTestBase#newPainting()
     */
    protected Painting1 newPainting1() {
        Painting1 p1 = (Painting1) ctxt.createAndRegisterNewObject("Painting1");
        p1.setPaintingTitle(paintingName);
        return p1;
    }

    protected Painting1 fetchPainting1() {
        SelectQuery q =
            new SelectQuery(
                "Painting1",
                ExpressionFactory.matchExp("paintingTitle", paintingName));
        List pts = ctxt.performQuery(q);
        return (pts.size() > 0) ? (Painting1) pts.get(0) : null;
    }

    public void testNewAdd() throws Exception {
        Artist a1 = newArtist();
        Painting1 p1 = newPainting1();

        // *** TESTING THIS *** 
        p1.setToArtist(a1);

        // test before save
        assertSame(a1, p1.getToArtist());

        // do save
        ctxt.commitChanges();
        ctxt = createDataContext();

        // test database data
        Painting1 p2 = fetchPainting1();
        Artist a2 = p2.getToArtist();
        assertNotNull(a2);
        assertEquals(artistName, a2.getArtistName());
    }
}
