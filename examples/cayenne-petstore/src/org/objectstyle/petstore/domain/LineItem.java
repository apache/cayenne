package org.objectstyle.petstore.domain;

import java.math.BigDecimal;

import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.petstore.domain.auto._LineItem;

public class LineItem extends _LineItem {

    /**
     * Decreases the inventory based on the number of ordered items.
     */
    public void updateInventoryQuantity() {
        int orderedQty = getQuantity().intValue();
        int inventory = getItem().getInventory().getQuantity().intValue();
        getItem().getInventory().setQuantity(new Integer(inventory - orderedQty));
    }
    
    public Object getItemId() {
        return DataObjectUtils.pkForObject(getItem());
    }
    
    public BigDecimal getTotal() {
        int quantity = getQuantity() != null ? getQuantity().intValue() : 0;
        double price = getUnitPrice() != null ? getUnitPrice().doubleValue() : 0;
        return new BigDecimal(quantity * price);
    }
}
