package funa.util;

import java.util.ArrayList;
import java.util.Vector;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.Marker;

public class MarkerManager {
  static private class Data {
    char shortcut;
    int position;
    int line;
    int offsetFromEndOfLine;
    Buffer buffer;
    private Data(Marker marker, Buffer buffer) {
      this.shortcut = marker.getShortcut();
      this.position = marker.getPosition();
      this.buffer = buffer;
      this.line = buffer.getLineOfOffset(this.position);
      this.offsetFromEndOfLine = buffer.getLineEndOffset(this.line) - this.position - 1;
    }
    
    public int getPosition() {
      if (buffer.getLineCount() <= this.line) {
        return buffer.getLength();
      }
      int offset = buffer.getLineEndOffset(this.line) - 1;
      offset = offset - offsetFromEndOfLine;
      if (offset < buffer.getLineStartOffset(this.line)) {
        offset = buffer.getLineStartOffset(this.line);
      }
      return offset;
    }
  }
  private ArrayList<Data> markers = new ArrayList<Data>();
  
  public void save(Buffer buffer) {
    markers.clear();
    Vector<Marker> bufferMarkers = buffer.getMarkers();
    for(Marker marker: bufferMarkers) {
      markers.add(new Data(marker, buffer));
    }
  }
  
  public void restore(Buffer buffer) {
    for(Data data: markers) {
      // int position = data.position <= buffer.getLength() ? data.position : buffer.getLength();
      buffer.addMarker(data.shortcut, data.getPosition());
    }
  }
}