package org.objectstyle.petstore.dao.impl;

import org.objectstyle.petstore.dao.OrderDao;
import org.objectstyle.petstore.domain.Account;

import com.ibatis.common.util.PaginatedArrayList;
import com.ibatis.common.util.PaginatedList;

public class OrderCayenneDao extends CayenneDao implements OrderDao {

    public PaginatedList getOrdersByAccount(Account account) {
        return new PaginatedArrayList(account.getOrders(), PAGE_SIZE);
    }
}
