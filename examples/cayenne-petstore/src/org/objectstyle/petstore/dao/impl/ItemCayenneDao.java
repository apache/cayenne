package org.objectstyle.petstore.dao.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.objectstyle.petstore.dao.ItemDao;
import org.objectstyle.petstore.domain.Product;

import com.ibatis.common.util.PaginatedArrayList;
import com.ibatis.common.util.PaginatedList;

public class ItemCayenneDao extends CayenneDao implements ItemDao {

    public boolean isItemInStock(String itemId) {
        Map params = Collections.singletonMap("itemId", itemId);
        List results = getDataContext()
                .performQuery("getInventoryQuantity", params, true);

        if (results.size() == 0) {
            return false;
        }

        Map row = (Map) results.get(0);
        Number quantity = (Number) row.get("quantity");
        return quantity != null && quantity.intValue() > 0;
    }

    public PaginatedList getItemListByProduct(Product product) {
        return new PaginatedArrayList(product.getItems(), PAGE_SIZE);
    }
}
