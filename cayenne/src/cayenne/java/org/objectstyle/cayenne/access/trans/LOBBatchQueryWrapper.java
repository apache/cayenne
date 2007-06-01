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

package org.objectstyle.cayenne.access.trans;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.query.BatchQuery;

/**
 * Helper class to extract the information from BatchQueries, essential for
 * LOB columns processing.
 * 
 * @author Andrei Adamchik
 */
public class LOBBatchQueryWrapper {
    protected BatchQuery query;

    protected List dbAttributes;

    // attribute list decoders
    protected boolean[] qualifierAttributes;
    protected boolean[] allLOBAttributes;
    protected Object[] updatedLOBAttributes;

    protected boolean hasNext;

    public LOBBatchQueryWrapper(BatchQuery query) {
        this.query = query;
        this.dbAttributes = query.getDbAttributes();

        int len = dbAttributes.size();
        this.qualifierAttributes = new boolean[len];
        this.allLOBAttributes = new boolean[len];
        this.updatedLOBAttributes = new Object[len];

        indexQualifierAttributes();
    }

    public boolean next() {
        hasNext = query.next();

        if (hasNext) {
            indexLOBAttributes();
        }

        return hasNext;
    }

    /**
      * Indexes attributes 
      */
    protected void indexQualifierAttributes() {
        int len = this.dbAttributes.size();
        for (int i = 0; i < len; i++) {
            DbAttribute attribute = (DbAttribute) this.dbAttributes.get(i);
            int type = attribute.getType();
            qualifierAttributes[i] = attribute.isPrimaryKey();
            allLOBAttributes[i] = (type == Types.BLOB || type == Types.CLOB);
        }
    }

    /**
     * Indexes attributes 
     */
    protected void indexLOBAttributes() {
        int len = updatedLOBAttributes.length;
        for (int i = 0; i < len; i++) {
            updatedLOBAttributes[i] = null;

            if (allLOBAttributes[i]) {
                // skip null and empty LOBs
                Object value = query.getObject(i);

                if (value == null) {
                    continue;
                }

                if (((DbAttribute) dbAttributes.get(i)).getType() == Types.BLOB) {
                    updatedLOBAttributes[i] = convertToBlobValue(value);
                }
                else {
                    updatedLOBAttributes[i] = convertToClobValue(value);
                }
            }
        }
    }

    /**
     * Converts value to byte[] if possible.
     */
    protected byte[] convertToBlobValue(Object value) {
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            return bytes.length == 0 ? null : bytes;
        }

        return null;
    }

    /**
     * Converts to char[] or String. Both are acceptable when writing CLOBs.
     */
    protected Object convertToClobValue(Object value) {

        if (value instanceof char[]) {
            char[] chars = (char[]) value;
            return (chars.length == 0) ? null : chars;
        }
        else {
            String strValue = value.toString();
            return (strValue.length() == 0) ? null : strValue;
        }
    }

    /**
     * Returns a list of DbAttributes used in the qualifier of the query 
     * that selects a LOB row for LOB update.
     */
    public List getDbAttributesForLOBSelectQualifier() {

        int len = qualifierAttributes.length;
        List attributes = new ArrayList(len);

        for (int i = 0; i < len; i++) {
            if (this.qualifierAttributes[i]) {
                attributes.add(this.dbAttributes.get(i));
            }
        }
        return attributes;
    }

    /**
     * Returns a list of DbAttributes that correspond to 
     * the LOB columns updated in the current row in the batch query. 
     * The list will not include LOB attributes that are null or empty.
     */
    public List getDbAttributesForUpdatedLOBColumns() {
        if (!hasNext) {
            throw new IllegalStateException("No more rows in the BatchQuery.");
        }

        int len = updatedLOBAttributes.length;
        List attributes = new ArrayList(len);

        for (int i = 0; i < len; i++) {
            if (this.updatedLOBAttributes[i] != null) {
                attributes.add(this.dbAttributes.get(i));
            }
        }
        return attributes;
    }

    public List getValuesForLOBSelectQualifier() {
        if (!hasNext) {
            throw new IllegalStateException("No more rows in the BatchQuery.");
        }

        int len = this.qualifierAttributes.length;
        List values = new ArrayList(len);
        for (int i = 0; i < len; i++) {
            if (this.qualifierAttributes[i]) {
                values.add(query.getObject(i));
            }
        }
        
        return values;
    }
    
	public List getValuesForUpdatedLOBColumns() {
		if (!hasNext) {
			throw new IllegalStateException("No more rows in the BatchQuery.");
		}

		int len = this.updatedLOBAttributes.length;
		List values = new ArrayList(len);
		for (int i = 0; i < len; i++) {
			if (this.updatedLOBAttributes[i] != null) {
				values.add(this.updatedLOBAttributes[i]);
			}
		}
        
		return values;
	}

    /**
     * Returns wrapped BatchQuery.
     */
    public BatchQuery getQuery() {
        return query;
    }
    
    public Level getLoggingLevel() {
    	return this.query.getLoggingLevel();
    }
}
