<%-- 
    Document   : generic_error
    Created on : 04/03/2011, 10:34:29 AM
    Author     : craig
--%>

<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Application Error</title>
    </head>
    <body>
        <h1>Application Error</h1>
        
        <p>The error has been reported to the site administrator and will be examined.
        You might be able to try what you were doing again by pressing the back button.
        Alternately, please go to the <a href="${pageContext.servletContext.contextPath}/">main page</a> to continue.</p>
        
        <p>If you are using an unusual or very new web browser, please report this
        error to the <a href="mailto:${fileHandlerConfig.adminEmail}">site administrator</a>.</p>
    </body>
</html>