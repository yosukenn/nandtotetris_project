package modules.data;

import java.util.Arrays;

public enum IdentifierAttr {
  STATIC("static"),
  FIELD("field"),
  ARG("arg"),
  VAR("var"),
  NONE(null);

  private String code;

  public static IdentifierAttr fromCode(String code) {
    IdentifierAttr[] attrs = values();
    return Arrays.stream(attrs).filter(attr -> attr.code.equals(code)).findFirst().orElseThrow();
  }

  IdentifierAttr(String raw) {
    this.code = raw;
  }
}
