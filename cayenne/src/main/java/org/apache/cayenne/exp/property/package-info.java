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

/**
 * <h3>Property API</h3>
 * <p>
 * This API allows to use type aware {@link org.apache.cayenne.exp.Expression expression} factories aka Properties.<br>
 * These properties are normally generated as static constants in model classes, but they can also be created manually by
 * {@link org.apache.cayenne.exp.property.PropertyFactory} if needed.
 * <p>
 * Typical usage in select queries:
 * <pre>{@code
 * Painting painting = ...
 * Artist artist = ObjectSelect.query(Artist.class)
 *        .where(Artist.PAINTING_ARRAY.containsValue(painting))
 *        .and(Artist.DATE_OF_BIRTH.year().gt(1950))
 *        .and(Artist.ARTIST_NAME.like("Pablo%"))
 *        .orderBy(Artist.ARTIST_NAME.asc())
 *        .prefetch(Artist.PAINTING_ARRAY.disjointById())
 *        .selectOne(context);
 * }</pre>
 * <p>
 * Currently supported Property types:
 * <ul>
 *     <li>{@link org.apache.cayenne.exp.property.NumericProperty} for all data types inherited from {@link java.lang.Number}.<br>
 *     Supports comparison and math functions (like {@link org.apache.cayenne.exp.property.NumericProperty#sqrt() sqrt()}).
 *     <br>
 *     <li>{@link org.apache.cayenne.exp.property.StringProperty} for all data types inherited from {@link java.lang.CharSequence}.<br>
 *     Supports multiple string functions ({@link org.apache.cayenne.exp.property.StringProperty#like(java.lang.String) like()},
 *     {@link org.apache.cayenne.exp.property.StringProperty#concat(java.lang.Object...) concat()}, etc.)
 *     <br>
 *     <li>{@link org.apache.cayenne.exp.property.DateProperty} for {@link java.util.Date} (and {@link java.sql} variants)
 *     and {@link java.time.LocalDate}, {@link java.time.LocalTime}, {@link java.time.LocalDateTime}.<br>
 *     Supports date functions like {@link org.apache.cayenne.exp.property.DateProperty#year() year()}.
 *     <br>
 *     <li>{@link org.apache.cayenne.exp.property.EntityProperty} for to-one relationships.<br>
 *     Supports prefetch related methods, {@link org.apache.cayenne.exp.property.RelationshipProperty#dot(org.apache.cayenne.exp.property.BaseProperty) dot()} methods, etc.
 *     <br>
 *     <li>{@link org.apache.cayenne.exp.property.ListProperty}, {@link org.apache.cayenne.exp.property.SetProperty}
 *     and {@link org.apache.cayenne.exp.property.MapProperty} are for to-many relationships.<br>
 *     In addition to to-one related methods these properties support collection comparison methods
 *     like {@link org.apache.cayenne.exp.property.ListProperty#containsValue(org.apache.cayenne.Persistent) contains()}.
 *     <br>
 *     <li>{@link org.apache.cayenne.exp.property.EmbeddableProperty} for embeddable objects
 *     <br>
 *     <li>{@link org.apache.cayenne.exp.property.NumericIdProperty} for numeric PK properties
 *     <br>
 *     <li>{@link org.apache.cayenne.exp.property.BaseIdProperty} for non-numeric PK properties
 *     <br>
 *     <li>{@link org.apache.cayenne.exp.property.BaseProperty} for all other data types, supports basic operations (equality, sorting).
 * </ul>
 *
 * @since 4.2
 */
package org.apache.cayenne.exp.property;