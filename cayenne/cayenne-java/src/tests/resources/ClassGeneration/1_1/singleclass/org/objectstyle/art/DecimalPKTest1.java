package org.apache.art;

public class DecimalPKTest1 extends org.apache.cayenne.CayenneDataObject {

    public static final String DECIMAL_PK_PROPERTY = "decimalPK";
    public static final String NAME_PROPERTY = "name";

    public static final String DECIMAL_PK_PK_COLUMN = "DECIMAL_PK";

    public void setDecimalPK(Double decimalPK) {
        writeProperty("decimalPK", decimalPK);
    }
    public Double getDecimalPK() {
        return (Double)readProperty("decimalPK");
    }
    
    
    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
}



