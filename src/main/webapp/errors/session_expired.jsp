<%-- 
    Document   : session_expired
    Created on : 04/03/2011, 10:35:27 AM
    Author     : craig
--%>

<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Session Expired</title>
    </head>
    <body>
        <h1>Session Expired</h1>
        
        <p>It has been a while since you last did anything on this website,
            and the server has forgotten what you were doing at the time.
            You will need to go back to the <a href="${pageContext.servletContext.contextPath}/">main page</a> to continue.</p>
        
        <p>If you were using the limited-functionality uploader and had added
            files but not yet pressed the "finished" button, your files should have
            been sent. It would still be a good idea to confirm with the intended 
            recipient that they got them.</p>
    </body>
</html>
