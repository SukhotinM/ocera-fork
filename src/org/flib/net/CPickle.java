package org.flib.net;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.StringTokenizer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.nio.ByteBuffer;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * class to pickle pickled class packet header
 */
class CPickleHead
{
    /**
     * you should introduce such a <b>public static</b> variable if you vant to pickle arrays,
     * it does not matter where in class declaration it is located.
     */
    final static public String PACKET_HEAD_MAGIC = "CPICKLE_";
    final static public int headMagic_ARRAY_SIZE = PACKET_HEAD_MAGIC.length();
    public byte[] headMagic = PACKET_HEAD_MAGIC.getBytes();
    public byte termZero = 0;
    public int packetSize; // size of packet with head
}

class CPickleClassDef
{
    public String className = "";

    public CPickleClassDef() {}

    public CPickleClassDef(String s)
    {
        this.className = s;
    }
}

class CField
{
    public static String getCPPType(Class type) throws CPickleException
    {
        if(type.isArray()) return getCPPType(type.getComponentType());
        String s = "";
        if(type.equals(java.lang.String.class)) s = "FCPickleString";
        else s = getCType(type);
        return s;
    }

    public static String getCType(Class type) throws CPickleException
    {
        if(type.isArray()) return getCType(type.getComponentType());
        String s = "";
        if(type.equals(char.class)) s = "uint16_t";
        else if(type.equals(byte.class)) s = "int8_t";
        else if(type.equals(short.class)) s = "int16_t";
        else if(type.equals(int.class)) s = "int32_t";
        else if(type.equals(long.class)) s = "int64_t";
        else if(type.equals(double.class)) s = "double";
        else if(type.equals(java.lang.String.class)) s = "const char*";
        else {
            throw new CPickleException("getCType() - unsupported type '" + type + "'");
        }
        return s;
    }

    public static int getCSize(Class type, CPickleOptions opts) throws CPickleException
    {
        if(type.isArray()) {
            throw new CPickleException("getCSize() internal error: type is array");
        }
        int size = 0;
        if(type.equals(char.class)) size = 2;
        else if(type.equals(byte.class)) size = 1;
        else if(type.equals(short.class)) size = 2;
        else if(type.equals(int.class)) size = 4;
        else if(type.equals(long.class)) size = 8;
        else if(type.equals(double.class)) size = 8;
        else if(type.equals(String.class)) {
            // string is prepended by its length and appended by zero
            size = 1; // terminating zero
            size += opts.arraySizeTypeLen;
        }
        else {
            throw new CPickleException("getCSize() - unsupported type '" + type + "'");
        }
        return size;
    }

    public static String getCName(Field fld)
    {
        return fld.getName();
    }

    /**
     * @return C function type for correct conversion of this type to network endian
     */
    public static String getCAlignFncType(Class type) throws CPickleException
    {
        if(type.isArray()) return getCAlignFncType(type.getComponentType());
        String s;
        if(type.equals(char.class)) s = "s";
        else if(type.equals(byte.class)) s = "";
        else if(type.equals(short.class)) s = "s";
        else if(type.equals(int.class)) s = "l";
        else if(type.equals(long.class)) s = "ll";
        else if(type.equals(double.class)) s = "dbl";
        else if(type.equals(String.class)) s = "";
        else {
            throw new CPickleException("getCAlignFnc() - unsupported type '" + type + "'");
        }
        return s;
    }
}

/**
 * Class used to pickle Java class to C structure and send it by a socket.
 * packet structure:<br>
 * <pre>
 * HEAD:
 *   8b CPickleHead.PACKET_HEAD_MAGIC  (currently "CPICKLE_")
 *   1b zero terminator
 *   4b packet length including head
 * OBJECT:
 *   OBJECT_NAME:
 *   4b classname length
 *   ?b classname
 *   1b zero terminator
 *
 *   OBJECT_DATA:
 *   every primitive type is transfered in the network endian (big endian) in binary form.
 *
 *   arrays are transfered as they are in network endian withouth any suplementary data.
 *   NOTE!! if you want to use arrays, you should introduce <b>public static</b> metafield with array size
 *          example
 *          final static public int arrayField_ARRAY_SIZE = 10;
 *          public byte[] arrayField;
 *
 *   String has format | (2/4)b string length (withouth term zero) | string data | 1b terminating zero |
 *   String arrays are not supported till now
 * </pre>
 */
public class CPickle
{
    public static String CPP_EXTENSION = "cc";

    public CPickleHead pickledPacketHead = new CPickleHead();
    public CPickleClassDef pickledClassDef = new CPickleClassDef("");
    public String pickledPacketHead_cname;
    public String pickledClassDef_cname;
    private CPickleOptions pickleOptions;

    public CPickle()
    {
        this(new CPickleOptions(2));
    }
    public CPickle(CPickleOptions opts)
    {
        pickleOptions = opts;

        pickledPacketHead_cname = opts.getCName(pickledPacketHead.getClass());
        pickledClassDef_cname = opts.getCName(pickledClassDef.getClass());

    }

    /**
     * put field fld of object o to ByteBuffer
     * @param bb ByteBuffer to pickle to
     * @param fld field to pickle
     * @param o object which own pickled field
     * @throws CPickleException
     */
    void putToBuffer(ByteBuffer bb, Field fld, Object o, CPickleOptions opts) throws CPickleException
    {
        Class type = fld.getType();
        try {
            if(type.isArray()) {
                int cnt = getCArrayDeclaredLen(fld);
                // put array members
                type = type.getComponentType();
                if(type.equals(byte.class)) {
                    byte[] bytes = (byte[])fld.get(o);
                    if(cnt == 0) {
                        // put arraylen to buffer
                        cnt = bytes.length;
                        if(opts.arraySizeTypeLen == 2) bb.putShort((short)cnt); // length
                        else bb.putInt(cnt);
                    }
                    for(int i=0; i<bytes.length && i<cnt; i++) bb.put(bytes[i]);
                }
                else if(type.equals(int.class)) {
                    int[] ints = (int[])fld.get(o);
                    if(cnt == 0) {
                        cnt = ints.length;
                        if(opts.arraySizeTypeLen == 2) bb.putShort((short)cnt); // length
                        else bb.putInt(cnt);
                    }
                    for(int i=0; i<ints.length && i<cnt; i++) bb.putInt(ints[i]);
                }
                else {
                    throw new CPickleException("putToBuffer() - unsupported array type " + type);
                }
            }
            else if(type.equals(String.class)) {
                String s = (String)fld.get(o);
                if(opts.arraySizeTypeLen == 2)
                    bb.putShort((short) s.length()); // length
                else if(opts.arraySizeTypeLen == 4)
                    bb.putInt(s.length()); // length
                else
                    throw new CPickleException("putToBuffer() - CPickle.arraySizeTypeLen: " + opts.arraySizeTypeLen + " unsupported");
                bb.put(s.getBytes());  // chars
                bb.put((byte) 0);      // teminating zero
            }
            else if(type.equals(char.class)) bb.putChar(fld.getChar(o));
            else if(type.equals(byte.class)) bb.put(fld.getByte(o));
            else if(type.equals(short.class)) bb.putShort(fld.getShort(o));
            else if(type.equals(int.class)) bb.putInt(fld.getInt(o));
            else if(type.equals(long.class)) bb.putLong(fld.getLong(o));
            else if(type.equals(double.class)) bb.putDouble(fld.getDouble(o));
            else {
                throw new CPickleException("putToBuffer() - Field '" + fld.getName() + "': unsupported type '" + type + "'");
            }
        }
        catch (IllegalAccessException e) {
            throw new CPickleException("putToBuffer() - " + e.getMessage());
        }
    }

    /**
     * get field fld of object o from ByteBuffer
     * @param bb ByteBuffer to pickle from
     * @param fld field to read
     * @param o object which own read field
     * @throws CPickleException
     */
    void getFromBuffer(ByteBuffer bb, Field fld, Object o, CPickleOptions opts) throws CPickleException
    {
        Class type = fld.getType();
        try {
            if(type.isArray()) {
                int cnt = getCArrayDeclaredLen(fld);
                if(cnt == 0) {
                    // variable size array
                    if(opts.arraySizeTypeLen == 2) cnt = bb.getShort();
                    else cnt = bb.getInt();
                }
                // get array members
                type = type.getComponentType();
                if(type.equals(byte.class)) {
                    byte[] bytes = (byte[])fld.get(o);
                    bytes = new byte[cnt];
                    for(int i=0; i<bytes.length && i<cnt; i++) bytes[i] = bb.get();
                    fld.set(o, bytes);
                    //System.err.println("setting bytes to field: '" + new String(bytes) + "'");
                }
                else if(type.equals(int.class)) {
                    int[] ints = (int[])fld.get(o);
                    ints = new int[cnt];
                    for (int i = 0; i < ints.length; i++) ints[i] = bb.getInt();
                    fld.set(o, ints);
                }
                else {
                    //System.err.println("why throw ????????????????");
                    throw new CPickleException("getFromBuffer() - unsupported array type " + type);
                }
            }
            else if(type.equals(String.class)) {
                int len = 0;
                if(opts.arraySizeTypeLen == 2)
                    len = bb.getShort(); // length
                else if(opts.arraySizeTypeLen == 4)
                    len = bb.getInt(); // length
                else
                    throw new CPickleException("getFromBuffer() - CPickle.arraySizeTypeLen: " + opts.arraySizeTypeLen + " unsupported");
                byte[] bytes = new byte[len]; bb.get(bytes);  // chars
                String s = new String(bytes); fld.set(o, s);
                bb.get();      // teminating zero
            }
            else if(fld.getType().equals(char.class)) fld.setChar(o, bb.getChar());
            else if(fld.getType().equals(byte.class)) fld.setByte(o, bb.get());
            else if(fld.getType().equals(short.class)) fld.setShort(o, bb.getShort());
            else if(fld.getType().equals(int.class)) fld.setInt(o, bb.getInt());
            else if(fld.getType().equals(long.class)) fld.setLong(o, bb.getLong());
            else if(fld.getType().equals(double.class)) fld.setDouble(o, bb.getDouble());
            else {
                throw new CPickleException("getFromBuffer() - Field '" + fld.getName() + "': unsupported type '" + fld.getType() + "'");
            }
        }
        catch (IllegalAccessException e) {
            throw new CPickleException("getFromBuffer() - " + e.getMessage());
        }
    }
    /*
    int getMinimalBufferSize(Object o) throws CPickleException
    {
        int size = 0;
        size += getPickledObjectSize(pickledPacketHead);
        size += getPickledObjectSize(o);
        return size;
    }
    */
    static Field[] getPicklableFields(Class c)
    {
        Field[] flds = c.getDeclaredFields();
        ArrayList lst = new ArrayList(flds.length);
        for(int i = 0; i < flds.length; i++) {
            Field fld = flds[i];
            // skip non public fields (you do not have an access to it)
            if(!Modifier.isPublic(fld.getModifiers())) continue;
            // skip static fields
            if(Modifier.isStatic(fld.getModifiers())) continue;
            lst.add(fld);
        }
        return (Field[])lst.toArray(new Field[lst.size()]);
    }

    static Field[] getPicklableFinalConstFields(Class c)
    {
        Field[] flds = c.getDeclaredFields();
        ArrayList lst = new ArrayList(flds.length);
        for(int i = 0; i < flds.length; i++) {
            Field fld = flds[i];
            // skip non public fields (you do not have an access to it)
            if(!Modifier.isFinal(fld.getModifiers())) continue;
            if(!Modifier.isStatic(fld.getModifiers())) continue;
            if(!Modifier.isPublic(fld.getModifiers())) continue;
            Class type = fld.getType();
            // only primitive types can be initialized inside C++ class definition
            if(!type.isPrimitive()) continue;
            lst.add(fld);
        }
        return (Field[])lst.toArray(new Field[lst.size()]);
    }

    void putToBuffer(ByteBuffer bb, Object o, CPickleOptions opts) throws CPickleException
    {
        putToBuffer(bb, o.getClass(), o, opts);
    }

    void putToBuffer(ByteBuffer bb, Class c, Object o, CPickleOptions opts) throws CPickleException
    {
        if(c == null) return;
        if(c.equals(Object.class)) return;

        //FLog.log(getClass().toString(), FLog.LOG_TRASH,  "putToBuffer('" + c.getName() + "') - buffer position: " + bb.position() + " size: " + bb.capacity());
        // put superclass
        Class sc = c.getSuperclass();
        putToBuffer(bb, sc, o, opts);
        Field[] flds = getPicklableFields(c);
        for(int i = 0; i < flds.length; i++) {
            putToBuffer(bb, flds[i], o, opts);
        }
    }

    void getFromBuffer(ByteBuffer bb, Object o, CPickleOptions opts) throws CPickleException
    {
        getFromBuffer(bb, o.getClass(), o, opts);
    }

    void getFromBuffer(ByteBuffer bb, Class c, Object o, CPickleOptions opts) throws CPickleException
    {
        if(c == null) return;
        if(c.equals(Object.class)) return;

        Class sc = c.getSuperclass();
        // get superclass
        getFromBuffer(bb, sc, o, opts);
        Field[] flds = getPicklableFields(c);
        for(int i = 0; i < flds.length; i++) {
            getFromBuffer(bb, flds[i], o, opts);
        }
    }

    /**
     * converts object content to byte array in BIG_ENDIAN
     * @param o object to pickle
     * @return
     * @throws CPickleException
     */
    public byte[] toNet(Object o) throws CPickleException
    {
        Class c = o.getClass();
        CPickleClassDef class_name = new CPickleClassDef(c.getName());

        int size = getPickledPacketSize(o);
        pickledPacketHead.packetSize = size;

        byte data[] = new byte[size];
        ByteBuffer bb = ByteBuffer.wrap(data);

        putToBuffer(bb, pickledPacketHead, pickleOptions);
        putToBuffer(bb, class_name, pickleOptions);
        putToBuffer(bb, o, pickleOptions);

        assert bb.position() == size :  "ByteBuffer position: " + bb.position() + " should be size: " + size;
        //if(bb.position() != size) throw new CPickleException("assertion failed");
        return data;
    }

    public Object fromNet(byte[] data) throws CPickleException
    {
        ByteBuffer bb = ByteBuffer.wrap(data);
        return fromNet(bb, pickleOptions);
    }

    /**
     * create new object from data read from byte array
     * @param data_buff data to parse, check data.position() to see how many bytes was processed
     * @return object of type read form data created from data
     * @throws CPickleException
     */
    public Object fromNet(ByteBuffer data_buff, CPickleOptions opts) throws CPickleException
    {
        Object o = null;
        int buffer_entry_position = data_buff.position();
        // check headMagic
        CPickleHead pch = new CPickleHead();
        getFromBuffer(data_buff, pch, opts);
        // check packet headMagic name
        if(!Arrays.equals(pch.headMagic, pickleOptions.headMagic.getBytes())) {
            data_buff.position(buffer_entry_position);
            throw new NotCPickleDataException("Data does not contain valid packet header ('" + CPickleHead.PACKET_HEAD_MAGIC + "')");
        }
        // check terminating zero
        if(pch.termZero != 0) {
            throw new CPickleException("Data header corrupted");
        }
        // get class name
        CPickleClassDef pckstr = new CPickleClassDef("");
        getFromBuffer(data_buff, pckstr, opts);
        // create new instance of class name
        Class c = null;
        try {
            String class_name = pckstr.className;
            c = Class.forName(class_name);
            o = c.newInstance();
            getFromBuffer(data_buff, o, opts);
            return o;
        }
        catch(Exception e) {
            throw new CPickleException(e.getMessage());
        }
    }

    public int getPickledFieldSize(Field fld, Object o) throws CPickleException
    {
        int size = 0;
        Class type = fld.getType();
        if(type.isArray()) {
            if(type.equals(String.class)) {
                throw new CPickleException("getPickledObjectSize() - String arrays are not supported.");
            }
            int len = getCArrayDeclaredLen(fld);
            if(len == 0) {
                // size is not defined, array is of variable length
                try {
                    // find the actual array length
                    Class comp_type = type.getComponentType();
                    if(comp_type.equals(byte.class)) {
                        byte[] array = (byte[])fld.get(o);
                        len = array.length;
                    }
                    else if(type.equals(int.class)) {
                        int[] array = (int[])fld.get(o);
                        len = array.length;
                    }
                    else {
                        throw new CPickleException("getFromBuffer() - unsupported array type " + type);
                    }
                }
                catch (IllegalAccessException e) {
                    throw new CPickleException("getPickledFieldSize() '" + fld.getName() + "' - unsupported array type");
                }
                size = len * CField.getCSize(type.getComponentType(), pickleOptions);
                size += pickleOptions.arraySizeTypeLen;
            }
            else {
                size = len * CField.getCSize(type.getComponentType(), pickleOptions);
            }
        }
        else {
            size = CField.getCSize(type, pickleOptions);
            if(type.equals(String.class)) {
                try {
                    size += ((String)fld.get(o)).length();
                }
                catch (IllegalAccessException e) {
                    throw new CPickleException("getPickledObjectSize() - " + e.getMessage());
                }
            }
        }
        return size;
    }

    int getPickledObjectSize(Object o) throws CPickleException
    {
        return getPickledObjectSize(o, o.getClass());
    }

    /**
     * size of pickled object withouth head and classname
     * @param o pickled object
     * @param c class to pickle to
     * @throws CPickleException
     */
    int getPickledObjectSize(Object o, Class c) throws CPickleException
    {
        int size = 0;
        Field[] flds = getPicklableFields(c);
        // add superclass size
        Class sc = c.getSuperclass();
        if(sc != null && !sc.equals(Object.class)) {
            size += getPickledObjectSize(o, sc);
        }
        for(int i = 0; i < flds.length; i++) {
            size += getPickledFieldSize(flds[i], o);
        }
        return size;
    }

    /**
     * size of pickled packet including head and classname
     * @throws CPickleException
     */
    int getPickledPacketSize(Object o) throws CPickleException
    {
        int size = 0;
        size += getPickledObjectSize(pickledPacketHead);
        pickledClassDef.className = o.getClass().getName();
        size += getPickledObjectSize(pickledClassDef);
        size += getPickledObjectSize(o);
        return size;
    }

    /**
     * reads one class packet (head and class data) from in
     */
    public byte[] readObjectPacket(BufferedInputStream in) throws CPickleException, IOException
    {
        //FLog.log(getClass().getName(), FLog.LOG_TRASH, "ENTERING readObjectPacket()");
        byte packet[] = null;
        CPickleHead head = new CPickleHead();

        int c;
        int head_size = getPickledObjectSize(head);
        ByteBuffer bb = ByteBuffer.allocate(head_size);
        bb.limit(head_size);
        while(bb.position() < bb.limit()) {
            c = in.read();
            if(c < 0) break;
            bb.put((byte)c);
            /*
            FLog.log(getClass().getName(), FLog.LOG_TRASH, "readObjectPacket() bb.position(): " + bb.position() + " head:");
            FLog.log(getClass().getName(), FLog.LOG_TRASH, "data: " + FString.bytes2String(bb.array()));
            */
            if(bb.position() == bb.limit()) {
                bb.position(0);
                getFromBuffer(bb, head, pickleOptions);
                if(Arrays.equals(head.headMagic, pickleOptions.headMagic.getBytes())) {
                    if(head.termZero == 0) break;
                }
                // shift buffer one byte left
                bb.position(1);
                bb.compact();
            }
        }

        // read rest of packet
        packet = new byte[head.packetSize];
        int cnt = bb.limit();
        byte[] a = bb.array();
        for(int i=0; i<cnt; i++) packet[i] = a[i];
        for(; cnt<head.packetSize; cnt++) {
            c = in.read();
            if(c < 0) break;
            packet[cnt] = (byte)c;
        }

        //FLog.log(getClass().getName(), FLog.LOG_TRASH, "readObjectPacket() got packet");
        //FLog.log(getClass().getName(), FLog.LOG_TRASH, "data: " + FString.bytes2String(packet));
        return packet;
    }

    private static void help()
    {
        String s = "\n\nCPickle -c class_names [--use-package-names] [--long-arrays] [-d output_dir]" +
                " [--debug-print] [--cpp-extension ext]\n\n";
        s += "Generates headers needed for C - C++ - Java socket communication.\n";
        s += "class_names - it is a coma separated list of class names to parse.\n";
        s += "creates two files class_name.h and class_name_impl.h in output_dir\n\n";
        s += "-cpp: generate C++ implementation code (default is C).\n";
        s += "--use-package-names: C/C++ type will contains class package name\n";
        s += "\t'.' chars in class_name are replaced by '__'\n\n";
        s += "--long-arrays: int64_t will be used for String and arrays size coding instead of int32_t\n\n";
        s += "--debug-print: include debug printf() to generated code\n\n";
        s += "--cpp-extension ext: use .ext as extension of generated C++ sources (default is cc)\n\n";
        System.out.println(s);
    }

    public static void main(String[] args)
    {
        String class_names = "", s;
        String out_dir = "";
        CPickleOptions opts = new CPickleOptions(2);
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if(arg.equals("-c")) {class_names = args[++i]; continue;}
            if(arg.equals("-cpp")) {opts.generate_cpp_code = true; continue;}
            if(arg.equals("-d")) {out_dir = args[++i]; continue;}
            if(arg.equals("--cpp-extension")) {CPP_EXTENSION = args[++i]; continue;}
            if(arg.equals("-h")) {help(); System.exit(0);}
            if(arg.equals("--use-package-names")) {opts.use_package_names = true; continue;}
            if(arg.equals("--long-arrays")) {opts.setArraySizeTypeLen(4); continue;}
            if(arg.equals("--debug-print")) {opts.debug_print = true; continue;}
        }
        if(class_names.length() == 0) {
            System.err.println("You should specify generated classes (-c switch)");
            help(); System.exit(0);
        }

        CPickle cpck = new CPickle(opts);

        boolean gen_cpp = cpck.pickleOptions.generate_cpp_code;
        if(gen_cpp) {
            // generate fpickle string header
            s = "fcpicklestring";
            writeToFile(cpck.writePickleStringDefinition(s), out_dir, s + ".h");
            writeToFile(cpck.writePickleStringCustDefinition(s), out_dir, s + "_cust.h");
        }
        s = "fcpickle";// if(cpp) s += "_cc";
        writeToFile(cpck.writePickleDefinition(s), out_dir, s + ".h");
        s = "fcpickle_impl"; if(gen_cpp) s += "_cc";
        writeToFile(cpck.writePickleImplementation(s), out_dir, s + ".h");
        class_names = class_names.replaceAll("\\s", ""); // remove all white spaces
        System.out.println("classnames: '" + class_names + "'");
        LinkedList names = new LinkedList();
        for(StringTokenizer stringTokenizer = new StringTokenizer(class_names, ","); stringTokenizer.hasMoreTokens();) {
            String class_name = stringTokenizer.nextToken();
            names.add(class_name);
        }
        // add also CPickleHead and CPickleClassDef service classes
        names.add("org.flib.net.CPickleHead");
        names.add("org.flib.net.CPickleClassDef");
        // generate headers
        for (int i = 0; i < names.size(); i++) {
            String class_name = (String) names.get(i);
            try {
                // parse class and all its superclasses
                Class c = Class.forName(class_name);
                for(; c != null && !c.equals(Object.class); c = c.getSuperclass()) {
                    CClassProps ccp = new CClassProps(c, opts);
                    System.err.println("Parsing: " + c.getName());
                    writeToFile(cpck.writeDefinition(ccp), out_dir, ccp.header_name);
                    writeToFile(cpck.writeImplementation(ccp), out_dir, ccp.impl_name);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                System.err.println(e.getMessage());
            }
        }

        System.exit(0);
    }

    static void writeToFile(String text, String out_dir, String filename)
    {
        if(out_dir.length() > 0) filename = out_dir + "/" + filename;
        System.err.println("Writing: " + filename);
        try {
            FileOutputStream ff = new FileOutputStream(filename);
            ff.write(text.getBytes());
            ff.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check field array length
     * @param fld
     * @return -1 if field is not array<br>
     *         0 if array has variable legth<br>
     *         <b>public static int FieldName_ARRAY_SIZE</b> value if field is an array with declared length.
     * @throws CPickleException
     */
    public int getCArrayDeclaredLen(Field fld) throws CPickleException
    {
        if(!fld.getType().isArray()) return -1; // not an array
        Field meta_fld = getFieldArrayMetadata(fld);
        if(meta_fld == null) return 0;          // variable length
        int size = 0;
        try {
            size = meta_fld.getInt(null);
        }
        catch (IllegalAccessException e) {
            throw new CPickleException("getCArrayDeclaredLen() - " + e.getMessage());
        }
        return size;
    }

    protected Field getFieldArrayMetadata(Field fld)
    {
        if(!fld.getType().isArray()) return null;
        String meta_field_name = getArraySizeFieldMetaName(fld);
        Class c = fld.getDeclaringClass();
        try {
            Field meta_fld = c.getField(meta_field_name);
            return meta_fld;
        }
        catch (NoSuchFieldException e) {
            return null; // variable length
            //throw new CPickleException("getCArrayDeclaredLen() - " + meta_field_name + " not found - " + e.getMessage());
        }
    }

    protected String getArraySizeFieldMetaName(Field fld)
    {
        String s = CField.getCName(fld);
        return s + "_ARRAY_SIZE";
    }

    protected String getArraySizeFieldMetaCName(Field fld, CClassProps ccp)
    {
        String s = getArraySizeFieldMetaName(fld);
        if(!pickleOptions.generate_cpp_code) s = ccp.c_name + "_" + s;
        return s;
    }

    String writePickleStringDefinition(String file_name)
    {
        String s = "";
        s  = "/* AUTOMATICALY GENERATED by org.flib.net.CPickle.java */\n";
        s += "#ifndef " + file_name.toUpperCase() + "_H\n";
        s += "#define " + file_name.toUpperCase() + "_H\n";
        s += "\n";
        s += "//#define FCPICKLE_STRING_CUST\n";
        s += "#ifdef FCPICKLE_STRING_CUST\n";
        s += "// edit fcpicklestring_cust.h file if you want to use other string representation\n";
        s += "// your string must have methods:\n";
        s += "//   constructor YourString(const char*)\n";
        s += "//   int YourString::size() const\n";
        s += "//   ... YourString::operator=(const char*)\n";
        s += "//   char YourString::operator[] const\n";
        s += "\n";
        s += "#include <fcpicklestring_cust.h>\n";
        s += "\n";
        s += "#else //#ifdef FCPICKLE_STRING_CUST\n";
        s += "\n";
        s += "#include <string>\n";
        s += "\n";
        s += "typedef std::string FCPickleString;\n";
        s += "\n";
        s += "#endif //#ifdef FCPICKLE_STRING_CUST\n";
        s += "\n";
        s += "#endif\n";
        return s;
    }

    String writePickleStringCustDefinition(String file_name)
    {
        String s = "/* AUTOMATICALY GENERATED by org.flib.net.CPickle.java */\n" +
            "#ifndef FCPICKLESTRING_CUST_H\n" +
            "#define FCPICKLESTRING_CUST_H\n" +
            "\n" +
            "// edit this file if you want to use other string representation\n" +
            "// your string must have methods:\n" +
            "//   constructor YourString(const char* str=\"\")\n" +
            "//   int YourString::size() const\n" +
            "//   ... YourString::operator=(const char*)\n" +
            "//   char YourString::operator[] const\n" +
            "\n" +
            "// this is custom definition example for QString\n" +
            "\n" +
            "#include <qstring.h>\n" +
            "\n" +
            "struct FCPickleString : public QString\n" +
            "{\n" +
            "    FCPickleString(const char *str=\"\") : QString(str) {}\n" +
            "    int size() {return length();}\n" +
            "#ifdef QT4\n" +
            "    char operator[](int pos) const {return ((const QString*)this)->operator[](pos).toAscii();}\n" +
            "#else\n" +
            "    char operator[](int pos) const {return ((const QString*)this)->operator[](pos);}\n" +
            "#endif\n" +
            "};\n" +
            "\n" +
            "#endif\n";

        return s;
    }

    String writePickleDefinition(String file_name)
    {
        String s = "";
        s  = "/* AUTOMATICALY GENERATED by org.flib.net.CPickle.java */\n";
        s += "#ifndef " + file_name.toUpperCase() + "_H\n";
        s += "#define " + file_name.toUpperCase() + "_H\n";
        s += "\n";
        s += "#include <stdint.h>\n";
        //s += "#include \"cpickleclassdef.h\"\n";
//        s += "\n";
//        s += "#ifdef __cplusplus\n";
//        s += "\n";
//        s += "extern \"C\" {\n";
//        s += "#endif\n";
        s += "\n";
        s += "typedef " + pickleOptions.arraySizeType + " fcpickle_array_size_t;\n";
        //s += "#define fcpickle_packet_head_t " + pickledPacketHead_cname + "_t\n";
        //s += "#define fcpickle_class_def_t " + pickledClassDef_cname + "_t\n";
        s += "\n";
        try {
            s += "#define FCPICKLE_PACKET_HEAD_SIZE " + getPickledObjectSize(pickledPacketHead) + "\n";
        }
        catch (CPickleException e) {
            e.printStackTrace();  //this should newer happen
        }
        /*
        s += "/// get class name form the buffer\n";
        s += "/// @return zero terminated C string or NULL if error\n";
        s += "inline const char* fpickle_getClassName(fpickle_class_name_t *class_def, const uint8_t *buffer, int buff_len) {\n";
        s += "    fpickle_class_def_t cn;\n";
        s += "    if(" + pickledClassDef_cname + "_unpickleObject(&cn, buffer, buff_len) < 0) return NULL;\n";
        s += "    return cn.className;\n";
        s += "}\n";
        s += "\n";
        s += "#ifdef __cplusplus\n";
        s += "}\n";
        s += "#endif\n";
        */
        s += "\n";
        s += "#endif\n";
        return s;
    }

    String writePickleImplementation(String file_name)
    {
        boolean gen_cpp = pickleOptions.generate_cpp_code;
        String s = "";
        s  = "/* AUTOMATICALY GENERATED by org.flib.net.CPickle.java */\n";
        s += "#ifndef " + file_name.toUpperCase() + "_H\n";
        s += "#define " + file_name.toUpperCase() + "_H\n";
        s += "\n";
        s += "#include <netinet/in.h>\n";
        s += "#include <fcpickle.h>\n";
        if(gen_cpp) {
            s += "#include <cpicklehead_cc.h>\n";
            s += "#include <cpickleclassdef_cc.h>\n";
        }
        else {
            s += "#include <cpicklehead.h>\n";
            s += "#include <cpickleclassdef.h>\n";
            s += "\n";
            s += "static inline int16_t n2hs(int16_t n) {return (int16_t)ntohs(n);}\n";
            s += "static inline int16_t h2ns(int16_t n) {return (int16_t)htons(n);}\n";
            s += "static inline int32_t n2hl(int32_t n) {return (int32_t)ntohl(n);}\n";
            s += "static inline int32_t h2nl(int32_t n) {return (int32_t)htonl(n);}\n";
            s += "static inline int64_t n2hll(int64_t n) {return (((int64_t)n2hl(n)) << 32) + n2hl(n >> 32);}\n";
            s += "static inline int64_t h2nll(int64_t n) {return (((int64_t)h2nl(n)) << 32) + h2nl(n >> 32);}\n";
            s += "static inline double n2hdbl(double d) {*(int64_t*)(void*)&d = n2hll(*(int64_t*)(void*)&d); return *(double*)(void*)&d;}\n";
            s += "static inline double h2ndbl(double d) {*(int64_t*)(void*)&d = h2nll(*(int64_t*)(void*)&d); return *(double*)(void*)&d;}\n";
        }
        s += "\n";
        if(gen_cpp) {
            s += "\n";
            s += "// definitions for convinience\n";
            s += "typedef " + pickledPacketHead_cname + " fcpickle_packet_head_t;\n";
            s += "typedef " + pickledClassDef_cname + " fcpickle_class_def_t;\n";
            s += "\n";
            s += "struct FCPickle {\n";
            s += "    /// get packet head form the buffer\n";
            s += "    /// @return number of used bytes or negative value if error\n";
            s += "    static int getHead(fcpickle_packet_head_t &head, const uint8_t *buffer, int buff_len) {\n";
            s += "        return head.unpickle(buffer, buff_len);\n";
            s += "    }\n";
            s += "\n";
            s += "    /// get pickled class definition form the buffer\n";
            s += "    /// @return number of used bytes or negative value if error\n";
            s += "    static int getClassDef(fcpickle_class_def_t &def, const uint8_t *buffer, int buff_len) {\n";
            s += "        return def.unpickle(buffer, buff_len);\n";
            s += "    }\n";
            s += "\n";
            s += "    // use this function if you want to know pickled class name in incomming data\n";
            s += "    static const char* getClassName(const uint8_t *buffer, int buff_len) {\n";
            s += "        if(buff_len <= FCPICKLE_PACKET_HEAD_SIZE) return \"\";\n";
            s += "        return (const char*)(buffer + FCPICKLE_PACKET_HEAD_SIZE + " + pickleOptions.arraySizeTypeLen + ");\n";
            s += "    }\n";
            s += "\n";
            s += "    static int16_t n2hs(int16_t n) {return (int16_t)ntohs(n);}\n";
            s += "    static int16_t h2ns(int16_t n) {return (int16_t)htons(n);}\n";
            s += "    static int32_t n2hl(int32_t n) {return (int32_t)ntohl(n);}\n";
            s += "    static int32_t h2nl(int32_t n) {return (int32_t)htonl(n);}\n";
            s += "    static int64_t n2hll(int64_t n) {return (((int64_t)n2hl(n)) << 32) + n2hl(n >> 32);}\n";
            s += "    static int64_t h2nll(int64_t n) {return (((int64_t)h2nl(n)) << 32) + h2nl(n >> 32);}\n";
            s += "    static double n2hdbl(double d) {*(int64_t*)(void*)&d = n2hll(*(int64_t*)(void*)&d); return *(double*)(void*)&d;}\n";
            s += "    static double h2ndbl(double d) {*(int64_t*)(void*)&d = h2nll(*(int64_t*)(void*)&d); return *(double*)(void*)&d;}\n";
            s += "};\n";
        }
        else {
            s += "typedef " + pickledPacketHead_cname + "_t fcpickle_packet_head_t;\n";
            s += "typedef " + pickledClassDef_cname + "_t fcpickle_class_def_t;\n";
            s += "\n";
            s += "/// get packet head form the buffer\n";
            s += "/// @return number of used bytes or negative value if error\n";
            s += "static inline int fcpickle_getHead(fcpickle_packet_head_t *head, const uint8_t *buffer, int buff_len) {\n";
            s += "    return " + pickledPacketHead_cname + "_unpickleObject(head, buffer, buff_len);\n";
            s += "}\n";
            s += "\n";
            s += "/// get pickled class definition form the buffer\n";
            s += "/// @return number of used bytes or negative value if error\n";
            s += "static inline int fcpickle_getClassDef(fcpickle_class_def_t *def, const uint8_t *buffer, int buff_len) {\n";
            s += "    return " + pickledClassDef_cname + "_unpickleObject(def, buffer, buff_len);\n";
            s += "}\n";
            s += "\n";
            s += "// use this function if you want to know pickled class name in incomming data\n";
            s += "static inline const char* fcpickle_getClassName(const uint8_t *buffer, int buff_len) {\n";
            s += "    if(buff_len <= FCPICKLE_PACKET_HEAD_SIZE) return \"\";\n";
            s += "    return (const char*)(buffer + FCPICKLE_PACKET_HEAD_SIZE + " + pickleOptions.arraySizeTypeLen + ");\n";
            s += "}\n";
        }
        s += "\n";
        //s += "inline int fcpickle_getHeadSize() {return " + pickledPacketHead_cname + "_getObjectBufferSize(NULL);}\n";
        //s += "\n";
        s += "#endif\n";
        return s;
    }

    String writeDefinition(CClassProps ccp) throws CPickleException
    {
        Field[] flds = ccp.picklableFields;
        Field[] final_flds = ccp.picklableFinalFields;
        String s = "";
        s  = "/* AUTOMATICALY GENERATED by org.flib.net.CPickle.java */\n";
        boolean gen_cpp = pickleOptions.generate_cpp_code;
        if(gen_cpp) {
            s += "#ifndef " + ccp.c_name.toUpperCase() + "_CC_H\n";
            s += "#define " + ccp.c_name.toUpperCase() + "_CC_H\n";
            s += "\n";
            s += "#include <fcpicklestring.h>\n";
            s += "#include <fcpickle.h>\n";
            if(ccp.superclass != null) {
                s += "#include <" + ccp.sc_name.toLowerCase() + "_cc.h>\n";
            }
        }
        else {
            s += "#ifndef " + ccp.c_name.toUpperCase() + "_H\n";
            s += "#define " + ccp.c_name.toUpperCase() + "_H\n";
            s += "\n";
            s += "#include <fcpickle.h>\n";
            if(ccp.superclass != null) {
                s += "#include <" + ccp.sc_name.toLowerCase() + ".h>\n";
            }
        }
        s += "\n";
        if(gen_cpp) {
            s += "struct " + ccp.ctype_name;
            if(ccp.superclass != null) {
                s += " : public " + ccp.sc_name;
            }
            s += "\n{\n";
            for(int i = 0; i < final_flds.length; i++) {
                Field fld = final_flds[i];
                s += "    static const " + CField.getCPPType(fld.getType()) + " ";
                s += CField.getCName(fld);
                s += " = ";
                String val;
                try {
                    val = fld.get(null).toString();
                }
                catch (IllegalAccessException e) {
                    throw new CPickleException("writeDefinition() - this should never happen. " + e.getMessage());
                }
                if(fld.getType().equals(String.class)) s += "\"" + val + "\"";
                else                                   s += val;
                s += ";\n";
            }
            s += "\n";
            for(int i = 0; i < flds.length; i++) {
                Field fld = flds[i];
                Class type = fld.getType();
                if(type.isArray()) {
                    int arrcnt = getCArrayDeclaredLen(fld);
                    if(arrcnt == 0) {
                        s += "    " + CField.getCPPType(type) + " ";
                        // variable size array, it is neccessary to introduce new field for array length
                        s += "*" + CField.getCName(fld) + ";\n";
                        s += "    fcpickle_array_size_t " + CField.getCName(fld) + "_arraylen;";
                        s += " // helper field to store/retrieve length of variable size arrays\n";
                    }
                    else {
                        // fixed length array
                        String s1 = CField.getCName(fld); // + "_ARRAY_SIZE";
                        //s += "    static const int " + s1 + " = " + arrcnt + ";\n";
                        s += "    " + CField.getCPPType(type) + " " + s1 + "[" + getArraySizeFieldMetaCName(fld, ccp) + "];\n";
                    }
                }
                //else if(type.equals(String.class)) {
                else {
                    s += "    " + CField.getCPPType(type) + " " + CField.getCName(fld) + ";\n";
                }
            }
            s += "\n";

            s += "    //default constructor\n";
            s += "    " + ccp.ctype_name + "() {_init();}\n";
            s += "\n";
            s += writeClassInitFunction(ccp);
            s += "\n";

            s += "    int getObjectBufferSize();\n";
            s += "    int getPacketBufferSize();\n";
            s += "    int pickle(uint8_t *buffer, int buff_len);\n";
            s += "    int unpickle(const uint8_t *buffer, int buff_len);\n";
            s += "    int toNet(uint8_t *buffer, int buff_len);\n";
            s += "    int fromNet(const uint8_t *buffer, int buff_len);\n";
            s += "};\n"; // end of C++ struct definition
        }
        else {
            s += "#ifdef __cplusplus\n";
            s += "extern \"C\" {\n";
            s += "#endif\n";
            s += "\n";
            for(int i = 0; i < final_flds.length; i++) {
                Field fld = final_flds[i];
                s += "#define  " + ccp.c_name + "_" + CField.getCName(fld) + " ";
                String val;
                try {
                    val = fld.get(null).toString();
                }
                catch (IllegalAccessException e) {
                    throw new CPickleException("writeDefinition() - this should never happen. " + e.getMessage());
                }
                if(fld.getType().equals(String.class)) s += " \"" + val + "\"";
                else                                   s += val;
                s += "\n";
            }
            s += "\n";
            s += "typedef struct " + ccp.ctype_name + "\n";
            s += "{\n";
            if(ccp.superclass != null) {
                s += "// superclass is the first member of the structure\n";
                s += "    " + ccp.sc_name + "_t super;\n";
            }
            for(int i = 0; i < flds.length; i++) {
                Field fld = flds[i];
                Class type = fld.getType();
                if(fld.getType().isArray()) {
                    int arrcnt = getCArrayDeclaredLen(fld);
                    if(arrcnt == 0) {
                        // variable size array, it is neccessary to introduce new field for array length
                        s += "    " + CField.getCType(type) + " ";
                        s += "*" + CField.getCName(fld) + ";\n";
                        s += "    fcpickle_array_size_t " + CField.getCName(fld) + "_arraylen;";
                        s += " // helper field to store/retrieve length of variable size arrays\n";
                    }
                    else {
                        // fixed size array
                        String s1 = CField.getCName(fld);
                        //s += "    #define " + ccp.c_name + "_" + CField.getArraySizeFieldMetaName(s1) + " " + arrcnt + "\n";
                        s += "    " + CField.getCType(type) + " " + s1 + "[" + getArraySizeFieldMetaCName(fld, ccp) + "];\n";
                    }
                }
                else {
                    s += "    " + CField.getCType(type) + " " + CField.getCName(fld) + ";\n";
                }
            }
            s += "} " + ccp.ctype_name + ";\n"; // C++ defines function prototypes inside struct definition
            s += "\n";

            s += "void " + ccp.c_name + "_init(" + ccp.ctype_name + " *o);  //< explicit constructor\n";
            s += "int " + ccp.c_name + "_getObjectBufferSize(" + ccp.ctype_name + " *o);\n";
            s += "int " + ccp.c_name + "_getPacketBufferSize(" + ccp.ctype_name + " *o);\n";
            s += "int " + ccp.c_name + "_pickleObject(" + ccp.ctype_name + " *o, uint8_t *buffer, int buff_len);\n";
            s += "int " + ccp.c_name + "_unpickleObject(" + ccp.ctype_name + " *o, const uint8_t *buffer, int buff_len);\n";
            s += "int " + ccp.c_name + "_toNet(" + ccp.ctype_name + " *o, uint8_t *buffer, int buff_len);\n";
            s += "int " + ccp.c_name + "_fromNet(" + ccp.ctype_name + " *o, const uint8_t *buffer, int buff_len);\n";
            s += "\n";
            s += "#ifdef __cplusplus\n";
            s += "}\n";
            s += "#endif\n";
        }
        s += "\n";
        s += "#endif\n";
        return s;
    }

    String writeClassInitFunction(CClassProps ccp)
    {
        Field flds[] = ccp.picklableFields;
        String ident = "    ";
        String c_this = "";
        String s = "";
        boolean gen_cpp = pickleOptions.generate_cpp_code;
        if(gen_cpp) {
            s += ident + "void _init()\n";
        }
        else {
            ident = "";
            c_this = "o->";
            s += ident + "void ";
            s += ccp.c_name;
            s += "_init(";
            s += ccp.ctype_name + " *o";
            s += ")\n";
        }
        s += ident + "{\n";
        try {
            Object constructor_o = ccp.thisclass.newInstance();
            if(!gen_cpp) {
                // for C structures, super class constructor have to be called explicitly
                if(ccp.superclass != null) {
                    s += "// init superclass\n";
                    s += "    " + ccp.sc_name + "_init(&(o->super));\n";
                }
            }
            for(int i = 0; i < flds.length; i++) {
                Field fld = flds[i];
                Class type = fld.getType();
                if(type.isArray()) {
                    int arrcnt = getCArrayDeclaredLen(fld);
                    if(arrcnt == 0) {
                        // variable size array
                        s += ident + "    " + c_this + CField.getCName(fld) + " = ";
                        s += "NULL;\n";
                        s += ident + "    " + c_this + CField.getCName(fld) + "_arraylen = 0;\n";
                    }
                    else {
                        // fixed length array
                        s += ident + "    {int i; for(i=0; i<" + getArraySizeFieldMetaCName(fld, ccp) +"; i++)\n";
                        s += ident + "        " + c_this + CField.getCName(fld) + "[i] = 0;}\n";
                    }
                }
                else if(type.equals(String.class)) {
                    s += ident + "    " + c_this + CField.getCName(fld) + " = ";
                    s += "\"" + fld.get(constructor_o) + "\";\n";
                }
                else {
                    s += ident + "    " + c_this + CField.getCName(fld) + " = ";
                    s += fld.get(constructor_o) + ";\n";
                }
            }
        }
        catch (InstantiationException e) {
            System.err.println("EXCEPTION: your class '" + e.getMessage() + "' propably does not have the default constructor.");
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (CPickleException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        s += ident + "}\n";
        return s;
    }

    String writeImplementation(CClassProps ccp) throws CPickleException
    {
        boolean gen_cpp = pickleOptions.generate_cpp_code;
        Field[] flds = ccp.picklableFields;
        String s = "";
        s  = "/* AUTOMATICALY GENERATED by org.flib.net.CPickle.java */\n";
        //s += "#ifndef " + ccp.c_name.toUpperCase() + "_IMPL_H\n";
        //s += "#define " + ccp.c_name.toUpperCase() + "_IMPL_H\n";
        s += "\n";
        if(!gen_cpp) {
            s += "#include <netinet/in.h>\n";
            s += "#include <string.h>\n";
        }
        if(pickleOptions.debug_print) s += "#include <stdio.h>\n";
        s += "\n";
        if(gen_cpp) {
            s += "#include <fcpickle_impl_cc.h>\n";
        }
        else {
            s += "#include <fcpickle_impl.h>\n";
        }
        s += "#include <" + ccp.header_name + ">\n";
        s += "\n";

        if(!gen_cpp) {
            s += writeClassInitFunction(ccp);
            s += "\n";
        }
        s += "/// This value varies only if dynamic length arrays or String type are used.\n";
        s += "/// @param o If class does not contain variable length field types (String) or superclass,";
        s += "/// then o can be NULL.\n";
        s += "/// @return size of buffer needed to pickle object only data\n";
        if(gen_cpp) {
            s += "int " + ccp.c_name + "::getObjectBufferSize()\n";
            s += "{\n";
            s += "    int size = 0;\n";
            int size = 0;
            if(ccp.superclass != null) {
                s += "    size += " + ccp.sc_name + "::getObjectBufferSize();\n";
            }
            for(int i = 0; i < flds.length; i++) {
                Field fld = flds[i];
                Class type = fld.getType();

                if(type.isArray()) {
                    if(type.equals(String.class)) {
                        throw new CPickleException("getPickledObjectSize() - String arrays are not supported.");
                    }
                    int len = getCArrayDeclaredLen(fld);
                    if(len == 0) {
                        // variable size array
                        s += "    size += sizeof(fcpickle_array_size_t);\n";
                        s += "    size += " + CField.getCName(fld) + "_arraylen" +
                                " * sizeof(" + CField.getCType(type.getComponentType()) + ");\n";
                    }
                    else {
                        size += len * CField.getCSize(type.getComponentType(), pickleOptions);
                    }
                }
                else {
                    size += CField.getCSize(type, pickleOptions);
                    if(type.equals(String.class)) {
                        s += "    size += " + CField.getCName(fld) + ".size();\n";
                    }
                }
            }
            s += "    size += " + size + ";\n";
            s += "    return size;\n";
            s += "}\n";
        }
        else {
            s += "int " + ccp.c_name + "_getObjectBufferSize(" + ccp.ctype_name + " *o)\n";
            s += "{\n";
            s += "    int size = 0;\n";
            int size = 0;
            if(ccp.superclass != null) {
                s += "    // get superclass size\n";
                s += "    if(!o) return -1;\n";
                s += "    size += " + ccp.sc_name + "_getObjectBufferSize(&(o->super));\n";
            }
            for(int i = 0; i < flds.length; i++) {
                Field fld = flds[i];
                Class type = fld.getType();

                if(type.isArray()) {
                    if(type.equals(String.class)) {
                        throw new CPickleException("getPickledObjectSize() - String arrays are not supported.");
                    }
                    int len = getCArrayDeclaredLen(fld);
                    if(len == 0) {
                        // variable size array
                        s += "    if(!o) return -1;\n";
                        s += "    size += sizeof(fcpickle_array_size_t);\n";
                        s += "    size += o->" + CField.getCName(fld) + "_arraylen" +
                                " * sizeof(" + CField.getCType(type.getComponentType()) + ");\n";
                    }
                    else {
                        size += len * CField.getCSize(type.getComponentType(), pickleOptions);
                    }
                }
                else {
                    size += CField.getCSize(type, pickleOptions);
                    if(type.equals(String.class)) {
                        s += "    size += strlen(o->" + CField.getCName(fld) + ");\n";
                    }
                }
            }
            s += "    size += " + size + ";\n";
            s += "    return size;\n";
            s += "}\n";
        }
        s += "\n";

        s += "/// This value varies only if dynamic length arrays or String type are used\n";
        s += "/// @return size of buffer needed to pickle object data and service data\n";
        if(gen_cpp) {
            s += "int " + ccp.c_name + "::getPacketBufferSize()\n";
            s += "{\n";
            s += "    size_t len = 0;\n";
            s += "    CPickleClassDef class_def;\n";
            s += "    class_def.className = \"" + ccp.thisclass.getName() + "\";\n";
            s += "    len += FCPICKLE_PACKET_HEAD_SIZE;\n";
            s += "    len += class_def.getObjectBufferSize();\n";
            s += "    len += getObjectBufferSize();\n";
            s += "    return len;\n";
            s += "}\n";
            s += "\n";
        }
        else {
            s += "int " + ccp.c_name + "_getPacketBufferSize(" + ccp.ctype_name + " *o)\n";
            s += "{\n";
            s += "    int32_t len = 0;\n";
            s += "    fcpickle_class_def_t class_def;\n";
            s += "    class_def.className = \"" + ccp.thisclass.getName() + "\";\n";
            s += "    len += FCPICKLE_PACKET_HEAD_SIZE;\n";
            s += "    len += " + pickledClassDef_cname + "_getObjectBufferSize(&class_def);\n";
            s += "    len += " + ccp.c_name + "_getObjectBufferSize(o);\n";
            s += "    return len;\n";
            s += "}\n";
            s += "\n";
        }

        if(gen_cpp) {
            s += "int " + ccp.c_name + "::pickle(uint8_t *buffer, int buff_len)\n";
            s += "{\n";
            s += "    size_t len = 0;\n";
            if(pickleOptions.debug_print) {
                s += "    printf(\"ENTERING " + ccp.c_name + "::pickle(o: %p, buffer: %p, buff_len: %i)\\n\", this, buffer, buff_len);\n";
            }
            // pickle superclass
            if(ccp.superclass != null) {
                s += "    // pickle superclass\n";
                s += "    int i = " + ccp.sc_name + "::pickle(buffer+len, buff_len-len);\n";
                s += "    if(i < 0) return i;\n";
                s += "    len += i;\n\n";
            }
            for(int i = 0; i < flds.length; i++) {
                Field fld = flds[i];
                Class jtype = fld.getType();

                String ctype = CField.getCType(jtype);
                String fnc = CField.getCAlignFncType(jtype);
                String cfldname = CField.getCName(fld);
                if(fnc.length() > 0) fnc = "FCPickle::h2n" + fnc;
                s += "\n";
                s += "    // pickle field '" + fld.getName() + "'\n";
                if(jtype.isArray()) {
                    s += "    {\n";
                    s += "        int i;\n";
                    s += "        fcpickle_array_size_t arraylen;\n";
                    ctype = CField.getCType(jtype.getComponentType());
                    int arrcnt = getCArrayDeclaredLen(fld);
                    if(arrcnt == 0) {
                        // variable size array
                        s += "        arraylen = " + cfldname + "_arraylen;\n";
                        s += "        if(arraylen > 1024) arraylen = 1024; // limit max packet size\n";
                        s += "        if(len + sizeof(arraylen) > (size_t)buff_len) return -2;\n";
                        s += "        *(/*packed*/ fcpickle_array_size_t*)(buffer + len) = FCPickle::h2n" + pickleOptions.arraySizeHtonTag + "(arraylen);\n";
                        s += "        len += sizeof(fcpickle_array_size_t);\n";
                    }
                    else {
                        s += "        arraylen = " + arrcnt + ";\n";
                    }
                    s += "        for(i=0; i<arraylen; i++) {\n";
                    s += "            if(len + sizeof(" + ctype + ") > (size_t)buff_len) return -3;\n";
                    s += "            *(/*packed*/ " + ctype + "*)(buffer + len) = " + fnc + "(" + cfldname + "[i]); len += sizeof(" + ctype + ");\n";
                    s += "        }\n";
                    s += "    }\n";
                }
                else if(jtype.equals(String.class)) {
                    s += "    {\n";
                    s += "        fcpickle_array_size_t arraylen;\n";
                    s += "        arraylen = " + cfldname + ".size();\n";
                    s += "        if(arraylen > 1024) arraylen = 1024; // limit max packet size\n";
                    s += "        if(len + arraylen + " + CField.getCSize(jtype, pickleOptions) + " > (size_t)buff_len) return -4;\n";
                    s += "        *(/*packed*/ fcpickle_array_size_t*)(buffer + len) = FCPickle::h2n" + pickleOptions.arraySizeHtonTag + "(arraylen);\n";
                    s += "        len += sizeof(fcpickle_array_size_t);\n";
                    s += "        for(int i=0; i<arraylen; i++)\n";
                    s += "            buffer[len+i] = (uint8_t)((const " + ccp.c_name + "*)this)->" + cfldname + "[i];\n";
                    s += "        len += arraylen;\n";
                    s += "        buffer[len++] = 0;\n";
                    s += "    }\n";
                }
                else {
                    s += "    if(len + sizeof(" + ctype + ") > (size_t)buff_len) return -5;\n";
                    s += "    *(/*packed*/ " + ctype + "*)(buffer + len) = " + fnc + "(" + cfldname + ");\n" +
                         "    len += sizeof(" + ctype + ");\n";
                }
            }
            //s += "    if(len != packetlen) return -6;\n";
            if(pickleOptions.debug_print) {
                s += "    printf(\"EXIT successfully pickled len: %i\\n\", len);\n";
            }
            s += "    return len;\n";
            s += "}\n";
        }
        else { // C version
            s += "int " + ccp.c_name + "_pickleObject(" + ccp.ctype_name + " *o, uint8_t *buffer, int buff_len)\n";
            s += "{\n";
            s += "    int32_t len = 0;\n";
            //s += "    int32_t packetlen = " + ccp.c_name + "_getObjectBufferSize(o);\n";
            if(pickleOptions.debug_print) {
                s += "    printf(\"ENTERING " + ccp.c_name + "_pickleObject(o: %p, buffer: %p, buff_len: %i)\\n\", o, buffer, buff_len);\n";
            }
            // pickle superclass
            if(ccp.superclass != null) {
                s += "    // pickle superclass\n";
                s += "    {\n";
                s += "        int i;\n";
                s += "        i = " + ccp.sc_name + "_pickleObject(&(o->super), buffer+len, buff_len-len);\n";
                s += "        if(i < 0) return i;\n";
                s += "        len += i;\n";
                s += "    }\n";
            }
            for(int i = 0; i < flds.length; i++) {
                Field fld = flds[i];
                Class jtype = fld.getType();

                String ctype = CField.getCType(jtype);
                String fnc = CField.getCAlignFncType(jtype);
                String cfldname = CField.getCName(fld);
                if(fnc.length() > 0) fnc = "h2n" + fnc;
                s += "\n";
                s += "    // pickle field '" + fld.getName() + "'\n";
                if(jtype.isArray()) {
                    s += "    {\n";
                    s += "        int i;\n";
                    s += "        fcpickle_array_size_t arraylen;\n";
                    ctype = CField.getCType(jtype.getComponentType());
                    int arrcnt = getCArrayDeclaredLen(fld);
                    if(arrcnt == 0) {
                        // variable size array
                        s += "        if(!o) return -1;\n";
                        s += "        arraylen = o->" + cfldname + "_arraylen;\n";
                        s += "        if(arraylen > 1024) arraylen = 1024; // limit max packet size\n";
                        s += "        if(len + sizeof(arraylen) > (size_t)buff_len) return -2;\n";
                        s += "        *(/*packed*/ fcpickle_array_size_t*)(buffer + len) = h2n" + pickleOptions.arraySizeHtonTag + "(arraylen);";
                        s += " len += sizeof(fcpickle_array_size_t);\n";
                    }
                    else {
                        s += "        arraylen = " + arrcnt + ";\n";
                    }
                    s += "        for(i=0; i<arraylen; i++) {\n";
                    s += "            if(!o) return -1;\n";
                    s += "            if(len + sizeof(" + ctype + ") > (size_t)buff_len) return -3;\n";
                    s += "            *(/*packed*/ " + ctype + "*)(buffer + len) = " + fnc + "(o->" + cfldname + "[i]); len += sizeof(" + ctype + ");\n";
                    s += "        }\n";
                    s += "    }\n";
                }
                else if(jtype.equals(String.class)) {
                    s += "    {\n";
                    s += "        fcpickle_array_size_t arraylen;\n";
                    s += "        arraylen = strlen(o->" + cfldname + ");\n";
                    s += "        if(arraylen > 1024) arraylen = 1024; // limit max packet size\n";
                    s += "        if(len + arraylen + " + CField.getCSize(jtype, pickleOptions) + " > (size_t)buff_len) return -4;\n";
                    s += "        *(/*packed*/ fcpickle_array_size_t*)(buffer + len) = h2n" + pickleOptions.arraySizeHtonTag + "(arraylen);";
                    s += " len += sizeof(fcpickle_array_size_t);\n";
                    s += "        memcpy(buffer + len, o->" + cfldname + ", ++arraylen);\n";
                    s += "        len += arraylen;\n";
                    s += "    }\n";
                }
                else {
                    s += "    if(len + sizeof(" + ctype + ") > (size_t)buff_len) return -5;\n";
                    s += "    *(/*packed*/ " + ctype + "*)(buffer + len) = " + fnc + "(o->" + cfldname + "); len += sizeof(" + ctype + ");\n";
                }
            }
            //s += "    if(len != packetlen) return -6;\n";
            if(pickleOptions.debug_print) {
                s += "    printf(\"EXIT successfully pickled len: %i\\n\", len);\n";
            }
            s += "    return len;\n";
            s += "}\n";
        }
        s += "\n";

        s += "/// align structure to array and set network endianing\n";
        s += "/// @param buffer sufficient size of buffer is  xx_getBufferSize()\n";
        s += "/// @param buff_len actual size of buffer\n";
        s += "/// @return number of filled bytes or negative value if error\n";
        if(gen_cpp) {
            s += "int " + ccp.c_name + "::toNet(uint8_t *buffer, int buff_len)\n";
            s += "{\n";
            s += "    size_t len = 0;\n";
            s += "    int i;\n";
            s += "    " + pickledPacketHead_cname + " head;\n";
            s += "    " + pickledClassDef_cname + " class_def;\n";
            s += "    // set class definition\n";
            s += "    class_def.className = \"" + ccp.thisclass.getName() + "\";\n";
            s += "    // check buffer size\n";
            s += "    len = getPacketBufferSize();\n";
            s += "    if(len > (size_t)buff_len) return -1;\n";
            s += "    // set packet head\n";
            s += "    memcpy(&(head.headMagic), \"" + pickleOptions.headMagic + "\", " + pickleOptions.headMagic.length() + ");\n";
            s += "    head.termZero = '\\0';\n";
            s += "    head.packetSize = len;\n";
            s += "    len = 0;\n";
            s += "    // pickle all to packet\n";
            s += "    i = head.pickle(buffer+len, buff_len-len);\n";
            s += "    if(i < 0) return i;\n";
            s += "    len += i;\n";
            s += "    i = class_def.pickle(buffer+len, buff_len-len);\n";
            s += "    if(i < 0) return i;\n";
            s += "    len += i;\n";
            s += "    i = pickle(buffer+len, buff_len-len);\n";
            s += "    if(i < 0) return i;\n";
            s += "    len += i;\n";
            s += "    return len;\n";
            s += "}\n";
        }
        else {
            s += "int " + ccp.c_name + "_toNet(" + ccp.ctype_name + " *o, uint8_t *buffer, int buff_len)\n";
            s += "{\n";
            s += "    int32_t len = 0, i;\n";
            s += "    " + pickledPacketHead_cname + "_t head;\n";
            s += "    " + pickledClassDef_cname + "_t class_def;\n";
            s += "    // set class definition\n";
            s += "    class_def.className = \"" + ccp.thisclass.getName() + "\";\n";
            s += "    // check buffer size\n";
            s += "    len = " + ccp.c_name + "_getPacketBufferSize(o);\n";
            s += "    if(len > (size_t)buff_len) return -1;\n";
            s += "    // set packet head\n";
            s += "    memcpy(&(head.headMagic), \"" + pickleOptions.headMagic + "\", " + pickleOptions.headMagic.length() + ");\n";
            s += "    head.termZero = '\\0';\n";
            s += "    head.packetSize = len;\n";
            s += "    len = 0;\n";
            s += "    // pickle all to packet\n";
            s += "    i = " + pickledPacketHead_cname + "_pickleObject(&head, buffer+len, buff_len-len);\n";
            s += "    if(i < 0) return i;\n";
            s += "    len += i;\n";
            s += "    i = " + pickledClassDef_cname + "_pickleObject(&class_def, buffer+len, buff_len-len);\n";
            s += "    if(i < 0) return i;\n";
            s += "    len += i;\n";
            s += "    i = " + ccp.c_name + "_pickleObject(o, buffer+len, buff_len-len);\n";
            s += "    if(i < 0) return i;\n";
            s += "    len += i;\n";
            s += "    return len;\n";
            s += "}\n";
        }
        s += "\n";

        s += "/// load object data from buffer and set host endianing\n";
        s += "/// @param buffer data to read from\n";
        s += "/// NOTE!! char* (String in Java) fields are backuped by buffer (no memory is allocated during loading)\n";
        s += "/// @return number of read bytes or negative value if error\n";
        if(gen_cpp) {
            s += "int " + ccp.c_name + "::unpickle(const uint8_t *buffer, int buff_len)\n";
            s += "{\n";
            s += "    size_t len = 0;\n";
            if(pickleOptions.debug_print) {
                s += "    printf(\"ENTERING " + ccp.c_name + "::unpickle(o: %p, buffer: %p, buff_len: %i)\\n\", this, buffer, buff_len);\n";
            }
            // unpickle superclass
            if(ccp.superclass != null) {
                s += "    // unpickle superclass\n";
                s += "    {\n";
                s += "        int i = " + ccp.sc_name + "::unpickle(buffer+len, buff_len-len);\n";
                s += "        if(i < 0) return i;\n";
                s += "        len += i;\n";
                s += "    }\n";
            }
            for(int i = 0; i < flds.length; i++) {
                Field fld = flds[i];
                Class jtype = fld.getType();
                String ctype = CField.getCType(jtype);
                String fnc = CField.getCAlignFncType(jtype);
                String cfldname = CField.getCName(fld);
                if(fnc.length() > 0) fnc = "FCPickle::n2h" + fnc;

                s += "\n";
                s += "    // unpickle field '" + fld.getName() + "'\n";
                if(jtype.isArray()) {
                    s += "    {\n";
                    s += "        fcpickle_array_size_t arraylen;\n";
                    ctype = CField.getCType(jtype.getComponentType());
                    int arrcnt = getCArrayDeclaredLen(fld);
                    if(arrcnt == 0) {
                        // variable size array
                        s += "        arraylen = FCPickle::n2h" + pickleOptions.arraySizeHtonTag + "(*(/*packed*/ fcpickle_array_size_t*)(buffer + len));\n";
                        s += "        len += sizeof(fcpickle_array_size_t);\n";
                        s += "        " + cfldname + "_arraylen = arraylen;\n";
                        s += "        // unpickled varriable size arrays are backuped by packet buffer (library should alocate dynamic memory in other case)\n";
                        s += "        " + cfldname + " = (/*packed*/ " + ctype + "*)(buffer + len); len += arraylen * sizeof(" + ctype + ");\n";
                    }
                    else {
                        // fixed array
                        s += "        int i;\n";
                        s += "        arraylen = " + arrcnt + ";\n";
                        s += "        for(i=0; i<arraylen; i++) {\n";
                        s += "            if(len + sizeof(" + ctype + ") > (size_t)buff_len) return -3;\n";
                        s += "            // unpickled fixed size arrays are not backuped by packet buffer (data are copied)\n";
                        s += "            " + cfldname + "[i] = " + fnc + "(*(/*packed*/ " + ctype + "*)(buffer + len)); len += sizeof(" + ctype + ");\n";
                        s += "        }\n";
                    }
                    s += "    }\n";
                }
                else if(jtype.equals(String.class)) {
                    s += "    {\n";
                    s += "        fcpickle_array_size_t string_size;\n";
                    s += "        string_size = FCPickle::n2h" + pickleOptions.arraySizeHtonTag + "(*(/*packed*/ fcpickle_array_size_t*)(buffer + len));\n";
                    s += "        len += sizeof(fcpickle_array_size_t);\n";
                    s += "        if(len + string_size + 1 > (size_t)buff_len) return -2;\n";
                    s += "        " + cfldname + " = (const char*)(buffer+len); // using operator=(const char*)\n";
                    s += "        len += ++string_size; // skip terminating zero\n";
                    s += "    }\n";
                }
                else {
                    s += "    if(len + sizeof(" + ctype + ") > (size_t)buff_len) return -4;\n";
                    s += "    " + cfldname + " = " + fnc + "(*(/*packed*/ " + ctype + "*)(buffer + len)); len += sizeof(" + ctype + ");\n";
                }
            }
            //s += "    if(len != packetlen) return -5;\n";
            if(pickleOptions.debug_print) {
                s += "    printf(\"EXIT successfully unpickled len: %i\\n\", len);\n";
            }
            s += "    return len;\n";
        }
        else {
            s += "int " + ccp.c_name + "_unpickleObject(" + ccp.ctype_name + " *o, const uint8_t *buffer, int buff_len)\n";
            s += "{\n";
            s += "    int32_t len = 0;\n";
            if(pickleOptions.debug_print) {
                s += "    printf(\"ENTERING " + ccp.c_name + "_unpickleObject(o: %p, buffer: %p, buff_len: %i)\\n\", o, buffer, buff_len);\n";
            }
            // unpickle superclass
            if(ccp.superclass != null) {
                s += "    // unpickle superclass\n";
                s += "    {\n";
                s += "        int i;\n";
                s += "        i = " + ccp.sc_name + "_unpickleObject(&(o->super), buffer+len, buff_len-len);\n";
                s += "        if(i < 0) return i;\n";
                s += "        len += i;\n";
                s += "    }\n";
            }
            for(int i = 0; i < flds.length; i++) {
                Field fld = flds[i];
                Class jtype = fld.getType();
                String ctype = CField.getCType(jtype);
                String fnc = CField.getCAlignFncType(jtype);
                String cfldname = CField.getCName(fld);
                if(fnc.length() > 0) fnc = "n2h" + fnc;

                s += "\n";
                s += "    // unpickle field '" + fld.getName() + "'\n";
                if(jtype.isArray()) {
                    s += "    {\n";
                    s += "        fcpickle_array_size_t arraylen;\n";
                    ctype = CField.getCType(jtype.getComponentType());
                    int arrcnt = getCArrayDeclaredLen(fld);
                    if(arrcnt == 0) {
                        // variable size array
                        s += "        arraylen = n2h" + pickleOptions.arraySizeHtonTag + "(*(/*packed*/ fcpickle_array_size_t*)(buffer + len));\n";
                        s += "        len += sizeof(fcpickle_array_size_t);\n";
                        s += "        o->" + cfldname + "_arraylen = arraylen;\n";
                        s += "        // unpickled varriable size arrays are backuped by packet buffer (library should alocate dinamic memory in other case)\n";
                        s += "        o->" + cfldname + " = (/*packed*/ " + ctype + "*)(buffer + len); len += arraylen * sizeof(" + ctype + ");\n";
                    }
                    else {
                        s += "        int i;\n";
                        s += "        arraylen = " + arrcnt + ";\n";
                        s += "        for(i=0; i<arraylen; i++) {\n";
                        s += "            if(!o) return -1;\n";
                        s += "            if(len + sizeof(" + ctype + ") > (size_t)buff_len) return -3;\n";
                        s += "            // unpickled fixed size arrays are not backuped by packet buffer (data are copied)\n";
                        s += "            o->" + cfldname + "[i] = " + fnc + "(*(/*packed*/ " + ctype + "*)(buffer + len)); len += sizeof(" + ctype + ");\n";
                        s += "        }\n";
                    }
                    s += "    }\n";
                }
                else if(jtype.equals(String.class)) {
                    s += "    {\n";
                    s += "        fcpickle_array_size_t arraylen;\n";
                    s += "        if(!o) return -1;\n";
                    s += "        arraylen = n2h" + pickleOptions.arraySizeHtonTag + "(*(/*packed*/ fcpickle_array_size_t*)(buffer + len));\n";
                    s += "        len += sizeof(fcpickle_array_size_t);\n";
                    s += "        if(len + arraylen + 1 > (size_t)buff_len) return -2;\n";
                    s += "        o->" + cfldname + " = buffer+len;\n";
                    s += "        len += ++arraylen;\n";
                    s += "    }\n";
                }
                else {
                    s += "    if(len + sizeof(" + ctype + ") > (size_t)buff_len) return -4;\n";
                    s += "    o->" + cfldname + " = " + fnc + "(*(/*packed*/ " + ctype + "*)(buffer + len)); len += sizeof(" + ctype + ");\n";
                }
            }
            //s += "    if(len != packetlen) return -5;\n";
            if(pickleOptions.debug_print) {
                s += "    printf(\"EXIT successfully unpickled len: %i\\n\", len);\n";
            }
            s += "    return len;\n";
        }
        s += "}\n";

        s += "/// load object and service data from buffer and set host endianing\n";
        s += "/// @param buffer data to read from\n";
        s += "/// NOTE!! char* (String in Java) fields are backuped by buffer (no memory is allocated during loading)\n";
        s += "/// @return number of read bytes or negative value if error\n";
        if(gen_cpp) {
            s += "int " + ccp.c_name + "::fromNet(const uint8_t *buffer, int buff_len)\n";
            s += "{\n";
            s += "    size_t len = 0;\n";
            s += "    int i;\n";
            s += "    " + pickledPacketHead_cname + " head;\n";
            s += "    " + pickledClassDef_cname + " class_def;\n";
            s += "    i = head.unpickle(buffer+len, buff_len-len);\n";
            s += "    if(i < 0) return i;\n";
            s += "    len += i;\n";
            s += "    i = class_def.unpickle(buffer+len, buff_len-len);\n";
            s += "    if(i < 0) return i;\n";
            s += "    len += i;\n";
            s += "    i = unpickle(buffer+len, buff_len-len);\n";
            s += "    if(i < 0) return i;\n";
            s += "    len += i;\n";
            s += "    return len;\n";
            s += "}\n";
        }
        else {
            s += "int " + ccp.c_name + "_fromNet(" + ccp.ctype_name + " *o, const uint8_t *buffer, int buff_len)\n";
            s += "{\n";
            s += "    int32_t len = 0, i;\n";
            s += "    " + pickledPacketHead_cname + "_t head;\n";
            s += "    " + pickledClassDef_cname + "_t class_def;\n";
            s += "    i = " + pickledPacketHead_cname + "_unpickleObject(&head, buffer+len, buff_len-len);\n";
            s += "    if(i < 0) return i;\n";
            s += "    len += i;\n";
            s += "    i = " + pickledClassDef_cname + "_unpickleObject(&class_def, buffer+len, buff_len-len);\n";
            s += "    if(i < 0) return i;\n";
            s += "    len += i;\n";
            s += "    i = " + ccp.c_name + "_unpickleObject(o, buffer+len, buff_len-len);\n";
            s += "    if(i < 0) return i;\n";
            s += "    len += i;\n";
            s += "    return len;\n";
            s += "}\n";
        }
        s += "\n";
        //s += "#endif\n";

        return s;
    }

}

class CClassProps
{
    //String head_c_name = ""; // C CPickleHead class name
    String c_name = ""; // C class name
    String sc_name = ""; // C super class name
    String ctype_name;
    Class thisclass;
    Class superclass;
    String header_name;
    String impl_name;
    Field[] picklableFields;
    Field[] picklableFinalFields;

    CClassProps(Class c, CPickleOptions opts)
    {
        assert c != null: "CClassProps() class cann't be NULL";

        thisclass = c;
        superclass = thisclass.getSuperclass();
        if(superclass != null && superclass.equals(Object.class)) superclass = null;

        picklableFields = CPickle.getPicklableFields(c);
        picklableFinalFields = CPickle.getPicklableFinalConstFields(c);

        c_name = opts.getCName(thisclass);
        if(superclass != null) sc_name = opts.getCName(superclass);

        if(opts.generate_cpp_code) {
            ctype_name = c_name;
            header_name = c_name.toLowerCase() + "_cc.h";
            impl_name = c_name.toLowerCase() + "_impl." + CPickle.CPP_EXTENSION;
        }
        else {
            ctype_name = c_name + "_t";
            header_name = c_name.toLowerCase() + ".h";
            impl_name = c_name.toLowerCase() + "_impl.c";
        }
    }

}

class CPickleOptions
{
    public boolean use_package_names = false;
    public boolean generate_cpp_code = false;
    public boolean debug_print = false;
    public String headMagic = CPickleHead.PACKET_HEAD_MAGIC;

    public String arraySizeHtonTag = "l";
    public String arraySizeType = "int32_t";
    /**
     * tells which C type will be used for array and String size coding<br>
     * supported types:<br>
     * 2 (deault) - int16_t will be used - max size of pickled array or String is 0x7FFF, negative values are reserved for NULL<br>
     * 4 - int32_t will be used - max size of pickled array or String is 0x7FFFFFFE<br>
     */
    protected int arraySizeTypeLen = 2;

    public int getArraySizeTypeLen()
    {
        return arraySizeTypeLen;
    }
    public void setArraySizeTypeLen(int len)
    {
        if(len == 2) {
            arraySizeTypeLen = len;
            arraySizeType = "int16_t";
            arraySizeHtonTag = "s";
        }
        else {
            arraySizeTypeLen = 4;
            arraySizeType = "int32_t";
            arraySizeHtonTag = "l";
        }
    }

    public CPickleOptions(int arraySizeTypeLen)
    {
        setArraySizeTypeLen(arraySizeTypeLen);
    }

    public String getCName(Class c)
    {
        String ret = "";
        if(!use_package_names) {
            String s = c.getName();
            // exclude package name from c_name
            int ix = s.lastIndexOf('.');
            if(ix > 0) ret = s.substring(ix + 1);
            else ret = s;
        }
        else {
            ret = c.getName().replaceAll("\\.", "__");
        }
        return ret;
    }
}
