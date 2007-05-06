package org.objectstyle.art;

import java.math.BigDecimal;

import org.objectstyle.cayenne.CayenneDataObject;

public class DecimalPKTest extends CayenneDataObject {

    public static final String DECIMAL_PK_PROPERTY = "decimalPK";
    public static final String NAME_PROPERTY = "name";

    public static final String DECIMAL_PK_PK_COLUMN = "DECIMAL_PK";

    public void setDecimalPK(BigDecimal decimalPK) {
        writeProperty("decimalPK", decimalPK);
    }
    public BigDecimal getDecimalPK() {
        return (BigDecimal)readProperty("decimalPK");
    }
    
    
    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
}



