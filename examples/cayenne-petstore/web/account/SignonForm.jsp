<%@ include file="../common/IncludeTop.jsp" %>

<div id="Catalog">
  <html:form action="/shop/signon" method="POST">

    <p>Please enter your username and password.</p>
    <p>
      Username:<input type="text" name="username" value="j2ee"/>
      <br/>
      Password:<input type="password" name="password" value="j2ee"/>
    </p>
    <input type="submit" name="submit" value="Login"/>

  </html:form>

  Need a username and password?
  <html:link page="/shop/newAccountForm.shtml">Register Now!</html:link>

</div>

<%@ include file="../common/IncludeBottom.jsp" %>

