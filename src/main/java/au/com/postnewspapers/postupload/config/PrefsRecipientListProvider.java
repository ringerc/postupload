package au.com.postnewspapers.postupload.config;

import au.com.postnewspapers.postupload.common.EmailAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.mail.internet.AddressException;

/**
 * The default RecipientListProvider allows recipients to be configured
 * using the web interface. It stores the recipient collection in the
 * Java Preferences API.
 * 
 * @author craig
 */
public class PrefsRecipientListProvider extends RecipientListProvider {
    private static final long serialVersionUID = 123123L;
    private static final Preferences prefsRoot = Preferences.userNodeForPackage(PrefsRecipientListProvider.class);
    private static final Logger logger = Logger.getLogger(PrefsRecipientListProvider.class.getName());
    
    private final List<EmailAddress> recipients = new ArrayList<EmailAddress>();
    
    public PrefsRecipientListProvider() throws ConfigurationError {
        loadFromPreferences();
    }
    
    private void loadFromPreferences() throws ConfigurationError {
        try {
            Preferences prefsRecipients = prefsRoot.node("recipients");
            int numKeys = prefsRecipients.keys().length;
            for (int i = 0; i < numKeys; i++) {
                String addr = prefsRecipients.get(Integer.toString(i), null);
                if (addr == null) {
                    throw new ConfigurationError("Unexpectedly null address in preferences!");
                }
                recipients.add(new EmailAddress(addr));
            }
        } catch (AddressException ex) {
            throw new ConfigurationError(ex);
        } catch (BackingStoreException ex) {
            throw new ConfigurationError(ex);
        }
    }

    @Override
    public synchronized List<EmailAddress> getPossibleRecipients() {
        return recipients;
    }

    @Override
    public synchronized void setPossibleRecipients(List<EmailAddress> addresses) throws ConfigurationError {
        recipients.clear();
        recipients.addAll(addresses);
    }
    
    @Override
    public synchronized String addRecipient(EmailAddress address) {
        recipients.add(address);
        return Integer.toString(recipients.size() - 1);
    }

    @Override
    public synchronized void deleteRecipient(int recipientIndex) throws IndexOutOfBoundsException {
        logger.log(Level.INFO, "Deleting recipient at index: {0}, details: {1}", new Object[]{recipientIndex, recipients.get(recipientIndex)});
        recipients.remove(recipientIndex);
    }

    @Override
    public synchronized EmailAddress getRecipient(int recipientIndex) throws IndexOutOfBoundsException  {
        return recipients.get(recipientIndex);
    }

    @Override
    public synchronized void setRecipient(int recipientIndex, EmailAddress newAddress) throws IndexOutOfBoundsException  {
        recipients.set(recipientIndex, newAddress);
    }
    
}
