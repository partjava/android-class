package com.example.toutiao.demo;

import android.util.AtomicFile;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Separate backups; reading never modifies the live SharedPreferences profile. */
public final class ProfileBackup {
    public static final String TEXT_FILE = "profile_backup.txt", XML_FILE = "profile_backup.xml";
    private final File directory;
    public ProfileBackup(File directory) { this.directory = directory; }
    public void writeText(Map<String, String> data) throws IOException {
        validate(data);
        StringBuilder out = new StringBuilder("PROFILE-1\n");
        for (String key : ProfileStore.FIELDS) out.append(key).append('=').append(escape(data.get(key))).append('\n');
        write(TEXT_FILE, out.toString());
    }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r"); }
    private static String unescape(String value) throws IOException {
        StringBuilder out = new StringBuilder();
        for (int i=0;i<value.length();i++) {
            char c=value.charAt(i);
            if(c=='\\') {
                if(++i==value.length()) throw new IOException("文本备份转义损坏");
                c=value.charAt(i);
                if(c=='n') c='\n'; else if(c=='r') c='\r'; else if(c!='\\') throw new IOException("文本备份转义损坏");
            }
            out.append(c);
        }
        return out.toString();
    }
    public Map<String, String> readText() throws IOException {
        Map<String,String> data=new LinkedHashMap<>();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(open(TEXT_FILE),StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)))) {
            if(!"PROFILE-1".equals(reader.readLine())) throw new IOException("文本备份格式损坏");
            String line;
            while((line=reader.readLine())!=null) {
                int split=line.indexOf('='); if(split<1) throw new IOException("文本备份字段损坏");
                String key=line.substring(0,split);
                if(data.put(key,unescape(line.substring(split+1)))!=null) throw new IOException("文本备份字段重复");
            }
        }
        validate(data); return data;
    }
    public void writeXml(Map<String,String> data) throws IOException {
        validate(data);
        StringWriter writer=new StringWriter(); XmlSerializer xml=Xml.newSerializer();
        xml.setOutput(writer); xml.startDocument("UTF-8",true); xml.startTag(null,"profile"); xml.attribute(null,"version","1");
        for(String key:ProfileStore.FIELDS) { xml.startTag(null,"field"); xml.attribute(null,"key",key); xml.text(data.get(key)); xml.endTag(null,"field"); }
        xml.endTag(null,"profile"); xml.endDocument(); write(XML_FILE,writer.toString());
    }
    public Map<String,String> readXml() throws IOException {
        Map<String,String> data=new LinkedHashMap<>();
        try(InputStream input=open(XML_FILE)) {
            XmlPullParser xml=Xml.newPullParser(); xml.setInput(input,"UTF-8");
            xml.nextTag();
            if(!"profile".equals(xml.getName()) || !"1".equals(xml.getAttributeValue(null,"version"))) throw new IOException("XML备份格式损坏");
            while(xml.nextTag()==XmlPullParser.START_TAG) {
                if(!"field".equals(xml.getName())) throw new IOException("XML备份字段损坏");
                String key=xml.getAttributeValue(null,"key"); String value=xml.nextText();
                if(key==null || data.put(key,value)!=null) throw new IOException("XML备份字段重复或缺失");
            }
            if(!"profile".equals(xml.getName()) || xml.next()!=XmlPullParser.END_DOCUMENT) throw new IOException("XML备份尾部损坏");
        } catch(org.xmlpull.v1.XmlPullParserException e) { throw new IOException("XML备份损坏",e); }
        validate(data); return data;
    }
    private static void validate(Map<String,String> data) throws IOException {
        if(data.size()!=ProfileStore.FIELDS.length) throw new IOException("备份字段不完整");
        try { for(String key:ProfileStore.FIELDS) ProfileStore.validate(key,data.get(key)); }
        catch(IllegalArgumentException e) { throw new IOException("备份损坏："+e.getMessage(),e); }
    }
    private InputStream open(String name) throws IOException {
        AtomicFile file=new AtomicFile(new File(directory,name));
        try { return file.openRead(); } catch(FileNotFoundException e) { throw new IOException("备份文件不存在，请先写入："+name,e); }
    }
    private void write(String name,String value) throws IOException {
        if(!directory.exists() && !directory.mkdirs()) throw new IOException("无法创建备份目录");
        AtomicFile file=new AtomicFile(new File(directory,name)); FileOutputStream output=null;
        try { output=file.startWrite(); output.write(value.getBytes(StandardCharsets.UTF_8)); file.finishWrite(output); }
        catch(IOException e) { if(output!=null) file.failWrite(output); throw e; }
    }
}
