package org.apache.cayenne.map.naming;

/**
 *
 * @since 3.1 moved from project package
 */

public interface NameChecker {


    /**
     * Returns a base default name, like "UntitledEntity", etc.
     *
     * */
    String baseName();

    /**
     * Checks if the name is already taken by another sibling in the same
     * context.
     *
     */
    boolean isNameInUse(Object namingContext, String name);


}
