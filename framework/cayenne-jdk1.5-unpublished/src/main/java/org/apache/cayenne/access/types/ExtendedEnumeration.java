package org.apache.cayenne.access.types;

public interface ExtendedEnumeration
{
    // Return the value to be stored in the database for this enumeration.  In
    // actuallity, this should be an Integer or a String.
    public Object getDatabaseValue();
}
