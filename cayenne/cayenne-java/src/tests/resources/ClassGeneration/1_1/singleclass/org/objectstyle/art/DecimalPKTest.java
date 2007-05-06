package org.apache.art;

public class DecimalPKTest extends org.apache.cayenne.CayenneDataObject {

    public static final String DECIMAL_PK_PROPERTY = "decimalPK";
    public static final String NAME_PROPERTY = "name";

    public static final String DECIMAL_PK_PK_COLUMN = "DECIMAL_PK";

    public void setDecimalPK(java.math.BigDecimal decimalPK) {
        writeProperty("decimalPK", decimalPK);
    }
    public java.math.BigDecimal getDecimalPK() {
        return (java.math.BigDecimal)readProperty("decimalPK");
    }
    
    
    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
}



