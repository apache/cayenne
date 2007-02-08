<%@ include file="../common/IncludeTop.jsp" %>

<div id="Catalog">

  <html:form method="post" action="/shop/editAccount.shtml">

    <html:hidden name="accountBean" property="validation" value="edit"/>
    <html:hidden name="accountBean" property="username"/>

    <h3>User Information</h3>

    <table>
      <tr>
        <td>User ID:</td><td><bean:write name="accountBean" property="username"/></td>
      </tr><tr>
      <td>New password:</td><td><html:password name="accountBean" property="password"/></td>
    </tr><tr>
      <td>Repeat password:</td><td><html:password name="accountBean" property="repeatedPassword"/></td>
    </tr>
    </table>
    <%@ include file="IncludeAccountFields.jsp" %>

    <input type="submit" name="submit" value="Save Account Information"/>

  </html:form>

  <html:link page="/shop/listOrders.shtml">My Orders</html:link>

</div>

<%@ include file="../common/IncludeBottom.jsp" %>


