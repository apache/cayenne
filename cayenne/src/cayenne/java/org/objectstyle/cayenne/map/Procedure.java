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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.XMLEncoder;

/**
 * A mapping descriptor for a database stored procedure. 
 * 
 * @author Andrei Adamchik
 */
public class Procedure extends MapObject {
    protected String catalog;
    protected String schema;
    protected boolean returningValue;
    protected List callParameters = new ArrayList();

    /**
     * Creates an unnamed procedure object.
     */
    public Procedure() {
        super();
    }

    /**
     * Creates a named Procedure object.
     */
    public Procedure(String name) {
        super(name);
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<procedure name=\"");
        encoder.print(Util.encodeXmlAttribute(getName()));
        encoder.print('\"');

        if (getSchema() != null && getSchema().trim().length() > 0) {
            encoder.print(" schema=\"");
            encoder.print(getSchema().trim());
            encoder.print('\"');
        }

        if (getCatalog() != null && getCatalog().trim().length() > 0) {
            encoder.print(" catalog=\"");
            encoder.print(getCatalog().trim());
            encoder.print('\"');
        }

        if (isReturningValue()) {
            encoder.print(" returningValue=\"true\"");
        }

        encoder.println('>');

        encoder.indent(1);
        encoder.print(getCallParameters());
        encoder.indent(-1);
        
        encoder.println("</procedure>");
    }

    /**
     * Returns procedure name including schema, if present.
     */
    public String getFullyQualifiedName() {
        return (schema != null) ? schema + '.' + getName() : getName();
    }

    /**
     * @return parent DataMap of this entity.
     */
    public DataMap getDataMap() {
        return (DataMap) getParent();
    }

    /**
     * Sets parent DataMap of this entity.
     */
    public void setDataMap(DataMap dataMap) {
        this.setParent(dataMap);
    }

    public void setCallParameters(List parameters) {
        clearCallParameters();
        callParameters.addAll(parameters);
    }

    /**
     * Adds new call parameter to the stored procedure. Also sets
     * <code>param</code>'s parent to be this procedure.
     */
    public void addCallParameter(ProcedureParameter param) {
        if (param.getName() == null) {
            throw new IllegalArgumentException("Attempt to add unnamed parameter.");
        }

        if (callParameters.contains(param)) {
            throw new IllegalArgumentException(
                "Attempt to add the same parameter more than once:" + param);
        }

        param.setProcedure(this);
        callParameters.add(param);
    }

    /** Removes a named call parameter. */
    public void removeCallParameter(String name) {
        for (int i = 0; i < callParameters.size(); i++) {
            ProcedureParameter nextParam = (ProcedureParameter) callParameters.get(i);
            if (name.equals(nextParam.getName())) {
                callParameters.remove(i);
                break;
            }
        }
    }

    public void clearCallParameters() {
        callParameters.clear();
    }

    /**
     * Returns a list of call parameters.
     * 
     * @return List
     */
    public List getCallParameters() {
        return callParameters;
    }

    /**
     * Returns a list of OUT and INOUT call parameters. If procedure has a
     * return value, it will also be included as a call parameter.
     */
    public List getCallOutParameters() {
        List outParams = new ArrayList(callParameters.size());
        Iterator it = callParameters.iterator();
        while (it.hasNext()) {
            ProcedureParameter param = (ProcedureParameter) it.next();
            if (param.isOutParam()) {
                outParams.add(param);
            }
        }

        return outParams;
    }

    /**
     * Returns parameter describing the return value of the StoredProcedure, or
     * null if procedure does not support return values. If procedure supports return parameters,
     * its first parameter is always assumed to be a return result.
     */
    public ProcedureParameter getResultParam() {
        // if procedure returns parameters, this must be the first parameter
        // otherwise, return null
        return (returningValue && callParameters.size() > 0)
            ? (ProcedureParameter) callParameters.get(0)
            : null;
    }

    /**
     * Returns <code>true</code> if a stored procedure returns a value.
     * The first parameter in a list of parameters will be assumed to be 
     * a descriptor of return value.
     * 
     * @return boolean
     */
    public boolean isReturningValue() {
        return returningValue;
    }

    public void setReturningValue(boolean returningValue) {
        this.returningValue = returningValue;
    }

    public String getCatalog() {
        return catalog;
    }

    public String getSchema() {
        return schema;
    }

    /**
     * Sets stored procedure's catalog.
     */
    public void setCatalog(String string) {
        catalog = string;
    }

    /**
     * Sets stored procedure's database schema.
     */
    public void setSchema(String string) {
        schema = string;
    }
}
