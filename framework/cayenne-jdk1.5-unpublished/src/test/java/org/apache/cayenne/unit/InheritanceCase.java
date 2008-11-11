package org.apache.cayenne.unit;

/**
 * A superclass of test cases using "inheritance" DataMap for its access stack.
 * 
 */
public abstract class InheritanceCase extends CayenneCase {
    public static final String INHERITANCE_ACCESS_STACK = "InheritanceStack";

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(INHERITANCE_ACCESS_STACK);
    }
}
