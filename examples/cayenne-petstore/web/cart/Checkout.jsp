<%@ include file="../common/IncludeTop.jsp" %>

<bean:define id="cart" name="cartBean" property="cart"/>

<div id="BackLink">
  <html:link page="/shop/viewCart.shtml">Return to Shopping Cart</html:link>
</div>

<div id="Catalog">

  <table>
    <tr>
      <td>
        <h2>Checkout Summary</h2>

        <table>

          <tr>
            <td><b>Item ID</b></td>  <td><b>Product ID</b></td>  <td><b>Description</b></td> <td><b>In Stock?</b></td>
            <td><b>Quantity</b></td>  <td><b>List Price</b></td> <td><b>Total Cost</b></td>
          </tr>

          <logic:iterate id="cartItem" name="cart" property="cartItems">
            <tr>
              <td>
                <html:link paramId="itemId" paramName="cartItem" paramProperty="item.itemId"
                           page="/shop/viewItem.shtml">
                  <bean:write name="cartItem" property="item.itemId"/></html:link></td>
              <td><bean:write name="cartItem" property="item.productId"/></td>
              <td>
                <bean:write name="cartItem" property="item.attribute1"/>
                <bean:write name="cartItem" property="item.attribute2"/>
                <bean:write name="cartItem" property="item.attribute3"/>
                <bean:write name="cartItem" property="item.attribute4"/>
                <bean:write name="cartItem" property="item.attribute5"/>
                <bean:write name="cartItem" property="item.product.name"/>
              </td>
              <td><bean:write name="cartItem" property="inStock"/></td>
              <td>
                <bean:write name="cartItem" property="quantity"/>
              </td>
              <td><bean:write name="cartItem" property="item.listPrice" format="$#,##0.00"/></td>
              <td><bean:write name="cartItem" property="total" format="$#,##0.00"/></td>
            </tr>
          </logic:iterate>
          <tr>
            <td colspan="7">
              Sub Total: <bean:write name="cart" property="subTotal" format="$#,##0.00"/>
            </td>
          </tr>
        </table>
        <logic:notEqual name="cart" property="cartItemList.firstPage" value="true">
          <a href="switchCartPage.shtml?pageDirection=previous">&lt;&lt; Previous</a>
        </logic:notEqual>
        <logic:notEqual name="cart" property="cartItemList.lastPage" value="true">
          <a href="switchCartPage.shtml?pageDirection=next">Next &gt;&gt;</a>
        </logic:notEqual>
        <html:link styleClass="Button" page="/shop/newOrderForm.shtml">Continue</html:link>
      </td>
      <td>
        &nbsp;
      </td>

    </tr>
  </table>

</div>

<%@ include file="../common/IncludeBottom.jsp" %>





