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

package org.apache.cayenne.exp.parser;

import java.io.PrintWriter;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ASTObjPath extends ASTPath {
    private static final Log logObj = LogFactory.getLog(ASTObjPath.class);

    /**
     * Constructor used by expression parser. Do not invoke directly.
     */
    ASTObjPath(int id) {
        super(id);
    }

    public ASTObjPath() {
        super(ExpressionParserTreeConstants.JJTOBJPATH);
    }

    public ASTObjPath(Object value) {
        super(ExpressionParserTreeConstants.JJTOBJPATH);
        setPath(value);
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        return (o instanceof DataObject)
                ? ((DataObject) o).readNestedProperty(path)
                : (o instanceof Entity) ? evaluateEntityNode((Entity) o) : PropertyUtils
                        .getProperty(o, path);
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        ASTObjPath copy = new ASTObjPath(id);
        copy.path = path;
        return copy;
    }

    @Override
    public void encodeAsString(PrintWriter pw) {
        pw.print(path);
    }

    /**
     * @since 3.0
     */
    @Override
    public void encodeAsEJBQL(PrintWriter pw, String rootId) {
        pw.print(rootId);
        pw.print('.');
        pw.print(path);
    }

    @Override
    public int getType() {
        return Expression.OBJ_PATH;
    }

    void injectValue(Object source, Object value) {
        if (getPath().indexOf(ObjEntity.PATH_SEPARATOR) == -1) {
            try {
                if (source instanceof DataObject) {
                    ((DataObject) source).writeProperty(getPath(), value);
                }
                else {
                    PropertyUtils.setProperty(source, getPath(), value);
                }
            }   
            catch (CayenneRuntimeException ex) {
                logObj.warn("Failed to inject value " + value + 
                        " on path " + getPath() + " to " + source, ex);
            }
       }
    }
}
