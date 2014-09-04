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
package org.apache.cayenne.util;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * @since 3.2.
 * @Immutable
 */
public class DbType implements Comparable<DbType> {

    private static final Log LOG = LogFactory.getLog(DbType.class);

    public final String jdbc;

    public final Integer length;
    public final Integer precision;
    public final Integer scale;
    public final Boolean notNull;

    public DbType(String jdbc) {
        this(jdbc, null, null, null, null);
    }

    public DbType(String jdbc, Integer length, Integer precision, Integer scale, Boolean notNull) {
        if (isBlank(jdbc)) {
            throw new IllegalArgumentException("Jdbc type can't be null");
        }
        this.jdbc = jdbc;

        this.length = getValidInt(length);
        this.precision = getValidInt(precision);
        this.scale = getValidInt(scale);
        this.notNull = notNull;
    }

    public String getJdbc() {
        return jdbc;
    }

    public Integer getLength() {
        return length;
    }

    public Integer getPrecision() {
        return precision;
    }

    public Integer getScale() {
        return scale;
    }

    public Boolean getNotNull() {
        return notNull;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        DbType rhs = (DbType) obj;
        return new EqualsBuilder()
                .append(this.jdbc, rhs.jdbc)
                .append(this.length, rhs.length)
                .append(this.precision, rhs.precision)
                .append(this.scale, rhs.scale)
                .append(this.notNull, rhs.notNull)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(jdbc)
                .append(length)
                .append(precision)
                .append(scale)
                .append(notNull)
                .toHashCode();
    }


    @Override
    public String toString() {
        String res = jdbc;

        String len = "*";
        if (isPositive(length)) {
            len = length.toString();
        }
        if (isPositive(precision)) {
            len = precision.toString();
        }

        res += " (" + len;
        if (isPositive(scale)) {
            res += ", " + scale;
        }
        res += ")";

        if (notNull != null && notNull) {
            res += " NOT NULL";
        }

        return res;
    }

    private boolean isPositive(Integer num) {
        return num != null && num > 0;
    }

    private Integer getValidInt(Integer num) {
        if (num == null || num > 0) {
            return num;
        }

        LOG.warn("Invalid int value '" + num + "'");
        return null;
    }

    /**
     * Compare by specificity the most specific DbPath should be first in ordered list
     */
    @Override
    public int compareTo(DbType dbType) {
        return new CompareToBuilder()
                .append(dbType.jdbc, jdbc)
                .append(dbType.getSpecificity(), getSpecificity())
                .append(dbType.length, length)
                .append(dbType.precision, precision)
                .append(dbType.scale, scale)
                .append(dbType.notNull, notNull)
                .toComparison();
    }

    private int getSpecificity() {
        int res = 0;
        if (isPositive(length)) {
            res += 100;
        }
        if (isPositive(precision)) {
            res += 100;
        }
        if (isPositive(scale)) {
            res += 10;
        }
        if (this.notNull != null) {
            res += 5;
        }

        return res;
    }

    public boolean isCover(DbType type) {
        return this.jdbc.equals(type.jdbc)
            && (isCover(length, type.length) || length == null && type.length == null && isCover(precision, type.precision))
            && isCover(scale, type.scale)
            && isCover(notNull, type.notNull);
    }

    private boolean isCover(Object a, Object b) {
        return a == null || a.equals(b);
    }
}
