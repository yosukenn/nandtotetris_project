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

  Set<Identifier> table = new HashSet<>();

  /**
   * 新しいサブルーチンのスコープを開始する。<br>
   * （つまり、サブルーチンのシンボルテーブルをリセットする。）<br>
   * TODO シンボルテーブルへの定義と同時に、セグメントへの書き込みも行わなければならないのでは。 シンボルテーブルのインデックスとセグメントのインデックスは必ずしも同値ではない。
   */
  public void startSubroutine(String type) {
    define("this", type, ARG); // サブルーチンを呼び出したインスタンス自身を thisとしてシンボルテーブルに登録する。
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
      case FIELD:
        symbol.setIndex(fieldIndex);
        fieldIndex++;
      case VAR:
        symbol.setIndex(varIndex);
        varIndex++;
      case ARG:
        symbol.setIndex(argumentIndex);
        argumentIndex++;
      case NONE:
    }
    table.add(symbol);
  }

  /** 引数で与えられた属性について、それが現在のスコープで定義されている数を返す。 */
  public long varCount(IdentifierAttr kind) {
    return table.stream().filter(identifier -> identifier.kind == kind).count();
  }

  /**
   * 引数で与えられた名前の識別子を現在のスコープで探し、その属性を返す。<br>
   * その識別子が現在のスコープで見つからなければ、NONEを返す。
   */
  public IdentifierAttr kindOf(String name) {
    return NONE; // 仮
  }

  /** 引数で与えられた名前の識別子を現在のスコープで探し、その型を返す。 */
  public String typeOf(String name) {
    return "int"; // 仮
  }

  /** 引数で与えられた名前の識別子を現在のスコープで探し、そのインデックスを返す。 */
  public int indexOf(String name) {
    return 0; // 仮
  }

  /** 識別子を表すクラス。 */
  private class Identifier {
    private String name;
    private String type;
    private IdentifierAttr kind;
    private int index;

    private Identifier(String name, String type, IdentifierAttr kind) {
      this.name = name;
      this.type = type;
      this.kind = kind;
    }

    private void setIndex(int index) {
      this.index = index;
    }
  }
}
