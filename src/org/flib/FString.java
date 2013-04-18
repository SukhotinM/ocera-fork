package org.flib;

/**
 * @author ocera
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class FString 
{
	protected String fstr;

    public static final boolean THROW_EXC = true;

	public FString() {fstr = "";}
	public FString(String s) {fstr = s;}
	public FString(FString fs) {fstr = fs.fstr;}

	public String toString() {return fstr;}

    /**
     * @see org.flib.FString#slice(String s, int from, int to)
     * @return slice(str, from, Integer.MAX_VALUE);
     */
	public static String slice(String s, int from)
	{
        return slice(s, from, Integer.MAX_VALUE);
    }

    /**
     * @param from beggining position of slice (included), can be negative, slice is counted from the end of string.
     * @param to   ending position of slice (not included), can be negative, slice is counted from the end of string.
     * @return slice of str beginning on position from and ending on position to.<br>
     *         returned string is exactly the same as the Python's slice() returns
     */
    public static String slice(String s, int from, int to)
    {
        int len = s.length();
        if (from < 0) from += len;
        if (to < 0) to += len;
        if (from < 0) from = 0;
        if (to > len) to = len;
        if (to <= from) return "";
        return s.substring(from, to);
    }

    /**
     * @return length of string
     */
    public int len()
    {
        return fstr.length();
    }

    /**
     * @param ix position (can be negative)
     * @return char at position ix from begining of string. If ix is negative counts chars from end of the string.
     *         If ix falls out of string bounds, returns 0
     */
    static public char charAt(String s, int ix)
    {
        int l = s.length();
        if (ix < 0) ix = l + ix;
        if (ix < 0) return 0;
        if (ix >= l) return 0;
        return s.charAt(ix);
    }
    /**
     * @param ix position (can be negative)
     * @return char at position ix from begining of string. If ix is negative counts chars from end of the string.
     *         If ix falls out of string bounds, returns 0
     */
    public char charAt(int ix)
    {
        return fstr.charAt(ix);
    }

    /**
     * try to convert content of FString to inteeger value<br>
     * string can have format dddd or 0xhhhh or 0oooo
     * @param throw_exc if true function throws NumberFormatException
     * @return value if successful
     */
    public static int toInt(String s, boolean throw_exc)
    {
        if(throw_exc) {
            return Integer.decode(s).intValue();
        }
        else try {
            return Integer.decode(s).intValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * try to convert content of FString to inteeger value
     * @return value or 0 if not successful
     */
    public static int toInt(String s) {return toInt(s, false);}

    public static int toInt(String s, int radix) {return toInt(s, radix, false);}

    public static int toInt(String s, int radix, boolean throw_exc)
    {
        s = s.trim();
        if(throw_exc) {
            return Integer.parseInt(s, radix);
        }
        else try {
            return Integer.parseInt(s, radix);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * converts int val to hexadecimal string, function do not prepend 0x
     * @param val
     * @return converted val
     */
    public static String int2HexStr(int val)
    {
        return Integer.toString(val, 16);
    }

    /**
     * converts int val to hexadecimal string of defined length, function do not prepend 0x.
     * If string is shorter than len, the '0' chars are prepended.
     * @param val
     * @param len
     * @return converted val
     */
    public static String int2HexStr(int val, int len)
    {
        String s = FString.rep("0", len) + int2HexStr(val);
        int l = s.length();
        return s.substring(l-len, l);
    }

    /**
     * create count times repetition of what
     * @param what
     * @param count
     */
    public static String rep(String what, int count)
    {
        String s = "";
        for(int i=0; i<count; i++) s += what;
        return s;
    }

    public static String byte2Hex(byte val)
    {
        String s = "";
        int v = val;
        if(v < 0) v += 256;
        int d = v/16;
        if(d >= 0xA) s += (char)(d-0xA + 'A'); else s += (char)(d + '0');
        d = v%16;
        if(d >= 0xA) s += (char)(d-0xA + 'A'); else s += (char)(d + '0');
        return s;
        //return Integer.toString( ( val & 0xff ) + 0x100, 16 /* radix */ ) .substring( 1 );
    }

    public static String bytes2String(byte[] bytes)
    {
        String s = "";
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if(i > 0) s += " ";
            if(b > ' ' && b < 'z') s += " " + (char)b;
            else s += byte2Hex(b);
        }
        return s;
    }

}

