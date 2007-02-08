package org.objectstyle.petstore.domain;

import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.petstore.domain.auto._Category;

public class Category extends _Category {

    public Object getCategoryId() {
        return DataObjectUtils.pkForObject(this);
    }
}
