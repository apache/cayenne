/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.map;

import java.io.Serializable;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.util.ToStringBuilder;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * Defines a join between two attributes of a given relationship.
 * 
 * @since 1.1
 */
public class DbJoin implements XMLSerializable, Serializable {

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

        DbRelationship r = getNonNullRelationship();
        DbEntity entity = r.getSourceEntity();
        if (entity == null) {
            return null;
        }

        return entity.getAttribute(sourceName);
    }

    public DbAttribute getTarget() {
        if (targetName == null) {
            return null;
        }

        DbRelationship r = getNonNullRelationship();
        DbEntity entity = r.getTargetEntity();
        if (entity == null) {
            return null;
        }

        return entity.getAttribute(targetName);
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

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("source", getSourceName());
        builder.append("target", getTargetName());
        return builder.toString();
    }
}
