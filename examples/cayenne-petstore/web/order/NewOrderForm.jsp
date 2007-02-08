<%@ include file="../common/IncludeTop.jsp" %>

<div id="Catalog">

  <html:form action="/shop/newOrder.shtml" styleId="orderBean" method="post">

    <table>
      <tr><th colspan=2>
        Payment Details
      </th></tr><tr><td>
      Card Type:</td><td>
      <html:select name="orderBean" property="order.cardType">
        <html:options name="orderBean" property="creditCardTypes"/>
      </html:select>
    </td></tr>
      <tr><td>
        Card Number:</td><td><html:text name="orderBean" property="order.creditCard"/>
        * Use a fake number!
      </td></tr>
      <tr><td>
        Expiry Date (MM/YYYY):</td><td><html:text name="orderBean" property="order.expiryDate"/>
      </td></tr>
      <tr><th colspan=2>
        Billing Address
      </th></tr>

      <tr><td>
        First name:</td><td><html:text name="orderBean" property="order.billToFirstName"/>
      </td></tr>
      <tr><td>
        Last name:</td><td><html:text name="orderBean" property="order.billToLastName"/>
      </td></tr>
      <tr><td>
        Address 1:</td><td><html:text size="40" name="orderBean" property="order.billAddress1"/>
      </td></tr>
      <tr><td>
        Address 2:</td><td><html:text size="40" name="orderBean" property="order.billAddress2"/>
      </td></tr>
      <tr><td>
        City: </td><td><html:text name="orderBean" property="order.billCity"/>
      </td></tr>
      <tr><td>
        State:</td><td><html:text size="4" name="orderBean" property="order.billState"/>
      </td></tr>
      <tr><td>
        Zip:</td><td><html:text size="10" name="orderBean" property="order.billZip"/>
      </td></tr>
      <tr><td>
        Country: </td><td><html:text size="15" name="orderBean" property="order.billCountry"/>
      </td></tr>

      <tr><td colspan=2>
        <html:checkbox name="orderBean" property="shippingAddressRequired"/> Ship to different address...
      </td></tr>

    </table>

    <input type="submit" name="submit" value="Continue">

  </html:form>

</div>

<%@ include file="../common/IncludeBottom.jsp" %>