package org.objectstyle.art;

import java.util.Date;

import org.objectstyle.cayenne.CayenneDataObject;

public class DateTest extends CayenneDataObject {

    public static final String DATE_COLUMN_PROPERTY = "dateColumn";
    public static final String TIME_COLUMN_PROPERTY = "timeColumn";
    public static final String TIMESTAMP_COLUMN_PROPERTY = "timestampColumn";

    public static final String DATE_TEST_ID_PK_COLUMN = "DATE_TEST_ID";

    public void setDateColumn(Date dateColumn) {
        writeProperty("dateColumn", dateColumn);
    }
    public Date getDateColumn() {
        return (Date)readProperty("dateColumn");
    }
    
    
    public void setTimeColumn(Date timeColumn) {
        writeProperty("timeColumn", timeColumn);
    }
    public Date getTimeColumn() {
        return (Date)readProperty("timeColumn");
    }
    
    
    public void setTimestampColumn(Date timestampColumn) {
        writeProperty("timestampColumn", timestampColumn);
    }
    public Date getTimestampColumn() {
        return (Date)readProperty("timestampColumn");
    }
    
    
}



