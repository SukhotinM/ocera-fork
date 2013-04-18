package ocera.rtcan.eds;

/**
 * @author vacek
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */

public class EdsAttribute
{
	public String key = "";
	public String value = "";
	public String comment = "";

    public EdsAttribute(String key) {this.key = key;}
    public EdsAttribute(String key, String val) {this.key = key; this.value = val;}

	public boolean equals(Object obj)
	{
		if(obj instanceof EdsAttribute) {
			if(((EdsAttribute)obj).key == key) return true;
		}
		return false;
	}

	public String toString()
	{
		return key + '=' + value;
	}

	public String[] toStrings()
	{
		String[] sa = {key, value, comment};
		return sa;
	}

}

