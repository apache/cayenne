package org.objectstyle.art;

public class ROArtist2 extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String SQL_DATE_OF_BIRTH_PROPERTY = "sqlDateOfBirth";
    public static final String UTIL_DATE_OF_BIRTH_PROPERTY = "utilDateOfBirth";

    public static final String ARTIST_ID_PK_COLUMN = "ARTIST_ID";

    public java.sql.Date getSqlDateOfBirth() {
        return (java.sql.Date)readProperty("sqlDateOfBirth");
    }
    
    
    public java.util.Date getUtilDateOfBirth() {
        return (java.util.Date)readProperty("utilDateOfBirth");
    }
    
    
}



