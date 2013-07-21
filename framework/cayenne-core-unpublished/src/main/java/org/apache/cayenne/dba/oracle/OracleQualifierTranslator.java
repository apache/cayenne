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
package org.apache.cayenne.dba.oracle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.trans.TrimmingQualifierTranslator;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTIn;
import org.apache.cayenne.exp.parser.ASTList;
import org.apache.cayenne.exp.parser.ASTNegate;
import org.apache.cayenne.exp.parser.ASTNotIn;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.commons.collections.Transformer;

/**
 * Oracle qualifier translator. In particular, trims INs with more than 1000 elements to
 * an OR-set of INs with <= 1000 elements
 */
public class OracleQualifierTranslator extends TrimmingQualifierTranslator {

    public OracleQualifierTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler, OracleAdapter.TRIM_FUNCTION);
    }

    @Override
    protected void doAppendPart(Expression rootNode) throws IOException {
        if (rootNode == null) {
            return;
        }
        rootNode = rootNode.transform(new INTrimmer());
        rootNode.traverse(this);
    }

    public static class INTrimmer implements Transformer {

        public Expression trimmedInExpression(Expression exp, int maxInSize) {
            Expression list = (Expression) exp.getOperand(1);
            Object[] objects = (Object[]) list.evaluate(null);

            if (objects.length <= maxInSize) {
                return exp;
            }

            Expression trimmed = trimmedInExpression(
                    (ASTPath) exp.getOperand(0),
                    objects,
                    maxInSize);
            if (exp instanceof ASTNotIn) {
                return new ASTNegate(trimmed);
            }
            return trimmed;
        }

        Expression trimmedInExpression(ASTPath path, Object[] values, int maxInSize) {
            Expression res = null;

            List<Object> in = new ArrayList<Object>(maxInSize);
            for (Object v : values) {
                in.add(v);
                if (in.size() == maxInSize) {
                    Expression inExp = new ASTIn(path, new ASTList(in));
                    res = res != null ? res.orExp(inExp) : inExp;
                    in = new ArrayList<Object>(maxInSize);
                }
            }
            if (in.size() > 0) {
                Expression inExp = new ASTIn(path, new ASTList(in));
                res = res != null ? res.orExp(inExp) : inExp;
            }
            return res;
        }

        public Object transform(Object input) {
            if (input instanceof ASTIn || input instanceof ASTNotIn) {
                return trimmedInExpression((Expression) input, 1000);
            }
            return input;
        }
    }
}
