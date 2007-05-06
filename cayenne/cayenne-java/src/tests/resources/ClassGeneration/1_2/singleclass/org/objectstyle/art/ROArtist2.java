package org.objectstyle.art;

import java.sql.Date;

import org.objectstyle.cayenne.CayenneDataObject;

public class ROArtist2 extends CayenneDataObject {

    public static final String SQL_DATE_OF_BIRTH_PROPERTY = "sqlDateOfBirth";
    public static final String UTIL_DATE_OF_BIRTH_PROPERTY = "utilDateOfBirth";

    public static final String ARTIST_ID_PK_COLUMN = "ARTIST_ID";

    public Date getSqlDateOfBirth() {
        return (Date)readProperty("sqlDateOfBirth");
    }
    
    
    public java.util.Date getUtilDateOfBirth() {
        return (java.util.Date)readProperty("utilDateOfBirth");
    }
    
    
}



