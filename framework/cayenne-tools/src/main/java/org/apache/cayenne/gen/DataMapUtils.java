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

package org.apache.cayenne.gen;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionParameter;
import org.apache.cayenne.exp.parser.ASTList;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.PathComponent;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.QualifiedQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.NameConverter;
import org.apache.commons.collections.set.ListOrderedSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Attributes and Methods for working with Queries.
 *
 * @since 3.0
 */
public class DataMapUtils {

    Map<String, Map<String, String>> queriesMap = new HashMap<String, Map<String, String>>();

    /**
     * Return valid method name based on query name (replace all illegal characters with
     * underscore '_').
     * 
     * @param query
     * @return Method name that perform query.
     */
    public String getQueryMethodName(Query query) {
        return NameConverter.underscoredToJava(query.getName(), true);
    }

    /**
     * Get all parameter names that used in query qualifier.
     *
     * @param query
     * @return Parameter names.
     */
    public Collection getParameterNames(QualifiedQuery query) {
        
        if(query.getQualifier() == null) {
            return Collections.EMPTY_SET;
        }
        
        Map<String, String> queryParameters = queriesMap.get(query.getName());

        if (queryParameters == null) {
            queryParameters = getParameterNames(query.getQualifier(), query.getRoot());
            queriesMap.put(query.getName(), queryParameters);
        }

        return parseQualifier(query.getQualifier().toString());
    }

    public Boolean isValidParameterNames(QualifiedQuery query) {
        
        if(query.getQualifier() == null) {
            return true;
        }
        
        Map<String, String> queryParameters = queriesMap.get(query.getName());

        if (queryParameters == null) {
            try {
                queryParameters = getParameterNames(query.getQualifier(), query.getRoot());
            } catch (Exception e) {
                // if we have wrong path in queryParameters return false.
                return false;
            }
        }
        
        if(query instanceof SelectQuery) {
            for(Ordering ordering: ((SelectQuery<?>)query).getOrderings()) {
                // validate paths in ordering
                String path = ordering.getSortSpecString();
                Iterator<CayenneMapEntry> it = ((ObjEntity)query.getRoot()).resolvePathComponents(path);
                while (it.hasNext()) {
                    try {
                        it.next();
                    } catch (ExpressionException e) {
                        // if we have wrong path in orderings return false.
                        return false;
                    }
                }
            }
        }

        return true;
    }
    
    /**
     * Get list of parameter names in the same order as in qualifier.
     * 
     * @param qualifierString to be parsed
     * @return List of parameter names.
     */
    private Set parseQualifier(String qualifierString) {
        Set result = new ListOrderedSet();
        Pattern pattern = Pattern.compile("\\$[\\w]+");
        Matcher matcher = pattern.matcher(qualifierString);
        while(matcher.find()) {
            String name = matcher.group();
            result.add(NameConverter.underscoredToJava(name.substring(1), false));
        }
        
        return result;
    }

    public boolean hasParameters(QualifiedQuery query) {
        Map queryParameters = queriesMap.get(query.getName());

        if (queryParameters == null) {
            return false;
        }

        return queryParameters.keySet().size() > 0;

    }

    /**
     * Get type of parameter for given name.
     *
     * @param query
     * @param name
     * @return Parameter type.
     */
    public String getParameterType(QualifiedQuery query, String name) {
            return queriesMap.get(query.getName()).get(name);
    }

    private Map<String, String> getParameterNames(Expression expression, Object root) {
        if (expression != null) {
            Map<String, String> types = new HashMap<String, String>();
            String typeName = "";
            List<String> names = new LinkedList<String>();

            for (int i = 0; i < expression.getOperandCount(); i++) {
                Object operand = expression.getOperand(i);

                if (operand instanceof Expression) {
                    types.putAll(getParameterNames((Expression) operand, root));
                }

                if (operand instanceof ASTObjPath) {
                    PathComponent<ObjAttribute, ObjRelationship> component = ((Entity) root).lastPathComponent((ASTObjPath) operand, null);
                    ObjAttribute attribute = component.getAttribute();
                    if (attribute != null) {
                        typeName = attribute.getType();
                    } else {
                        ObjRelationship relationship = component.getRelationship();
                        if (relationship != null) {
                            typeName = ((ObjEntity) relationship.getTargetEntity()).getClassName();
                        } else {
                            typeName = "Object";
                        }
                    }
                }
                
                if (operand instanceof ASTList) {
                    Object[] values = (Object[]) ((ASTList) operand).getOperand(0);
                    for (Object value : values) {
                        if (value instanceof ExpressionParameter) {
                            names.add(((ExpressionParameter) value).getName());
                        }
                    }
                }

                if (operand instanceof ExpressionParameter) {
                    names.add(((ExpressionParameter) operand).getName());
                }

            }

            for (String name : names) {
                types.put(NameConverter.underscoredToJava(name, false), typeName);
            }

            return types;
        }
        return Collections.EMPTY_MAP;
    }
}
