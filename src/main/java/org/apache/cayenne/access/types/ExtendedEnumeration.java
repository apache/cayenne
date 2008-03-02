package org.apache.cayenne.access.types;

public interface ExtendedEnumeration
{
    /**
     * Return the value to be stored in the database for this enumeration.  In
     * actuality, this should be an Integer or a String.
     */
    public Object getDatabaseValue();
}
