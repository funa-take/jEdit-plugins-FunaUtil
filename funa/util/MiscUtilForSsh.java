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

import funa.util.MiscUtil.ReadThread;

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
  
  public static ExecResult exec(String host, List<String> commands, String processInput) throws Exception {
    String encoding = System.getProperty("file.encoding");
    return exec(host, commands, processInput, null, encoding, encoding);
  }
  
  public static ExecResult exec(String host, List<String> commands, String processInput, String encoding) throws Exception {
    return exec(host, commands, processInput, null, encoding, encoding);
  }
  
  public static ExecResult exec(String host, List<String> commands, String processInput, Map<String, String> envp, String outEncoding, String inEncoding) throws Exception {
    String lineSep = "\n";
    BufferedReader pbr = null;
    BufferedReader pbe = null;
    BufferedWriter pbw = null;
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
      
      ReadThread stdOut = new ReadThread(pbr);
      stdOut.start();
      ReadThread stdErr = new ReadThread(pbe);
      stdErr.start();
      
      pbw.write(processInput);
      pbw.flush();
      pbw.close();
      
      stdOut.join();
      stdErr.join();
      
      return new ExecResult(stdOut.getReadedString(), stdErr.getReadedString(), channel.getExitStatus());
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
  }
  
  public static ExecResult format(String host, TextArea textArea, List<String> command) throws Exception {
    String encoding = System.getProperty("file.encoding");
    return format(host, textArea, command, null, encoding, encoding);
  }
  
  public static ExecResult format(String host, TextArea textArea, List<String> command, String encoding) throws Exception {
    return format(host, textArea, command, null, encoding, encoding);
  }
  
  public static ExecResult format(String host, TextArea textArea, List<String> command, Map<String, String> envp, String outEncoding, String inEncoding) throws Exception {
    
    Selection[] sel = textArea.getSelection();
    int startIndex = 0;
    int endIndex = textArea.getText().length();
    if (sel.length > 0) {
      startIndex = sel[0].getStart();
      endIndex = sel[0].getEnd();
    }
    
    String source = textArea.getText().substring(startIndex, endIndex);
    ExecResult execResult = execResult = exec(host, command, source, envp, outEncoding, inEncoding);
    
    MiscUtil.format(textArea, execResult.getStdOut(), startIndex, endIndex);
    return execResult;
  }
  
  private static String sftp = "sftp://";
  public static boolean isSftpPath(String path) {
    return path.startsWith(sftp);
  }
  
  public static class SftpInfo {
    private String hostInfo = "";
    private String path = "";
    private String user = "";
    
    public SftpInfo(String path) {
      if (!MiscUtilForSsh.isSftpPath(path)) {
        return;
      }
      int index = path.indexOf("/", sftp.length());
      if (index < 0) {
        return;
      }
      
      hostInfo = path.substring(sftp.length(), index);
      index = hostInfo.indexOf("@");
      if (index >= 0) {
        user = hostInfo.substring(0, hostInfo.indexOf("@"));
      }
      this.path = path.substring(index);
    }
    
    public String getHostInfo() {
      return this.hostInfo;
    }
    public String getPath() {
      return this.path;
    }
    
    public String getUser() {
      return this.user;
    }
  }
} 