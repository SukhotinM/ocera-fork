/**
 * Created by IntelliJ IDEA.
 * User: fanda
 * Date: May 15, 2003
 * Time: 3:44:19 PM
 * To change this template use Options | File Templates.
 */
package ocera.util.xmlconfig;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.io.StringWriter;

/**
 *  JDOM XML document representing a configuration file
 * Example of such a file<br>
 * <pre>
 * &lt;?xml version="1.0" encoding="windows-1250"?>
 * &lt;config>
 *     &lt;macros hidden="yes"/>
 *     &lt;data>
 *         &lt;connection caption="Connection" hidden="yes">
 *             &lt;database hidden="yes" value="well"/>
 *             &lt;host default="localhost" value="168.192.111.99"/>
 *             &lt;password value="6:>1154=b20/9"/>
 *             &lt;user value="fanda"/>
 *         &lt;/connection>
 *         &lt;browser caption="Internet browser">
 *             &lt;name caption="Name" default="iexplore.exe" help="Input executable name for jour default HTML browser." type="file" value="C:\Program Files\Internet Explorer\IEXPLORE.EXE"/>
 *         &lt;/browser>
 *         &lt;filter hidden="yes">
 *             &lt;showStorno value="No"/>
 *         &lt;/filter>
 *     &lt;/data>
 * &lt;/config>
 * </pre>
 */
public class XmlConfig
{
    protected Document doc = null;

    /**
     * Load contens of document from URI (URI can be a filename)
     * @param uri
     */
    public void fromURI(String uri)
    {
        doc = null;
        SAXBuilder builder = new SAXBuilder();
        try {
            doc = builder.build(uri);
        }
        catch (JDOMException e) {
          System.err.println(uri + " is not well-formed.");
          System.err.println(e.getMessage());
        }
        catch (IOException e) {
          System.err.println("Could not check " + uri);
          System.err.println(" because " + e.getMessage());
        }
    }

    public Document getDocument()
    {
        return doc;
    }

    /**
     * @return configuration root element
     */
    public XmlConfElement getRootElement()
    {
        if(doc == null) return null;
        XmlConfElement el = new XmlConfElement(doc.getRootElement());
        return el.getRootElement();
    }

    /**
     *
     * @param path path to desired element
     * @param defval value to return if anything fails
     * @return If the path is valid returns value attribut of element on this path<br>
     *         else returns defval
     * @see XmlConfElement#cd(String path)
     */
    public String getValue(String path, String defval)
    {
        XmlConfElement el = getRootElement();
        if(el == null) return defval;
        el = el.cd(path);
        if(el == null) return defval;
        return el.getValue(defval);
    }

    /**
     * @param path path to desired element
     * @param val value to set
     * @return true if succed
     */
    public boolean setValue(String path, String val)
    {
        XmlConfElement el = getRootElement();
        if(el == null) return false;
        el = el.cd(path);
        if(el == null) return false;
        el.setAttribute("value", val);
        return true;
    }

    /**
     * @param path path to desired element
     * @param val value to set
     * @throws XmlConfElementException
     */
    public void setValueThrow(String path, String val) throws XmlConfElementException
    {
        XmlConfElement el = getRootElement();
        if(el == null)
            throw new XmlConfElementException("Root element == null");
        el = el.cd(path);
        if(el == null)
            throw new XmlConfElementException("Path '" + path + "' does not exists.");
        el.setAttributeThrow("value", val);
    }

    public String toString()
    {
        if(doc == null) return "{null}";
        XMLOutputter serializer = new XMLOutputter();
        serializer.setIndent("  "); // use two space indent
        serializer.setNewlines(true);
        serializer.setTextNormalize(true);
        serializer.setEncoding("ISO-8859-2");
        try {
            StringWriter sw = new StringWriter();
            serializer.output(doc, sw);
            return sw.toString();
        } catch (IOException e) {
            return e.getMessage();
        }
    }
}
