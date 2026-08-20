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
package org.apache.cayenne.dbsync.naming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnglishInflectorTest {

    @Test
    public void regular() {
        assertEquals("paintings", EnglishInflector.pluralOf("painting"));
        assertEquals("artists", EnglishInflector.pluralOf("artist"));
    }

    @Test
    public void sibilantSuffixes() {
        assertEquals("boxes", EnglishInflector.pluralOf("box"));
        assertEquals("addresses", EnglishInflector.pluralOf("address"));
        assertEquals("buses", EnglishInflector.pluralOf("bus"));
        assertEquals("dishes", EnglishInflector.pluralOf("dish"));
        assertEquals("matches", EnglishInflector.pluralOf("match"));
    }

    @Test
    public void ySuffix() {
        assertEquals("categories", EnglishInflector.pluralOf("category"));
        assertEquals("days", EnglishInflector.pluralOf("day"));
    }

    @Test
    public void fSuffix() {
        assertEquals("lives", EnglishInflector.pluralOf("life"));
        assertEquals("knives", EnglishInflector.pluralOf("knife"));
        assertEquals("wolves", EnglishInflector.pluralOf("wolf"));
        assertEquals("shelves", EnglishInflector.pluralOf("shelf"));
        assertEquals("leaves", EnglishInflector.pluralOf("leaf"));
        assertEquals("loaves", EnglishInflector.pluralOf("loaf"));
    }

    @Test
    public void oSuffix() {
        assertEquals("heroes", EnglishInflector.pluralOf("hero"));
        assertEquals("potatoes", EnglishInflector.pluralOf("potato"));
        // -o words that take a plain -s must not be over-inflected
        assertEquals("photos", EnglishInflector.pluralOf("photo"));
        assertEquals("logos", EnglishInflector.pluralOf("logo"));
    }

    @Test
    public void sSuffix() {
        // a plural must never equal the singular
        assertEquals("campuses", EnglishInflector.pluralOf("campus"));
        assertEquals("biases", EnglishInflector.pluralOf("bias"));
        assertEquals("lenses", EnglishInflector.pluralOf("lens"));
        assertEquals("buses", EnglishInflector.pluralOf("bus"));
        assertEquals("statuses", EnglishInflector.pluralOf("status"));
        // silent-s loanwords stay invariant
        assertEquals("corps", EnglishInflector.pluralOf("corps"));
        assertEquals("debris", EnglishInflector.pluralOf("debris"));
    }

    @Test
    public void irregular() {
        assertEquals("people", EnglishInflector.pluralOf("person"));
        assertEquals("children", EnglishInflector.pluralOf("child"));
        assertEquals("men", EnglishInflector.pluralOf("man"));
        assertEquals("mice", EnglishInflector.pluralOf("mouse"));
    }

    @Test
    public void classicalPlurals() {
        assertEquals("criteria", EnglishInflector.pluralOf("criterion"));
        assertEquals("phenomena", EnglishInflector.pluralOf("phenomenon"));
        assertEquals("nuclei", EnglishInflector.pluralOf("nucleus"));
        assertEquals("alumni", EnglishInflector.pluralOf("alumnus"));
        assertEquals("genera", EnglishInflector.pluralOf("genus"));
        assertEquals("axes", EnglishInflector.pluralOf("axis"));
        assertEquals("data", EnglishInflector.pluralOf("datum"));
        assertEquals("indices", EnglishInflector.pluralOf("index"));
    }

    @Test
    public void classicalRulesDoNotOverreach() {
        // ordinary words that superficially resemble Latin/Greek endings must stay regular
        assertEquals("buttons", EnglishInflector.pluralOf("button"));
        assertEquals("lemons", EnglishInflector.pluralOf("lemon"));
        assertEquals("censuses", EnglishInflector.pluralOf("census"));
        assertEquals("bonuses", EnglishInflector.pluralOf("bonus"));
        assertEquals("bases", EnglishInflector.pluralOf("basis"));
    }

    @Test
    public void uncountable() {
        assertEquals("series", EnglishInflector.pluralOf("series"));
        assertEquals("fish", EnglishInflector.pluralOf("fish"));
        assertEquals("equipment", EnglishInflector.pluralOf("equipment"));
        assertEquals("stats", EnglishInflector.pluralOf("stats"));
    }

    @Test
    public void compoundNames() {
        // irregular and uncountable forms apply to the last "_"-separated word
        assertEquals("playoff_series", EnglishInflector.pluralOf("playoff_series"));
        assertEquals("team_season_stats", EnglishInflector.pluralOf("team_season_stats"));
        assertEquals("art_people", EnglishInflector.pluralOf("art_person"));
        assertEquals("order_lines", EnglishInflector.pluralOf("order_line"));
    }

    @Test
    public void nullAndEmpty() {
        assertEquals(null, EnglishInflector.pluralOf(null));
        assertEquals("", EnglishInflector.pluralOf(""));
    }
}
