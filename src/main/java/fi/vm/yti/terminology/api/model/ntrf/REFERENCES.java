//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.09.18 at 09:30:55 AM EEST 
//


package fi.vm.yti.terminology.api.model.ntrf;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element ref="{}REF"/>
 *         &lt;element ref="{}REFHEAD"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "refOrREFHEAD"
})
@XmlRootElement(name = "REFERENCES")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-09-18T09:30:55+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class REFERENCES {

    @XmlElements({
        @XmlElement(name = "REF", type = REF.class),
        @XmlElement(name = "REFHEAD", type = String.class)
    })
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-09-18T09:30:55+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<Object> refOrREFHEAD;

    /**
     * Gets the value of the refOrREFHEAD property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the refOrREFHEAD property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getREFOrREFHEAD().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link REF }
     * {@link String }
     * 
     * 
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-09-18T09:30:55+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<Object> getREFOrREFHEAD() {
        if (refOrREFHEAD == null) {
            refOrREFHEAD = new ArrayList<Object>();
        }
        return this.refOrREFHEAD;
    }

}
