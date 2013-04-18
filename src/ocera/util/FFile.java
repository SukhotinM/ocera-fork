package ocera.util;

/**
 * @author vacek
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */

import java.io.*;
import java.util.*;

public class FFile 
{
    public static final boolean MODE_APPEND = true;
    public static final boolean MODE_OVERWRITE = false;
    public static final boolean OVERWRITE_FILE = true;
    public static final boolean CAN_NOT_OVERWRITE_FILE = false;

	private String fileName;
	
	public FFile(String fname) {fileName = fname;}

//-----------------------------------------------------------------------------	
	public String getFileName() {return fileName;}

//-----------------------------------------------------------------------------
	/**
     * opens file and returns its contens as LinkedList of its lines
     * @return LinkedList of line strings
     * @throws FileNotFoundException
     * @throws IOException
     */
    public LinkedList toStringLList() throws FileNotFoundException, IOException
	{
		BufferedReader reader = null;
		LinkedList al = new LinkedList();
		try {
			reader = new BufferedReader(new FileReader(fileName));
			for(String nextLine = reader.readLine(); nextLine != null; nextLine = reader.readLine()) {
				al.add(nextLine);
			}
		} 
/*			
			catch(FileNotFoundException e) {
				System.err.println(fileName + "File_not_found");
			} 
			catch (IOException e ) {
				System.err.println(fileName + "IO_error_read");
			} 
	*/		
		finally {	
			if(reader != null) {
				try {
					reader.close();
				} catch(IOException e) {
					System.err.println(fileName + " - IO_error_close");
				}
			}
		}
		return al;
	}

//-----------------------------------------------------------------------------
	/**
     * @return contens of the file as a single string
     * @throws FileNotFoundException
     * @throws IOException
     */
    public String asString() throws FileNotFoundException, IOException
	{
		LinkedList ll = toStringLList();
		String s = "";
		for (ListIterator it = ll.listIterator(); it.hasNext();) {
			if(it.hasPrevious()) s += '\n';
			s += (String) it.next();
		}
		return s;
	}
//-----------------------------------------------------------------------------	
	public String[] toStringArray() throws FileNotFoundException, IOException
	{
		LinkedList ll = toStringLList();
        return (String[]) ll.toArray(new String[ll.size()]);
	}

    /**
     * @return true if file on its path exists
     */
    boolean exists()
    {
        return new File(fileName).exists();
    }

    public void writeString(String new_contens, boolean append, boolean can_overwrite) throws IOException, FFileException
    {
        if(!can_overwrite) {
            File f = new File(fileName);
            if(f.exists()) throw new FFileException("File '" + fileName + "' exists.");
        }
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(fileName, append));
        os.write(new_contens.getBytes());
        os.flush();
    }
}

