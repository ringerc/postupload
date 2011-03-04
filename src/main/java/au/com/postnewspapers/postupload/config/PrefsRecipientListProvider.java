package au.com.postnewspapers.postupload.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

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
    
    private final List<InternetAddress> recipients = new ArrayList<InternetAddress>();
    
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
                recipients.add(InternetAddress.parse(addr)[0]);
            }
        } catch (AddressException ex) {
            throw new ConfigurationError(ex);
        } catch (BackingStoreException ex) {
            throw new ConfigurationError(ex);
        }
    }

    @Override
    public synchronized List<InternetAddress> getPossibleRecipients() {
        return recipients;
    }

    @Override
    public synchronized void setPossibleRecipients(List<InternetAddress> addresses) throws ConfigurationError {
        recipients.clear();
        recipients.addAll(addresses);
    }
    
    @Override
    public synchronized int addRecipient(InternetAddress address) {
        recipients.add(address);
        return recipients.size() - 1;
    }

    @Override
    public synchronized void deleteRecipient(int recipientIndex) throws IndexOutOfBoundsException {
        recipients.remove(recipientIndex);
    }

    @Override
    public synchronized InternetAddress getRecipient(int recipientIndex) throws IndexOutOfBoundsException  {
        return recipients.get(recipientIndex);
    }

    @Override
    public synchronized void setRecipient(int recipientIndex, InternetAddress newAddress) throws IndexOutOfBoundsException  {
        recipients.set(recipientIndex, newAddress);
    }
    
}
