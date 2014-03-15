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
package org.apache.cayenne.access.translator.ejbql;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLParserFactory;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * Handles appending joins to the content buffer at a marked position.
 * 
 * @since 3.0
 */
public class EJBQLJoinAppender {

    protected EJBQLTranslationContext context;
    private Map<String, String> reusableJoins;

    static String makeJoinTailMarker(String id) {
        return "FROM_TAIL" + id;
    }

    public EJBQLJoinAppender(EJBQLTranslationContext context) {
        this.context = context;
    }

    /**
     * Registers a "reusable" join, returning a preexisting ID if the join is already
     * registered. Reusable joins are the implicit inner joins that are added as a result
     * of processing of path expressions in SELECT or WHERE clauses. Note that if an
     * implicit INNER join overlaps with an explicit INNER join, both joins are added to
     * the query.
     */
    public String registerReusableJoin(
            String sourceIdPath,
            String relationship,
            String targetId) {

        if (reusableJoins == null) {
            reusableJoins = new HashMap<String, String>();
        }

        String key = sourceIdPath + ":" + relationship;

        String oldId = reusableJoins.put(key, targetId);
        if (oldId != null) {
            // revert back to old id
            reusableJoins.put(key, oldId);
            return oldId;
        }

        return null;
    }

    public void appendInnerJoin(String marker, EJBQLTableId lhsId, EJBQLTableId rhsId) {
        appendJoin(marker, lhsId, rhsId, "INNER JOIN");
    }

    public void appendOuterJoin(String marker, EJBQLTableId lhsId, EJBQLTableId rhsId) {
        appendJoin(marker, lhsId, rhsId, "LEFT OUTER JOIN");
    }

    protected void appendJoin(
            String marker,
            EJBQLTableId lhsId,
            EJBQLTableId rhsId,
            String semantics) {

        List<DbRelationship> joinRelationships = context.getIncomingRelationships(rhsId);
        if (joinRelationships.isEmpty()) {
            throw new EJBQLException("No join configured for id " + rhsId);
        }

        QuotingStrategy quoter = context.getQuotingStrategy();
        DbRelationship incomingDB = joinRelationships.get(0);

        // TODO: andrus, 1/6/2008 - move reusable join check here...

        Entity sourceEntity = incomingDB.getSourceEntity();
        String tableName;

        if (sourceEntity instanceof DbEntity) {
            tableName = quoter.quotedFullyQualifiedName((DbEntity) sourceEntity);
        }
        else {
            tableName = sourceEntity.getName();
        }

        String sourceAlias = context.getTableAlias(lhsId.getEntityId(), tableName);

        if (marker != null) {
            context.pushMarker(marker, false);
        }

        try {

            context.append(" ").append(semantics);
            String targetAlias = "";
            if (joinRelationships.size() > 1) {
                // if size of relationship list greater than 1,
                // it's a flattened relationship
                context.append(" ");

                for (int i = 1; i < joinRelationships.size(); i++) {
                    DbRelationship dbRelationship = joinRelationships.get(i);

                    String subquerySourceTableName = quoter.quotedFullyQualifiedName((DbEntity) dbRelationship
                            .getSourceEntity());
                    String subquerySourceAlias = context.getTableAlias(
                            subquerySourceTableName,
                            subquerySourceTableName);

                    String subqueryTargetTableName = quoter.quotedFullyQualifiedName(
                            (DbEntity) dbRelationship.getTargetEntity());
                    
                    String subqueryTargetAlias = "";
                    if(i==joinRelationships.size()-1){
                        // it's the last table alias
                        subqueryTargetAlias = context.getTableAlias(rhsId.getEntityId(), subqueryTargetTableName);
                    } else {
                        subqueryTargetAlias = context.getTableAlias(
                                subqueryTargetTableName,
                                subqueryTargetTableName);
                    }
                    if (i == 1) {
                        // first apply the joins defined in query
                        context.append(subquerySourceTableName).append(' ').append(
                                subquerySourceAlias);

                        generateJoiningExpression(
                                incomingDB,
                                sourceAlias,
                                subquerySourceAlias);

                    }

                    context.append(" JOIN ");
                    context.append(subqueryTargetTableName).append(' ').append(
                            subqueryTargetAlias);
                    generateJoiningExpression(
                            dbRelationship,
                            subquerySourceAlias,
                            subqueryTargetAlias);
                }

            }
            else {
                // non-flattened relationship
                targetAlias = appendTable(rhsId);
                // apply the joins defined in query
                generateJoiningExpression(incomingDB, sourceAlias, targetAlias);
            }

        }
        finally {
            if (marker != null) {
                context.popMarker();
            }
        }

    }

    private void generateJoiningExpression(
            DbRelationship incomingDB,
            String sourceAlias,
            String targetAlias) {
        context.append(" ON (");
        QuotingStrategy quoter = context.getQuotingStrategy();

        Iterator<DbJoin> it = incomingDB.getJoins().iterator();
        if (it.hasNext()) {
            DbJoin dbJoin = it.next();
            context
                    .append(sourceAlias)
                    .append('.')
                    .append(quoter.quotedSourceName(dbJoin))
                    .append(" = ")
                    .append(targetAlias)
                    .append('.')
                    .append(quoter.quotedTargetName(dbJoin));
        }

        while (it.hasNext()) {
            context.append(", ");
            DbJoin dbJoin = it.next();
            context
                    .append(sourceAlias)
                    .append('.')
                    .append(quoter.quotedSourceName(dbJoin))
                    .append(" = ")
                    .append(targetAlias)
                    .append('.')
                    .append(quoter.quotedTargetName(dbJoin));
        }

        context.append(")");
    }

    public String appendTable(EJBQLTableId id) {

        DbEntity dbEntity = id.getDbEntity(context);

        String tableName = context.getQuotingStrategy().quotedFullyQualifiedName(dbEntity);

        String alias;

        if (context.isUsingAliases()) {
            // TODO: andrus 1/5/2007 - if the same table is joined more than once, this
            // will create an incorrect alias.
            alias = context.getTableAlias(id.getEntityId(), tableName);
            
            // not using "AS" to separate table name and alias name - OpenBase doesn't
            // support
            // "AS", and the rest of the databases do not care
            context.append(' ').append(tableName).append(' ').append(alias);
            
            generateJoinsForFlattenedAttributes(id, alias);
           
        }
        else {
            context.append(' ').append(tableName);
            alias = tableName;
        }

        // append inheritance qualifier...
        if (id.isPrimaryTable()) {

            Expression qualifier = context
                    .getEntityDescriptor(id.getEntityId())
                    .getEntityQualifier();

            if (qualifier != null) {

                EJBQLExpression ejbqlQualifier = ejbqlQualifierForEntityAndSubclasses(
                        qualifier,
                        id.getEntityId());

                context.pushMarker(context.makeWhereMarker(), true);
                context.append(" WHERE");
                context.popMarker();

                context.pushMarker(context.makeEntityQualifierMarker(), false);

                ejbqlQualifier.visit(context
                        .getTranslatorFactory()
                        .getConditionTranslator(context));

                context.popMarker();
            }
        }

        return alias;
    }

    /**
     * Generates Joins statements for those flattened attributes that appear after the
     * FROM clause, e.g. in WHERE, ORDER BY, etc clauses. Flattened attributes of the
     * entity from the SELECT clause are processed earlier and therefore are omitted.
     * 
     * @param id table to JOIN id
     * @param alias table alias
     */
    private void generateJoinsForFlattenedAttributes(EJBQLTableId id, String alias) {
        String entityName = context
                .getEntityDescriptor(id.getEntityId())
                .getEntity()
                .getName();
        boolean isProcessingOmitted = false;
        // if the dbPath is not null, all attributes of the entity are processed earlier
        isProcessingOmitted = id.getDbPath() != null;
        String sourceExpression = context.getCompiledExpression().getSource();

        List<Object> resultSetMapping = context.getMetadata().getResultSetMapping();
        for (Object mapping : resultSetMapping) {
            if (mapping instanceof EntityResultSegment) {
                if (entityName.equals(((EntityResultSegment) mapping)
                        .getClassDescriptor()
                        .getEntity()
                        .getName())) {
                    // if entity is included into SELECT clause, all its attributes are processed earlier
                    isProcessingOmitted = true;
                    break;
                }

            }
        }

        if (!isProcessingOmitted) {
            
            QuotingStrategy quoter = context.getQuotingStrategy();

            
            Collection<ObjAttribute> attributes = context.getEntityDescriptor(
                    id.getEntityId()).getEntity().getAttributes();
            for (ObjAttribute objAttribute : attributes) {
                if (objAttribute.isFlattened()
                        && sourceExpression.contains(id.getEntityId()
                                + "."
                                + objAttribute.getName())) {
                    // joins for attribute are generated if it is flattened and appears in original statement
                    Iterator<CayenneMapEntry> dbPathIterator = objAttribute
                            .getDbPathIterator();
                    while (dbPathIterator.hasNext()) {
                        CayenneMapEntry next = dbPathIterator.next();
                        if (next instanceof DbRelationship) {
                            DbRelationship rel = (DbRelationship) next;
                            context.append(" LEFT OUTER JOIN ");
                            String targetEntityName = quoter.quotedFullyQualifiedName((DbEntity) rel.getTargetEntity());
                            String subqueryTargetAlias = context.getTableAlias(id.getEntityId(), targetEntityName);
                            context.append(targetEntityName).append(' ').append(subqueryTargetAlias);
                            generateJoiningExpression(
                                    rel,
                                    context.getTableAlias(id.getEntityId(),
                                            quoter.quotedFullyQualifiedName((DbEntity) rel.getSourceEntity())),
                                    subqueryTargetAlias);
                        }

                    }
                }

            }
        }
    }

    private EJBQLExpression ejbqlQualifierForEntityAndSubclasses(
            Expression qualifier,
            String entityId) {

        // parser only works on full queries, so prepend a dummy query and then strip it
        // out...
        String ejbqlChunk = qualifier.toEJBQL(entityId);
        EJBQLExpression expression = EJBQLParserFactory.getParser().parse(
                "DELETE FROM DUMMY WHERE " + ejbqlChunk);

        final EJBQLExpression[] result = new EJBQLExpression[1];
        expression.visit(new EJBQLBaseVisitor() {

            @Override
            public boolean visitWhere(EJBQLExpression expression) {
                result[0] = expression.getChild(0);
                return false;
            }
        });

        return result[0];
    }
}
