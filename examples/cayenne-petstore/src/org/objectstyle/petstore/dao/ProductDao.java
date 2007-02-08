package org.objectstyle.petstore.dao;

import org.objectstyle.petstore.domain.Category;

import com.ibatis.common.util.PaginatedList;

public interface ProductDao {

    PaginatedList searchProductList(String keywords);

    PaginatedList getProductListByCategory(Category category);
}
