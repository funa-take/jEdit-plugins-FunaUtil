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

import org.gjt.sp.jedit.View;
import java.util.StringJoiner;
import java.util.Map;
import java.util.Iterator;
import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JOptionPane;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;
import ftp.*;
import com.jcraft.jsch.*;

public class MiscUtilForSsh {
  
  public static class SshExecConnection implements UserInfo {
    private String passphrase = null;
    private ConnectionInfo info = null;
    
    public SshExecConnection(final ConnectionInfo info) throws IOException, JSchException {
      this.info = info;
      
      if (ConnectionManager.client == null)  {
        ConnectionManager.client = new JSch();
      }
      String dir = getUserConfigDir();
      if (dir != null) {
        File f= new File(dir);
        if (!f.exists()) f.mkdir();
        File configFile = new File(getUserConfigFile());
        if (configFile.exists()) {
          ConfigRepository configRepository =
          com.jcraft.jsch.OpenSSHConfig.parseFile(getUserConfigFile());
          ConnectionManager.client.setConfigRepository(configRepository);
        }
        String known_hosts = MiscUtilities.constructPath(getUserConfigDir(), "known_hosts");
        File knownHostsFile = new File(known_hosts);
        if (!knownHostsFile.exists()) {
          knownHostsFile.createNewFile();
        }
        ConnectionManager.client.setKnownHosts(known_hosts);
      }
      JSch.setLogger(new SftpLogger());
    }
    
    public Session createSession() throws IOException, JSchException {
      Proxy proxy = null;
      if (jEdit.getBooleanProperty("vfs.ftp.useProxy")) {
        
        if (jEdit.getBooleanProperty("firewall.socks.enabled", false) ) {
          //Detect SOCKS Proxy
          proxy = new ProxySOCKS5(jEdit.getProperty("firewall.socks.host"), jEdit.getIntegerProperty("firewall.socks.port", 3128));
        } else if (jEdit.getBooleanProperty("firewall.enabled", false)) {
          // HTTP-Proxy detect
          ProxyHTTP httpProxy =  new ProxyHTTP(jEdit.getProperty("firewall.host"), jEdit.getIntegerProperty("firewall.port", 3128) );
          if (!jEdit.getProperty("firewall.user", "").equals(""))
            httpProxy.setUserPasswd(jEdit.getProperty("firewall.user"), jEdit.getProperty("firewall.password"));
          proxy = httpProxy;
        }
      }
      
      Session session = null;
      try {
        
        if (info.user.isEmpty())
          session = ConnectionManager.client.getSession(info.host);
        else
          session = ConnectionManager.client.getSession(info.user, info.host, info.port);
        if (proxy != null)
          session.setProxy(proxy);
        
        Log.log(Log.DEBUG, this, "info.privateKey=" + info.privateKey);
        if (info.privateKey != null && info.privateKey.length()>0) {
          Log.log(Log.DEBUG,this,"Attempting public key authentication");
          Log.log(Log.DEBUG,this,"Using key: "+info.privateKey);
          ConnectionManager.client.addIdentity(info.privateKey);
        }
        session.setUserInfo(this);
        
        if (jEdit.getBooleanProperty("vfs.sftp.compression")) {
          session.setConfig("compression.s2c", "zlib@openssh.com,zlib,none");
          session.setConfig("compression.c2s", "zlib@openssh.com,zlib,none");
          session.setConfig("compression_level", "9");
        }
        
        // Don't lock out user when exceeding bad password attempts on some servers
        session.setConfig("MaxAuthTries", jEdit.getProperty("vfs.sftp.MaxAuthTries", "2"));	// (default was 6)
        
      } finally {
        if (session != null) {
          session.disconnect();
        }
      }
      
      return session;
    }
    
    public String getPassphrase() {
      return passphrase;
    }
    
    public String getPassword()
    {
      return info.password;
    }
    
    public boolean promptPassphrase(String message) {
      Log.log(Log.DEBUG,this,message);
      passphrase = ConnectionManager.getPassphrase(info.privateKey);
      if (passphrase==null)
      {
        
        GUIUtilities.hideSplashScreen();
        PasswordDialog pd = new PasswordDialog(jEdit.getActiveView(),
          jEdit.getProperty("login.privatekeypassword"), message);
        if (!pd.isOK())
          return false;
        passphrase = new String(pd.getPassword());
        ConnectionManager.setPassphrase(info.privateKey,passphrase);
      }
      
      return true;
    }
    
    public boolean promptPassword(String message){ return true;}
    
    static String getUserConfigDir() {
      return MiscUtilities.constructPath(System.getProperty("user.home"), ".ssh");
    }
    
    static String getUserConfigFile() {
      String defaultValue = MiscUtilities.constructPath(getUserConfigDir(), "config");
      return jEdit.getProperty("ssh.config", defaultValue);
    }
    
    public boolean promptYesNo(final String message) {
      final int ret[] = new int[1];
      try
      {
        Runnable runnable = new Runnable()
        {
          public void run()
          {
            Object[] options = {"yes", "no"};
            ret[0] = JOptionPane.showOptionDialog(jEdit.getActiveView(),
              message,
              "Warning",
              JOptionPane.DEFAULT_OPTION,
              JOptionPane.WARNING_MESSAGE,
              null, options, options[0]);
          }
        };
        if (EventQueue.isDispatchThread())
        {
          runnable.run();
        }
        else
        {
          EventQueue.invokeAndWait(runnable);
        }
      }
      catch (InterruptedException e)
      {
        Log.log(Log.ERROR, this, e);
      }
      catch (InvocationTargetException e)
      {
        Log.log(Log.ERROR, this, e);
      }
      return ret[0]==0;
    }
    
    public void showMessage(final String message) {
      Log.log(Log.ERROR, this, message);
    }
  }
  
  public static String exec(String host, List<String> commands, String processInput) throws IOException, JSchException {
    String encoding = System.getProperty("file.encoding");
    return exec(host, commands, processInput, null, encoding, encoding);
  }
  
  public static String exec(String host, List<String> commands, String processInput, String encoding) throws IOException, JSchException {
    return exec(host, commands, processInput, null, encoding, encoding);
  }
  
  public static String exec(String host, List<String> commands, String processInput, Map<String, String> envp, String outEncoding, String inEncoding) throws IOException, JSchException {
    String lineSep = "\n";
    BufferedReader pbr = null;
    BufferedReader pbe = null;
    BufferedWriter pbw = null;
    StringBuffer result = new StringBuffer();
    ChannelExec channel = null;
    Session session = null;
    
    try {
      StringJoiner sj = new StringJoiner(" ");
      Iterator<String> iterator = commands.iterator();
      while(iterator.hasNext()) {
        sj.add(iterator.next());
      }
      String command = sj.toString();
      
      View view = jEdit.getActiveView();
      FtpAddress ad = new FtpAddress("sftp://" + host);
      ConnectionInfo info = ConnectionManager.getConnectionInfo(view, ad, true);
      SshExecConnection connection = new SshExecConnection(info);
      session = connection.createSession();
      session.connect();
      
      channel = (ChannelExec)session.openChannel("exec");
      
      if (envp != null) {
        iterator = envp.keySet().iterator();
        while(iterator.hasNext()) {
          String key = iterator.next();
          String value = envp.get(key);
          channel.setEnv(key, value);
        }
      }
      
      channel.setCommand(command);
      
      pbw = new BufferedWriter(new OutputStreamWriter(channel.getOutputStream(), outEncoding));
      pbr = new BufferedReader(new InputStreamReader(channel.getInputStream(), inEncoding));
      pbe = new BufferedReader(new InputStreamReader(channel.getErrStream(), inEncoding));
      
      channel.connect();
      channel.setAgentForwarding(true);
      
      pbw.write(processInput);
      pbw.flush();
      pbw.close();
      
      String line = null;
      while( (line = pbr.readLine()) != null) {
        result.append(line);
        result.append(lineSep);
      }
      pbr.close();
      
      while( (line = pbe.readLine()) != null) {
        System.err.println(line);
      }
      pbe.close();
    } finally {
      IOUtil.close(pbr);
      IOUtil.close(pbe);
      IOUtil.close(pbw);
      if (session != null) {
        session.disconnect();
      }
      if (channel != null) {
        channel.disconnect();
      }
    }
    return result.toString();
  }
  
  public static boolean format(String host, TextArea textArea, List<String> command) {
    String encoding = System.getProperty("file.encoding");
    return format(host, textArea, command, null, encoding, encoding);
  }
  
  public static boolean format(String host, TextArea textArea, List<String> command, String encoding) {
    return format(host, textArea, command, null, encoding, encoding);
  }
  
  public static boolean format(String host, TextArea textArea, List<String> command, Map<String, String> envp, String outEncoding, String inEncoding) {
    
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
      
      result = exec(host, command, source, envp, outEncoding, inEncoding);
      
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