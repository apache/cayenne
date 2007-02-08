package org.objectstyle.petstore.domain;

import java.util.Date;
import java.util.Iterator;

import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.petstore.domain.auto._Order;

public class Order extends _Order {

    /**
     * Provides quick access to order id. Returns -1 if the order is transient.
     */
    public int getOrderId() {
        return (getObjectId() != null && !getObjectId().isTemporary()) ? DataObjectUtils
                .intPKForObject(this) : -1;
    }

    public String getStatus() {

        if (getLineItems().isEmpty()) {
            return "unknown";
        }

        // TODO: this is bogus ... we need an enum of possible statuses and business logic
        // analyzing line item statuses...
        LineItem li = (LineItem) getLineItems().get(0);
        return li.getStatus() != null ? li.getStatus().getStatus() : "unknown";
    }

    public void initOrder(Account account, Cart cart) {

        setOrderDate(new Date());
        setShipToFirstName(account.getFirstName());
        setShipToLastName(account.getLastName());
        setShipAddress1(account.getAddress1());
        setShipAddress2(account.getAddress2());
        setShipCity(account.getCity());
        setShipState(account.getState());
        setShipZip(account.getZip());
        setShipCountry(account.getCountry());

        setBillToFirstName(account.getFirstName());
        setBillToLastName(account.getLastName());
        setBillAddress1(account.getAddress1());
        setBillAddress2(account.getAddress2());
        setBillCity(account.getCity());
        setBillState(account.getState());
        setBillZip(account.getZip());
        setBillCountry(account.getCountry());

        setTotalPrice(cart.getSubTotal());

        setCreditCard("999 9999 9999 9999");
        setExpiryDate("12/03");
        setCardType("Visa");
        setCourier("UPS");
        setLocale("CA");
    }

    public void addLineItem(CartItem item) {
        LineItem lineItem = (LineItem) getDataContext().createAndRegisterNewObject(
                LineItem.class);

        lineItem.setUnitPrice(item.getItem().getListPrice());
        lineItem.setItem(item.getItem());
        lineItem.setOrder(this);
        lineItem.setQuantity(new Integer(item.getQuantity()));

        OrderStatus status = (OrderStatus) getDataContext().createAndRegisterNewObject(
                OrderStatus.class);
        status.setStatus("P");
        status.setTimestamp(this.getOrderDate());
        lineItem.setStatus(status);
    }

    public void updateAllQuantities() {
        Iterator it = getLineItems().iterator();
        while (it.hasNext()) {
            LineItem lineItem = (LineItem) it.next();
            lineItem.updateInventoryQuantity();
        }
    }
}
