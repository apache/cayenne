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

import java.sql.ResultSet;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.access.trans.SelectQueryTranslator;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.unit.JDBCAccessTestCase;

public class DefaultResultIteratorTst extends JDBCAccessTestCase {
    protected DefaultResultIterator it;

    protected void init() throws Exception {
        super.init();
        SelectQueryTranslator assembler = (SelectQueryTranslator) translator;
        ResultSet rs = st.executeQuery();

        it =
            new DefaultResultIterator(
                translator.getCon(),
                st,
                rs,
                assembler.getResultDescriptor(rs),
                -1);
    }

    protected void cleanup() throws Exception {
        if (it != null) {
            it.close();
            st = null;
        }

        super.cleanup();
    }

    public void testClose1() throws Exception {
        try {
            init();
            assertFalse(connection.isClosed());

            it.setClosingConnection(false);
            it.close();

            // caller must close the connection
            assertFalse(connection.isClosed());
        }
        finally {
            connection.close();
        }
    }

    public void testClose2() throws Exception {
        init();
        assertFalse(connection.isClosed());

        it.setClosingConnection(true);
        it.close();

        // iterator must close the connection
        assertTrue(connection.isClosed());
    }

    public void testCheckNextRow() throws Exception {
        try {
            init();

            assertTrue(it.hasNextRow());
            it.checkNextRow();
            assertTrue(it.hasNextRow());

        }
        finally {
            cleanup();
        }
    }

    public void testHasNextRow() throws java.lang.Exception {
        try {
            init();
            assertTrue(it.hasNextRow());
        }
        finally {
            cleanup();
        }
    }

    public void testNextDataRow() throws java.lang.Exception {
        try {
            init();

            // must be as many rows as we have artists
            // inserted in the database
            for (int i = 0; i < DataContextTst.artistCount; i++) {
                assertTrue(it.hasNextRow());
                it.nextDataRow();
            }

            // rows must end here
            assertFalse(it.hasNextRow());

        }
        finally {
            cleanup();
        }
    }

    public void testNextObjectId() throws Exception {
        try {
            init();

            DbEntity entity =
                getDomain().getEntityResolver().lookupDbEntity(Artist.class);

            // must be as many rows as we have artists
            // inserted in the database
            for (int i = 0; i < DataContextTst.artistCount; i++) {
                assertTrue(it.hasNextRow());
                it.nextObjectId(entity);
            }

            // rows must end here
            assertFalse(it.hasNextRow());

        }
        finally {
            cleanup();
        }
    }

    public void testIsClosingConnection() throws Exception {
        try {
            init();
            assertFalse(it.isClosingConnection());
            it.setClosingConnection(true);
            assertTrue(it.isClosingConnection());
        }
        finally {
            it.setClosingConnection(false);
            cleanup();
        }
    }

    public void testReadDataRow() throws java.lang.Exception {
        try {
            init();

            // must be as many rows as we have artists
            // inserted in the database
            Map dataRow = null;
            for (int i = 1; i <= DataContextTst.artistCount; i++) {
                assertTrue(it.hasNextRow());
                dataRow = it.nextDataRow();
            }

            assertEquals("Failed row: " + dataRow, "artist9", dataRow.get("ARTIST_NAME"));
        }
        finally {
            cleanup();
        }
    }
}