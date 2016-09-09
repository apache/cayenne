package org.apache.cayenne.access.loader.filters;

import junit.framework.TestCase;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class FiltersConfigTest extends TestCase {

    public void testToString_01() {
        FiltersConfig config = FiltersConfig.create(null, null,
                TableFilter.everything(), PatternFilter.INCLUDE_EVERYTHING);

        assertEquals("Catalog: null\n" +
                     "  Schema: null\n" +
                     "    Tables: \n" +
                     "      Include: null Columns: ALL\n" +
                     "    Procedures: ALL\n", config.toString());
    }

    public void testToString_02() {
        FiltersConfig config = new FiltersConfig(
                new CatalogFilter("catalog_01",
                        new SchemaFilter("schema_11", TableFilter.everything(), PatternFilter.INCLUDE_EVERYTHING)),
                new CatalogFilter("catalog_02",
                        new SchemaFilter("schema_21", TableFilter.everything(), PatternFilter.INCLUDE_NOTHING),
                        new SchemaFilter("schema_22",
                                new TableFilter(
                                        includes(new IncludeTableFilter(null, PatternFilter.INCLUDE_NOTHING)),
                                        excludes("aaa")),
                                PatternFilter.INCLUDE_NOTHING),
                        new SchemaFilter("schema_23", TableFilter.include("include"), PatternFilter.INCLUDE_NOTHING)
                )
        );

        assertEquals("Catalog: catalog_01\n" +
                     "  Schema: schema_11\n" +
                     "    Tables: \n" +
                     "      Include: null Columns: ALL\n" +
                     "    Procedures: ALL\n" +
                     "Catalog: catalog_02\n" +
                     "  Schema: schema_21\n" +
                     "    Tables: \n" +
                     "      Include: null Columns: ALL\n" +
                     "    Procedures: NONE\n" +
                     "  Schema: schema_22\n" +
                     "    Tables: \n" +
                     "      Include: null Columns: NONE\n" +
                     "      aaa\n" +
                     "    Procedures: NONE\n" +
                     "  Schema: schema_23\n" +
                     "    Tables: \n" +
                     "      Include: include Columns: ALL\n" +
                     "    Procedures: NONE\n", config.toString());
    }

    private SortedSet<Pattern> excludes(String ... p) {
        SortedSet<Pattern> patterns = new TreeSet<Pattern>(PatternFilter.PATTERN_COMPARATOR);
        for (String pattern : p) {
            patterns.add(PatternFilter.pattern(pattern));
        }
        return patterns;
    }

    protected SortedSet<IncludeTableFilter> includes(IncludeTableFilter ... filters) {
        SortedSet<IncludeTableFilter> includeTableFilters = new TreeSet<IncludeTableFilter>();
        Collections.addAll(includeTableFilters, filters);

        return includeTableFilters;
    }

}