package modules;

import static modules.data.IdentifierAttr.*;

import java.util.HashSet;
import java.util.Set;
import modules.data.IdentifierAttr;

public class SymbolTable {

  int staticIndex = 0;

  int fieldIndex = 0;
  int argumentIndex = 0;

  int varIndex = 0;

  public int getStaticIndex() {
    return staticIndex;
  }

  public int getFieldIndex() {
    return fieldIndex;
  }

  Set<Identifier> table = new HashSet<>();

  /**
   * 新しいサブルーチンのスコープを開始する。<br>
   * （つまり、サブルーチンのシンボルテーブルをリセットする。）<br>
   */
  public void startSubroutine(String type) {
    // define("this", type, ARG); // TODO サブルーチンを呼び出したインスタンス自身を thisとしてシンボルテーブルに登録する必要がある？
  }

  /**
   * 引数の名前、型、属性で指定された新しい識別子を定義し、それに実行インデックスを割り当てる。<br>
   * STATICとFIELD属性の識別子はクラスのスコープを持ち、ARGとVAR属性の識別子はサブルーチンのスコープを持つ。
   */
  public void define(String name, String type, IdentifierAttr kind) {
    var symbol = new Identifier(name, type, kind);
    switch (kind) {
      case STATIC:
        symbol.setIndex(staticIndex);
        staticIndex++;
        break;
      case FIELD:
        symbol.setIndex(fieldIndex);
        fieldIndex++;
        break;
      case VAR:
        symbol.setIndex(varIndex);
        varIndex++;
        break;
      case ARG:
        symbol.setIndex(argumentIndex);
        argumentIndex++;
        break;
      case NONE:
        break;
    }
    table.add(symbol);
  }

  /** 引数で与えられた属性について、それが現在のスコープで定義されている数を返す。 */
  public long varCount(IdentifierAttr kind) {
    return table.stream().filter(identifier -> identifier.getKind() == kind).count();
  }

  /**
   * 引数で与えられた名前の識別子を現在のスコープで探し、その属性を返す。<br>
   * その識別子が現在のスコープで見つからなければ、NONEを返す。
   */
  public IdentifierAttr kindOf(String name) {
    return table.stream()
        .filter(identifier -> identifier.getName().equals(name))
        .findFirst()
        .orElse(new Identifier(NONE))
        .getKind();
  }

  /** 引数で与えられた名前の識別子を現在のスコープで探し、その型を返す。 */
  public String typeOf(String name) {
    return "int"; // 仮
  }

  /** 引数で与えられた名前の識別子を現在のスコープで探し、そのインデックスを返す。 */
  public int indexOf(String name) {
    return table.stream()
        .filter(identifier -> identifier.getName().equals(name))
        .findFirst()
        .orElseThrow()
        .getIndex();
  }

  /** 識別子を表すクラス。 */
  private class Identifier {

    private String name;
    private String type;
    private IdentifierAttr kind;
    private int index;

    public String getName() {
      return name;
    }

    public int getIndex() {
      return index;
    }

    public IdentifierAttr getKind() {
      return kind;
    }

    private Identifier(String name, String type, IdentifierAttr kind) {
      this.name = name;
      this.type = type;
      this.kind = kind;
    }

    public Identifier(IdentifierAttr kind) {
      this.kind = kind;
    }

    private void setIndex(int index) {
      this.index = index;
    }
  }
}
