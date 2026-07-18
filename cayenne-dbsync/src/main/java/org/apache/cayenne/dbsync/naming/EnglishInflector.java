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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A self-contained English pluralizer used to derive to-many relationship names during DB-to-Object
 * name generation. It combines a set of ordered suffix rules with tables of irregular and uncountable
 * nouns, following the well-known two-form English inflection algorithm.
 *
 * @since 5.0
 */
public final class EnglishInflector {

    // nouns whose plural form is identical to the singular (including silent-s loanwords that must be
    // shielded from the generic "-s -> -ses" rule below)
    private static final Set<String> UNCOUNTABLE = Set.of(
            "equipment", "information", "rice", "money", "species", "series",
            "fish", "sheep", "deer", "news", "police", "aircraft",
            "corps", "chassis", "debris");

    // nouns that don't follow any regular rule
    private static final Map<String, String> IRREGULAR = Map.ofEntries(
            Map.entry("person", "people"),
            Map.entry("man", "men"),
            Map.entry("woman", "women"),
            Map.entry("child", "children"),
            Map.entry("foot", "feet"),
            Map.entry("tooth", "teeth"),
            Map.entry("goose", "geese"),
            Map.entry("mouse", "mice"),
            Map.entry("ox", "oxen"),
            Map.entry("cactus", "cacti"),
            Map.entry("focus", "foci"),
            Map.entry("genus", "genera"),
            Map.entry("testis", "testes"),
            Map.entry("leaf", "leaves"),
            Map.entry("loaf", "loaves"));

    // ordered suffix rules, evaluated most-specific to most-general; first match wins
    private static final Map<Pattern, String> RULES = new LinkedHashMap<>();

    static {
        rule("(quiz)$", "$1zes");
        rule("(matr|vert|ind)(?:ix|ex)$", "$1ices");
        rule("(x|ch|ss|sh)$", "$1es");
        rule("([^aeiouy]|qu)y$", "$1ies");
        rule("(?:([^f])fe|([lr])f)$", "$1$2ves");
        // classical Latin/Greek plurals, applied only to whitelisted stems so ordinary words
        // (button, census, basis...) fall through to the regular rules below
        rule("(criteri|phenomen|prolegomen|noumen|organ|asyndet|hyperbat|perihel|aphel)on$", "$1a");
        rule("(alumn|alveol|bacill|bronch|loc|nucle|stimul|menisc)us$", "$1i");
        rule("([cx])is$", "$1es");
        rule("sis$", "ses");
        rule("([ti])um$", "$1a");
        rule("(hero|echo|veto|potato|tomato|buffalo|domino|embargo|torpedo|mosquito)$", "$1es");
        // any other singular noun ending in "-s" (campus, bias, lens...) takes "-es"; silent-s
        // loanwords are shielded via UNCOUNTABLE above
        rule("s$", "ses");
        rule("$", "s");
    }

    private static void rule(String pattern, String replacement) {
        RULES.put(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE), replacement);
    }

    private EnglishInflector() {
    }

    /**
     * Returns the English plural form of the provided noun. Never throws for any input; a {@code null}
     * or empty argument is returned unchanged.
     */
    public static String pluralOf(String noun) {
        if (noun == null || noun.isEmpty()) {
            return noun;
        }

        String lower = noun.toLowerCase();
        if (UNCOUNTABLE.contains(lower)) {
            return noun;
        }

        String irregular = IRREGULAR.get(lower);
        if (irregular != null) {
            return irregular;
        }

        for (Map.Entry<Pattern, String> e : RULES.entrySet()) {
            Matcher m = e.getKey().matcher(noun);
            if (m.find()) {
                return m.replaceFirst(e.getValue());
            }
        }

        return noun;
    }
}
