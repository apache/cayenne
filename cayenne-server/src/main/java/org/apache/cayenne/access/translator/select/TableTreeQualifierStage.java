/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.access.sqlbuilder.NodeBuilder;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTPath;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.exp;
import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.node;

/**
 * @since 4.2
 */
class TableTreeQualifierStage implements TranslationStage {

    @Override
    public void perform(TranslatorContext context) {
        context.getTableTree().visit(node -> {
            Expression dbQualifier = node.getEntity().getQualifier();
            if (dbQualifier != null) {
                String pathToRoot = node.getAttributePath().getPath();
                dbQualifier = dbQualifier.transform(input -> {
                    if (input instanceof ASTPath) {
                        String path = ((ASTPath) input).getPath();
                        if(!pathToRoot.isEmpty()) {
                            path = pathToRoot + '.' + path;
                        }
                        return new ASTDbPath(path);
                    }
                    return input;
                });
                Node rootQualifier = context.getQualifierNode();
                Node translatedQualifier = context.getQualifierTranslator().translate(dbQualifier);
                if (rootQualifier != null) {
                    NodeBuilder expressionNodeBuilder = exp(node(rootQualifier)).and(node(translatedQualifier));
                    context.setQualifierNode(expressionNodeBuilder.build());
                } else {
                    context.setQualifierNode(translatedQualifier);
                }
            }
        });

        if(context.getQualifierNode() != null) {
            context.getSelectBuilder().where(context.getQualifierNode());
        }
    }
}
