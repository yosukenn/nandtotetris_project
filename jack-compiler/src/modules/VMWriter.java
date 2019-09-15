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

  /** 新しいファイルを作り、それに書き込む準備をする。 */
  public VMWriter(File file) throws IOException {
    super(new FileWriter(file, true)); // 上書きを許容する。
  }

  /** pushコマンドを書く。 */
  public void bufferPushCommand(Segment segment, int index) {
    stringBuffer.append("push " + segment.getCode() + " " + index + "\n");
  }

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
  public void bufferCallCommand(String name, int nArgs) {
    stringBuffer.append("call " + name + " " + nArgs + "\n");
  }

  /** functionコマンドを書く。 */
  public void writeFunction(String name, int nLocals) throws IOException {
    write("function " + name + " " + nLocals + "\n");
  }

  /** returnコマンドを書く。 */
  public void writeReturn() {}

  /** string buffer に溜め込んだ文字列をvmファイルに書き込む。 */
  public void writeStringBuffer() throws IOException {
    write(stringBuffer.toString());
  }

  /** 出力ファイルを閉じる。 */
  public void close() throws IOException {
    super.close();
  }
}
