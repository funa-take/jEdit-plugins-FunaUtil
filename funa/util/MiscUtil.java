package funa.util;

import java.util.ArrayList;
import java.io.*;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.Buffer;

public class MiscUtil {
  public static String exec(ArrayList<String> command, String processInput) {
    String encoding = System.getProperty("file.encoding");
    return exec(command, processInput, null, encoding, encoding);
  }
  
  public static String exec(ArrayList<String> command, String processInput, String encoding) {
    return exec(command, processInput, null, encoding, encoding);
  }
  
  public static String exec(ArrayList<String> command, String processInput, ArrayList<String> envp, String outEncoding, String inEncoding) {
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
      Process p = runtime.exec(commandArray, envpArray);
      
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
      
    } catch (Exception e){
      e.printStackTrace();
    } finally {
      IOUtil.close(pbr);
      IOUtil.close(pbe);
      IOUtil.close(pbw);
    }
    
    return result.toString();
  }
  
  public static boolean format(TextArea textArea, ArrayList<String> command) {
    String encoding = System.getProperty("file.encoding");
    return format(textArea, command, null, encoding, encoding);
  }
  
  public static boolean format(TextArea textArea, ArrayList<String> command, String encoding) {
    return format(textArea, command, null, encoding, encoding);
  }
  
  public static boolean format(TextArea textArea, ArrayList<String> command, ArrayList<String> envp, String outEncoding, String inEncoding) {
    Selection[] sel = textArea.getSelection();
    int startIndex = 0;
    int endIndex = textArea.getText().length();
    if (sel.length > 0) {
      startIndex = sel[0].getStart();
      endIndex = sel[0].getEnd();
    }
    
    textArea.selectNone();
    try {
      String source = textArea.getText().substring(startIndex, endIndex);
      String result = exec(command, source, envp, outEncoding, inEncoding);
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
  
}