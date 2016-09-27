package funa.util.indent;

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
  
  Tag(String src, int srcTagIndex, int startIndex) {
    tagName = null;
    blnEmptyTag = false;
    blnStartTag = false;
    startPos = new Pos(startIndex);
    endPos = new Pos(startIndex);
    
    int startTagIndex =  src.indexOf("<",srcTagIndex);
    if (startTagIndex < 0) {
      return;
    }
    
    int endTagIndex = src.indexOf(">", startTagIndex + 1);
    if (startTagIndex < 0) {
      return;
    }
    startPos.setPos(startTagIndex + startIndex);
    endPos.setPos(endTagIndex + startIndex);
    setTagInfo(src, startTagIndex, endTagIndex);
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
