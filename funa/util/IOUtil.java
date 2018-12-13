import java.io.*;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.io.VFSFile;
import org.gjt.sp.jedit.io.VFS;

public class IOUtil {
  
  public static File searchAndCopyFile(Buffer buffer, String fileName) throws Exception {
    VFS vfs = buffer.getVFS();
    Object session = vfs.createVFSSession(buffer.getPath(), null);
    File temporayFile = null;
    
    try {
      String parent = MiscUtilities.getParentOfPath(buffer.getPath());
      String findFile = searchFile(vfs, session, parent, fileName);
      
      if (findFile == null) {
        return null;
      }
      return copyToTemporary(vfs, session, findFile);
    } finally {
      vfs._endVFSSession(session, null);
    }
  }
  
  public static String searchFile(VFS vfs, Object session, String dir, String fileName) throws Exception {
    VFSFile[] list = vfs._listFiles(session, dir, null);
    
    for(VFSFile file: list) {
      if (file.getType() == VFSFile.FILE && fileName.equals(file.getName())) {
        return file.getPath();
      }
    }
    
    String parent = MiscUtilities.getParentOfPath(dir);
    if (parent.equals(dir)) return null;
    return searchFile(vfs, session, parent, fileName);
  }
  
  public static File copyToTemporary(VFS vfs, Object session, String filePath) throws Exception {
    if (filePath == null) {
      return null;
    }
    File configFile = File.createTempFile("FunaUtil", null);
    InputStream is = null;
    OutputStream os = null;
    try {
      is = vfs._createInputStream(session, filePath, false, null);
      os = new FileOutputStream(configFile);
      BufferedInputStream bis = new BufferedInputStream(is);
      BufferedOutputStream bos = new BufferedOutputStream(os);
      byte[] data = new byte[2048];
      int readLength = 0;
      while( (readLength = bis.read(data, 0, data.length)) >= 0) {
        os.write(data, 0, readLength);
      }
      os.flush();
      return configFile;
    } finally {
      close(os);
      close(is);
      configFile.deleteOnExit();
    }
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
  
}