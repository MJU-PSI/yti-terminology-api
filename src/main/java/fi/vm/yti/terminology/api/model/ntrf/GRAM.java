//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2022.03.30 at 12:09:20 PM EEST 
//


package fi.vm.yti.terminology.api.model.ntrf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="value" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *       &lt;attribute name="gend">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *             &lt;enumeration value="m"/>
 *             &lt;enumeration value="n"/>
 *             &lt;enumeration value="f"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="pos" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *       &lt;attribute name="infl" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *       &lt;attribute name="synt" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
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
@XmlRootElement(name = "GRAM")
public class GRAM {

    @XmlValue
    protected String content;
    @XmlAttribute(name = "value")
    @XmlSchemaType(name = "anySimpleType")
    protected String value;
    @XmlAttribute(name = "gend")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String gend;
    @XmlAttribute(name = "pos")
    @XmlSchemaType(name = "anySimpleType")
    protected String pos;
    @XmlAttribute(name = "infl")
    @XmlSchemaType(name = "anySimpleType")
    protected String infl;
    @XmlAttribute(name = "synt")
    @XmlSchemaType(name = "anySimpleType")
    protected String synt;

    /**
     * Gets the value of the content property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the value of the content property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContent(String value) {
        this.content = value;
    }

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the gend property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGend() {
        return gend;
    }

    /**
     * Sets the value of the gend property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGend(String value) {
        this.gend = value;
    }

    /**
     * Gets the value of the pos property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPos() {
        return pos;
    }

    /**
     * Sets the value of the pos property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPos(String value) {
        this.pos = value;
    }

    /**
     * Gets the value of the infl property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInfl() {
        return infl;
    }

    /**
     * Sets the value of the infl property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInfl(String value) {
        this.infl = value;
    }

    /**
     * Gets the value of the synt property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSynt() {
        return synt;
    }

    /**
     * Sets the value of the synt property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSynt(String value) {
        this.synt = value;
    }

}
