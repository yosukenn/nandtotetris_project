package modules;

import modules.data.ArithmeticCommand;
import modules.data.Segment;

/** VMコマンドの構文に従い、VMコマンドをファイルへ書き出す。 */
public class VMWriter {

  /** 新しいファイルを作り、それに書き込む準備をする。 */
  public VMWriter() {}

  /** pushコマンドを書く。 */
  public void writePush(Segment segment, int index) {}

  /** popコマンドを書く。 */
  public void writePop(Segment segment, int index) {}

  /** 算術コマンドを書く。 */
  public void writeArithmetic(ArithmeticCommand command) {}

  /** labelコマンドを書く。 */
  public void writeLabel(String label) {}

  /** gotoコマンドを書く。 */
  public void writeGoto(String label) {}

  /** If-gotoコマンドを書く。 */
  public void writeIf(String label) {}

  /** callコマンドを書く。 */
  public void writeCall(String name, int nArgs) {}

  /** functionコマンドを書く。 */
  public void writeFunction(String name, int nLocals) {}

  /** returnコマンドを書く。 */
  public void writeReturn() {}

  /** 出力ファイルを閉じる。 */
  public void close() {}
}
