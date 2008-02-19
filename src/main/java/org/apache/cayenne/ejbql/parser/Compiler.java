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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.EntityResult;
import org.apache.cayenne.query.SQLResultSetMapping;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.Property;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

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
    private Map<String, ClassDescriptor> descriptorsById;
    private Map<String, ObjRelationship> incomingById;
    private Collection<EJBQLPath> paths;
    private EJBQLExpressionVisitor fromItemVisitor;
    private EJBQLExpressionVisitor joinVisitor;
    private EJBQLExpressionVisitor pathVisitor;
    private EJBQLExpressionVisitor rootDescriptorVisitor;
    private List<Object> resultSetMappings;

    Compiler(EntityResolver resolver) {
        this.resolver = resolver;
        this.descriptorsById = new HashMap<String, ClassDescriptor>();
        this.incomingById = new HashMap<String, ObjRelationship>();

        this.rootDescriptorVisitor = new SelectExpressionVisitor();
        this.fromItemVisitor = new FromItemVisitor();
        this.joinVisitor = new JoinVisitor();
        this.pathVisitor = new PathVisitor();
    }

    CompiledExpression compile(String source, EJBQLExpression parsed) {
        parsed.visit(new CompilationVisitor());

        // postprocess paths, now that all id vars are resolved
        if (paths != null) {
            for (EJBQLPath path : paths) {
                String id = normalizeIdPath(path.getId());

                ClassDescriptor descriptor = descriptorsById.get(id);
                if (descriptor == null) {
                    throw new EJBQLException("Unmapped id variable: " + id);
                }

                StringBuilder buffer = new StringBuilder(id);

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

        if (resultSetMappings != null) {
            SQLResultSetMapping mapping = new SQLResultSetMapping();

            for (int i = 0; i < resultSetMappings.size(); i++) {
                Object nextMapping = resultSetMappings.get(i);
                if (nextMapping instanceof String) {
                    mapping.addColumnResult((String) nextMapping);
                }
                else if (nextMapping instanceof EJBQLExpression) {
                    mapping.addEntityResult(compileEntityResult(
                            (EJBQLExpression) nextMapping,
                            i));
                }
            }

            compiled.setResultSetMapping(mapping);
        }

        return compiled;
    }

    private EntityResult compileEntityResult(EJBQLExpression expression, int position) {
        String id = expression.getText().toLowerCase();
        ClassDescriptor descriptor = descriptorsById.get(id);
        final EntityResult entityResult = new EntityResult(descriptor.getObjectClass());
        final String prefix = "ec" + position + "_";
        final int[] index = {
            0
        };

        final Set<String> visited = new HashSet<String>();

        PropertyVisitor visitor = new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                ObjAttribute oa = property.getAttribute();
                if (visited.add(oa.getDbAttributePath())) {
                    entityResult.addObjectField(
                            oa.getEntity().getName(),
                            oa.getName(),
                            prefix + index[0]++);
                }
                return true;
            }

            public boolean visitToMany(ToManyProperty property) {
                return true;
            }

            public boolean visitToOne(ToOneProperty property) {
                ObjRelationship rel = property.getRelationship();
                DbRelationship dbRel = rel.getDbRelationships().get(0);

                for (DbJoin join : dbRel.getJoins()) {
                    DbAttribute src = join.getSource();
                    if (src.isForeignKey() && visited.add(src.getName())) {
                        entityResult.addDbField(src.getName(), prefix + index[0]++);
                    }
                }

                return true;
            }
        };

        // EJBQL queries are polymorphic by definition - there is no distinction between
        // inheritance/no-inheritance fetch
        descriptor.visitAllProperties(visitor);

        // append id columns ... (some may have been appended already via relationships)
        DbEntity table = descriptor.getEntity().getDbEntity();
        for (DbAttribute pk : table.getPrimaryKeys()) {
            if (visited.add(pk.getName())) {
                entityResult.addDbField(pk.getName(), prefix + index[0]++);
            }
        }

        // append inheritance discriminator columns...
        Iterator<DbAttribute> discriminatorColumns = descriptor.getDiscriminatorColumns();
        while (discriminatorColumns.hasNext()) {
            DbAttribute column = discriminatorColumns.next();

            if (visited.add(column.getName())) {
                entityResult.addDbField(column.getName(), prefix + index[0]++);
            }
        }

        return entityResult;
    }

    private void addPath(EJBQLPath path) {
        if (paths == null) {
            paths = new ArrayList<EJBQLPath>();
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

        @Override
        public boolean visitSelect(EJBQLExpression expression) {
            appendingResultColumns = true;
            return true;
        }

        @Override
        public boolean visitFrom(EJBQLExpression expression, int finishedChildIndex) {
            appendingResultColumns = false;
            return true;
        }

        @Override
        public boolean visitSelectExpression(EJBQLExpression expression) {
            expression.visit(rootDescriptorVisitor);
            return false;
        }

        @Override
        public boolean visitFromItem(EJBQLFromItem expression, int finishedChildIndex) {
            expression.visit(fromItemVisitor);
            return false;
        }

        @Override
        public boolean visitInnerFetchJoin(EJBQLJoin join) {
            join.visit(joinVisitor);
            return false;
        }

        @Override
        public boolean visitInnerJoin(EJBQLJoin join) {
            join.visit(joinVisitor);
            return false;
        }

        @Override
        public boolean visitOuterFetchJoin(EJBQLJoin join) {
            join.visit(joinVisitor);
            return false;
        }

        @Override
        public boolean visitOuterJoin(EJBQLJoin join) {
            join.visit(joinVisitor);
            return false;
        }

        @Override
        public boolean visitWhere(EJBQLExpression expression) {
            expression.visit(pathVisitor);

            // continue with children as there may be subselects with their own id
            // variable declarations
            return true;
        }

        @Override
        public boolean visitOrderBy(EJBQLExpression expression) {
            expression.visit(pathVisitor);
            return false;
        }

        @Override
        public boolean visitSubselect(EJBQLExpression expression) {
            return super.visitSubselect(expression);
        }
    }

    class FromItemVisitor extends EJBQLBaseVisitor {

        private String entityName;

        @Override
        public boolean visitFromItem(EJBQLFromItem expression, int finishedChildIndex) {

            if (finishedChildIndex + 1 == expression.getChildrenCount()) {

                // resolve class descriptor
                ClassDescriptor descriptor = resolver.getClassDescriptor(entityName);
                if (descriptor == null) {
                    throw new EJBQLException("Unmapped abstract schema name: "
                            + entityName);
                }

                // per JPA spec, 4.4.2, "Identification variables are case insensitive."
                String id = normalizeIdPath(expression.getId());

                ClassDescriptor old = descriptorsById.put(id, descriptor);
                if (old != null && old != descriptor) {
                    throw new EJBQLException(
                            "Duplicate identification variable definition: "
                                    + id
                                    + ", it is already used for "
                                    + old.getEntity().getName());
                }

                // if root wasn't detected in the Select Clause, use the first id var as
                // root
                if (Compiler.this.rootId == null) {
                    Compiler.this.rootId = id;
                }

                this.entityName = null;
            }

            return true;
        }

        @Override
        public boolean visitIdentificationVariable(EJBQLExpression expression) {
            entityName = expression.getText();
            return true;
        }
    }

    class JoinVisitor extends EJBQLBaseVisitor {

        private String id;
        private ObjRelationship incoming;
        private ClassDescriptor descriptor;

        @Override
        public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {
            if (finishedChildIndex + 1 < expression.getChildrenCount()) {
                this.id = ((EJBQLPath) expression).getId();
                this.descriptor = descriptorsById.get(id);

                if (descriptor == null) {
                    throw new EJBQLException("Unmapped id variable: " + id);
                }
            }

            return true;
        }

        @Override
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

        @Override
        public boolean visitIdentifier(EJBQLExpression expression) {
            if (incoming != null) {

                String aliasId = expression.getText();

                // map id variable to class descriptor
                ClassDescriptor old = descriptorsById.put(aliasId, descriptor);
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

        @Override
        public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {
            addPath((EJBQLPath) expression);
            return false;
        }
    }

    class SelectExpressionVisitor extends EJBQLBaseVisitor {

        @Override
        public boolean visitIdentifier(EJBQLExpression expression) {
            if (appendingResultColumns) {
                rootId = normalizeIdPath(expression.getText());
                addEntityResult(expression);
            }
            return false;
        }

        @Override
        public boolean visitAggregate(EJBQLExpression expression) {
            addResultSetColumn();
            return false;
        }

        @Override
        public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {
            addPath((EJBQLPath) expression);
            addResultSetColumn();
            return false;
        }

        private void addEntityResult(EJBQLExpression expression) {
            if (appendingResultColumns) {
                if (resultSetMappings == null) {
                    resultSetMappings = new ArrayList<Object>();
                }

                // defer EntityResult creation until we resolve all ids...
                resultSetMappings.add(expression);
            }
        }

        private void addResultSetColumn() {
            if (appendingResultColumns) {
                if (resultSetMappings == null) {
                    resultSetMappings = new ArrayList<Object>();
                }

                String column = "sc" + resultSetMappings.size();
                resultSetMappings.add(column);
            }
        }
    }
}