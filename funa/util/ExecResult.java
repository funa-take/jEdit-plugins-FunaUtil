package funa.util;

public class ExecResult {
  protected String stdOut = "";
  protected String stdErr = "";
  protected int exitStatus = 0;
  
  public ExecResult(String stdOut, String stdErr, int exitStatus) {
    this.stdOut = stdOut;
    this.stdErr = stdErr;
    this.exitStatus = exitStatus;
  }
  
  public String getStdOut() {
    return this.stdOut;
  }
  
  public String getStdErr() {
    return this.stdErr;
  }
  
  public int getExitStatus() {
    return this.exitStatus;
  }
} 
