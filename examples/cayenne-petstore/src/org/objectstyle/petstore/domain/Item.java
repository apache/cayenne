package org.objectstyle.petstore.domain;

import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.petstore.domain.auto._Item;

public class Item extends _Item {

    public Object getItemId() {
        return DataObjectUtils.pkForObject(this);
    }

    public Object getProductId() {
        return DataObjectUtils.pkForObject(getProduct());
    }

    public Integer getQuantity() {
        return getInventory().getQuantity();
    }

    public void setQuantity(Integer quantity) {
        getInventory().setQuantity(quantity);
    }

}
