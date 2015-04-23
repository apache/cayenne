package org.apache.cayenne.access.loader.filters;

import java.util.Arrays;

/**
 * @since 4.0.
 */
public class FiltersConfig {

    public final CatalogFilter[] catalogs;

    public FiltersConfig(CatalogFilter ... catalogs) {
        if (catalogs == null || catalogs.length == 0) {
            throw new IllegalArgumentException("catalogs(" + Arrays.toString(catalogs) + ") can't be null or empty");
        }

        this.catalogs = catalogs;
    }

    public PatternFilter proceduresFilter(String catalog, String schema) {
        return getSchemaFilter(catalog, schema).procedures;
    }

    public TableFilter tableFilter(String catalog, String schema) {
        return getSchemaFilter(catalog, schema).tables;
    }

    protected SchemaFilter getSchemaFilter(String catalog, String schema) {
        CatalogFilter catalogFilter = getCatalog(catalog);
        if (catalogFilter == null) {
            return null;
        }

        return catalogFilter.getSchema(schema);
    }

    protected CatalogFilter getCatalog(String catalog) {
        for (CatalogFilter catalogFilter : catalogs) {
            if (catalogFilter.name == null || catalogFilter.name.equals(catalog)) {
                return catalogFilter;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (CatalogFilter catalog : catalogs) {
            catalog.toString(builder, "");
        }

        return builder.toString();
    }

    public static FiltersConfig create(String catalog, String schema, TableFilter tableFilter, PatternFilter procedures) {
        return new FiltersConfig(
                    new CatalogFilter(catalog,
                        new SchemaFilter(schema, tableFilter, procedures)));
    }
}
