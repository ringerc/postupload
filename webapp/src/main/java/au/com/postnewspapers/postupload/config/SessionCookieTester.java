package au.com.postnewspapers.postupload.config;

import java.io.Serializable;
import javax.enterprise.context.SessionScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * It is often necessary to know whether the client has working session cookies.
 * To test this without using redirects or other user-visible tricks, the 
 * easiest way is to make a pair of AJAX requests to the server. The first
 * request sets an attribute in the server-side session and returns. The second
 * request tests to see if that attribute exists. If the attribute has vanished,
 * we know we're not correctly maintaining session state, most likely because the
 * client is discarding session cookies.
 * 
 * Because of JAX-RS's session management, this test can be simplified down to
 * "is the instance responding to the /test request the same instance that received
 * the /set request".
 * 
 * The downside of using AJAX requests within one page is that we cannot detect
 * if a client clears cookies after a page transition.
 *  
 * @author Craig
 */
@Path("/sessiontest")
@SessionScoped
public class SessionCookieTester implements Serializable {
    
    private static final long serialVersionUID = 5541141L;
    
    private boolean sessionOk = false;
    
    /**
     * Call this first to set up some internal state.
     * 
     * @return 204 No Content
     */
    @GET
    @Path("/set")
    public synchronized Response setSessionAttribute() {
        sessionOk = true;
        return Response.noContent().build();
    }
    
    /**
     * Test to see if the state set up by /set still exists. 
     * 
     * @return 204 No Content on success, 410 Gone on failure
     */
    @GET
    @Path("/test")
    public synchronized Response testSessionAttribute() {
        if (sessionOk) {
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.GONE).build();
        }
    }

}
