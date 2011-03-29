package au.com.postnewspapers.postupload.common;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * The internal Java InternetAddress class is ... clumsy ... for
 * JSON transmission. It includes the address schema, encoding, and other
 * things we don't want to throw around.
 * 
 * Provide a simpler type for exchanging addresses, and conversion routines to/from
 * InternetAddress. Ideally it'd be immutable, but JAXB is a bit clumsy.
 * 
 * This class uses InternetAddress for the grunt work, and it's not particularly
 * efficient about it.
 * 
 * @author craig
 */
@XmlType
@XmlAccessorType(XmlAccessType.PROPERTY)
public class EmailAddress implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final String ACCEPTED_PERSONAL_ENCODING = "UTF-8";
    
    private final InternetAddress addr;
    
    public EmailAddress() {
        addr = new InternetAddress();
    }
    
    public EmailAddress(String address, String personal) throws AddressException {
        try {
            addr = new InternetAddress(address, personal, ACCEPTED_PERSONAL_ENCODING);
            addr.validate();
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("Address conversion failed due to lack of utf-8 support; should be impossible", ex);
        }
    }
    
    public EmailAddress(InternetAddress addr) throws AddressException {
        this.addr = (InternetAddress) addr.clone();
        addr.validate();
    }
    
    public EmailAddress(String addrAndPersonal) throws AddressException {
        InternetAddress[] addrs = InternetAddress.parse(addrAndPersonal);
        if (addrs.length != 1) {
            throw new AddressException("Number of parsed addresses ("+addrs.length+") not equal to 1");
        }
        this.addr = addrs[0];
        addr.validate();
    }
    
    public String getAddress() {
        return addr.getAddress();
    }

    public String getPersonal() {
        return addr.getPersonal();
    }

    public void setAddress(String address) {
        addr.setAddress(address);
    }

    public void setPersonal(String personal) {
        try {
            addr.setPersonal(personal, ACCEPTED_PERSONAL_ENCODING);
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("Address conversion failed due to lack of utf-8 support; should be impossible", ex);
        }
    }
    
    public InternetAddress toInternetAddress() {
        return (InternetAddress) addr.clone();
    }
    
    public String toString() {
        return addr.toString();
    }
    
    public void validate() throws AddressException {
        addr.validate();
    }

}
