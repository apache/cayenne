package org.apache.cayenne.access.loader;

/**
 * @since 3.2.
 */
public class BooleanNameFilter implements NameFilter {
    private final boolean isInclude;

    public BooleanNameFilter(boolean isInclude) {
        this.isInclude = isInclude;
    }

    @Override
    public boolean isIncluded(String string) {
        return this.isInclude;
    }
}
