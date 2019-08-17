package modules.data;

public enum IdentifierAttr {
  STATIC("static"),
  FIELD("field"),
  ARG("arg"),
  VAR("var"),
  NONE(null);

  private String rawString;

  IdentifierAttr(String raw) {
    this.rawString = raw;
  }
}
