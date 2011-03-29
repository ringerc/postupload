package au.com.postnewspapers.postupload.config;

import au.com.postnewspapers.postupload.common.EmailAddress;
import java.io.Serializable;
import java.util.List;
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
    public abstract List<EmailAddress> getPossibleRecipients() throws ConfigurationError;
    
    /**
     * Replace the entire address list with a new one
     * @param addresses New address list
     * @throws UnsupportedOperationException If the operation isn't supported
     */
    @PUT
    @Consumes("application/json")
    public void setPossibleRecipients(List<EmailAddress> addresses) throws UnsupportedOperationException, ConfigurationError {
        throw new UnsupportedOperationException("Cannot modify address list");
    }
    
    /**
     * Create a new address entry, specified as a JSON object. Return the index
     * of the new address in the address list. Clumsily, this is returned as a
     * String because of limitations in the Jersey REST interface.
     * 
     * @param address recipient address as json
     * @return index of newly created entry in recipient list, as string
     * @throws UnsupportedOperationException 
     */
    @POST
    @Consumes("application/json")
    @Produces("text/plain")
    public String addRecipient(EmailAddress address) throws UnsupportedOperationException, ConfigurationError {
        throw new UnsupportedOperationException("Cannot add to address list");
    }
    
    /**
     * Return a recipient, fetched by index into the recipient list
     * @param recipientIndex index of recipient
     * @return  recipient as json 
     * @throws IndexOutOfBoundsException if no such offset exists
     * @throws UnsupportedOperationException 
     */
    @GET
    @Path("{recipientIndex}")
    public EmailAddress getRecipient(@PathParam("recipientIndex") int recipientIndex) throws IndexOutOfBoundsException, UnsupportedOperationException, ConfigurationError {
        return getPossibleRecipients().get(recipientIndex);
    }
    
    /**
     * Overwrite an entry in the recipient list with a new recipient
     * 
     * @param recipientIndex Index to overwrite
     * @param newAddress New value
     * @throws IndexOutOfBoundsException
     * @throws UnsupportedOperationException 
     */
    @PUT
    @Path("{recipientIndex}")
    public void setRecipient(@PathParam("recipientIndex") int recipientIndex, EmailAddress newAddress) throws IndexOutOfBoundsException, UnsupportedOperationException, ConfigurationError {
        throw new UnsupportedOperationException("Cannot update recipient");
    }
    
    /**
     * Delete an entry in the recipient list. All indexes higher than this entry
     * will be shifted down by one.
     * 
     * @param recipientIndex Index to delete
     * @throws IndexOutOfBoundsException
     * @throws UnsupportedOperationException 
     */
    @DELETE
    @Path("{recipientIndex}")
    public void deleteRecipient(@PathParam("recipientIndex") int recipientIndex) throws IndexOutOfBoundsException, UnsupportedOperationException, ConfigurationError {
        throw new UnsupportedOperationException("Cannot delete recipient");
    }
    
}
