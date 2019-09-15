package modules.data;

import java.util.Arrays;

public enum Segment {
  CONST("constant"),
  ARG("argument"),
  LOCAL("local"),
  STATIC("static"),
  THIS("this"),
  THAT("that"),
  POINTER("pointer"),
  TEMP("temp");

  private String code;

  public String getCode() {
    return code;
  }

  public static Segment fromCode(String code) {
    Segment[] attrs = values();
    return Arrays.stream(attrs).filter(attr -> attr.code.equals(code)).findFirst().orElseThrow();
  }

  Segment(String raw) {
    this.code = raw;
  }
}
