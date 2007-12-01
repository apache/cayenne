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

package org.apache.cayenne.wocompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.ExpressionParameter;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.SelectQuery;

/**
 * A descriptor of SelectQuery loaded from EOModel. It is an informal "decorator" of
 * Cayenne SelectQuery to provide access to the extra information of WebObjects
 * EOFetchSpecification.
 * 
 * @author Andrus Adamchik
 * @since 1.1
 */
public class EOQuery extends SelectQuery {

    protected Map plistMap;
    protected Map bindings;

    public EOQuery(ObjEntity root, Map plistMap) {
        super(root);
        this.plistMap = plistMap;
        initFromPlist(plistMap);
    }

    protected void initFromPlist(Map plistMap) {

        setResolvingInherited("YES".equalsIgnoreCase((String) plistMap.get("isDeep")));
        setRefreshingObjects("YES".equalsIgnoreCase((String) plistMap
                .get("refreshesRefetchedObjects")));

        setDistinct("YES".equalsIgnoreCase((String) plistMap.get("usesDistinct")));

        Object fetchLimit = plistMap.get("fetchLimit");
        if (fetchLimit != null) {
            try {
                if (fetchLimit instanceof Number) {
                    setFetchLimit(((Number) fetchLimit).intValue());
                }
                else {
                    setFetchLimit(Integer.parseInt(fetchLimit.toString()));
                }
            }
            catch (NumberFormatException nfex) {
                // ignoring...
            }
        }

        // sort orderings
        List orderings = (List) plistMap.get("sortOrderings");
        if (orderings != null && !orderings.isEmpty()) {
            Iterator it = orderings.iterator();
            while (it.hasNext()) {
                Map ordering = (Map) it.next();
                boolean asc = !"compareDescending:".equals(ordering.get("selectorName"));
                String key = (String) ordering.get("key");
                if (key != null) {
                    addOrdering(key, asc);
                }
            }
        }

        // qualifiers
        Map qualifierMap = (Map) plistMap.get("qualifier");
        if (qualifierMap != null && !qualifierMap.isEmpty()) {
            this.setQualifier(makeQualifier(qualifierMap));
        }

        // prefetches
        List prefetches = (List) plistMap.get("prefetchingRelationshipKeyPaths");
        if (prefetches != null && !prefetches.isEmpty()) {
            Iterator it = prefetches.iterator();
            while (it.hasNext()) {
                addPrefetch((String) it.next());
            }
        }

        // data rows - note that we do not support fetching individual columns in the
        // modeler...
        if(plistMap.containsKey("rawRowKeyPaths")) {
            setFetchingDataRows(true);
        }
    }

    public String getEOName() {
        if (root instanceof EOObjEntity) {
            return ((EOObjEntity) root).localQueryName(getName());
        }
        else {
            return getName();
        }
    }

    public Collection getBindingNames() {
        if (bindings == null) {
            initBindings();
        }

        return bindings.keySet();
    }

    public String bindingClass(String name) {
        if (bindings == null) {
            initBindings();
        }

        return (String) bindings.get(name);
    }

    private synchronized void initBindings() {
        if (bindings != null) {
            return;
        }

        bindings = new HashMap();

        if (!(root instanceof Entity)) {
            return;
        }

        Map qualifier = (Map) plistMap.get("qualifier");
        initBindings(bindings, (Entity) root, qualifier);
    }

    private void initBindings(Map bindings, Entity entity, Map qualifier) {
        if (qualifier == null) {
            return;
        }

        if ("EOKeyValueQualifier".equals(qualifier.get("class"))) {
            String key = (String) qualifier.get("key");
            if (key == null) {
                return;
            }

            Object value = qualifier.get("value");
            if (!(value instanceof Map)) {
                return;
            }

            Map valueMap = (Map) value;
            if (!"EOQualifierVariable".equals(valueMap.get("class"))
                    || !valueMap.containsKey("_key")) {
                return;
            }

            String name = (String) valueMap.get("_key");
            String className = null;

            // we don't know whether its obj path or db path, so the expression can blow
            // ... in fact we can't support DB Path as the key is different from external
            // name,
            // so we will use Object type for all DB path...
            try {
                Object lastObject = new ASTObjPath(key).evaluate(entity);

                if (lastObject instanceof ObjAttribute) {
                    className = ((ObjAttribute) lastObject).getType();
                }
                else if (lastObject instanceof ObjRelationship) {
                    ObjEntity target = (ObjEntity) ((ObjRelationship) lastObject)
                            .getTargetEntity();
                    if (target != null) {
                        className = target.getClassName();
                    }
                }
            }
            catch (ExpressionException ex) {
                className = "java.lang.Object";
            }

            if (className == null) {
                className = "java.lang.Object";
            }

            bindings.put(name, className);

            return;
        }

        List children = (List) qualifier.get("qualifiers");
        if (children != null) {
            Iterator it = children.iterator();
            while (it.hasNext()) {
                initBindings(bindings, entity, (Map) it.next());
            }
        }
    }

    /**
     * Creates the Expression equivalent of the EOFetchSpecification represented by the
     * Map.
     * 
     * @param qualifierMap - FetchSpecification to translate
     * @return Expression equivalent to FetchSpecification
     */
    public synchronized Expression makeQualifier(Map qualifierMap) {
        if (qualifierMap == null) {
            return null;
        }

        return EOFetchSpecificationParser.makeQualifier(
                (EOObjEntity) getRoot(),
                qualifierMap);
    }

    /**
     * EOFetchSpecificationParser parses EOFetchSpecifications from a WebObjects-style
     * EOModel. It recursively builds Cayenne Expression objects and assembles them into
     * the final aggregate Expression.
     * 
     * @author Travis Cripps
     */
    static class EOFetchSpecificationParser {

        // selector strings
        static final String IS_EQUAL_TO = "isEqualTo:";
        static final String IS_NOT_EQUAL_TO = "isNotEqualTo:";
        static final String IS_LIKE = "isLike:";
        static final String CASE_INSENSITIVE_LIKE = "isCaseInsensitiveLike:";
        static final String IS_LESS_THAN = "isLessThan:";
        static final String IS_LESS_THAN_OR_EQUAL_TO = "isLessThanOrEqualTo:";
        static final String IS_GREATER_THAN = "isGreaterThan:";
        static final String IS_GREATER_THAN_OR_EQUAL_TO = "isGreaterThanOrEqualTo:";

        private static HashMap selectorToExpressionBridge;

        /**
         * selectorToExpressionBridge is just a mapping of EOModeler's selector types to
         * Cayenne Expression types.
         * 
         * @return HashMap of Expression types, keyed by the corresponding selector name
         */
        static HashMap selectorToExpressionBridge() {
            if (null == selectorToExpressionBridge) {
                // initialize selectorToExpressionBridge
                selectorToExpressionBridge = new HashMap(8);
                selectorToExpressionBridge.put(IS_EQUAL_TO, new Integer(
                        Expression.EQUAL_TO));
                selectorToExpressionBridge.put(IS_NOT_EQUAL_TO, new Integer(
                        Expression.NOT_EQUAL_TO));
                selectorToExpressionBridge.put(IS_LIKE, new Integer(Expression.LIKE));
                selectorToExpressionBridge.put(CASE_INSENSITIVE_LIKE, new Integer(
                        Expression.LIKE_IGNORE_CASE));
                selectorToExpressionBridge.put(IS_LESS_THAN, new Integer(
                        Expression.LESS_THAN));
                selectorToExpressionBridge.put(IS_LESS_THAN_OR_EQUAL_TO, new Integer(
                        Expression.LESS_THAN_EQUAL_TO));
                selectorToExpressionBridge.put(IS_GREATER_THAN, new Integer(
                        Expression.GREATER_THAN));
                selectorToExpressionBridge.put(IS_GREATER_THAN_OR_EQUAL_TO, new Integer(
                        Expression.GREATER_THAN_EQUAL_TO));
            }
            return selectorToExpressionBridge;
        }

        /**
         * isAggregate determines whether a qualifier is "aggregate" -- has children -- or
         * "simple".
         * 
         * @param qualifier - a Map containing the qualifier settings
         * @return boolean indicating whether the qualifier is "aggregate" qualifier
         */
        static boolean isAggregate(Map qualifier) {
            boolean result = true;

            String theClass = (String) qualifier.get("class");
            if (theClass == null) {
                return false; // should maybe throw an exception?
            }
            if (theClass.equalsIgnoreCase("EOKeyValueQualifier")
                    || theClass.equalsIgnoreCase("EOKeyComparisonQualifier")) {
                result = false;
            }

            return result;
        }

        /**
         * expressionTypeForQualifier looks at a qualifier containing the EOModeler
         * FetchSpecification and returns the equivalent Cayenne Expression type for its
         * selector.
         * 
         * @param qualifierMap - a Map containing the qualifier settings to examine.
         * @return int Expression type
         */
        static int expressionTypeForQualifier(Map qualifierMap) {
            // get selector
            String selector = (String) qualifierMap.get("selectorName");
            return expressionTypeForSelector(selector);
        }

        /**
         * expressionTypeForSelector looks at a selector from an EOModeler
         * FetchSpecification and returns the equivalent Cayenne Expression type.
         * 
         * @param selector - a String containing the selector name.
         * @return int Expression type
         */
        static int expressionTypeForSelector(String selector) {
            Integer expType = (Integer) selectorToExpressionBridge().get(selector);
            return (expType != null ? expType.intValue() : -1);
        }

        /**
         * aggregateExpressionClassForQualifier looks at a qualifer and returns the
         * aggregate type: one of Expression.AND, Expression.OR, or Expression.NOT
         * 
         * @param qualifierMap - containing the qualifier to examine
         * @return int aggregate Expression type
         */
        static int aggregateExpressionClassForQualifier(Map qualifierMap) {
            String qualifierClass = (String) qualifierMap.get("class");
            if (qualifierClass != null) {
                if (qualifierClass.equalsIgnoreCase("EOAndQualifier")) {
                    return Expression.AND;
                }
                else if (qualifierClass.equalsIgnoreCase("EOOrQualifier")) {
                    return Expression.OR;
                }
                else if (qualifierClass.equalsIgnoreCase("EONotQualifier")) {
                    return Expression.NOT;
                }
            }

            return -1; // error
        }

        /**
         * makeQualifier recursively builds an Expression for each condition in the
         * qualifierMap and assembles from them the complex Expression to represent the
         * entire EOFetchSpecification.
         * 
         * @param qualifierMap - Map representation of EOFetchSpecification
         * @return Expression translation of the EOFetchSpecification
         */
        static Expression makeQualifier(EOObjEntity entity, Map qualifierMap) {
            if (isAggregate(qualifierMap)) {
                // the fetch specification has more than one qualifier
                int aggregateClass = aggregateExpressionClassForQualifier(qualifierMap); // AND,
                // OR,
                // NOT

                if (aggregateClass == Expression.NOT) {
                    // NOT qualifiers only have one child, keyed with "qualifier"
                    Map child = (Map) qualifierMap.get("qualifier");
                    // build the child expression
                    Expression childExp = makeQualifier(entity, child);

                    return childExp.notExp(); // add the "not" clause and return the
                    // result
                }
                else {
                    // AND, OR qualifiers can have multiple children, keyed with
                    // "qualifiers"
                    // get the list of children
                    List children = (List) qualifierMap.get("qualifiers");
                    if (children != null) {
                        ArrayList childExpressions = new ArrayList();
                        // build an Expression for each child
                        Iterator it = children.iterator();
                        while (it.hasNext()) {
                            Expression childExp = makeQualifier(entity, (Map) it.next());
                            childExpressions.add(childExp);
                        }
                        // join the child expressions and return the result
                        return ExpressionFactory
                                .joinExp(aggregateClass, childExpressions);
                    }
                }

            } // end if isAggregate(qualifierMap)...

            // the query has a single qualifier
            // get expression selector type
            String qualifierClass = (String) qualifierMap.get("class");

            // the key or key path we're comparing
            String key = null;
            // the key, keyPath, value, or parameterized value against which we're
            // comparing the key
            Object comparisonValue = null;

            if ("EOKeyComparisonQualifier".equals(qualifierClass)) {
                // Comparing two keys or key paths
                key = (String) qualifierMap.get("leftValue");
                comparisonValue = qualifierMap.get("rightValue");

                // FIXME: I think EOKeyComparisonQualifier sytle Expressions are not
                // supported...
                return null;
            }
            else if ("EOKeyValueQualifier".equals(qualifierClass)) {
                // Comparing key with a value or parameterized value
                key = (String) qualifierMap.get("key");
                Object value = qualifierMap.get("value");

                if (value instanceof Map) {
                    Map valueMap = (Map) value;
                    String objClass = (String) valueMap.get("class"); // can be a
                    // qualifier class
                    // or java type

                    if ("EOQualifierVariable".equals(objClass)
                            && valueMap.containsKey("_key")) {
                        // make a parameterized expression
                        String paramName = (String) valueMap.get("_key");
                        comparisonValue = new ExpressionParameter(paramName);
                    }
                    else {
                        Object queryVal = valueMap.get("value");
                        if ("NSNumber".equals(objClass)) {
                            // comparison to NSNumber -- cast
                            comparisonValue = queryVal;
                        }
                        else if ("EONull".equals(objClass)) {
                            // comparison to null
                            comparisonValue = null;
                        }
                        else { // Could there be other types? boolean, date, etc.???
                            // no cast
                            comparisonValue = queryVal;
                        }
                    }

                }
                else if (value instanceof String) {
                    // value expression
                    comparisonValue = value;
                } // end if (value instanceof Map) else...
            }

            // check whether the key is an object path; if at least one component is not,
            // switch to db path..

            Expression keyExp = Expression.fromString(key);
            try {
                entity.lastPathComponent(keyExp);
            }
            catch (ExpressionException e) {
                keyExp = entity.translateToDbPath(keyExp);
            }

            try {
                Expression exp = ExpressionFactory
                        .expressionOfType(expressionTypeForQualifier(qualifierMap));

                exp.setOperand(0, keyExp);
                exp.setOperand(1, comparisonValue);
                return exp;
            }
            catch (ExpressionException e) {
                return null;
            }
        }
    }
}
