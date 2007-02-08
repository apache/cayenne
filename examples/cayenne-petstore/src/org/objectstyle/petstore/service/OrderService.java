package org.objectstyle.petstore.service;

import java.util.Iterator;

import org.objectstyle.petstore.dao.DaoManager;
import org.objectstyle.petstore.dao.OrderDao;
import org.objectstyle.petstore.dao.PersistenceManager;
import org.objectstyle.petstore.domain.Account;
import org.objectstyle.petstore.domain.Cart;
import org.objectstyle.petstore.domain.CartItem;
import org.objectstyle.petstore.domain.Order;

import com.ibatis.common.util.PaginatedList;

public class OrderService {

    private PersistenceManager persistenceManager;
    private OrderDao orderDao;

    public OrderService() {
        DaoManager daoManager = DaoManager.getManager();
        orderDao = (OrderDao) daoManager.getDao(OrderDao.class);
        persistenceManager = (PersistenceManager) daoManager
                .getDao(PersistenceManager.class);
    }

    public void insertOrder(Order order, Account account, Cart cart) {
        persistenceManager.persistObject(order);

        order.setAccount(account);
        Iterator i = cart.getAllCartItems();
        while (i.hasNext()) {
            CartItem cartItem = (CartItem) i.next();
            order.addLineItem(cartItem);
        }

        order.updateAllQuantities();
    }

    public void commitChanges() {
        persistenceManager.commitChanges();
    }

    public Order getOrder(int orderId) {
        return (Order) persistenceManager.findObject(Order.class, new Integer(orderId));
    }

    public PaginatedList getOrdersForAccount(Account account) {
        return orderDao.getOrdersByAccount(account);
    }
}
