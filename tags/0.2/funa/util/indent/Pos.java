package funa.util.indent;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.jEdit;

public class Pos {
  private int line = 0;
  private int pos = 0;
  
  Pos(int offset) {
    setPos(offset);
  }
  
  public void setPos(int offset) {
    Buffer buffer = getBuffer();
    
    this.line = buffer.getLineOfOffset(offset);
    int endOffset = buffer.getLineEndOffset(this.line) - 1;
    this.pos = endOffset - offset;
  }
  
  public int getOffset() {
    Buffer buffer = getBuffer();
    
    int startOffset = buffer.getLineStartOffset(this.line); 
    int endOffset = buffer.getLineEndOffset(this.line) - 1;
    int offset = endOffset - pos;
    
    return offset;
  }
  
  public int getLine() {
    return line;
  }
  
  public String toString() {
    return "line = " + line + ", pos = " + pos;
  }
  
  public static Buffer getBuffer() {
    return jEdit.getActiveView().getBuffer();
  }
}
