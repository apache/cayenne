package org.objectstyle.art;

public class DateTest extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String DATE_COLUMN_PROPERTY = "dateColumn";
    public static final String TIME_COLUMN_PROPERTY = "timeColumn";
    public static final String TIMESTAMP_COLUMN_PROPERTY = "timestampColumn";

    public static final String DATE_TEST_ID_PK_COLUMN = "DATE_TEST_ID";

    public void setDateColumn(java.util.Date dateColumn) {
        writeProperty("dateColumn", dateColumn);
    }
    public java.util.Date getDateColumn() {
        return (java.util.Date)readProperty("dateColumn");
    }
    
    
    public void setTimeColumn(java.util.Date timeColumn) {
        writeProperty("timeColumn", timeColumn);
    }
    public java.util.Date getTimeColumn() {
        return (java.util.Date)readProperty("timeColumn");
    }
    
    
    public void setTimestampColumn(java.util.Date timestampColumn) {
        writeProperty("timestampColumn", timestampColumn);
    }
    public java.util.Date getTimestampColumn() {
        return (java.util.Date)readProperty("timestampColumn");
    }
    
    
}



