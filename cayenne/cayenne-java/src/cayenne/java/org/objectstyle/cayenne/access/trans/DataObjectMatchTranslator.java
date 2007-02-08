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

package org.objectstyle.cayenne.access.trans;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;

/**
 * @author Andrei Adamchik
 */
public class DataObjectMatchTranslator {
    protected Map attributes;
    protected Map values;
    protected String operation;
    protected Expression expression;
    protected DbRelationship relationship;

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public void reset() {
        attributes = null;
        values = null;
        operation = null;
        expression = null;
        relationship = null;
    }

    /**
     * Initializes itself to do translation of the match ending 
     * with a DbRelationship.
     */
    public void setRelationship(DbRelationship rel) {
        this.relationship = rel;
        attributes = new HashMap(rel.getJoins().size() * 2);

        if (rel.isToMany() || !rel.isToPK()) {

            // match on target PK
            DbEntity ent = (DbEntity) rel.getTargetEntity();
            Iterator pk = ent.getPrimaryKey().iterator();

            // index by name
            while (pk.hasNext()) {
                DbAttribute pkAttr = (DbAttribute) pk.next();
                attributes.put(pkAttr.getName(), pkAttr);
            }
        } else {

            // match on this FK
            Iterator joins = rel.getJoins().iterator();
            while (joins.hasNext()) {
                DbJoin join = (DbJoin) joins.next();

                // index by target name
                attributes.put(join.getTargetName(), join.getSource());
            }
        }
    }

    public void setDataObject(DataObject obj) {
        if (obj == null) {
            values = Collections.EMPTY_MAP;
            return;
        }

        ObjectId id = obj.getObjectId();

        setObjectId(id);
    }
    
    /**
     * @since 1.2
     */
    public void setObjectId(ObjectId id) {
        if (id == null) {
            throw new CayenneRuntimeException(
                    "Null ObjectId, probably an attempt to use TRANSIENT object as a query parameter.");
        }
        else if (id.isTemporary()) {
            throw new CayenneRuntimeException(
                    "Temporary id, probably an attempt to use NEW object as a query parameter.");
        }
        else {
            values = id.getIdSnapshot();
        }
    }

    public Iterator keys() {
        if (attributes == null) {
            throw new IllegalStateException(
                "An attempt to use uninitialized DataObjectMatchTranslator: "
                    + "[attributes: null, values: "
                    + values
                    + "]");
        }

        return attributes.keySet().iterator();
    }
    
    public DbRelationship getRelationship() {
        return relationship;
    }

    public DbAttribute getAttribute(String key) {
        return (DbAttribute) attributes.get(key);
    }

    public Object getValue(String key) {
        return values.get(key);
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
