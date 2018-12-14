package funa.util;

import java.io.*;
import java.nio.file.*;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.io.VFSFile;
import org.gjt.sp.jedit.io.VFS;

public class IOUtil {
  
  public static File createTemporaryDirectory() throws Exception {
    File temporaryDir = Files.createTempDirectory(null).toFile();
    temporaryDir.deleteOnExit();
    
    return temporaryDir;
  }
  
  public static VFSFile searchFile(Buffer buffer, String fileName) throws Exception {
    VFS vfs = buffer.getVFS();
    Object session = vfs.createVFSSession(buffer.getPath(), null);
    try {
      if (session == null) return null;
      
      String parent = MiscUtilities.getParentOfPath(buffer.getPath());
      return _searchFile(vfs, session, parent, fileName);
    } finally {
      if (session != null) {
        vfs._endVFSSession(session, null);
      }
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
  
  public static File copyToDir(Buffer buffer, VFSFile fromFile, File dir) throws Exception {
    if (fromFile == null) {
      return null;
    }
    
    InputStream is = null;
    OutputStream os = null;
    VFS vfs = buffer.getVFS();
    Object session = vfs.createVFSSession(buffer.getPath(), null);
    try {
      if (session == null) return null;
      
      File outFile = new File(dir, fromFile.getName());
      is = vfs._createInputStream(session, fromFile.getPath(), false, null);
      os = new FileOutputStream(outFile);
      copy(is, os);
      return outFile;
    } finally {
      close(os);
      close(is);
      if (session != null) {
        vfs._endVFSSession(session, null);
      }
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