package modules;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import modules.data.ArithmeticCommand;
import modules.data.Segment;

/** VMコマンドの構文に従い、VMコマンドをファイルへ書き出す。 */
public class VMWriter extends BufferedWriter implements AutoCloseable {

  private StringBuilder stringBuffer = new StringBuilder();

  private int ladelIndex = 0;

  /** @return 現在のラベルインデックス */
  public int getCurrentLadelIndex() {
    var index = ladelIndex;
    ladelIndex++;
    return index;
  }

  /** 新しいファイルを作り、それに書き込む準備をする。 */
  public VMWriter(File file) throws IOException {
    super(new FileWriter(file, true)); // 上書きを許容する。
  }

  /** pushコマンドをバッファに詰めておく。 */
  public void bufferPush(Segment segment, int index) {
    stringBuffer.append("push " + segment.getCode() + " " + index + "\n");
    ladelIndex++;
  }

  /** popコマンドをバッファに詰めておく。 */
  public void bufferPop(Segment segment, int index) {
    stringBuffer.append("pop " + segment.getCode() + " " + index + "\n");
  }

  /** 算術コマンドを書く。 */
  public void bufferArithmetic(ArithmeticCommand command) {
    switch (command) {
      case ADD:
        stringBuffer.append("add\n");
        break;
      case SUB:
        stringBuffer.append("sub\n");
        break;
      case NEG:
        stringBuffer.append("neg\n");
        break;
      case EQ:
        stringBuffer.append("eq\n");
        break;
      case GT:
        stringBuffer.append("gt\n");
        break;
      case LT:
        stringBuffer.append("lt\n");
        break;
      case OR:
        stringBuffer.append("or\n");
        break;
      case AND:
        stringBuffer.append("and\n");
        break;
      case NOT:
        stringBuffer.append("not\n");
        break;
    }
  }

  /** labelコマンドを書く。 */
  public void bufferLabel(String label) {
    stringBuffer.append("label " + label + "\n");
  }

  /** gotoコマンドを書く。 */
  public void bufferGoto(String label) {
    stringBuffer.append("goto " + label + "\n");
  }

  /** If-gotoコマンドを書く。 */
  public void bufferIf(String label) {
    stringBuffer.append("if-goto " + label + "\n");
  }

  /** callコマンドを書く。 */
  public void bufferCall(String name, int nArgs) {
    stringBuffer.append("call " + name + " " + nArgs + "\n");
  }

  /** functionコマンドを書く。 */
  public void writeFunction(String name, long nLocals) throws IOException {
    write("function " + name + " " + nLocals + "\n");
  }

  /** returnコマンドを書く。 */
  public void bufferReturn() {
    stringBuffer.append("return\n");
  }

  /** string buffer に溜め込んだ文字列をvmファイルに書き込む。 */
  public void writeStringBuffer() throws IOException {
    write(stringBuffer.toString());
    stringBuffer.setLength(0); // バッファのクリア
  }

  /** 出力ファイルを閉じる。 */
  public void close() throws IOException {
    super.close();
  }
}
