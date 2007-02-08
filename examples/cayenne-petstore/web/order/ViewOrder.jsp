<%@ include file="../common/IncludeTop.jsp" %>

<bean:define id="order" name="orderBean" property="order"/>
<bean:define id="itemList" name="orderBean" property="order.lineItems"/>

<div id="BackLink">
  <html:link page="/shop/index.shtml">Return to Main Menu</html:link>
</div>

<div id="Catalog">

<table>
<tr><th align="center" colspan="2">
  Order #<bean:write name="order" property="orderId"/>
  <bean:write name="order" property="orderDate" format="yyyy/MM/dd hh:mm:ss"/>
</th></tr>
<tr><th colspan="2">
  Payment Details
</th></tr>
<tr><td>
  Card Type:</td><td>
  <bean:write name="order" property="cardType"/>
</td></tr>
<tr><td>
  Card Number:</td><td><bean:write name="order" property="creditCard"/> * Fake number!
</td></tr>
<tr><td>
  Expiry Date (MM/YYYY):</td><td><bean:write name="order" property="expiryDate"/>
</td></tr>
<tr><th colspan="2">
  Billing Address
</th></tr>
<tr><td>
  First name:</td><td><bean:write name="order" property="billToFirstName"/>
</td></tr>
<tr><td>
  Last name:</td><td><bean:write name="order" property="billToLastName"/>
</td></tr>
<tr><td>
  Address 1:</td><td><bean:write name="order" property="billAddress1"/>
</td></tr>
<tr><td>
  Address 2:</td><td><bean:write name="order" property="billAddress2"/>
</td></tr>
<tr><td>
  City: </td><td><bean:write name="order" property="billCity"/>
</td></tr>
<tr><td>
  State:</td><td><bean:write name="order" property="billState"/>
</td></tr>
<tr><td>
  Zip:</td><td><bean:write name="order" property="billZip"/>
</td></tr>
<tr><td>
  Country: </td><td><bean:write name="order" property="billCountry"/>
</td></tr>
<tr><th colspan="2">
  Shipping Address
</th></tr><tr><td>
  First name:</td><td><bean:write name="order" property="shipToFirstName"/>
</td></tr>
<tr><td>
  Last name:</td><td><bean:write name="order" property="shipToLastName"/>
</td></tr>
<tr><td>
  Address 1:</td><td><bean:write name="order" property="shipAddress1"/>
</td></tr>
<tr><td>
  Address 2:</td><td><bean:write name="order" property="shipAddress2"/>
</td></tr>
<tr><td>
  City: </td><td><bean:write name="order" property="shipCity"/>
</td></tr>
<tr><td>
  State:</td><td><bean:write name="order" property="shipState"/>
</td></tr>
<tr><td>
  Zip:</td><td><bean:write name="order" property="shipZip"/>
</td></tr>
<tr><td>
  Country: </td><td><bean:write name="order" property="shipCountry"/>
</td></tr>
<tr><td>
  Courier: </td><td><bean:write name="order" property="courier"/>
</td></tr>
<tr><td colspan="2">
  Status: <bean:write name="order" property="status"/>
</td></tr>
<tr><td colspan="2">
  <table>
    <tr>
      <th>Item ID</th>
      <th>Description</th>
      <th>Quantity</th>
      <th>Price</th>
      <th>Total Cost</th>
    </tr>
    <logic:iterate id="item" name="itemList">
      <tr>
        <td><html:link paramId="itemId" paramName="item" paramProperty="itemId" page="/shop/viewItem.shtml">
          <bean:write name="item" property="itemId"/></html:link></td>
        <td>
          <logic:present name="item" property="item">
            <bean:write name="item" property="item.attribute1"/>
            <bean:write name="item" property="item.attribute2"/>
            <bean:write name="item" property="item.attribute3"/>
            <bean:write name="item" property="item.attribute4"/>
            <bean:write name="item" property="item.attribute5"/>
            <bean:write name="item" property="item.product.name"/>
          </logic:present>
          <logic:notPresent name="item" property="item">
            <i>{description unavailable}</i>
          </logic:notPresent>
        </td>

        <td><bean:write name="item" property="quantity"/></td>
        <td><bean:write name="item" property="unitPrice" format="$#,##0.00"/></td>
        <td><bean:write name="item" property="total" format="$#,##0.00"/></td>
      </tr>
    </logic:iterate>
    <tr>
      <th colspan="5">Total: <bean:write name="order" property="totalPrice" format="$#,##0.00"/>
      </th>
    </tr>
  </table>
</td></tr>

</table>

</div>

<%@ include file="../common/IncludeBottom.jsp" %>



