/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.cayenne.value;

import java.util.Objects;

import org.apache.cayenne.value.json.JsonUtils;

/**
 * A Cayenne-supported values object that holds GeoJson string.
 *
 * @since 4.2
 */
public class GeoJson {

    private final String geometry;

    public GeoJson(String geometry) {
        this.geometry = geometry;
    }

    public String getGeometry() {
        return geometry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeoJson other = (GeoJson) o;

        return JsonUtils.compare(geometry, other.geometry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(geometry);
    }

    @Override
    public String toString() {
        return "GeoJSON value: " + geometry;
    }
}
