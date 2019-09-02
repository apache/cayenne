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

package org.apache.cayenne.access.translator.select;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.Entity;

/**
 * @since 4.2
 */
abstract class PathProcessor<T extends Entity> implements PathTranslationResult {

    public static final char OUTER_JOIN_INDICATOR = '+';
    public static final char SPLIT_PATH_INDICATOR = '#';

    protected final Map<String, String> pathSplitAliases;
    protected final TranslatorContext context;
    protected final List<String> attributePaths;
    protected final List<DbAttribute> attributes;
    protected final StringBuilder currentDbPath;

    protected boolean lastComponent;
    private boolean isOuterJoin;
    protected T entity;
    protected DbRelationship relationship;
    protected String currentAlias;

    public PathProcessor(TranslatorContext context, T entity) {
        this.context = Objects.requireNonNull(context);
        this.entity = Objects.requireNonNull(entity);
        this.pathSplitAliases = context.getMetadata().getPathSplitAliases();
        this.currentDbPath = new StringBuilder();
        this.attributes = new ArrayList<>(1);
        this.attributePaths = new ArrayList<>(1);
    }

    public PathTranslationResult process(String path) {
        PathComponents components = new PathComponents(path);
        String[] rawComponents = components.getAll();
        for(int i=0; i<rawComponents.length; i++) {
            String next = rawComponents[i];
            isOuterJoin = false;
            lastComponent = i == rawComponents.length - 1;
            String alias = pathSplitAliases.get(next);
            if(alias != null) {
                currentAlias = next;
                processAliasedAttribute(next, alias);
                currentAlias = null;
            } else {
                if(next.charAt(next.length() - 1) == OUTER_JOIN_INDICATOR) {
                    isOuterJoin = true;
                    next = next.substring(0, next.length() - 1);
                }
                processNormalAttribute(next);
            }
        }

        return this;
    }

    protected void addAttribute(String path, DbAttribute attribute) {
        attributePaths.add(path);
        attributes.add(attribute);
    }

    abstract protected void processAliasedAttribute(String next, String alias);

    abstract protected void processNormalAttribute(String next);

    @Override
    public List<DbAttribute> getDbAttributes() {
        return attributes;
    }

    @Override
    public List<String> getAttributePaths() {
        return attributePaths;
    }

    @Override
    public Optional<DbRelationship> getDbRelationship() {
        if(relationship == null) {
            return Optional.empty();
        }
        return Optional.of(relationship);
    }

    @Override
    public Optional<Embeddable> getEmbeddable() {
        return Optional.empty();
    }

    @Override
    public String getFinalPath() {
        return currentDbPath.toString();
    }

    protected void appendCurrentPath(String nextSegment) {
        if(currentDbPath.length() > 0) {
            currentDbPath.append('.');
        }
        currentDbPath.append(nextSegment);
        if(currentAlias != null) {
            currentDbPath.append(SPLIT_PATH_INDICATOR).append(currentAlias);
        }
        if(isOuterJoin) {
            currentDbPath.append(OUTER_JOIN_INDICATOR);
        }
    }

    public boolean isOuterJoin() {
        return isOuterJoin;
    }
}
