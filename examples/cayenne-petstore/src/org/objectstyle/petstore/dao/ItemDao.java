package org.objectstyle.petstore.dao;

import org.objectstyle.petstore.domain.Product;

import com.ibatis.common.util.PaginatedList;

public interface ItemDao {

    /**
     * Checking whether item is in stock is done via DAO to ensure that no caching occurs.
     */
    boolean isItemInStock(String itemId);
    
    PaginatedList getItemListByProduct(Product product);
}
