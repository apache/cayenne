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

import java.io.IOException;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.2
 */
public class ASTJoinPath extends ASTPath{

    private static final long serialVersionUID = -2964659647083196005L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ASTJoinPath.class);

    private ASTDbPath pathExp;
    private String prefix;

    ASTJoinPath(int id) {
        super(id);
    }

    public ASTJoinPath(ASTDbPath pathExp, String prefix) {
        super(ExpressionParserTreeConstants.JJTOBJPATH);
        this.pathExp = pathExp;
        this.prefix = prefix;
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        return pathExp.evaluateNode(o);
    }

    @Override
    public Expression shallowCopy() {
        ASTJoinPath copy = new ASTJoinPath(id);
        copy.path = path;
        copy.setPathAliases(pathAliases);
        copy.prefix = prefix;
        return copy;
    }

    @Override
    public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {
        pathExp.appendAsEJBQL(parameterAccumulator, out, rootId);
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        pathExp.appendAsString(out);
    }

    @Override
    public int getType() {
        return Expression.JOIN_PATH;
    }

    @Override
    public int hashCode() {
        return pathExp.hashCode();
    }

    public ASTPath getPathExp() {
        return pathExp;
    }

    public String getPrefix() {
        return prefix;
    }
}
