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
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.2
 */
class QualifierTranslationStage implements TranslationStage {

    @Override
    public void perform(TranslatorContext context) {
        QualifierTranslator translator = context.getQualifierTranslator();

        Expression expression = context.getQuery().getQualifier();

        // Attaching Obj entity's qualifier
        ObjEntity entity = context.getMetadata().getObjEntity();
        if (entity != null) {
            ClassDescriptor descriptor = context.getMetadata().getClassDescriptor();
            Expression entityQualifier = descriptor.getEntityInheritanceTree().qualifierForEntityAndSubclasses();
            if (entityQualifier != null) {
                expression = expression == null ? entityQualifier : expression.andExp(entityQualifier);
            }
        }

        Node qualifierNode = translator.translate(expression);
        context.setQualifierNode(qualifierNode);
    }
}
