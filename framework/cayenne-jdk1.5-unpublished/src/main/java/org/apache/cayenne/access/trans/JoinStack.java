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
package org.apache.cayenne.access.trans;

import java.io.IOException;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;

/**
 * Encapsulates join reuse/split logic used in SelectQuery processing. All expression
 * path's that exist in the query (in the qualifier, etc.) are processed to produce a
 * combined join tree.
 * 
 * @since 3.0
 */
public class JoinStack {

    protected JoinTreeNode rootNode;
    protected JoinTreeNode topNode;    
    private String identifiersStartQuote;
    private String identifiersEndQuote;

    private int aliasCounter;
    
    /**
     * @deprecated since 3.0
     */   
    protected JoinStack() {
        this.rootNode = new JoinTreeNode(this);
        this.rootNode.setTargetTableAlias(newAlias());
        
        resetStack();
    }
    
    protected JoinStack(DbAdapter dbAdapter, DataMap dataMap) {
        this.rootNode = new JoinTreeNode(this);
        this.rootNode.setTargetTableAlias(newAlias());
        
        if(dataMap.isQuotingSQLIdentifiers()){
            this.identifiersStartQuote = dbAdapter.getIdentifiersStartQuote();
            this.identifiersEndQuote = dbAdapter.getIdentifiersEndQuote();
        } else {
            this.identifiersStartQuote = "";
            this.identifiersEndQuote = "";
        }
        resetStack();
    }
    
    
    String getCurrentAlias() {
        return topNode.getTargetTableAlias();
    }

    /**
     * Returns the number of configured joins.
     */
    protected int size() {
        // do not count root as a join
        return rootNode.size() - 1;
    }

    void appendRoot(Appendable out, DbEntity rootEntity) throws IOException {
        out.append(rootEntity.getFullyQualifiedName());
        out.append(' ').append(rootNode.getTargetTableAlias());
    }
    
    void appendRootWithQuoteSqlIdentifiers(Appendable out, DbEntity rootEntity) throws IOException {
        if(rootEntity.getSchema() != null) { 
            nameWithQuoteSqlIdentifiers(rootEntity.getSchema(),out);
            out.append(".");
        }
        nameWithQuoteSqlIdentifiers(rootEntity.getName(),out);
        out.append(' ');
        nameWithQuoteSqlIdentifiers(rootNode.getTargetTableAlias(),out);       
    }

    /**
     * Appends all configured joins to the provided output object.
     */
    protected void appendJoins(Appendable out) throws IOException {

        // skip root, recursively append its children
        for (JoinTreeNode child : rootNode.getChildren()) {
            appendJoinSubtree(out, child);
        }
    }

    protected void appendJoinSubtree(Appendable out, JoinTreeNode node) throws IOException {

        DbRelationship relationship = node.getRelationship();

        DbEntity targetEntity = (DbEntity) relationship.getTargetEntity();
        String srcAlias = node.getSourceTableAlias();
        String targetAlias = node.getTargetTableAlias();

        switch (node.getJoinType()) {
            case INNER:
                out.append(" JOIN");
                break;
            case LEFT_OUTER:
                out.append(" LEFT JOIN");
                break;
            default:
                throw new IllegalArgumentException("Unsupported join type: "
                        + node.getJoinType());
        }

        out.append(' ');
        
        if(targetEntity.getSchema()!= null){
            
            nameWithQuoteSqlIdentifiers(targetEntity.getSchema(),out);
            out.append(".");
        }
        nameWithQuoteSqlIdentifiers(targetEntity.getName(), out);
 
        out.append(' ');
        nameWithQuoteSqlIdentifiers(targetAlias, out);
        out.append(" ON (");

        List<DbJoin> joins = relationship.getJoins();
        int len = joins.size();
        for (int i = 0; i < len; i++) {
            DbJoin join = joins.get(i);
            if (i > 0) {
                out.append(" AND ");
            }            
            
            nameWithQuoteSqlIdentifiers(srcAlias, out);
            out.append('.');
            nameWithQuoteSqlIdentifiers(join.getSourceName(), out);
            out.append(" = ");
            nameWithQuoteSqlIdentifiers(targetAlias, out);
            out.append('.');
            nameWithQuoteSqlIdentifiers(join.getTargetName(), out);
        }

        out.append(')');

        for (JoinTreeNode child : node.getChildren()) {
            appendJoinSubtree(out, child);
        }
    }
    
    /**
     * Append join information to the qualifier - the part after "WHERE".
     */
    protected void appendQualifier(Appendable out, boolean firstQualifyerElement)
            throws IOException {
        // nothing as standard join is performed before "WHERE"
    }

    /**
     * Pops the stack all the way to the root node.
     */
    void resetStack() {
        topNode = rootNode;
    }

    /**
     * Finds or creates a JoinTreeNode for the given arguments and sets it as the next
     * current join.
     */
    void pushJoin(DbRelationship relationship, JoinType joinType, String alias) {
        topNode = topNode.findOrCreateChild(relationship, joinType, alias);
    }

    protected String newAlias() {
        return "t" + aliasCounter++;
    }
    
    protected void nameWithQuoteSqlIdentifiers(String name, Appendable out) throws IOException{
        out.append(identifiersStartQuote).append(name).append(identifiersEndQuote);        
    }  
}
