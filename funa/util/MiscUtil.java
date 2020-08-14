package funa.util;

import java.util.List;
import java.io.*;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSFile;
import org.gjt.sp.jedit.io.FileVFS;
import org.gjt.sp.jedit.MiscUtilities;

public class MiscUtil {
  public static ExecResult exec(List<String> command, String processInput) throws IOException {
    String encoding = System.getProperty("file.encoding");
    return exec(command, processInput, null, null, encoding, encoding);
  }
  
  public static ExecResult exec(List<String> command, String processInput, File workDir) throws IOException {
    String encoding = System.getProperty("file.encoding");
    return exec(command, processInput, null, workDir, encoding, encoding);
  }
  
  public static ExecResult exec(List<String> command, String processInput, String encoding) throws IOException {
    return exec(command, processInput, null, null, encoding, encoding);
  }
  
  public static ExecResult exec(List<String> command, String processInput, List<String> envp, File workDir, String outEncoding, String inEncoding) throws IOException {
    String lineSep = "\n";
    BufferedReader pbr = null;
    BufferedReader pbe = null;
    BufferedWriter pbw = null;
    StringBuffer stdOut = new StringBuffer();
    StringBuffer stdErr = new StringBuffer();
    
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
        stdOut.append(line);
        stdOut.append(lineSep);
      }
      pbr.close();
      
      while ( (line = pbe.readLine()) != null){
        stdErr.append(line);
        stdErr.append(lineSep);
      }
      pbe.close();
    } finally {
      IOUtil.close(pbr);
      IOUtil.close(pbe);
      IOUtil.close(pbw);
    }
    
    return new ExecResult(stdOut.toString(), stdErr.toString());
  }
  
  public static ExecResult format(TextArea textArea, List<String> command) throws Exception {
    String encoding = System.getProperty("file.encoding");
    return format(textArea, command, null, null, encoding, encoding);
  }
  
  public static ExecResult format(TextArea textArea, List<String> command, File workDir) throws Exception {
    String encoding = System.getProperty("file.encoding");
    return format(textArea, command, null, workDir, encoding, encoding);
  }
  
  public static ExecResult format(TextArea textArea, List<String> command, String encoding) throws Exception {
    return format(textArea, command, null, null, encoding, encoding);
  }
  
  public static ExecResult format(TextArea textArea, List<String> command, List<String> envp, File workDir, String outEncoding, String inEncoding) throws Exception {
    return format(textArea, command, envp, workDir, outEncoding, inEncoding, null);
  }
  
  public static ExecResult format(TextArea textArea, List<String> command, List<String> envp, File workDir, String outEncoding, String inEncoding, String configFileName) throws Exception {
    Selection[] sel = textArea.getSelection();
    int startIndex = 0;
    int endIndex = textArea.getText().length();
    if (sel.length > 0) {
      startIndex = sel[0].getStart();
      endIndex = sel[0].getEnd();
    }
    
    String source = textArea.getText().substring(startIndex, endIndex);
    ExecResult execResult = null;
    
    if (configFileName == null) {
      execResult = exec(command, source, envp, workDir, outEncoding, inEncoding);
    } else {
      Buffer buffer = (Buffer)textArea.getBuffer();
      execResult = execWithConfig(buffer, command, source, envp, configFileName, outEncoding, inEncoding);
    }
    
    format(textArea, execResult.getStdOut(), startIndex, endIndex);
    
    return execResult;
  }
  
  
  public static void format(TextArea textArea, String result) {
    format(textArea, result, 0, textArea.getText().length());
  }
  public static void format(TextArea textArea, String result, int startIndex, int endIndex) {
    textArea.selectNone();
    
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
  }
  
  public static ExecResult formatWithConfig(TextArea textArea, List<String> command, String configFileName) throws Exception {
    String encoding = System.getProperty("file.encoding");
    return format(textArea, command, null, null, encoding, encoding, configFileName);
  }
  
  public static ExecResult execWithConfig(Buffer buffer, List<String> command, String processInput, List<String> envp, String configFileName, String outEncoding, String inEncoding) throws Exception {
    Object session = null;
    File tempDir = null;
    VFS vfs = buffer.getVFS();
    
    try {
      File workDir = null;
      String currentDir = buffer.getDirectory();
      
      VFSFile vfsConfigFile = IOUtil.searchFile(currentDir, configFileName);
      if (vfsConfigFile != null) {
        if (vfsConfigFile.getClass().equals(FileVFS.LocalFile.class)) {
          workDir = new File(MiscUtilities.getParentOfPath(vfsConfigFile.getPath()));
        } else {
          tempDir = IOUtil.createTemporaryDirectory();
          File configFile = IOUtil.copyToDir(vfsConfigFile, tempDir);
          configFile.deleteOnExit();
          workDir = tempDir;
        }
      }
      
      System.out.println("Working Directory : " + (workDir == null ? "." : workDir));
      return exec(command, processInput, envp, workDir, outEncoding, inEncoding);
    } finally {
      IOUtil.deleteDirectory(tempDir);
      if (session != null) vfs._endVFSSession(session, null);
    }
  }
} 
