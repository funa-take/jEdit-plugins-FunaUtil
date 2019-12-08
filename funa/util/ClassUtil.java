package funa.util;

import java.util.HashSet;
import java.util.StringTokenizer;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;
import java.io.FileFilter;

public class ClassUtil {
  private static HashSet<URL> urlset = new HashSet<URL>();
  
  public static void addClassPath(String classpath) {
    StringTokenizer stok = new StringTokenizer(classpath,File.pathSeparator);
    URL url = null;
    
    while (stok.hasMoreTokens()) {
      String spec = stok.nextToken();
      String lowCaseSpec = spec.toLowerCase();
      
      if (File.pathSeparator.indexOf(":") >= 0 && 
        ("http".equals(lowCaseSpec) || "https".equals(lowCaseSpec) || "file".equals(lowCaseSpec))) 
      {
        try {
          url = new URL(spec + File.pathSeparator + stok.nextToken());
        } catch (Exception e){
          e.printStackTrace();
        }
      } else {
        if (lowCaseSpec.startsWith("http:") || lowCaseSpec.startsWith("https:") || lowCaseSpec.startsWith("file:")) 
        {
          try {
            url = new URL(spec);
          } catch (Exception e){
            e.printStackTrace();
          }
        } else {
          File file = new File(spec);
          try {
            url = file.toURL();
          } catch (Exception e){
            e.printStackTrace();
          }
        }
      }
      if (url != null) {
        System.out.println("add: " + url);
        urlset.add(url);
      }
    }
  }
  
  public static void addJars(String path) {
    File dir = new File(path);
    if (!dir.isDirectory()) {
      return;
    }
    
    File[] files = dir.listFiles(new FileFilter() {
        public boolean accept(File file) {
          if (!file.isFile()) {
            return false;
          }
          
          String lowCaseName = file.getName().toLowerCase();
          if (lowCaseName.endsWith(".jar") || lowCaseName.endsWith(".zip")) {
            return true;
          }
          
          return false;
        }
    });
    for (int i = 0; i < files.length; i++) {
      try {
        System.out.println("add: " + files[i].toURL());
        urlset.add(files[i].toURL());
      } catch (Exception e){
        e.printStackTrace();
      }
    }
  }
  
  public static ClassLoader getClassLoader() {
    return new URLClassLoader(
      urlset.toArray(new URL[]{}),
      ClassUtil.class.getClassLoader());
  }
} 