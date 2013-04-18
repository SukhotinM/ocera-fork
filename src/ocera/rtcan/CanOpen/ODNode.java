package ocera.rtcan.CanOpen;

import ocera.rtcan.monitor.EdsTreeNode;
import ocera.util.StringParser;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.flib.FString;

class DataTypeDef
{
    public String name = "UNKNOWN";
    public int len = -1;

    DataTypeDef(String n, int l) {name = n; len = l;}
}

/**
 * Represent EDS entry inserted to OD (almost all see EdsNode)
 */
public class ODNode implements EdsTreeNode, Comparable
{
    public final static String objectTypeNames[] = {
        "UNDEF", "UNDEF", "DOMAIN",  "UNDEF", "UNDEF", "DEFTYPE",
        "DEFSTRUCT", "VAR", "ARRAY", "RECORD"
    };

    public final static DataTypeDef dataTypeDefs[] = {
         new DataTypeDef("UNKNOWN", -1), new DataTypeDef("BOOLEAN", 1),
         new DataTypeDef("INTEGER8", 1), new DataTypeDef("INTEGER16", 2), new DataTypeDef("INTEGER32", 4),
         new DataTypeDef("UNSIGNED8", 1), new DataTypeDef("UNSIGNED16", 2), new DataTypeDef("UNSIGNED32", 4),
         new DataTypeDef("REAL32", 4),
         new DataTypeDef("VISIBLE_STRING", 0), new DataTypeDef("OCTET_STRING", 0), new DataTypeDef("UNICODE_STRING", 0),
         new DataTypeDef("TIME_OF_DAY", -1), new DataTypeDef("TIME_DIFERENCE", -1),
         new DataTypeDef("reserved", -1),
         new DataTypeDef("DOMAIN", -1),
         new DataTypeDef("INTEGER24", 3),
         new DataTypeDef("REAL64", 8),
         new DataTypeDef("INTEGER40", 5), new DataTypeDef("INTEGER48", 6), new DataTypeDef("INTEGER56", 7), new DataTypeDef("INTEGER64", 8),
         new DataTypeDef("UNSIGNED24", 3),
         new DataTypeDef("reserved", -1),
         new DataTypeDef("UNSIGNED40", 5), new DataTypeDef("UNSIGNED48", 6), new DataTypeDef("UNSIGNED56", 7), new DataTypeDef("UNSIGNED64", 8),
         new DataTypeDef("reserved", -1), new DataTypeDef("reserved", -1), new DataTypeDef("reserved", -1), new DataTypeDef("reserved", -1),
         new DataTypeDef("PDO_COMMUNICATION_PARAMETER", -1), new DataTypeDef("PDO_MAPPING", -1),
         new DataTypeDef("SDO_PARAMETER", -1),
         new DataTypeDef("IDENTITY", -1)
    };

    public final static int ACCES_TYPE_CONST = 0;
    public final static int ACCES_TYPE_RO = 1;
    public final static int ACCES_TYPE_RW = 2;
    public final static int ACCES_TYPE_RWR = 3;
    public final static int ACCES_TYPE_RWW = 4;
    public final static int ACCES_TYPE_WO = 5;

    public ODNode[] subNodes = null;

    public byte[] value = null;

    public boolean subObject = false; // is it item if struct or array ?

    // this array to know names of next fielt in runtime
    protected static Field[] fields = null;
    protected static int attrOffset; // skip index & subindex

    public int index;
    public int subIndex = 0;
    public String parameterName = "";
    public int subNumber = 0; // number of subindicies
    public int objectType;
    public int dataType;
    public byte accessType; //bitfield: 1 - read, 2 - write
    public boolean pdoMapping;
    public String lowLimit = "";
    public String highLimit = "";
    public String defaultValue = "";

    public ODNode()
    {
        if(fields == null) {
//            System.err.println("ODNode::fields init\n");
            try {
                fields = Class.forName("ocera.rtcan.CanOpen.ODNode").getFields();
                for(int i=0; i<fields.length; i++) {
                    if(fields[i].getName().equals("parameterName")) {
                        attrOffset = i;
                    }
                }
            } catch (SecurityException e) {
                e.printStackTrace();  //To change body of catch statement use Options | File Templates.
            } catch (ClassNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use Options | File Templates.
            }
        }
    }

    /**
     * @return true if object is sub object (is referenced by subindex)
     */
    public boolean isSubObject()
    {
        return subObject;
    }

    /**
     * @return number of subobjects
     */
    public int subObjectCnt()
    {
        if(subNodes == null) return 0;
        return subNodes.length;
    }

    public String toString()
	{
		return getName();
	}

    public String getName()
    {
        String s = "";
        if(isSubObject()) s = "." + Integer.toString(subIndex, 16);
        return Integer.toString(index, 16) + s + " - " + parameterName;
    }

//    public int getType()
//    {
//        if(subNodes != null) return STRUCT_NODE;
//        if(subObject) return STRUCT_ITEM;
//        return ALONE;
//    }

    public int getAttrCnt()
    {
        if(fields != null) {
            if(subNumber > 0) return 2; // index node
            return fields.length - attrOffset; // subindex node
        }
        return 0;
    }

    public String getAttrName(int ix)
    {
        if(fields == null) return "<NULL>";
        if(ix >= getAttrCnt()) return "index " + ix + " is out of range (" + getAttrCnt() + ")";
        try {
            return fields[ix + attrOffset].getName();
        } catch (Exception e) {}
        return "Exception occured getting attribute " + fields[ix + attrOffset].getName();
    }

    public String getAttrValue(int ix)
    {
        if(fields == null) return "<NULL>";
        if(ix >= getAttrCnt()) return "index " + ix + " is out of range (" + getAttrCnt() + ")";
        try {
            Field f = fields[ix + attrOffset];
            if(f.getName().equalsIgnoreCase("accessType")) {
                String ret = "";
                if(accessType == ACCES_TYPE_RW) ret = "rw";
                else if(accessType == ACCES_TYPE_WO) ret = "wo";
                else if(accessType == ACCES_TYPE_RO) ret = "ro";
                else if(accessType == ACCES_TYPE_RWR) ret = "rwr";
                else if(accessType == ACCES_TYPE_RWW) ret = "rww";
                else if(accessType == ACCES_TYPE_CONST) ret = "const";
                return ret;
            }
            return f.get(this).toString();
        } catch (Exception e) {}
        return "Exception occured getting attribute " + fields[ix + attrOffset].getName();
    }

    public String getAttrDescription(int ix)
    {
        if(fields == null) return "<NULL>";
        if(ix >= getAttrCnt()) return "index " + ix + " is out of range (" + getAttrCnt() + ")";
        try {
            String ret = "";
            Field f = fields[ix + attrOffset];
            if(f.getName().equalsIgnoreCase("ObjectType")) {
                int n = new Integer(f.get(this).toString()).intValue();
                return objectTypeNames[n];
            }
            if(f.getName().equalsIgnoreCase("DataType")) {
                int n = new Integer(f.get(this).toString()).intValue();
                return dataTypeDefs[n].name + " (" + dataTypeDefs[n].len + ")";
            }
            if(f.getName().equalsIgnoreCase("AccessType")) {
                if(accessType == ACCES_TYPE_RW) ret = "READ/WRITE";
                else if(accessType == ACCES_TYPE_WO) ret = "WRITE ONLY";
                else if(accessType == ACCES_TYPE_RO) ret = "READ ONLY";
                else if(accessType == ACCES_TYPE_RWR) ret = "RW on process input";
                else if(accessType == ACCES_TYPE_RWW) ret = "RW on process output";
                else if(accessType == ACCES_TYPE_CONST) ret = "CONST";
                return ret;
            }
            return "";
        } catch (Exception e) {}
        return "Exception occured getting attribute " + fields[ix + attrOffset].getName();
    }

    public void setAttrValue(int ix, String s)
    {
    }

    public void setAttr(String name, String val)
    {
        name = name.trim();
        val = val.trim();
        if(name.equalsIgnoreCase("ParameterName")) {
            parameterName = val;
        }
        else if(name.equalsIgnoreCase("SubNumber")) {
            subNumber = FString.toInt(val);
        }
        else if(name.equalsIgnoreCase("objectType")) {
            objectType = FString.toInt(val);
        }
        else if(name.equalsIgnoreCase("dataType")) {
            dataType = FString.toInt(val);
        }
        else if(name.equalsIgnoreCase("accessType")) {
            accessType = 0;
            val = val.toLowerCase();
            if(val.equalsIgnoreCase("rw")) accessType = ACCES_TYPE_RW;
            else if(val.equalsIgnoreCase("ro")) accessType = ACCES_TYPE_RO;
            else if(val.equalsIgnoreCase("wo")) accessType = ACCES_TYPE_WO;
            else if(val.equalsIgnoreCase("rwr")) accessType = ACCES_TYPE_RWR;
            else if(val.equalsIgnoreCase("rww")) accessType = ACCES_TYPE_RWW;
            else if(val.equalsIgnoreCase("const")) accessType = ACCES_TYPE_CONST;
        }
        else if(name.equalsIgnoreCase("pdoMapping")) {
            pdoMapping = (FString.toInt(val) == 0)? false: true;
        }
        else if(name.equalsIgnoreCase("lowLimit")) {
            lowLimit = val;
        }
        else if(name.equalsIgnoreCase("highLimit")) {
            highLimit = val;
        }
        else if(name.equalsIgnoreCase("defaultValue")) {
            defaultValue = val;
        }
    }

    public String valToString()
    {
        byte[] val = getValue();
        //if(value == null) return "value not loaded";
        String s = "";
        for(int i = 0; i < val.length; i++) {
            if(i > 0) s += " ";
            s += FString.byte2Hex(val[i]);
        }
        return s;
    }

    /**
     * convert '0xdddddddd' to byte[], 0xddddd is hexadecimal number, msb is first in returned array
     */
    public static byte[] string2ValArray(String s)
    {
        String s1 = FString.slice(s, 2); // cut 0x
        ArrayList al = new ArrayList(8);
        while(s1.length() > 0) {
            String s2 = FString.slice(s1, -2);
            s1 = FString.slice(s1, 0, -2);
            int i = FString.toInt(s2, 16);
            al.add(new Integer(i));
        }
        byte[] ret = new byte[al.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = ((Integer)al.get(i)).byteValue();
        }
        return ret;
    }

    /**
     * convert 'nn nn nn nn nn' to short[], nn are hexadecimal numbers
     */
    public static byte[] string2ValArray2(String s)
    {
        ArrayList lst = new ArrayList(8);
        String ss[];
        while(true) {
            ss = StringParser.cutInt(s, 16); s = ss[1];
            if(ss[0].trim().length() == 0) break;
            lst.add(new Short((short) FString.toInt(ss[0], 16)));
        }
        byte[] ret = new byte[lst.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = ((Short)lst.get(i)).byteValue();
        }
        return ret;
    }

    public byte[] getValue()
    {
        if(value == null) value = string2ValArray(defaultValue);
        return value;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.<p>
     *
     * In the foregoing description, the notation
     * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
     * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
     * <tt>0</tt>, or <tt>1</tt> according to whether the value of <i>expression</i>
     * is negative, zero or positive.
     *
     * The implementor must ensure <tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
     * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
     * <tt>y.compareTo(x)</tt> throws an exception.)<p>
     *
     * The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
     * <tt>x.compareTo(z)&gt;0</tt>.<p>
     *
     * Finally, the implementer must ensure that <tt>x.compareTo(y)==0</tt>
     * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
     * all <tt>z</tt>.<p>
     *
     * It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
     * class that implements the <tt>Comparable</tt> interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     *
     * @param   o the Object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *		is less than, equal to, or greater than the specified object.
     *
     * @throws ClassCastException if the specified object's type prevents it
     *         from being compared to this Object.
     */
    public int compareTo(Object o)
    {
        ODNode on = (ODNode) o;
        // objests with same indexes are equal
        if(index < on.index) return -1;
        if(index > on.index) return 1;
        return 0;
        /*
        if(index < on.index) return -1;
        if(index > on.index) return 1;
        if(!isSubObject() && subObjectCnt() == 0) return 0;
        if(subIndex < on.subIndex) return -1;
        if(subIndex > on.subIndex) return 1;
        return 0;
        */
    }
}
