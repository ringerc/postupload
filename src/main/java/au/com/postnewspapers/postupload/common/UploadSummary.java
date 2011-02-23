package au.com.postnewspapers.postupload.common;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.mail.internet.InternetAddress;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Upload results summary in a form relatively independent from the uploader
 * method used, and cleaned up for transfer around the app.
 * 
 * This is by design just a data structure. It'll be serialized, transferred
 * around as JSON, etc, and shouldn't have too much magic associated with it.
 */
@XmlType
@XmlAccessorType(value = XmlAccessType.FIELD)
public class UploadSummary implements Serializable {
    
    @XmlAccessorType(value = XmlAccessType.PROPERTY)
    public static final class FileInfo implements Serializable, Comparable<FileInfo> {
        public FileInfo() {}
        @XmlElement
        private String name;
        @XmlElement
        private int size;
        /** null or "" if no error */
        @XmlElement
        private String errorType;
        /** value UNDEFINED if no error; test errorType */
        @XmlElement
        private String errorInfo;
        
        // JSF2 can't cope with accessing members, so let's introduce some
        // bloat and pointless crap.

        public String getErrorInfo() {
            return errorInfo;
        }

        public void setErrorInfo(String errorInfo) {
            this.errorInfo = errorInfo;
        }

        public String getErrorType() {
            return errorType;
        }

        public void setErrorType(String errorType) {
            this.errorType = errorType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        // Now, some tedious Java crap that should really be done for us from
        // class metadata in simple cases...
        @Override
        public int compareTo(FileInfo rhs) {
            return new CompareToBuilder()
                    .append(this.name, rhs.name)
                    .append(this.size, rhs.size)
                    .append(this.errorType, rhs.errorType)
                    .append(this.errorInfo, rhs.errorInfo)
                    .toComparison();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj == this) return true;
            if (obj.getClass() != getClass()) {
             return false;
           }
           FileInfo rhs = (FileInfo) obj;
           return new EqualsBuilder()
                   .append(this.name, rhs.name)
                   .append(this.size, rhs.size)
                   .append(this.errorType, rhs.errorType)
                   .append(this.errorInfo, rhs.errorInfo)
                   .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(91, 73)
                    .append(name)
                    .append(size)
                    .append(errorType)
                    .append(errorInfo)
                    .toHashCode();
        }
        
                
    }

    public UploadSummary() {}
    @XmlElement
    public InternetAddress recipientAddress;
    @XmlElement
    public InternetAddress senderAddress;
    @XmlElement
    public String customerCode;
    @XmlElement
    public String bookingNumber;
    @XmlElement
    public String subject;
    @XmlElement
    public String comments;
    public final List<FileInfo> okFiles = new ArrayList<FileInfo>();
    public final List<FileInfo> badFiles = new ArrayList<FileInfo>();
    @XmlElement
    public File outputDirectory;

}
