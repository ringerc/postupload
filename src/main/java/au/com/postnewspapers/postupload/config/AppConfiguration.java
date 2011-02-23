package au.com.postnewspapers.postupload.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.mail.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * This resource is responsible for testing the application's configuration
 * and (in future) updating it.
 * 
 * @author Craig
 */
@Path("/config")
@RequestScoped
public class AppConfiguration {
    
    @XmlType
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TestResult {
        public boolean isOk;
        public String status;
        public String detail;
        public String helpUrl;
        public TestResult(boolean isOk, String status) {
            this.isOk = isOk;
            this.status = status;
        }
        public TestResult(boolean isOk, String status, String detail) {
            this.isOk = isOk;
            this.status = status;
            this.detail = detail;
        }
        public TestResult(boolean isOk, String status, String detail, String helpUrl) {
            this.isOk = isOk;
            this.status = status;
            this.detail = detail;
            this.helpUrl = helpUrl;
        }
    }
    
    @GET
    @Path("/test")
    @Produces("application/json")
    public List<TestResult> testConfiguration() {
        List<TestResult> results = new ArrayList<TestResult>();
        testJavaMail(results);        
        return results;
    }
    
    private void testJavaMail(List<TestResult> results) {
        //
        // Test to make sure we can get a JavaMail session via the JNDI
        // name 'mail/smtp'
        //
        Session session = null;
        try {
             session = InitialContext.doLookup("mail/smtp");
        } catch (NamingException ex) {
            // no action
        }
        if (session == null) {
            results.add(new TestResult(false, "JavaMail is not configured",
                    "You need to add a JavaMail resource named 'mail/smtp' "
                    + "to your container configuration so that the application "
                    + "can send email. See the README for more information."));
        } else {
            results.add(new TestResult(true, "JavaMail: mail/smtp resource exists"));
        }
    }
}
