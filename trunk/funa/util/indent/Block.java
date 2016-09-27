package funa.util.indent;

public class Block {
  private Pos start;
  private Pos end;
  
  Block(Pos start, Pos end) {
    this.start = start;
    this.end = end;
  }
  
  Pos getStart() {
    return start;
  }
    
  Pos getEnd() {
    return end;
  }
}
