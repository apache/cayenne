/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cayenne.dba;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.object.model.From;
import org.apache.cayenne.query.object.model.Select;
import org.apache.cayenne.query.object.model.SelectResult;
import org.apache.commons.collections.Transformer;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Normalize means that we still use only object-query terminology but we expand all syntax shortcuts.
 * 1) Select a.* -> Select a.f1, a.f2, a.f3 ...
 * 2) add Qualifiers for all join entity
 * 3) shorten all pathes into form of 2 steps
 *      Select a.paintings.galleries.owner.name
 *        From Artist a
 *
 *      Select o.name
 *        From Artist a
 *        join a.paintings p
 *        join p.galleries g
 *        join g.owner o
 *    and this rule applicable for all pathes (in select clause and in all expressions)
 *
 * We have to repeat steps 2, 3 recursively until no changes will be detected.
 *
 * @since 4.0
 */
@NotThreadSafe
public class NormalizedObjectQueryBuilder {

    /**
     * original select list store what originally user want to extract,
     * it will be used during transformation of result row into object entities if any
     * */
    private final LinkedList<SelectResult> originalSelectList = new LinkedList<SelectResult>();
    private final LinkedList<SelectResult> normalizedSelectList = new LinkedList<SelectResult>();

    /**
     *
     * */
    private final LinkedList<From> from = new LinkedList<From>();

    /* Map<from.tableExpression(), from> */
    private final Map<String, From> expressionsToFrom = new HashMap<String, From>();

    /* Map<from.name()> */
    private final Set<String> nameToFrom = new HashSet<String>();

    /**
     *
     * */
    private final LinkedList<Expression> where = new LinkedList<Expression>();

    public void addSelectResult(From from) {
        for (ObjAttribute attr : from.entity().getAttributes()) {
            normalizedSelectList.add(new SelectResult.Attribute(from, attr));
        }

        originalSelectList.add(new SelectResult.SelectFrom(from));
    }

    @Nullable
    public From addFrom(From from, ObjRelationship rel) {
        String joinExpr = from.name() + "." + rel.getName(); // TODO duplicate functionality: expression key from From.Relation
        From existingFrom = this.expressionsToFrom.get(joinExpr);
        if (existingFrom != null) {
            return existingFrom;
        }

        String name = guessName(rel.getTargetEntityName().substring(0, 1),
                                rel.getTargetEntityName(),
                                rel.getName(),
                                from.name() + rel.getName());

        return addFrom(new From.Relation(name, from, rel));
    }

    public From addFrom(From from) {
        this.from.add(from);
        this.expressionsToFrom.put(from.tableExpression(), from);
        this.nameToFrom.add(from.name());

        //from.entity().resolvePathComponents()
        //addWhere(from.entity().getDeclaredQualifier()); // TODO how path relations resolved here? what is the root?

        return from;
    }

    protected String guessName(String ... names) {
        for (String name : names){
            if (!nameToFrom.contains(name)) {
                return name;
            }
        }

        for (int i = 0; i < 100; i++) {
            if (!nameToFrom.contains(names[0] + i)) {
                return names[0] + i;
            }
        }

        return names[0] + System.currentTimeMillis(); // TODO more attempts to guess
    }

    public Select buildSelect() {
        return new Select(normalizedSelectList, from, where);
    }

    public List<SelectResultReader> buildResultReader() {
        return null; // TODO implement
    }

    public static final Transformer pathExpander = new Transformer() {

        public Object transform(Object input) {
            if (input instanceof ASTObjPath) {
                ASTObjPath path = (ASTObjPath) input;
                String[] pathStr = ((String) path.getOperand(0)).split(".");
            }
            return input;
        }
    };
}
