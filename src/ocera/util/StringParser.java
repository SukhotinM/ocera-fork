package ocera.util;

/**
 * @author vacek
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
import java.util.*;

public class StringParser {
	
	public char fieldDelimiter = ',';
	public char fieldQuote = '"';
	protected boolean trimFields = true;

//--------------------------------------------------------------
	public StringParser (char fielddelimiter, char fieldquote) 
	{
		fieldDelimiter = fielddelimiter;
		fieldQuote = fieldquote;
	}
	public StringParser (char fielddelimiter) {this(fielddelimiter, '\0');}
	public StringParser () {this('\t', '\0');}

	//--------------------------------------------------------------
	/**
	 * Method split.
	 * @param s - splitted string
	 * @return String[] - an array containing delimited parts of s
	 */
	public String[] split(String s)
	{
		ArrayList al = new ArrayList();
		String[] sl;
		
		do{
			sl = cut(s);
			al.add(sl[0]);
			s = sl[1];
		} while(s.length() > 0);

		return (String[]) al.toArray(new String[2]);
	}

	/**
	 * Method cut.
	 * @param s - cutted string
	 * @return String[]<br>
	 * s[0] contains before delimiter part of the s<br>
	 * s[1] contains after delimiter part of the s<br>
	 */
	public String[] cut(String s)
	{
		boolean inquotes = false;
		char c;
		int slen = s.length(), i;
		
		for (i = 0; i < slen; i++) {
			c = s.charAt(i);
			if (c == fieldQuote) {
				inquotes = !inquotes;
			} 
			else if (c == fieldDelimiter && !inquotes) {
				break;
			}
		}	
		String[] sa = new String[2];
		sa[0] = s.substring(0, i++);
		if(i < slen) sa[1] = s.substring(i);
		else sa[1] = "";
		if(trimFields) sa[0] = sa[0].trim();
		// remove quotes if defined
		if(fieldQuote != '\0') {
			s = sa[0].trim();
			if(s.length() > 1) if(s.charAt(0) == fieldQuote) sa[0] = s.substring(1, s.length() - 1);
		}
		return sa;
	}

    /**
     * Method cut.
     * @param s cutted string
     * @param delim field delimiter
     * @return behaves exactly like {@link #cut(String s)} but it sets fieldDelimiter to the <code>delim</code> before cutting.
     */
    public String[] cut(String s, char delim)
    {
        fieldDelimiter = delim;
        return cut(s);
    }
//--------------------------------------------------------------
	public String toString(String[] sa)
	{
		String s = "";
		for (int i = 0; i < sa.length; i++) {
			if (i > 0) s += fieldDelimiter;
			if (fieldQuote != '\0') s += fieldQuote;
			s += sa[i];
			if (fieldQuote != '\0') s += fieldQuote;
		}
		return s;		
	}
	
	/**
	 * Sets the fieldDelimiter.
	 * @param fieldDelimiter The fieldDelimiter to set
	 */
	public void setFieldDelimiter(char fieldDelimiter) {
		this.fieldDelimiter = fieldDelimiter;
	}

	/**
	 * Sets the fieldQuote.
	 * @param fieldQuote The fieldQuote to set
	 */
	public void setFieldQuote(char fieldQuote) {
		this.fieldQuote = fieldQuote;
	}

	/**
	 * Sets the trimFields.<br>
	 * if trimFields is set (default) cutted string is trimmed
	 * @param trimFields The trimFields to set
	 */
	public void setTrimFields(boolean trimFields) {
		this.trimFields = trimFields;
	}

    /**
     * Cut string after integer
     *
     * @param s string to be cutted
     * @return array of two strings, first one contains part of str, which can be converted to the int, second one
     * contains rest of the str
     */
    public static String[] cutInt(String s)
    {
        return cutInt(s, 10);
    }

    /**
     * Cut string after integer
     *
     * @param s string to be cutted
     * @param radix set the default number base
     * @return array of two strings, first one contains part of str, which can be converted to the int, second one
     * contains rest of the str
     */
    public static String[] cutInt(String s, int radix)
    {
        char str[] = s.toLowerCase().toCharArray();
        int len = str.length;
        boolean hex = false;
        if(radix == 16) hex = true;
        int start = 0;
        // skip white spaces
        for(start=0; start<len && str[start] <= ' '; start++);
        if(!hex) if(len > start + 2) {
            // check hex number
            if(str[0] == '0' && str[1] == 'x') {start += 2; hex = true;}
        }
        int ix;
        for(ix = 0; ix + start < len; ix++) {
            char c = str[ix + start];
            if(c < '0' || c > '9') {
                if(!hex) break;
                if(c < 'a' || c > 'f') break;
            }
        }
        start += ix;
        if(ix > 0) {
            String[] ret = {s.substring(0, start), s.substring(start)};
            return ret;
        }
        String[] ret = {"", s};
        return ret;
    }
}
//--------------------------------------------------------------


