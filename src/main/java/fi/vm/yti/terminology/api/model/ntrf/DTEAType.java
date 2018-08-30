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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DTEAType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DTEAType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="EQUI" type="{}EQUIType" minOccurs="0"/>
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
 *         &lt;element name="SCOPE" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="HOGR" type="{http://www.w3.org/2001/XMLSchema}byte" minOccurs="0"/>
 *         &lt;element name="GEOG" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="SOURF" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DTEAType", propOrder = {
    "equi",
    "term",
    "scope",
    "hogr",
    "geog",
    "sourf"
})
public class DTEAType {

    @XmlElement(name = "EQUI")
    protected EQUIType equi;
    @XmlElement(name = "TERM", required = true)
    protected DTEAType.TERM term;
    @XmlElement(name = "SCOPE")
    protected String scope;
    @XmlElement(name = "HOGR")
    protected Byte hogr;
    @XmlElement(name = "GEOG")
    protected String geog;
    @XmlElement(name = "SOURF", required = true)
    protected String sourf;

    /**
     * Gets the value of the equi property.
     * 
     * @return
     *     possible object is
     *     {@link EQUIType }
     *     
     */
    public EQUIType getEQUI() {
        return equi;
    }

    /**
     * Sets the value of the equi property.
     * 
     * @param value
     *     allowed object is
     *     {@link EQUIType }
     *     
     */
    public void setEQUI(EQUIType value) {
        this.equi = value;
    }

    /**
     * Gets the value of the term property.
     * 
     * @return
     *     possible object is
     *     {@link DTEAType.TERM }
     *     
     */
    public DTEAType.TERM getTERM() {
        return term;
    }

    /**
     * Sets the value of the term property.
     * 
     * @param value
     *     allowed object is
     *     {@link DTEAType.TERM }
     *     
     */
    public void setTERM(DTEAType.TERM value) {
        this.term = value;
    }

    /**
     * Gets the value of the scope property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSCOPE() {
        return scope;
    }

    /**
     * Sets the value of the scope property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSCOPE(String value) {
        this.scope = value;
    }

    /**
     * Gets the value of the hogr property.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getHOGR() {
        return hogr;
    }

    /**
     * Sets the value of the hogr property.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setHOGR(Byte value) {
        this.hogr = value;
    }

    /**
     * Gets the value of the geog property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGEOG() {
        return geog;
    }

    /**
     * Sets the value of the geog property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGEOG(String value) {
        this.geog = value;
    }

    /**
     * Gets the value of the sourf property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSOURF() {
        return sourf;
    }

    /**
     * Sets the value of the sourf property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSOURF(String value) {
        this.sourf = value;
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
         * {@link String }
         * {@link JAXBElement }{@code <}{@link GRAMType }{@code >}
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
