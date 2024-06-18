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

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.path.CayennePathSegment;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @since 4.2
 */
abstract class PathProcessor<T extends Entity<?,?,?>> implements PathTranslationResult {

    public static final char SPLIT_PATH_INDICATOR = '#';
    public static final String DB_PATH_ALIAS_INDICATOR = "db:";

    protected final Map<String, String> pathSplitAliases;
    protected final TranslatorContext context;
    protected final List<CayennePath> attributePaths;
    protected final List<DbAttribute> attributes;

    protected CayennePath currentDbPath;
    protected boolean lastComponent;
    private boolean isOuterJoin;
    protected T entity;
    protected DbRelationship relationship;
    protected String currentAlias;

    public PathProcessor(TranslatorContext context, T entity) {
        this.context = Objects.requireNonNull(context);
        this.entity = Objects.requireNonNull(entity);
        this.pathSplitAliases = context.getMetadata().getPathSplitAliases();
        this.currentDbPath = CayennePath.EMPTY_PATH;
        this.attributes = new ArrayList<>(1);
        this.attributePaths = new ArrayList<>(1);
    }

    public PathTranslationResult process(CayennePath path) {
        if(path.marker() != CayennePath.NO_MARKER) {
            currentDbPath = currentDbPath.withMarker(path.marker());
        }
        List<CayennePathSegment> segments = path.segments();
        int size = segments.size();
        for (int i = 0; i < size; i++) {
            CayennePathSegment segment = segments.get(i);
            String next = segment.value();
            isOuterJoin = false;
            lastComponent = i == size - 1;
            String alias = pathSplitAliases.get(next);
            if (alias != null) {
                currentAlias = next;
                processAliasedAttribute(next, alias);
                currentAlias = null;
            } else {
                isOuterJoin = segment.isOuterJoin();
                processNormalAttribute(next);
            }
        }

        return this;
    }

    protected void addAttribute(CayennePath path, DbAttribute attribute) {
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
    public List<CayennePath> getAttributePaths() {
        return attributePaths;
    }

    @Override
    public Optional<DbRelationship> getDbRelationship() {
        if (relationship == null) {
            return Optional.empty();
        }
        return Optional.of(relationship);
    }

    @Override
    public CayennePath getFinalPath() {
        return currentDbPath;
    }

    protected void appendCurrentPath(String nextSegment) {
        CayennePathSegment segment = currentAlias != null
                ? CayennePath.segmentOf(SPLIT_PATH_INDICATOR + currentAlias)
                : CayennePath.segmentOf(nextSegment, isOuterJoin);
        currentDbPath = currentDbPath.dot(segment);
    }

    public boolean isOuterJoin() {
        return isOuterJoin;
    }
}
