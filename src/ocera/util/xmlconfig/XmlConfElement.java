/**
 * Created by IntelliJ IDEA.
 * User: fanda
 * Date: May 15, 2003
 * Time: 2:05:34 PM
 * To change this template use Options | File Templates.
 */
package ocera.util.xmlconfig;

import org.jdom.Element;
import ocera.util.*;

public class XmlConfElement
{
    protected  Element element;

    public XmlConfElement(Element el)
    {
        element = el;
    }

    public Element getElement()
    {
        return element;
    }

    public XmlConfElement getRootElement()
    {
        return new XmlConfElement(element.getDocument().getRootElement().getChild("data"));
    }

    /**
     *
     * @param path slash (/) delimited path (like on Unix filesystems)<br>
     * It can contain .. Example cd("../other_child"), cd("/")
     * @return Element on path from current position in XML tree or null
     */
    public XmlConfElement cd(String path)
    {
        String s = path.trim();
        StringParser sp = new StringParser();
        String sa[];
        Element elem = element;
        while(s.length() > 0) {
            sa = sp.cut(s, '/');
            if(sa[0].length() == 0) {
                elem = getRootElement().getElement();
            }
            else if(sa[0].equals("..")) {
                elem = elem.getParent();
            }
            else if(sa[0].equals(".")) {}
            else {
                elem = elem.getChild(sa[0]);
                if(elem == null) break;
            }
            s = sa[1];
        }
        return new XmlConfElement(elem);
    }

    /**
     * Like {@link org.jdom.Element#getAttributeValue(java.lang.String name, java.lang.String value)}.
     */
    public String getAttributeValue(String name, String defval)
    {
        return getElement().getAttributeValue(name, defval);
    }

    /**
     * Like {@link org.jdom.Element#getAttributeValue(java.lang.String name, java.lang.String value)}.
     */
    public void setAttribute(String name, String val)
    {
        getElement().setAttribute(name, val);
    }

    /**
     * Like {@link org.jdom.Element#setAttribute(java.lang.String name, java.lang.String value)}.
     * @throws XmlConfElementException if attribute 'name' does not exists.
     */
    public void setAttributeThrow(String name, String val) throws XmlConfElementException
    {
        if(getElement().getAttributeValue(name) == null)
            throw new XmlConfElementException("Attribute name '" + name + "' does not exists.");
        getElement().setAttribute(name, val);
    }

    /**
     * @param defval value to return if elemnt does not contains value
     * @return  attribute value or defval
     */
    public String getValue(String defval)
    {
        if(element == null) return defval;
        return element.getAttributeValue("value", defval);
    }

    /**
     * Sets value of attribute value to the val
     * @param val new value of attribute value
     */
    public void setValue(String val)
    {
        if(element == null)
            throw new NullPointerException("Cann't set value of null element.");
        if(val == null) val = "";
        getElement().setAttribute("value", val);
    }

    public String toString()
    {
        return element.getName();
    }
}
