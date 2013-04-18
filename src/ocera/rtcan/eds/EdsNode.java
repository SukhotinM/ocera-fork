package ocera.rtcan.eds;

/**
 * @author vacek
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */

import java.util.*;
import ocera.util.*;
import ocera.rtcan.monitor.EdsTreeNode;

/**
 * Represents EDS entry, not inserted in OD
 */
public class EdsNode implements EdsTreeNode
{
    public String caption = "";
    public String name = "";
	public int childrenCnt = 0;
	public EdsAttribute[] attributes = {};
    public int index = -1;
    public int subindex = -1;

    public EdsNode(String name) {
        this.name = name;
    }

    public String toString()
	{
//		String s = caption + " {";
//		for (int i = 0; i < attributes.length; i++) {
//			if(i>0) s += ", ";
//			s += attributes[i].toString();
//		}
//		s += "} : " + childrenCnt + " children.";
        if(caption.length() == 0) return name;
		return name + " - " + caption;
	}

    public String getName()
    {
        return name;
    }

    public int getAttrCnt()
    {
        return attributes.length;
    }

    public String getAttrName(int ix)
    {
        if(ix >= getAttrCnt()) return "<NULL>";
        return attributes[ix].key;
    }

    public String getAttrValue(int ix)
    {
        if(ix >= getAttrCnt()) return "<NULL>";
        return attributes[ix].value;
    }

    public String getAttrDescription(int ix)
    {
        return "";
    }

//    public void setAttrName(int ix, String s)
//    {
//        if(ix >= getAttrCnt()) return;
//        attributes[ix].key = s;
//    }

    public void setAttrValue(int ix, String s)
    {
        if(ix >= getAttrCnt()) return;
        attributes[ix].value = s;
    }
}

