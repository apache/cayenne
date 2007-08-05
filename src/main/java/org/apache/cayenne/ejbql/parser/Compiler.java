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
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.SQLResultSetMapping;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.Property;

/**
 * Produces an {@link EJBQLCompiledExpression} out of an EJBQL expression tree.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class Compiler {

    // a flag indicating whether column expressions should be treated as result columns or
    // not.
    private boolean appendingResultColumns;

    private String rootId;
    private EntityResolver resolver;
    private Map descriptorsById;
    private Map incomingById;
    private Collection paths;
    private EJBQLExpressionVisitor fromItemVisitor;
    private EJBQLExpressionVisitor joinVisitor;
    private EJBQLExpressionVisitor pathVisitor;
    private EJBQLExpressionVisitor rootDescriptorVisitor;
    private SQLResultSetMapping resultSetMapping;

    Compiler(EntityResolver resolver) {
        this.resolver = resolver;
        this.descriptorsById = new HashMap();
        this.incomingById = new HashMap();

        this.rootDescriptorVisitor = new SelectExpressionVisitor();
        this.fromItemVisitor = new FromItemVisitor();
        this.joinVisitor = new JoinVisitor();
        this.pathVisitor = new PathVisitor();
    }

    CompiledExpression compile(String source, EJBQLExpression parsed) {
        parsed.visit(new CompilationVisitor());

        // postprocess paths, now that all id vars are resolved
        if (paths != null) {
            Iterator it = paths.iterator();
            while (it.hasNext()) {
                EJBQLPath path = (EJBQLPath) it.next();
                String id = normalizeIdPath(path.getId());

                ClassDescriptor descriptor = (ClassDescriptor) descriptorsById.get(id);
                if (descriptor == null) {
                    throw new EJBQLException("Unmapped id variable: " + id);
                }

                StringBuffer buffer = new StringBuffer(id);

                for (int i = 1; i < path.getChildrenCount(); i++) {

                    String pathChunk = path.getChild(i).getText();
                    buffer.append('.').append(pathChunk);

                    Property property = descriptor.getProperty(pathChunk);
                    if (property instanceof ArcProperty) {
                        ObjRelationship incoming = ((ArcProperty) property)
                                .getRelationship();
                        descriptor = ((ArcProperty) property).getTargetDescriptor();
                        String pathString = buffer.substring(0, buffer.length());

                        descriptorsById.put(pathString, descriptor);
                        incomingById.put(pathString, incoming);
                    }
                }
            }
        }

        CompiledExpression compiled = new CompiledExpression();
        compiled.setExpression(parsed);
        compiled.setSource(source);

        compiled.setRootId(rootId);
        compiled.setDescriptorsById(descriptorsById);
        compiled.setIncomingById(incomingById);
        compiled.setResultSetMapping(resultSetMapping);

        return compiled;
    }

    private void addPath(EJBQLExpression path) {
        if (paths == null) {
            paths = new ArrayList();
        }

        paths.add(path);
    }

    static String normalizeIdPath(String idPath) {

        // per JPA spec, 4.4.2, "Identification variables are case insensitive."

        int pathSeparator = idPath.indexOf('.');
        return pathSeparator < 0 ? idPath.toLowerCase() : idPath.substring(
                0,
                pathSeparator).toLowerCase()
                + idPath.substring(pathSeparator);
    }

    class CompilationVisitor extends EJBQLBaseVisitor {

        public boolean visitSelect(EJBQLExpression expression) {
            appendingResultColumns = true;
            return true;
        }

        public boolean visitFrom(EJBQLExpression expression, int finishedChildIndex) {
            appendingResultColumns = false;
            return true;
        }

        public boolean visitSelectExpression(EJBQLExpression expression) {
            expression.visit(rootDescriptorVisitor);
            return false;
        }

        public boolean visitFromItem(EJBQLFromItem expression) {
            expression.visit(fromItemVisitor);
            return false;
        }

        public boolean visitInnerFetchJoin(EJBQLJoin join) {
            join.visit(joinVisitor);
            return false;
        }

        public boolean visitInnerJoin(EJBQLJoin join) {
            join.visit(joinVisitor);
            return false;
        }

        public boolean visitOuterFetchJoin(EJBQLJoin join) {
            join.visit(joinVisitor);
            return false;
        }

        public boolean visitOuterJoin(EJBQLJoin join) {
            join.visit(joinVisitor);
            return false;
        }

        public boolean visitWhere(EJBQLExpression expression) {
            expression.visit(pathVisitor);

            // continue with children as there may be subselects with their own id
            // variable declarations
            return true;
        }

        public boolean visitOrderBy(EJBQLExpression expression) {
            expression.visit(pathVisitor);
            return false;
        }

        public boolean visitSubselect(EJBQLExpression expression) {
            return super.visitSubselect(expression);
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
            String rootId = normalizeIdPath(expression.getText());

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

            // if root wasn't detected in the Select Clause, use the first id var as root
            if (Compiler.this.rootId == null) {
                Compiler.this.rootId = rootId;
            }

            return true;
        }
    }

    class JoinVisitor extends EJBQLBaseVisitor {

        private String id;
        private ObjRelationship incoming;
        private ClassDescriptor descriptor;

        public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {
            if (finishedChildIndex + 1 < expression.getChildrenCount()) {
                this.id = ((EJBQLPath) expression).getId();
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

    class PathVisitor extends EJBQLBaseVisitor {

        public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {
            addPath(expression);
            return false;
        }
    }

    class SelectExpressionVisitor extends EJBQLBaseVisitor {

        public boolean visitIdentifier(EJBQLExpression expression) {
            rootId = normalizeIdPath(expression.getText());
            return false;
        }

        public boolean visitAggregate(EJBQLExpression expression) {
            addResultSetColumn();
            return false;
        }

        public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {
            addPath(expression);
            addResultSetColumn();
            return false;
        }

        private void addResultSetColumn() {
            if (appendingResultColumns) {
                if (resultSetMapping == null) {
                    resultSetMapping = new SQLResultSetMapping();
                }

                String column = "sc" + resultSetMapping.getColumnResults().size();
                resultSetMapping.addColumnResult(column);
            }
        }
    }
}