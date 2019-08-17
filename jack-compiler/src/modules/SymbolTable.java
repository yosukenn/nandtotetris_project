package modules;

import static modules.data.IdentifierAttr.*;

import modules.data.IdentifierAttr;

public class SymbolTable {

  /** 新しいサブルーチンのスコープを開始する。 （つまり、サブルーチンのシンボルテーブルをリセットする。） */
  public void startSubroutine() {}

  /**
   * 引数の名前、型、属性で指定された新しい識別子を定義し、それに実行インデックスを割り当てる。<br>
   * STATICとFIELD属性の識別子はクラスのスコープを持ち、ARGとVAR属性の識別子はサブルーチンのスコープを持つ。
   */
  public void define(String name, String type, IdentifierAttr kind) {}

  /** 引数で与えられた属性について、それが現在のスコープで定義されている数を返す。 */
  public int varCount(IdentifierAttr kind) {
    return 1; // 仮
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
}
