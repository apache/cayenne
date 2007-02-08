package org.objectstyle.petstore.domain;

import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.petstore.domain.auto._Product;

public class Product extends _Product {

    public Object getProductId() {
        return DataObjectUtils.pkForObject(this);
    }
    
    public Object getCategoryId() {
        return DataObjectUtils.pkForObject(getCategory());
    }
}
