package funa.util;

import java.io.*;
import java.nio.file.*;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSFile;
import org.gjt.sp.jedit.io.VFSManager;

public class IOUtil {
  
  public static File createTemporaryDirectory() throws Exception {
    File temporaryDir = Files.createTempDirectory(null).toFile();
    temporaryDir.deleteOnExit();
    
    return temporaryDir;
  }
  
  public static VFSFile createFile(String path) throws Exception {
    VFS vfs = VFSManager.getVFSForPath(path);
    Object session = vfs.createVFSSession(path, null);
    
    try {
      VFSFile file = vfs._getFile(session, path, null);
      // ファイルが存在する場合は処理中止
      if (file != null) return null;
      
      // ファイル生成
      vfs._createOutputStream(session, path, null).close();
      return vfs._getFile(session, path, null);
      
    } finally {
      vfs._endVFSSession(session, null);
    }
  }
  
  public static OutputStream createOutputStream(String path) throws Exception {
    VFS vfs = VFSManager.getVFSForPath(path);
    Object session = vfs.createVFSSession(path, null);
    
    try {
      VFSFile file = vfs._getFile(session, path, null);
      // ファイルが存在する場合は処理中止
      if (file != null) return null;
      
      return vfs._createOutputStream(session, path, null);
      
    } finally {
      vfs._endVFSSession(session, null);
    }
  }
  
  public static InputStream createInputStream(String path) throws Exception {
    VFS vfs = VFSManager.getVFSForPath(path);
    Object session = vfs.createVFSSession(path, null);
    
    try {
      VFSFile file = vfs._getFile(session, path, null);
      // ファイルが存在しない場合は処理中止
      if (file == null) return null;
      
      return vfs._createInputStream(session, path, true, null);
      
    } finally {
      vfs._endVFSSession(session, null);
    }
  }
  
  public static String readFile(String path, String encoding) throws Exception {
    InputStream is = null;
    StringBuilder sb = new StringBuilder();
    try {
      is = createInputStream(path);
      if (is == null) return "";
      
      BufferedReader br = new BufferedReader(new InputStreamReader(is, encoding));
      String line = null;
      while( (line = br.readLine()) != null) {
        sb.append(line).append("\n");
      }
      
      return  sb.toString();
    } finally {
      close(is);
    }
  }
  
  public static String createCopy(Buffer buffer, String prefix) throws Exception {
    String path = buffer.getPath();
    String filepath = MiscUtilities.constructPath(MiscUtilities.getParentOfPath(path), prefix + buffer.getName());
    String encoding = buffer.getStringProperty(Buffer.ENCODING);
    OutputStream os = null;
    try {
      os = createOutputStream(filepath);
      if (os == null) {
        return null;
      }
      PrintWriter pw = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(os), encoding));
      pw.print(buffer.getText());
      pw.flush();
      return filepath;
      
    } finally {
      funa.util.IOUtil.close(os);
    }
  }
  
  public static VFSFile searchFile(String path, String fileName) throws Exception {
    VFS vfs = VFSManager.getVFSForPath(path);
    Object session = vfs.createVFSSession(path, null);
    if (session == null) throw new IOException("Fail createVFSSession");
    
    try {
      return _searchFile(vfs, session, path, fileName);
    } finally {
      vfs._endVFSSession(session, null);
    }
  }
  
  private static VFSFile _searchFile(VFS vfs, Object session, String dir, String fileName) throws Exception {
    VFSFile[] list = vfs._listFiles(session, dir, null);
    
    for(VFSFile file: list) {
      if (file.getType() == VFSFile.FILE && fileName.equals(file.getName())) {
        return file;
      }
    }
    
    String parent = MiscUtilities.getParentOfPath(dir);
    VFSFile vfsFile = vfs._getFile(session, parent, null);
    if (vfsFile == null) return null;
    if (!vfsFile.isReadable()) return null;
    if (parent.equals(dir)) return null;
    return _searchFile(vfs, session, parent, fileName);
  }
  
  public static File copyToDir(VFSFile fromFile, File dir) throws Exception {
    InputStream is = null;
    OutputStream os = null;
    
    VFS vfs = fromFile.getVFS();
    Object session = vfs.createVFSSession(fromFile.getPath(), null);
    if (session == null) throw new IOException("Fail createVFSSession");
    
    try {
      File outFile = new File(dir, fromFile.getName());
      is = vfs._createInputStream(session, fromFile.getPath(), false, null);
      os = new FileOutputStream(outFile);
      copy(is, os);
      return outFile;
    } finally {
      close(os);
      close(is);
      vfs._endVFSSession(session, null);
    }
  }
  
  public static void copy(InputStream is, OutputStream os) throws Exception {
    BufferedInputStream bis = new BufferedInputStream(is);
    BufferedOutputStream bos = new BufferedOutputStream(os);
    byte[] data = new byte[2048];
    int readLength = 0;
    while( (readLength = bis.read(data, 0, data.length)) >= 0) {
      os.write(data, 0, readLength);
    }
    os.flush();
  }
  
  public static void close(Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (Exception e) {
    }
  }
  
  public static void delete(File file) {
    try {
      if (file != null) {
        file.delete();
      }
    } catch (Exception e) {
    }
  }
  
  public static boolean delete(String path) {
    VFS vfs = VFSManager.getVFSForPath(path);
    Object session = vfs.createVFSSession(path, null);
    
    try {
      return vfs._delete(session, path, null);
    } catch (IOException e) {
      return false;
    } finally {
      try {
        vfs._endVFSSession(session, null);
      } catch (Exception e){}
    }
  }
  
  public static boolean deleteDirectory(File dir) {
    if (dir == null) return false;
    if (!dir.isDirectory()) return false;
    if (!dir.canWrite()) return false;
    
    File[] files = dir.listFiles();
    if (files == null) return false;
    
    for(File file: files) {
      if (file.isDirectory()) {
        deleteDirectory(file);
      } else {
        file.delete();
      }
    }
    return dir.delete();
  }
  
}