package modules.data;

import java.util.Arrays;

public enum ArithmeticCommand {
  ADD("+"),
  SUB("-"),
  NEG("negate"),
  EQ("="),
  GT(">"),
  LT("<"),
  AND("&"),
  OR("|"),
  NOT("~");

  private String operand;

  public String getOperand() {
    return operand;
  }

  public static ArithmeticCommand fromCode(String code) {
    ArithmeticCommand[] attrs = values();
    return Arrays.stream(attrs).filter(attr -> attr.operand.equals(code)).findFirst().orElseThrow();
  }

  ArithmeticCommand(String raw) {
    this.operand = raw;
  }
}
