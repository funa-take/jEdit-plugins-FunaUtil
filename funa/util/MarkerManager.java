import java.util.ArrayList;
import java.util.Vector;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.Marker;

public class MarkerManager {
  static private class Data {
    char shortcut;
    int position;
    private Data(Marker marker) {
      this.shortcut = marker.getShortcut();
      this.position = marker.getPosition();
    }
  }
  private ArrayList<Data> markers = new ArrayList<Data>();
  
  public void save(Buffer buffer) {
    markers.clear();
    Vector<Marker> bufferMarkers = buffer.getMarkers();
    for(Marker marker: bufferMarkers) {
      markers.add(new Data(marker));
    }
  }
  
  public void restore(Buffer buffer) {
    for(Data data: markers) {
      buffer.addMarker(data.shortcut, data.position);
    }
  }
}