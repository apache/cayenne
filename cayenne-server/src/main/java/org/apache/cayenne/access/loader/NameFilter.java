package org.apache.cayenne.access.loader;

/**
 * @since 3.2.
 */
public interface NameFilter {

    boolean isIncluded(String string);
}
