package org.objectstyle.petstore.dao;

import org.objectstyle.petstore.domain.Account;

import com.ibatis.common.util.PaginatedList;

public interface OrderDao {

    PaginatedList getOrdersByAccount(Account account);
}
