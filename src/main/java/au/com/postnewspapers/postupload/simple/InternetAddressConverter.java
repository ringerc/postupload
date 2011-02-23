package au.com.postnewspapers.postupload.simple;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 *
 * @author Craig
 */
@FacesConverter(forClass=javax.mail.internet.InternetAddress.class)
public class InternetAddressConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        InternetAddress[] addresses;
        try {
            addresses = InternetAddress.parse(value);
            if (addresses.length != 1) {
                throw new ConverterException("More than one email address given or email had commas in it");
            }
            return addresses[0];
        } catch (AddressException ex) {
            throw new ConverterException("Unable to read email address: " + ex);
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
        return ((InternetAddress)value).toString();
    }

}
