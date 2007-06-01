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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;

/**
 * A ResultIterator that does in-memory filtering of rows to return only distinct rows.
 * Distinct comparison is done by comparing ObjectIds created from each row. Internally
 * DistinctResultIterator wraps another ResultIterator that provides the actual rows. The
 * current limitation is that once switched to reading ids instead of rows (i.e. when
 * "nextObjectId()" is called for the first time), it can't be used to read data rows
 * again. This is pretty sensible for most things in Cayenne.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
class DistinctResultIterator implements ResultIterator {

    protected ResultIterator wrappedIterator;
    protected Set fetchedIds;
    protected Map nextDataRow;
    protected DbEntity defaultEntity;
    protected boolean readingIds;

    /**
     * Creates new DistinctResultIterator wrapping another ResultIterator.
     * 
     * @param wrappedIterator
     * @param defaultEntity an entity needed to build ObjectIds for distinct comparison.
     */
    public DistinctResultIterator(ResultIterator wrappedIterator, DbEntity defaultEntity)
            throws CayenneException {
        if (wrappedIterator == null) {
            throw new CayenneException("Null wrapped iterator.");
        }

        if (defaultEntity == null) {
            throw new CayenneException("Null defaultEntity.");
        }

        this.wrappedIterator = wrappedIterator;
        this.defaultEntity = defaultEntity;
        this.fetchedIds = new HashSet();

        checkNextRow();
    }

    /**
     * CLoses underlying ResultIterator.
     */
    public void close() throws CayenneException {
        wrappedIterator.close();
    }

    /**
     * Returns all data rows.
     */
    public List dataRows(boolean close) throws CayenneException {
        List list = new ArrayList();

        try {
            while (this.hasNextRow()) {
                list.add(this.nextDataRow());
            }
            return list;
        }
        finally {
            if (close) {
                this.close();
            }
        }
    }

    public int getDataRowWidth() {
        return wrappedIterator.getDataRowWidth();
    }

    public boolean hasNextRow() throws CayenneException {
        return nextDataRow != null;
    }

    public Map nextDataRow() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        Map row = nextDataRow;
        checkNextRow();
        return row;
    }

    /**
     * @deprecated This method is deprecated in the interface.
     */
    public Map nextObjectId() throws CayenneException {
        return nextObjectId(defaultEntity);
    }

    /**
     * Returns a Map for the next ObjectId. After calling this method, calls to
     * "nextDataRow()" will result in exceptions.
     */
    public Map nextObjectId(DbEntity entity) throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        Map row = nextDataRow;

        // if we were previously reading full rows, we need to strip extra keys...
        if (!readingIds) {
            Iterator it = row.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String name = (String) entry.getKey();
                DbAttribute attribute = (DbAttribute) entity.getAttribute(name);
                if (attribute == null || !attribute.isPrimaryKey()) {
                    it.remove();
                }
            }
        }

        checkNextId(entity);
        return row;
    }

    public void skipDataRow() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        if (readingIds) {
            checkNextId(defaultEntity);
        }
        else {
            checkNextRow();
        }
    }

    void checkNextRow() throws CayenneException {
        if (readingIds) {
            throw new CayenneException(
                    "Can't go back from reading ObjectIds to reading rows.");
        }

        nextDataRow = null;
        while (wrappedIterator.hasNextRow()) {
            Map next = wrappedIterator.nextDataRow();

            // create id map...
            // TODO: this can be optimized by creating an array with id keys
            // to avoid iterating over default entity attributes...

            Map id = new HashMap();
            Iterator it = defaultEntity.getPrimaryKey().iterator();
            while (it.hasNext()) {
                DbAttribute pk = (DbAttribute) it.next();
                id.put(pk.getName(), next.get(pk.getName()));
            }

            if (fetchedIds.add(id)) {
                this.nextDataRow = next;
                break;
            }
        }
    }

    void checkNextId(DbEntity entity) throws CayenneException {
        if (entity == null) {
            throw new CayenneException("Null DbEntity, can't create id.");
        }

        this.readingIds = true;
        this.nextDataRow = null;

        while (wrappedIterator.hasNextRow()) {
            Map next = wrappedIterator.nextObjectId(entity);

            if (fetchedIds.add(next)) {
                this.nextDataRow = next;
                break;
            }
        }
    }
}