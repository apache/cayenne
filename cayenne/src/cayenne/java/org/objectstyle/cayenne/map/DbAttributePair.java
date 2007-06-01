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

import org.objectstyle.cayenne.util.XMLEncoder;
import org.objectstyle.cayenne.util.XMLSerializable;

/** 
 * A DbAttributePair represents a join between two database tables. A PK/FK
 * relationship consists of one or more joins. Correspinding Cayenne descriptor
 * object, DbRelationship, contains one or more DbAtributePairs.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 * 
 * @deprecated Since 1.1 {@link DbJoin} is used.
 */
public class DbAttributePair implements XMLSerializable {
    protected DbAttribute source;
    protected DbAttribute target;

    public DbAttributePair() {
    }

    public DbAttributePair(DbAttribute sourceAttribute, DbAttribute targetAttribute) {
        this.setSource(sourceAttribute);
        this.setTarget(targetAttribute);
    }

    /**
     * Used as DbJoin converter in deprecated methods.
     * 
     * @since 1.1
     */
    DbJoin toDbJoin(DbRelationship relationship) {
        DbJoin join = new DbJoin(relationship);
        join.setSourceName(source != null ? source.getName() : null);
        join.setTargetName(target != null ? target.getName() : null);
        return join;
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<db-attribute-pair");

        // sanity check
        if (getSource() != null) {
            encoder.print(" source=\"");
            encoder.print(getSource().getName());
            encoder.print("\"");
        }

        if (getTarget() != null) {
            encoder.print(" target=\"");
            encoder.print(getTarget().getName());
            encoder.print("\"");
        }

        encoder.println("/>");
    }

    /**
     * Creates and returns a new join going in reverse direction.
     * 
     * @since 1.0.5
     */
    public DbAttributePair createReverseJoin() {
        return new DbAttributePair(target, source);
    }

    /** Returns DbAttribute on on the left side of the join. */
    public DbAttribute getSource() {
        return source;
    }

    /** Set DbAttribute name on on the left side of the join. */
    public void setSource(DbAttribute sourceAttribute) {
        this.source = sourceAttribute;
    }

    /** Returns DbAttribute on on the right side of the join. */
    public DbAttribute getTarget() {
        return target;
    }

    /** Set DbAttribute name on on the right side of the join. */
    public void setTarget(DbAttribute targetAttribute) {
        this.target = targetAttribute;
    }

    public int hashCode() {
        return super.hashCode() + source.hashCode() + target.hashCode();
    }

    /**
     * Returns <code>true</code> if this join and 
     * object parameter both represent joins between
     * the same DbAttributes.
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o.getClass() != DbAttributePair.class) {
            return false;
        }

        if (o == this) {
            return true;
        }

        DbAttributePair j = (DbAttributePair) o;
        return j.source == this.source && j.target == this.target;
    }
}
