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
package org.apache.cayenne.ejbql.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLDelegatingVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.Property;

/**
 * A visitor that compiles an EJBQL expression.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class Compiler {

    private String rootId;
    private EntityResolver resolver;
    private Map descriptorsById;
    private Map incomingById;
    private Collection implicitJoins;
    private EJBQLExpressionVisitor fromItemVisitor;
    private EJBQLExpressionVisitor joinVisitor;
    private EJBQLExpressionVisitor whereClauseVisitor;
    private EJBQLExpressionVisitor rootDescriptorVisitor;

    Compiler(EntityResolver resolver) {
        this.resolver = resolver;
        this.descriptorsById = new HashMap();
        this.incomingById = new HashMap();
        this.implicitJoins = new ArrayList(2);

        this.rootDescriptorVisitor = new SelectExpressionVisitor();
        this.fromItemVisitor = new FromItemVisitor();
        this.joinVisitor = new JoinVisitor();
        this.whereClauseVisitor = new WhereClauseVisitor();
    }

    CompiledExpression compile(String source, EJBQLExpression parsed) {
        parsed.visit(new CompilationVisitor());

        CompiledExpression compiled = new CompiledExpression();
        compiled.setExpression(parsed);
        compiled.setSource(source);

        compiled.setRootId(rootId);
        compiled.setDescriptorsById(descriptorsById);
        compiled.setImplicitJoins(implicitJoins);
        compiled.setIncomingById(incomingById);

        return compiled;
    }

    class CompilationVisitor extends EJBQLDelegatingVisitor {

        CompilationVisitor() {
            super(true);
        }

        public boolean visitSelectExpression(EJBQLExpression expression) {
            updateSubtreeDelegate(rootDescriptorVisitor, expression, -1);
            return true;
        }

        public boolean visitFromItem(EJBQLFromItem expression, int finishedChildIndex) {
            updateSubtreeDelegate(fromItemVisitor, expression, finishedChildIndex);
            return true;
        }

        public boolean visitInnerFetchJoin(EJBQLJoin join, int finishedChildIndex) {
            updateSubtreeDelegate(joinVisitor, join, finishedChildIndex);
            return true;
        }

        public boolean visitInnerJoin(EJBQLJoin join, int finishedChildIndex) {
            updateSubtreeDelegate(joinVisitor, join, finishedChildIndex);
            return true;
        }

        public boolean visitOuterFetchJoin(EJBQLJoin join, int finishedChildIndex) {
            updateSubtreeDelegate(joinVisitor, join, finishedChildIndex);
            return true;
        }

        public boolean visitOuterJoin(EJBQLJoin join, int finishedChildIndex) {
            updateSubtreeDelegate(joinVisitor, join, finishedChildIndex);
            return true;
        }

        public boolean visitWhere(EJBQLExpression expression, int finishedChildIndex) {
            updateSubtreeDelegate(whereClauseVisitor, expression, finishedChildIndex);
            return true;
        }

        private void updateSubtreeDelegate(
                EJBQLExpressionVisitor delegate,
                EJBQLExpression expression,
                int finishedChildIndex) {

            if (finishedChildIndex < 0) {
                setDelegate(delegate);
            }
            else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
                setDelegate(null);
            }
        }
    }

    class FromItemVisitor extends EJBQLBaseVisitor {

        private String entityName;

        public boolean visitIdentificationVariable(EJBQLExpression expression) {
            entityName = expression.getText();
            return true;
        }

        public boolean visitIdentifier(EJBQLExpression expression) {

            // per JPA spec, 4.4.2, "Identification variables are case insensitive."
            String rootId = expression.getText();
            rootId = rootId.toLowerCase();

            // resolve class descriptor
            ClassDescriptor descriptor = resolver.getClassDescriptor(entityName);
            if (descriptor == null) {
                throw new EJBQLException("Unmapped abstract schema name: " + entityName);
            }

            ClassDescriptor old = (ClassDescriptor) descriptorsById.put(
                    rootId,
                    descriptor);
            if (old != null && old != descriptor) {
                throw new EJBQLException("Duplicate identification variable definition: "
                        + rootId
                        + ", it is already used for "
                        + old.getEntity().getName());
            }
            return true;
        }
    }

    class JoinVisitor extends EJBQLBaseVisitor {

        private String id;
        private ObjRelationship incoming;
        private ClassDescriptor descriptor;

        public boolean visitPath(EJBQLPath expression, int finishedChildIndex) {
            if (finishedChildIndex + 1 < expression.getChildrenCount()) {
                this.id = expression.getId();
                this.descriptor = (ClassDescriptor) descriptorsById.get(id);

                if (descriptor == null) {
                    throw new EJBQLException("Unmapped id variable: " + id);
                }
            }

            return true;
        }

        public boolean visitIdentificationVariable(EJBQLExpression expression) {
            Property property = descriptor.getProperty(expression.getText());
            if (property instanceof ArcProperty) {
                incoming = ((ArcProperty) property).getRelationship();
                descriptor = ((ArcProperty) property).getTargetDescriptor();
            }
            else {
                throw new EJBQLException("Incorrect relationship path: "
                        + expression.getText());
            }

            return true;
        }

        public boolean visitIdentifier(EJBQLExpression expression) {
            if (incoming != null) {

                String aliasId = expression.getText();

                // map id variable to class descriptor
                ClassDescriptor old = (ClassDescriptor) descriptorsById.put(
                        aliasId,
                        descriptor);
                if (old != null && old != descriptor) {
                    throw new EJBQLException(
                            "Duplicate identification variable definition: "
                                    + aliasId
                                    + ", it is already used for "
                                    + old.getEntity().getName());
                }

                incomingById.put(aliasId, incoming);

                id = null;
                descriptor = null;
                incoming = null;
            }

            return true;
        }
    }

    class WhereClauseVisitor extends EJBQLBaseVisitor {
        // TODO: andrus 4/9/2007 - load implicit joins
    }

    class SelectExpressionVisitor extends EJBQLBaseVisitor {

        public boolean visitIdentifier(EJBQLExpression expression) {
            rootId = expression.getText();
            return true;
        }
    }
}
