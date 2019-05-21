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

package org.apache.cayenne.template.parser;

import org.apache.cayenne.template.Context;
import org.apache.cayenne.template.directive.Directive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.1
 */
public class ASTDirective extends IdentifierNode {

    private static final Logger logger = LoggerFactory.getLogger(ASTDirective.class);

    public ASTDirective(int id) {
        super(id);
    }

    @Override
    public void evaluate(Context context) {
        Directive directive = context.getDirective(getIdentifier());
        if(directive == null) {
            logger.warn("Unknown directive #{}", getIdentifier());
            return;
        }

        ASTExpression[] expressions = new ASTExpression[children.length];
        for(int i=0;  i<children.length; i++) {
            expressions[i] = (ASTExpression)children[i];
        }

        directive.apply(context, expressions);
    }
}
