//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.08.20 at 09:11:46 AM EEST 
//


package fi.vm.yti.terminology.api.model.ntrf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SYType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SYType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="EQUI" type="{}EQUIType"/>
 *         &lt;element name="TERM">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="GRAM" type="{}GRAMType" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="HOGR" type="{http://www.w3.org/2001/XMLSchema}byte"/>
 *         &lt;element name="SCOPE" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="GEOG" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="SOURF" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ADD" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SYType", propOrder = {
    "equiOrTERMOrHOGR"
})
public class SYType {

    @XmlElementRefs({
        @XmlElementRef(name = "SOURF", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "TERM", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "SCOPE", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "EQUI", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "GEOG", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "HOGR", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "ADD", type = JAXBElement.class, required = false)
    })
    protected List<JAXBElement<?>> equiOrTERMOrHOGR;

    /**
     * Gets the value of the equiOrTERMOrHOGR property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the equiOrTERMOrHOGR property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEQUIOrTERMOrHOGR().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link SYType.TERM }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link EQUIType }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link Byte }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * 
     */
    public List<JAXBElement<?>> getEQUIOrTERMOrHOGR() {
        if (equiOrTERMOrHOGR == null) {
            equiOrTERMOrHOGR = new ArrayList<JAXBElement<?>>();
        }
        return this.equiOrTERMOrHOGR;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="GRAM" type="{}GRAMType" minOccurs="0"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "content"
    })
    public static class TERM {

        @XmlElementRef(name = "GRAM", type = JAXBElement.class, required = false)
        @XmlMixed
        protected List<Serializable> content;

        /**
         * Gets the value of the content property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the content property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getContent().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link JAXBElement }{@code <}{@link GRAMType }{@code >}
         * {@link String }
         * 
         * 
         */
        public List<Serializable> getContent() {
            if (content == null) {
                content = new ArrayList<Serializable>();
            }
            return this.content;
        }

    }

}
