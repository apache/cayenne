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

import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTPath;

/**
 * @since 4.2
 */
class TableTreeQualifierStage implements TranslationStage {

    @Override
    public void perform(TranslatorContext context) {
        context.getTableTree().visit(node -> {
            appendQualifier(context, node, node.getEntity().getQualifier());
            appendQualifier(context, node, node.getAdditionalQualifier());
        });

        if(context.getQualifierNode() != null) {
            context.getSelectBuilder().where(context.getQualifierNode());
        }
    }

    private static void appendQualifier(TranslatorContext context, TableTreeNode node, Expression dbQualifier) {
        if (dbQualifier == null) {
            return;
        }

        String pathToRoot = node.getAttributePath().getPath();
        dbQualifier = dbQualifier.transform(input -> {
            if (input instanceof ASTPath) {
                String path = ((ASTPath) input).getPath();
                // here we are not only marking path as prefetch, but changing ObjPath to DB
                // (without conversion, as it's a convenience option to allow path without "db:" in the Modeler)
                if(!pathToRoot.isEmpty()) {
                    path = pathToRoot + '.' + path;
                }
                return new ASTDbPath(path);
            }
            return input;
        });
        Node translatedQualifier = context.getQualifierTranslator().translate(dbQualifier);
        context.appendQualifierNode(translatedQualifier);
    }
}
