package org.objectstyle.petstore.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.struts.beanaction.ActionContext;
import org.objectstyle.petstore.domain.Order;
import org.objectstyle.petstore.service.OrderService;

import com.ibatis.common.util.PaginatedList;

public class OrderBean extends AbstractBean {

    private static final List CARD_TYPE_LIST;

    private OrderService orderService;

    private Order order;
    private int orderId;
    private boolean shippingAddressRequired;
    private boolean confirmed;
    private PaginatedList orderList;
    private String pageDirection;

    static {
        List cardList = new ArrayList();
        cardList.add("Visa");
        cardList.add("MasterCard");
        cardList.add("American Express");
        CARD_TYPE_LIST = Collections.unmodifiableList(cardList);
    }

    public OrderBean() {
        this(new OrderService());
    }

    public OrderBean(OrderService orderService) {
        order = new Order();
        shippingAddressRequired = false;
        confirmed = false;
        this.orderService = orderService;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public boolean isShippingAddressRequired() {
        return shippingAddressRequired;
    }

    public void setShippingAddressRequired(boolean shippingAddressRequired) {
        this.shippingAddressRequired = shippingAddressRequired;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public List getCreditCardTypes() {
        return CARD_TYPE_LIST;
    }

    public String getPageDirection() {
        return pageDirection;
    }

    public void setPageDirection(String pageDirection) {
        this.pageDirection = pageDirection;
    }

    public PaginatedList getOrderList() {
        return orderList;
    }

    public String listOrders() {
        Map sessionMap = ActionContext.getActionContext().getSessionMap();
        AccountBean accountBean = (AccountBean) sessionMap.get("accountBean");

        if (accountBean == null) {
            orderList = null;
            setMessage("You mist sign in to view orders.");
            return FAILURE;
        }

        orderList = orderService.getOrdersForAccount(accountBean.getAccount());
        return SUCCESS;
    }

    public String switchOrderPage() {
        if ("next".equals(pageDirection)) {
            orderList.nextPage();
        }
        else if ("previous".equals(pageDirection)) {
            orderList.previousPage();
        }
        return SUCCESS;
    }

    public String newOrderForm() {
        Map sessionMap = ActionContext.getActionContext().getSessionMap();
        AccountBean accountBean = (AccountBean) sessionMap.get("accountBean");
        CartBean cartBean = (CartBean) sessionMap.get("cartBean");

        clear();
        if (accountBean == null || !accountBean.isAuthenticated()) {
            setMessage("You must sign on before attempting to check out.  Please sign on and try checking out again.");
            return SIGNON;
        }
        else if (cartBean != null) {
            order.initOrder(accountBean.getAccount(), cartBean.getCart());
            return SUCCESS;
        }
        else {
            setMessage("An order could not be created because a cart could not be found.");
            return FAILURE;
        }
    }

    public String newOrder() {
        Map sessionMap = ActionContext.getActionContext().getSessionMap();

        if (shippingAddressRequired) {
            shippingAddressRequired = false;
            return SHIPPING;
        }
        else if (!isConfirmed()) {
            return CONFIRM;
        }
        else if (getOrder() != null) {

            AccountBean accountBean = (AccountBean) sessionMap.get("accountBean");
            CartBean cartBean = (CartBean) sessionMap.get("cartBean");

            orderService.insertOrder(order, accountBean.getAccount(), cartBean.getCart());
            orderService.commitChanges();

            cartBean.clear();

            setMessage("Thank you, your order has been submitted.");

            return SUCCESS;
        }
        else {
            setMessage("An error occurred processing your order (order was null).");
            return FAILURE;
        }
    }

    public String viewOrder() {
        Map sessionMap = ActionContext.getActionContext().getSessionMap();
        AccountBean accountBean = (AccountBean) sessionMap.get("accountBean");

        if (accountBean == null) {
            setMessage("You need to sign in to view your orders.");
            return FAILURE;
        }

        order = orderService.getOrder(orderId);

        if (order == null) {
            setMessage("Order not found: " + orderId);
            return FAILURE;
        }
        else if (accountBean.getAccount() == order.getAccount()) {
            return SUCCESS;
        }
        else {
            order = null;
            setMessage("You may only view your own orders.");
            return FAILURE;
        }
    }

    public void reset() {
        shippingAddressRequired = false;
    }

    public void clear() {
        order = new Order();
        orderId = 0;
        shippingAddressRequired = false;
        confirmed = false;
        orderList = null;
        pageDirection = null;
    }
}
