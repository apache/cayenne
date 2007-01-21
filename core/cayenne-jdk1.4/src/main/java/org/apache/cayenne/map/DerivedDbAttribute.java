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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;

/**
 * A DerivedDbAttribute is a DbAttribute that resolves to an SQL expression based on a set
 * of other attributes. DerivedDbAttribute's allow to build expressions like "
 * <code>count(id)</code>", "<code>sum(price)</code>", etc.
 * <p>
 * Internally DerivedDbAttribute is defined as a specification string and a set of
 * substitution DbAttribute parameters. Specification string is an SQL expression that
 * contains placeholders (<code>%@</code>) for attribute parameters, for example:
 * </p>
 * <p>
 * <code>sum(%@) + sum(%@)</code>
 * </p>
 * 
 * @author Andrus Adamchik
 */
public class DerivedDbAttribute extends DbAttribute {

    public static final String ATTRIBUTE_TOKEN = "%@";

    protected String expressionSpec;
    protected List params = new ArrayList();
    protected boolean groupBy;

    /**
     * Constructor for DerivedDbAttribute.
     */
    public DerivedDbAttribute() {
        super();
    }

    /**
     * Constructor for DerivedDbAttribute.
     */
    public DerivedDbAttribute(String name) {
        super(name);
    }

    /**
     * Constructor for DerivedDbAttribute.
     */
    public DerivedDbAttribute(String name, int type, DbEntity entity, String spec) {
        super(name, type, entity);
        setExpressionSpec(spec);
    }

    /**
     * Creates and initializes a derived attribute with an attribute of a parent entity.
     */
    public DerivedDbAttribute(DbEntity entity, DbAttribute parentProto) {
        setName(parentProto.getName());
        setType(parentProto.getType());
        setMandatory(parentProto.isMandatory());
        setMaxLength(parentProto.getMaxLength());
        setAttributePrecision(parentProto.getAttributePrecision());
        setScale(parentProto.getScale());
        setPrimaryKey(parentProto.isPrimaryKey());

        setExpressionSpec(ATTRIBUTE_TOKEN);
        addParam(parentProto);
        setEntity(entity);
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<db-attribute-derived name=\""
                + Util.encodeXmlAttribute(getName())
                + '\"');

        String type = TypesMapping.getSqlNameByType(getType());
        if (type != null) {
            encoder.print(" type=\"" + type + '\"');
        }

        // If attribute is part of primary key
        if (isPrimaryKey()) {
            encoder.print(" isPrimaryKey=\"true\"");
        }

        if (isMandatory())
            encoder.print(" isMandatory=\"true\"");

        if (getMaxLength() > 0) {
            encoder.print(" length=\"");
            encoder.print(getMaxLength());
            encoder.print('\"');
        }

        if (getScale() > 0) {
            encoder.print(" scale=\"");
            encoder.print(getScale());
            encoder.print('\"');
        }
        
        if (getAttributePrecision() > 0) {
            encoder.print(" attributePrecision=\"");
            encoder.print(getAttributePrecision());
            encoder.print('\"');
        }

        if (((DerivedDbEntity) getEntity()).getGroupByAttributes().contains(this)) {
            encoder.print(" isGroupBy=\"true\"");
        }

        String spec = getExpressionSpec();
        if (spec != null && spec.trim().length() > 0) {
            encoder.print(" spec=\"");
            encoder.print(spec);
            encoder.print('\"');
        }

        List params = getParams();

        if (params.size() > 0) {
            encoder.println(">");

            encoder.indent(1);

            Iterator refs = params.iterator();
            while (refs.hasNext()) {
                DbAttribute ref = (DbAttribute) refs.next();
                encoder.println("<db-attribute-ref name=\""
                        + Util.encodeXmlAttribute(ref.getName())
                        + "\"/>");
            }

            encoder.indent(-1);
            encoder.println("</db-attribute-derived>");
        }
        else {
            encoder.println("/>");
        }
    }

    public String getAliasedName(String alias) {
        if (expressionSpec == null) {
            return super.getAliasedName(alias);
        }

        int len = params.size();
        StringBuffer buf = new StringBuffer();
        int ind = 0;
        for (int i = 0; i < len; i++) {
            // no bound checking
            // expression is assumed to be valid
            int match = expressionSpec.indexOf(ATTRIBUTE_TOKEN, ind);
            DbAttribute at = (DbAttribute) params.get(i);
            if (match > i) {
                buf.append(expressionSpec.substring(ind, match));
            }
            buf.append(at.getAliasedName(alias));
            ind = match + 2;
        }

        if (ind < expressionSpec.length()) {
            buf.append(expressionSpec.substring(ind));
        }

        return buf.toString();
    }

    /**
     * Returns true if this attribute is used in GROUP BY clause of the parent entity.
     */
    public boolean isGroupBy() {
        return groupBy;
    }

    public void setGroupBy(boolean flag) {
        groupBy = flag;
    }

    /**
     * Returns the params.
     * 
     * @return List
     */
    public List getParams() {
        return Collections.unmodifiableList(params);
    }

    /**
     * Returns the expressionSpec.
     */
    public String getExpressionSpec() {
        return expressionSpec;
    }

    /**
     * Adds parameter.
     */
    public void addParam(DbAttribute param) {
        params.add(param);
    }

    public void removeParam(DbAttribute param) {
        params.remove(param);
    }

    public void clearParams() {
        params.clear();
    }

    /**
     * Sets the expressionSpec.
     */
    public void setExpressionSpec(String expressionSpec) {
        this.expressionSpec = expressionSpec;
    }
}
