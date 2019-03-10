package funa.util;

import java.util.ArrayList;
import java.io.*;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSFile;
import org.gjt.sp.jedit.io.FileVFS;
import org.gjt.sp.jedit.MiscUtilities;

public class MiscUtil {
  public static String exec(ArrayList<String> command, String processInput) throws IOException {
    String encoding = System.getProperty("file.encoding");
    return exec(command, processInput, null, null, encoding, encoding);
  }
  
  public static String exec(ArrayList<String> command, String processInput, File workDir) throws IOException {
    String encoding = System.getProperty("file.encoding");
    return exec(command, processInput, null, workDir, encoding, encoding);
  }
  
  public static String exec(ArrayList<String> command, String processInput, String encoding) throws IOException {
    return exec(command, processInput, null, null, encoding, encoding);
  }
  
  public static String exec(ArrayList<String> command, String processInput, ArrayList<String> envp, File workDir, String outEncoding, String inEncoding) throws IOException {
    String lineSep = "\n";
    BufferedReader pbr = null;
    BufferedReader pbe = null;
    BufferedWriter pbw = null;
    StringBuffer result = new StringBuffer();
    
    try {
      String[] commandArray = new String[command.size()];
      command.toArray(commandArray);
      
      String[] envpArray = null;
      if (envp != null) {
        envpArray = new String[envp.size()];
        envp.toArray(envpArray);
      }
      
      Runtime runtime = Runtime.getRuntime();
      Process p = runtime.exec(commandArray, envpArray, workDir);
      
      pbw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), outEncoding));
      pbr = new BufferedReader(new InputStreamReader(p.getInputStream(), inEncoding));
      pbe = new BufferedReader(new InputStreamReader(p.getErrorStream(), inEncoding));
      
      pbw.write(processInput);
      pbw.flush();
      pbw.close();
      
      String line = null;
      while ( (line = pbr.readLine()) != null){
        result.append(line);
        result.append(lineSep);
      }
      pbr.close();
      
      while ( (line = pbe.readLine()) != null){
        System.err.println(line);
      }
      pbr.close();
      
    } finally {
      IOUtil.close(pbr);
      IOUtil.close(pbe);
      IOUtil.close(pbw);
    }
    
    return result.toString();
  }
  
  public static boolean format(TextArea textArea, ArrayList<String> command) {
    String encoding = System.getProperty("file.encoding");
    return format(textArea, command, null, null, encoding, encoding);
  }
  
  public static boolean format(TextArea textArea, ArrayList<String> command, File workDir) {
    String encoding = System.getProperty("file.encoding");
    return format(textArea, command, null, workDir, encoding, encoding);
  }
  
  public static boolean format(TextArea textArea, ArrayList<String> command, String encoding) {
    return format(textArea, command, null, null, encoding, encoding);
  }
  
  public static boolean format(TextArea textArea, ArrayList<String> command, ArrayList<String> envp, File workDir, String outEncoding, String inEncoding) {
    return format(textArea, command, envp, workDir, outEncoding, inEncoding, null);
  }
  
  public static boolean format(TextArea textArea, ArrayList<String> command, ArrayList<String> envp, File workDir, String outEncoding, String inEncoding, String configFileName) {
    Selection[] sel = textArea.getSelection();
    int startIndex = 0;
    int endIndex = textArea.getText().length();
    if (sel.length > 0) {
      startIndex = sel[0].getStart();
      endIndex = sel[0].getEnd();
    }
    try {
      textArea.selectNone();
      String source = textArea.getText().substring(startIndex, endIndex);
      String result = "";
      
      if (configFileName == null) {
        result = exec(command, source, envp, workDir, outEncoding, inEncoding);
      } else {
        Buffer buffer = (Buffer)textArea.getBuffer();
        result = execWithConfig(buffer, command, source, envp, configFileName, outEncoding, inEncoding);
      }
      
      if (!result.equals("")){
        int caretPos = textArea.getCaretPosition();
        int caretLine = textArea.getCaretLine();
        int endPos = textArea.getLineEndOffset(caretLine);
        
        Buffer buffer = (Buffer)textArea.getBuffer();
        MarkerManager mm = new MarkerManager();
        mm.save(buffer);
        
        buffer.remove(startIndex, endIndex - startIndex);
        buffer.insert(startIndex, result);
        
        buffer.removeAllMarkers();
        mm.restore(buffer);
        
        if (caretLine < textArea.getLineCount()) {
          int newPos = textArea.getLineEndOffset(caretLine) - (endPos - caretPos);
          if (newPos > 0 && newPos < textArea.getText().length()){
            textArea.setCaretPosition(newPos);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    
    return true;
  }
  
  public static boolean formatWithConfig(TextArea textArea, ArrayList<String> command, String configFileName) {
    String encoding = System.getProperty("file.encoding");
    return format(textArea, command, null, null, encoding, encoding, configFileName);
  }
  
  public static String execWithConfig(Buffer buffer, ArrayList<String> command, String processInput, ArrayList<String> envp, String configFileName, String outEncoding, String inEncoding) throws Exception {
    Object session = null;
    File tempDir = null;
    VFS vfs = buffer.getVFS();
    
    try {
      File workDir = null;
      session = vfs.createVFSSession(buffer.getPath(), null);
      if (session == null) throw new IOException("Fail createVFSSession");
      
      VFSFile vfsFile = vfs._getFile(session, buffer.getPath(), null);
      if (vfsFile == null || !vfsFile.isReadable()) throw new IOException(buffer.getPath() + "can not read");
      
      if (vfsFile.getClass().equals(FileVFS.LocalFile.class)) {
        workDir = new File(buffer.getPath()).getParentFile();
      } else {
        VFSFile vfsConfigFile = IOUtil.searchFile(MiscUtilities.getParentOfPath(buffer.getPath()), configFileName);
        if (vfsConfigFile != null) {
          tempDir = IOUtil.createTemporaryDirectory();
          File configFile = IOUtil.copyToDir(vfsConfigFile, tempDir);
          configFile.deleteOnExit();
          workDir = tempDir;
        }
      }
      
      System.out.println("Working Directory : " + workDir);
      return exec(command, processInput, envp, workDir, outEncoding, inEncoding);
    } finally {
      IOUtil.deleteDirectory(tempDir);
      if (session != null) vfs._endVFSSession(session, null);
    }
  }
} 