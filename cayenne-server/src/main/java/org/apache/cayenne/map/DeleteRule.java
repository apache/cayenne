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

package org.apache.cayenne.map;

/**
 * Defines constants for the possible values of ObjRelationship delete rules.
 * 
 */
public class DeleteRule {
    public static final int NO_ACTION = 0;
    private static final String NO_ACTION_NAME = "No Action";
    
    /** 
     * Remove the reference that the destination has to this source (if the 
     * inverse relationship is toOne, nullify, if toMany, remove the source 
     * object)
     */
    public static final int NULLIFY = 1;
    private static final String NULLIFY_NAME = "Nullify";

    /** Delete the destination object(s)
     */
    public static final int CASCADE = 2;
    private static final String CASCADE_NAME = "Cascade";

    /** If the relationship has any objects (toOne or toMany), deny the delete.  
     * (Destination objects would therefore have to be deleted manually first)
     */
    public static final int DENY = 3;
    private static final String DENY_NAME = "Deny";
    
    /**
     * Default delete rule for one-to-many relationships. It is used when new rels are
     * created via modeler, or when synchrozining Obj- and DbEntities
     */
    public static final int DEFAULT_DELETE_RULE_TO_MANY = DeleteRule.DENY;
    
    /**
     * Default delete rule for many-to-one relationships. It is used when new rels are
     * created via modeler, or when synchrozining Obj- and DbEntities
     */
    public static final int DEFAULT_DELETE_RULE_TO_ONE = DeleteRule.NULLIFY;

    /** 
     * Returns String label for a delete rule state. Used for save/load (xml),
     * display in modeler etc. Must remain the same, or else great care taken
     * with loading old maps.
     */
    public static String deleteRuleName(int deleteRule) {
        switch (deleteRule) {
            case DeleteRule.NULLIFY :
                return NULLIFY_NAME;
            case DeleteRule.CASCADE :
                return CASCADE_NAME;
            case DeleteRule.DENY :
                return DENY_NAME;
            default :
                return NO_ACTION_NAME;
        }
    }

    /**
     * Translates a possible delete rule name (typically returned from
     * deleteRuleName at some stage), into a deleteRule constant
     */
    public static int deleteRuleForName(String name) {
        if (DENY_NAME.equals(name)) {
            return DENY;
        } else if (CASCADE_NAME.equals(name)) {
            return CASCADE;
        } else if (NULLIFY_NAME.equals(name)) {
            return NULLIFY;
        }
        return NO_ACTION;
    }

}
