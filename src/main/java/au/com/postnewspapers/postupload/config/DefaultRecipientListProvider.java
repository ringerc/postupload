package au.com.postnewspapers.postupload.config;

import java.util.ArrayList;
import java.util.List;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * The default RecipientListProvider allows recipients to be configured
 * using the web interface.
 * 
 * @author craig
 */
public class DefaultRecipientListProvider extends RecipientListProvider {
    private static final long serialVersionUID = 123123L;

    @Override
    public List<InternetAddress> getPossibleRecipients() {
        // TODO fetch this dynamically
        ArrayList<InternetAddress> possibleRecipients = new ArrayList<InternetAddress>();
        try {
            possibleRecipients.add(new InternetAddress("Craig Ringer <craig@postnewspapers.com.au>"));
        } catch (AddressException ex) {
            throw new RuntimeException(ex);
        }
        return possibleRecipients;
    }

}
