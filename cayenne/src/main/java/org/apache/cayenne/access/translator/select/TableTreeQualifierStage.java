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
import org.apache.cayenne.exp.path.CayennePath;

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

        CayennePath pathToRoot = node.getAttributePath();
        dbQualifier = dbQualifier.transform(input ->
                // here we are not only marking path as prefetch, but changing ObjPath to DB (without )
                input instanceof ASTPath
                        ? new ASTDbPath(pathToRoot.dot(((ASTPath) input).getPath()).withMarker(CayennePath.PREFETCH_MARKER))
                        : input
        );
        Node translatedQualifier = context.getQualifierTranslator().translate(dbQualifier);
        context.appendQualifierNode(translatedQualifier);
    }
}
