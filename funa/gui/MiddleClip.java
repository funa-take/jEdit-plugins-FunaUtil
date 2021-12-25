/**
 * startup等で指定する
 * if (jEdit.getPlugin("funa.util.FunaUtilPlugin") != null) {
 *   EditBus.addToBus(new funa.gui.MiddleClip());
 * }
 */

package funa.gui;

import java.awt.*;
import java.lang.reflect.Field;
import javax.swing.*;
import javax.swing.plaf.metal.MetalLabelUI;
import javax.swing.plaf.LabelUI;

import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EBPlugin;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.BufferSwitcher;
import org.gjt.sp.jedit.gui.StatusBar;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.PluginUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.msg.ViewUpdate;


class MetalLabelUIForMiddleClip extends MetalLabelUI {
  protected String layoutCL(
    JLabel label,
    FontMetrics fontMetrics,
    String text,
    Icon icon,
    Rectangle viewR,
    Rectangle iconR,
    Rectangle textR) {
  String clipString = "...";
  int clipLength = clipString.length();
  int clipWidth = fontMetrics.stringWidth(clipString);
  
  int width = viewR.width - viewR.x;
  if (icon != null) {
    width = width - (icon.getIconWidth() + label.getIconTextGap());
  }
  
  // 超える可能性あり
  if (fontMetrics.stringWidth(text) > width ) {
    // System.out.println(viewR);
    // System.out.println(iconR);
    // System.out.println(icon.getIconWidth());
    // System.out.println(label.getIconTextGap());
    
    int middleWidth = (width - clipWidth ) / 2;
    
    int length = text.length();
    int middle = length / 2;
    
    String left = text.substring(0, middle);
    left = cutString(left, fontMetrics, middleWidth);
    
    String right = text.substring(middle, length);
    StringBuilder sb = new StringBuilder(right);
    ;
    right = sb.reverse().toString();
    right = cutString(right, fontMetrics, middleWidth);
    sb.delete(0, sb.length());
    right = sb.append(right).reverse().toString();
    
    sb.delete(0, sb.length());
    sb.append(left).append("...").append(right);
    // System.out.println(left);
    // System.out.println(right);
    text = sb.toString();
  }
  // System.out.println(text);
  
  return SwingUtilities.layoutCompoundLabel(
    (JComponent) label,
    fontMetrics,
    text,
    icon,
    label.getVerticalAlignment(),
    label.getHorizontalAlignment(),
    label.getVerticalTextPosition(),
    label.getHorizontalTextPosition(),
    viewR,
    iconR,
    textR,
    label.getIconTextGap());
    }
    
    protected String cutString(String text, FontMetrics fm, int width) {
      if (width <= 0) {
        return "";
      }
      
      if (fm.stringWidth(text) <= width) {
        return text;
      }
      
      int left = 0;
      int right = text.length();
      while (right - left > 1) {
        int middle = (left + right) / 2;
        int middleWidth = fm.stringWidth(text.substring(0, middle));
        if (width == middleWidth) {
          left = middle;
          break;
        } else if (middleWidth < width) {
          left = middle;
        } else {
          right = middle;
        }
      }
      return text.substring(0, left);
    }
}


public class MiddleClip implements EBComponent {
  protected void setMiddleClipUI(EditPane ep) {
    BufferSwitcher bs = ep.getBufferSwitcher();
    if (bs == null) {
      return;
    }
    
    ListCellRenderer renderer = bs.getRenderer();
    if (!(renderer instanceof JLabel)) {
      return;
    }
    
    LabelUI labelui = ((JLabel)renderer).getUI();
    if (!MetalLabelUI.class.equals(labelui.getClass())) {
      return;
    }
    
    // System.out.println("OK");
    ((JLabel)renderer).setUI(new MetalLabelUIForMiddleClip());
  }
  
  protected void setMiddleClipUI(View view) {
    StatusBar statusbar = view.getStatus();
    Class c = view.getStatus().getClass();
    try {
      Field field = c.getDeclaredField("message");
      field.setAccessible(true);
      Object obj = field.get(statusbar);
      if (!(obj instanceof JLabel)) {
        return;
      }
      JLabel label = (JLabel)obj;
      LabelUI labelui = label.getUI();
      if (!MetalLabelUI.class.equals(labelui.getClass())) {
        return;
      }
      label.setUI(new MetalLabelUIForMiddleClip());
    } catch (Exception e) {}
  }
  
  public void handleMessage( EBMessage msg ) {
    // System.out.println("test " + msg);
    
    if ( msg instanceof ViewUpdate ) {
      ViewUpdate vmsg = (ViewUpdate)msg;
      if (ViewUpdate.CREATED.equals(vmsg.getWhat())) {
        View view = vmsg.getView();
        setMiddleClipUI(view);
        
        EditPane[] eps = view.getEditPanes();
        for (int i = 0; i < eps.length; i++) {
          setMiddleClipUI(eps[i]);
        }
      }
    }
    
    if ( msg instanceof EditPaneUpdate ) {
      // System.out.println("test2");
      EditPaneUpdate emsg = (EditPaneUpdate)msg;
      if (EditPaneUpdate.CREATED.equals(emsg.getWhat())) {
        setMiddleClipUI(emsg.getEditPane());
      }
    }
  }
}
