package funa.util.indent;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.jedit.syntax.DefaultTokenHandler;

public class Tag {
  Pos startPos;
  Pos endPos;
  boolean blnEmptyTag;
  boolean blnStartTag;
  Pos startOfStartTag;
  Pos endOfStartTag;
  Pos startOfEndTag;
  Pos endOfEndTag;
  String tagName;
  DefaultTokenHandler tokenHandler = null;
  
  Tag(Buffer buffer, int startIndex, int endIndex) {
    this(buffer, startIndex, endIndex, true);
  }
  
  Tag(Buffer buffer, int startIndex, int endIndex, boolean isMarkup) {
    tagName = null;
    blnEmptyTag = false;
    blnStartTag = false;
    startPos = new Pos(startIndex);
    endPos = new Pos(startIndex);
    String src = buffer.getText();
    tokenHandler = new DefaultTokenHandler();
    
    int startTagIndex = startIndex - 1;
    while(true) {
      startTagIndex = src.indexOf("<", startTagIndex + 1);
      if (startTagIndex < 0) {
        return;
      }
      if (isMarkup && !isMarkup(buffer, startTagIndex)) {
        continue;
      }
      break;
    }
    
    int endTagIndex = startTagIndex;
    while(true) {
      endTagIndex = src.indexOf(">", endTagIndex + 1);
      if (endTagIndex < 0 || endIndex <= endTagIndex ) {
        return;
      }
      if (isMarkup && !isMarkup(buffer, endTagIndex)) {
        continue;
      }
      break;
    };
    startPos.setPos(startTagIndex);
    endPos.setPos(endTagIndex);
    setTagInfo(src, startTagIndex, endTagIndex);
  }
  
  boolean isMarkup(Buffer buffer, int offset) {
    try {
      int line = buffer.getLineOfOffset(offset);
      int offsetInLine = offset - buffer.getLineStartOffset(line);
      
      tokenHandler.init();
      buffer.markTokens(line, tokenHandler);
      Token token = TextUtilities.getTokenAtOffset(tokenHandler.getTokens(), offsetInLine);
      if (token.id == Token.MARKUP) {
        return true;
      }
    } catch (Exception e) {}
    return false;
  }
  
  void setTagInfo(String src, int start, int end){
    String tagString = src.substring(start, end+1);
    int length = tagString.length();
    int tagNameStart = 1;
    for (int i = tagNameStart; i < length; i++){
      // if (tagString.charAt(i) != ' ') {
      if (!Character.isWhitespace(tagString.charAt(i))) {
        tagNameStart = i;
        break;
      }
    }
    int tagNameEnd = tagNameStart + 1;
    for (int i = tagNameEnd; i < length; i++){
      // if (tagString.charAt(i) == ' ' || i == length-1) {
      if (Character.isWhitespace(tagString.charAt(i)) || i == length-1) {
        tagNameEnd = i;
        break;
      }
    }
    tagName = tagString.substring(tagNameStart, tagNameEnd);
    if (tagName.startsWith("/")){
      blnStartTag = false;
      tagName = tagName.substring(1);
    } else {
      blnStartTag = true;
    }
    
    for (int i = length - 2; i >= 0; i--){
      // if (tagString.charAt(i) != ' '){
      if (!Character.isWhitespace(tagString.charAt(i))){
        if (tagString.charAt(i) == '/') {
          blnEmptyTag = true;
        }
        break;
      }
    }
  }
  
  
  // Tag(int line, int start, int end, String tagName){
  // this.tagName = tagName;
  // setStartInfo(line, start, end);
  // }
  
  boolean isCorrect(){
    return tagName != null;
  }
  
  boolean isEmptyTag(){
    return blnEmptyTag;
  }
  
  boolean isStartTag(){
    return blnStartTag;
  }
  
  void setStartInfo(int start, int end){
    this.startOfStartTag = new Pos(start);
    this.endOfStartTag = new Pos(end);
  }
  
  void setEndInfo(int start, int end){
    this.startOfEndTag = new Pos(start);
    this.endOfEndTag = new Pos(end);
  }
}
