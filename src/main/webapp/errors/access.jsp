<%-- 
    Document   : access
    Created on : 04/03/2011, 10:30:48 AM
    Author     : craig

    Why JSP? Facelets error pages don't seem to get processed.
--%>

<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Resource Not found or inaccessible</title>
    </head>
    <body>
        <h1>Resource Not found or inaccessible</h1>
        
        <p>If you followed a link from within this website, please consider reporting
            the problem to the <a href="mailto:${fileHandlerConfig.adminEmail}">site administrator</a>.</p>
        
        <p>Go back to the <a href="${pageContext.servletContext.contextPath}/">main page</a>.</a>
    
    </body>
</html>