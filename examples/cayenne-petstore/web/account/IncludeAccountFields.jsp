<h3>Account Information</h3>

<table>
  <tr>
    <td>First name:</td><td><html:text name="accountBean" property="account.firstName"/></td>
  </tr><tr>
  <td>Last name:</td><td><html:text name="accountBean" property="account.lastName"/></td>
</tr><tr>
  <td>Email:</td><td><html:text size="40" name="accountBean" property="account.email"/></td>
</tr><tr>
  <td>Phone:</td><td><html:text name="accountBean" property="account.phone"/></td>
</tr><tr>
  <td>Address 1:</td><td><html:text size="40" name="accountBean" property="account.address1"/></td>
</tr><tr>
  <td>Address 2:</td><td><html:text size="40" name="accountBean" property="account.address2"/></td>
</tr><tr>
  <td>City:</td><td><html:text name="accountBean" property="account.city"/></td>
</tr><tr>
  <td>State:</td><td><html:text size="4" name="accountBean" property="account.state"/></td>
</tr><tr>
  <td>Zip:</td><td><html:text size="10" name="accountBean" property="account.zip"/></td>
</tr><tr>
  <td>Country:</td><td><html:text size="15" name="accountBean" property="account.country"/></td>
</tr>
</table>

<h3>Profile Information</h3>

<table>
  <tr>
    <td>Language Preference:</td><td>
    <html:select name="accountBean" property="languagePreference">
      <html:options name="accountBean" property="languages"/>
    </html:select></td>
  </tr><tr>
  <td>Favourite Category:</td><td>
  <html:select name="accountBean" property="favouriteCategoryId">
    <html:options name="accountBean" property="categories"/>
  </html:select></td>
</tr><tr>
  <td>Enable MyList</td><td><html:checkbox name="accountBean" property="listOption"/></td>
</tr><tr>
  <td>Enable MyBanner</td><td><html:checkbox name="accountBean" property="bannerOption"/></td>
</tr>

</table>
