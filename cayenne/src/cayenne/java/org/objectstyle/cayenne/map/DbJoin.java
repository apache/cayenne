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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.util.XMLEncoder;
import org.objectstyle.cayenne.util.XMLSerializable;

/**
 * Defines a join between two attributes of a given relationship.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class DbJoin implements XMLSerializable {

    protected DbRelationship relationship;
    protected String sourceName;
    protected String targetName;

    protected DbJoin() {
    }

    public DbJoin(DbRelationship relationship) {
        this.relationship = relationship;
    }

    public DbJoin(DbRelationship relationship, String sourceName, String targetName) {
        this.relationship = relationship;
        this.sourceName = sourceName;
        this.targetName = targetName;
    }

    /**
     * Returns a "reverse" join. Join source relationship is not set and must be
     * initialized by the caller.
     */
    public DbJoin createReverseJoin() {
        DbJoin reverse = new DbJoin();
        reverse.setTargetName(sourceName);
        reverse.setSourceName(targetName);
        return reverse;
    }

    /**
     * Returns DbAttribute on on the left side of the join.
     */
    public DbAttribute getSource() {
        if (sourceName == null) {
            return null;
        }

        Relationship r = getNonNullRelationship();
        Entity entity = r.getSourceEntity();
        if (entity == null) {
            return null;
        }

        return (DbAttribute) entity.getAttribute(sourceName);
    }

    public DbAttribute getTarget() {
        if (targetName == null) {
            return null;
        }

        Relationship r = getNonNullRelationship();
        Entity entity = r.getTargetEntity();
        if (entity == null) {
            return null;
        }

        return (DbAttribute) entity.getAttribute(targetName);
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<db-attribute-pair");

        // sanity check
        if (getSourceName() != null) {
            encoder.print(" source=\"");
            encoder.print(getSourceName());
            encoder.print("\"");
        }

        if (getTargetName() != null) {
            encoder.print(" target=\"");
            encoder.print(getTargetName());
            encoder.print("\"");
        }

        encoder.println("/>");
    }

    public DbRelationship getRelationship() {
        return relationship;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setRelationship(DbRelationship relationship) {
        this.relationship = relationship;
    }

    public void setSourceName(String string) {
        sourceName = string;
    }

    public void setTargetName(String string) {
        targetName = string;
    }

    private final DbRelationship getNonNullRelationship() {
        if (relationship == null) {
            throw new CayenneRuntimeException("Join has no parent Relationship.");
        }

        return relationship;
    }

    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("source", getSourceName());
        builder.append("target", getTargetName());
        return builder.toString();
    }
}