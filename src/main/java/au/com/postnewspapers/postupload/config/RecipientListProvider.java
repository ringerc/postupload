package au.com.postnewspapers.postupload.config;

import java.io.Serializable;
import java.util.List;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Generic interface for the various recipient list classes,
 * allowing them to share common functionality and be injected
 * the same.
 * 
 * Implementations share the scope of FileHandlerConfig, so they
 * will only be instantiated once as ApplicationScoped beans, and need
 * to be aware of concurrency considerations.
 * 
 * The implementation will be accessible via JAX-RS method calls from
 * the config interface. It should behave like a RESTful collection.
 * 
 * If any methods cannot be supported, the class should throw
 * UnsupportedOperationException .
 * 
 * @author craig
 */
public abstract class RecipientListProvider implements Serializable {

    /**
     * @return a list of all possible recipients
     */
    @GET
    @Produces("application/json")
    public abstract List<InternetAddress> getPossibleRecipients();
    
    /**
     * Replace the entire address list with a new one
     * @param addresses New address list
     * @throws UnsupportedOperationException If the operation isn't supported
     */
    @PUT
    @Consumes("application/json")
    public void setPossibleRecipients(List<InternetAddress> addresses) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Cannot modify address list");
    }
    
    @POST
    @Consumes("applicatoin/json")
    public void addRecipient(InternetAddress address) {
        throw new UnsupportedOperationException("Cannot add to address list");
    }
    
    @GET
    @Path("{recipientIndex}")
    public InternetAddress getRecipient(@PathParam("recipientId") int recipientIndex) {
        return getPossibleRecipients().get(recipientIndex);
    }
    
    @PUT
    @Path("{recipientIndex}")
    public InternetAddress setRecipient(@PathParam("recipientId") int recipientIndex, InternetAddress newAddress) {
        throw new UnsupportedOperationException("Cannot update recipient");
    }
    
    @DELETE
    @Path("{recipientIndex}")
    public void deleteRecipient(@PathParam("recipientId") String recipientIndex) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Cannot delete recipient");
    }
    
}
