package funa.util;

public class ExecResult {
  protected String stdOut = "";
  protected String stdErr = "";
  
  public ExecResult(String stdOut, String stdErr) {
    this.stdOut = stdOut;
    this.stdErr = stdErr;
  }
  
  public String getStdOut() {
    return this.stdOut;
  }
  
  public String getStdErr() {
    return this.stdErr;
  }
} 
