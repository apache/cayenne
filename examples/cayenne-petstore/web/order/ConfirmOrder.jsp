<%@ include file="../common/IncludeTop.jsp" %>


<div id="BackLink">
  <html:link page="/shop/index.shtml">Return to Main Menu</html:link>
</div>

<div id="Catalog">

  Please confirm the information below and then press continue...

  <table>
    <tr><th align="center" colspan="2">
      <font size="4"><b>Order</b></font>
      <br/><font size="3"><b><bean:write name="orderBean" property="order.orderDate"
                                         format="yyyy/MM/dd hh:mm:ss"/></b></font>
    </th></tr>

    <tr><th colspan="2">
      Billing Address
    </th></tr>
    <tr><td>
      First name:</td><td><bean:write name="orderBean" property="order.billToFirstName"/>
    </td></tr>
    <tr><td>
      Last name:</td><td><bean:write name="orderBean" property="order.billToLastName"/>
    </td></tr>
    <tr><td>
      Address 1:</td><td><bean:write name="orderBean" property="order.billAddress1"/>
    </td></tr>
    <tr><td>
      Address 2:</td><td><bean:write name="orderBean" property="order.billAddress2"/>
    </td></tr>
    <tr><td>
      City: </td><td><bean:write name="orderBean" property="order.billCity"/>
    </td></tr>
    <tr><td>
      State:</td><td><bean:write name="orderBean" property="order.billState"/>
    </td></tr>
    <tr><td>
      Zip:</td><td><bean:write name="orderBean" property="order.billZip"/>
    </td></tr>
    <tr><td>
      Country: </td><td><bean:write name="orderBean" property="order.billCountry"/>
    </td></tr>
    <tr><th colspan="2">
      Shipping Address
    </th></tr><tr><td>
    First name:</td><td><bean:write name="orderBean" property="order.shipToFirstName"/>
  </td></tr>
    <tr><td>
      Last name:</td><td><bean:write name="orderBean" property="order.shipToLastName"/>
    </td></tr>
    <tr><td>
      Address 1:</td><td><bean:write name="orderBean" property="order.shipAddress1"/>
    </td></tr>
    <tr><td>
      Address 2:</td><td><bean:write name="orderBean" property="order.shipAddress2"/>
    </td></tr>
    <tr><td>
      City: </td><td><bean:write name="orderBean" property="order.shipCity"/>
    </td></tr>
    <tr><td>
      State:</td><td><bean:write name="orderBean" property="order.shipState"/>
    </td></tr>
    <tr><td>
      Zip:</td><td><bean:write name="orderBean" property="order.shipZip"/>
    </td></tr>
    <tr><td>
      Country: </td><td><bean:write name="orderBean" property="order.shipCountry"/>
    </td></tr>

  </table>


  <html:link styleClass="Button" page="/shop/newOrder.shtml?confirmed=true">Confirm</html:link>

</div>

<%@ include file="../common/IncludeBottom.jsp" %>





