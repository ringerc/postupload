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
    
    private void rewritePreferences() throws ConfigurationError {
        try {
            Preferences prefsRecipients = prefsRoot.node("recipients");
            prefsRecipients.clear();
            int numKeys = recipients.size();
            for (int i = 0; i < numKeys; i++) {
                prefsRecipients.put(Integer.toString(i), recipients.get(i).toString());
            }
            prefsRecipients.flush();
        } catch (BackingStoreException ex) {
            Logger.getLogger(PrefsRecipientListProvider.class.getName()).log(Level.SEVERE, null, ex);
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
        rewritePreferences();
    }
    
    @Override
    public synchronized String addRecipient(EmailAddress address) {
        recipients.add(address);
        Preferences prefsRecipients = prefsRoot.node("recipients");
        String index = Integer.toString(recipients.size() - 1);
        prefsRecipients.put(index, address.toString());
        try {
            prefsRecipients.flush();
        } catch (BackingStoreException ex) {
            Logger.getLogger(PrefsRecipientListProvider.class.getName()).log(Level.SEVERE, null, ex);
            throw new ConfigurationError(ex);
        }
        return index;
    }

    @Override
    public synchronized void deleteRecipient(int recipientIndex) throws IndexOutOfBoundsException {
        recipients.remove(recipientIndex);
        // We can't just delete the target key, because we'd leave a hole
        // in the list. It's simplest to clear the prefs and rewrite them.
        rewritePreferences();
    }

    @Override
    public synchronized EmailAddress getRecipient(int recipientIndex) throws IndexOutOfBoundsException  {
        return recipients.get(recipientIndex);
    }

    @Override
    public synchronized void setRecipient(int recipientIndex, EmailAddress newAddress) throws IndexOutOfBoundsException  {
        recipients.set(recipientIndex, newAddress);
        Preferences prefsRecipients = prefsRoot.node("recipients");
        prefsRecipients.put(Integer.toString(recipientIndex), newAddress.toString());
        try {
            prefsRecipients.flush();
        } catch (BackingStoreException ex) {
            Logger.getLogger(PrefsRecipientListProvider.class.getName()).log(Level.SEVERE, null, ex);
            throw new ConfigurationError(ex);
        }
    }
    
}
