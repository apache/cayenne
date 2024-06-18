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

/**
 * A Cayenne-supported value object holding a WKT geometry String. By itself it does not provide a WKT parser or
 * geometry functions. Its goal is to instruct Cayenne to read and write geometries as WKT Strings.
 *
 * @since 4.2
 */
public class Wkt {

    private final String wkt;

    public Wkt(String wkt) {
        this.wkt = wkt;
    }

    public String getWkt() {
        return wkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wkt wkt1 = (Wkt) o;
        return Objects.equals(wkt, wkt1.wkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wkt);
    }

    @Override
    public String toString() {
        return "WKT value: " + wkt;
    }
}