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
package org.apache.cayenne.ejbql.parser;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntityResult;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.SQLResult;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Produces an {@link EJBQLCompiledExpression} out of an EJBQL expression tree.
 * 
 * @since 3.0
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
    private List<Object> resultComponents;
    private PrefetchTreeNode prefetchTree = null;

    Compiler(EntityResolver resolver) {
        this.resolver = resolver;
        this.descriptorsById = new HashMap<>();
        this.incomingById = new HashMap<>();

        this.rootDescriptorVisitor = new SelectExpressionVisitor();
        this.fromItemVisitor = new FromItemVisitor();
        this.joinVisitor = new JoinVisitor();
        this.pathVisitor = new PathVisitor();
    }

    CompiledExpression compile(String source, EJBQLExpression parsed) {
        parsed.visit(new CompilationVisitor());

        Map<EJBQLPath, Integer> pathsInSelect = new HashMap<>();

        for (int i = 0; i < parsed.getChildrenCount(); i++) {
            if (parsed.getChild(i) instanceof EJBQLSelectClause) {

                EJBQLExpression parsedTemp = parsed.getChild(i);
                boolean stop = false;

                while (parsedTemp.getChildrenCount() > 0 && !stop) {
                    EJBQLExpression newParsedTemp = null;
                    for (int j = 0; j < parsedTemp.getChildrenCount(); j++) {
                        if (parsedTemp.getChild(j) instanceof EJBQLSelectExpression) {
                            for (int k = 0; k < parsedTemp
                                    .getChild(j)
                                    .getChildrenCount(); k++) {

                                if (parsedTemp.getChild(j).getChild(k) instanceof EJBQLPath) {
                                    pathsInSelect.put((EJBQLPath) parsedTemp
                                            .getChild(j)
                                            .getChild(k), j);

                                }
                            }
                        }
                        else {
                            if (parsedTemp.getChild(j).getChildrenCount() == 0) {
                                stop = true;
                            }
                            else {
                                newParsedTemp = parsedTemp.getChild(j);
                            }
                        }
                    }

                    if (!stop && newParsedTemp != null) {
                        parsedTemp = newParsedTemp;
                    }
                    else {
                        stop = true;
                    }
                }
            }
        }

        // postprocess paths, now that all id vars are resolved
        if (paths != null) {
            for (EJBQLPath path : paths) {
                String id = normalizeIdPath(path.getId());
                ClassDescriptor descriptor = descriptorsById.get(id);
                if (descriptor == null) {
                    throw new EJBQLException("Unmapped id variable: " + id);
                }

                StringBuilder buffer = new StringBuilder(id);

                ObjRelationship incoming = null;
                String pathRelationshipString = "";

                for (int i = 1; i < path.getChildrenCount(); i++) {

                    String pathChunk = path.getChild(i).getText();
                    if(pathChunk.endsWith(Entity.OUTER_JOIN_INDICATOR)) {
                    	pathChunk = pathChunk.substring(0, pathChunk.length() - 1);
                    }
                    
                    buffer.append('.').append(pathChunk);

                    PropertyDescriptor property = descriptor.getProperty(pathChunk);

                    if (property instanceof ArcProperty) {
                        incoming = ((ArcProperty) property).getRelationship();
                        descriptor = ((ArcProperty) property).getTargetDescriptor();
                        pathRelationshipString = buffer.substring(0, buffer.length());

                        descriptorsById.put(pathRelationshipString, descriptor);
                        incomingById.put(pathRelationshipString, incoming);

                    }
                }

                if (pathsInSelect.size() > 0
                        && incoming != null
                        && pathRelationshipString.length() > 0
                        && pathRelationshipString.equals(buffer.toString())) {

                    EJBQLIdentifier ident = new EJBQLIdentifier(0);
                    ident.text = pathRelationshipString;

                    Integer integer = pathsInSelect.get(path);
                    if (integer != null) {
                        resultComponents.remove(integer.intValue());
                        resultComponents.add(integer, ident);
                        rootId = pathRelationshipString;
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
        compiled.setPrefetchTree(prefetchTree);

        if (resultComponents != null) {
            SQLResult mapping = new SQLResult();

            for (int i = 0; i < resultComponents.size(); i++) {
                Object nextMapping = resultComponents.get(i);
                if (nextMapping instanceof String) {
                    mapping.addColumnResult((String) nextMapping);
                }
                else if (nextMapping instanceof EJBQLExpression) {
                    EntityResult compileEntityResult = compileEntityResult(
                            (EJBQLExpression) nextMapping,
                            i);
                    if (prefetchTree != null) {
                        for (PrefetchTreeNode prefetch : prefetchTree.getChildren()) {
                            if (((EJBQLExpression) nextMapping).getText().equals(
                                    prefetch.getEjbqlPathEntityId())) {
                                EJBQLIdentifier ident = new EJBQLIdentifier(0);
                                ident.text = prefetch.getEjbqlPathEntityId()
                                        + "."
                                        + prefetch.getPath();

                                compileEntityResult = compileEntityResultWithPrefetch(
                                        compileEntityResult,
                                        ident);

                            }
                        }
                    }
                    mapping.addEntityResult(compileEntityResult);

                }
            }

            compiled.setResult(mapping);

        }

        return compiled;
    }

    private EntityResult compileEntityResultWithPrefetch(EntityResult compiledResult, EJBQLExpression prefetchExpression) {
        String id = prefetchExpression.getText().toLowerCase();
        ClassDescriptor descriptor = descriptorsById.get(id);
        if (descriptor == null) {
            descriptor = descriptorsById.get(prefetchExpression.getText());
        }
        String prefix = prefetchExpression.getText().substring(prefetchExpression.getText().indexOf(".") + 1);
        Set<String> visited = new HashSet<>();

        PropertyVisitor visitor = new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                ObjAttribute oa = property.getAttribute();
                if (visited.add(oa.getDbAttributePath().value())) {
                    compiledResult.addObjectField(oa.getEntity().getName(), "fetch."
                            + prefix
                            + "."
                            + oa.getName(), prefix + "." + oa.getDbAttributeName());
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
                        compiledResult.addDbField("fetch." + prefix + "." + src.getName(), prefix
                                + "."
                                + src.getName());
                    }
                }

                return true;
            }
        };

        descriptor.visitAllProperties(visitor);

        // append id columns ... (some may have been appended already via relationships)
        for (String pkName : descriptor.getEntity().getPrimaryKeyNames()) {
            if (visited.add(pkName)) {
                compiledResult
                        .addDbField("fetch." + prefix + "." + pkName, prefix
                                + "."
                                + pkName);
            }
        }

        // append inheritance discriminator columns...
        for (ObjAttribute column : descriptor.getDiscriminatorColumns()) {
            String dbAttributePath = column.getDbAttributePath().value();
            if (visited.add(dbAttributePath)) {
                compiledResult.addDbField(
                        "fetch." + prefix + "." + dbAttributePath,
                        prefix + "." + dbAttributePath);
            }
        }

        return compiledResult;
    }

    private EntityResult compileEntityResult(EJBQLExpression expression, int position) {
        String id = expression.getText().toLowerCase();
        ClassDescriptor descriptor = descriptorsById.get(id);
        if (descriptor == null) {
            descriptor = descriptorsById.get(expression.getText());
        }
        if(descriptor == null) {
            throw new EJBQLException("the entity variable '" + id +"' does not refer to any entity in the FROM clause");
        }
        final EntityResult entityResult = new EntityResult(descriptor.getObjectClass());
        final String prefix = "ec" + position + "_";
        final int[] index = {
            0
        };

        final Set<String> visited = new HashSet<>();

        PropertyVisitor visitor = new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                ObjAttribute oa = property.getAttribute();
                if (visited.add(oa.getDbAttributePath().value())) {
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

        descriptor.visitAllProperties(visitor);

        // append id columns ... (some may have been appended already via relationships)
        for (String pkName : descriptor.getEntity().getPrimaryKeyNames()) {
            if (visited.add(pkName)) {
                entityResult.addDbField(pkName, prefix + index[0]++);
            }
        }

        // append inheritance discriminator columns...
        for (ObjAttribute column : descriptor.getDiscriminatorColumns()) {
            String dbAttributePath = column.getDbAttributePath().value();
            if (visited.add(dbAttributePath)) {
                entityResult.addDbField(dbAttributePath, prefix + index[0]++);
            }
        }

        return entityResult;
    }

    private void addPath(EJBQLPath path) {
        if (paths == null) {
            paths = new ArrayList<>();
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
            prepareFetchJoin(join);
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
            prepareFetchJoin(join);
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

        private void prepareFetchJoin(EJBQLJoin join) {
            if (prefetchTree == null) {
                prefetchTree = new PrefetchTreeNode();
            }
            EJBQLPath fetchJoin = (EJBQLPath) join.getChild(0);
            addPath(fetchJoin);

            PrefetchTreeNode node = prefetchTree.addPath(fetchJoin.getRelativePath());
            node.setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
            node.setPhantom(false);
            node.setEjbqlPathEntityId(fetchJoin.getChild(0).getText());
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
                this.id = normalizeIdPath(((EJBQLPath) expression).getId());
                this.descriptor = descriptorsById.get(id);

                if (descriptor == null) {
                    throw new EJBQLException("Unmapped id variable: " + id);
                }
            }

            return true;
        }

        @Override
        public boolean visitIdentificationVariable(EJBQLExpression expression) {
            PropertyDescriptor property = descriptor.getProperty(expression.getText());
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

                String aliasId = expression.getText().toLowerCase();

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
            expression.getChild(0).getChild(0).visit(pathVisitor);
            return false;
        }

        @Override
        public boolean visitDbPath(EJBQLExpression expression, int finishedChildIndex) {
            addPath((EJBQLPath) expression);
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
                if (resultComponents == null) {
                    resultComponents = new ArrayList<>();
                }

                // defer EntityResult creation until we resolve all ids...
                resultComponents.add(expression);
            }
        }

        private void addResultSetColumn() {
            if (appendingResultColumns) {
                if (resultComponents == null) {
                    resultComponents = new ArrayList<>();
                }

                String column = "sc" + resultComponents.size();
                resultComponents.add(column);
            }
        }
    }
}
