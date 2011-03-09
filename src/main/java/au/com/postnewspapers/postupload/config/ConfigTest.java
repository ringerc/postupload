package au.com.postnewspapers.postupload.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
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
@Path("/configtest")
@RequestScoped
public class ConfigTest {
    
    @Inject
    private FileHandlerConfig config;
    
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
        testAdminEmail(results);
        testJavaMail(results);        
        testOutputDir(results);
        testRecipients(results);
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
    
    private void testOutputDir(List<TestResult> results) {
        String outPath = config.getConfigOutputPath();
        if (outPath == null || outPath.isEmpty()) {
            results.add(new TestResult(false, "No output path is configured",
                    "You haven't set anywhere for postupload to save files to. It "
                    + "will save them to the temporary folder on your system, where "
                    + "your operating system will probably delete them after a while. "
                    + "You should set a path relative to the server to save files to. "
                    + "You can set this in the admin screen."));
        } else {
            results.add(new TestResult(true, "Output path is configured", "Output path is set to: " + outPath));
        }
    }
    
    private void testAdminEmail(List<TestResult> results)  {
        String adminEmail = config.getAdminEmail();
        if (adminEmail == null || adminEmail.isEmpty()) {
            results.add(new TestResult(false, "No admin email address is set", 
                    "You need to configure the address users will see on help and error pages. Set this in the admin screen."));
        } else {
            results.add(new TestResult(true, "Admin email set to: " + adminEmail));
        }
    }
    
    private void testRecipients(List<TestResult> results) {
        if (config.getPossibleRecipients().size() > 0) {
            results.add(new TestResult(false, "No recipients are configured", 
                    "You need to add one or more email addresess to the recipient list, or configure"
                    + " the application to look up recipients from an external source. Set this up in the admin screen."));
        } else {
            results.add(new TestResult(true, "Email recipients were found"));
        }
    }
}
