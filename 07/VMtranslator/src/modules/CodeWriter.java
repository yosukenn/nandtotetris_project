package modules;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/** VMコマンドをHackアセンブリコードに変換する。 */
public class CodeWriter extends BufferedWriter {

  private String inputFileName = "";
  private long writeCount = 0;

  public CodeWriter(String outputFile) throws IOException {
    super(new FileWriter(outputFile));
  }

  /**
   * CodeWriterモジュールに新しいVMファイルの変換が開始したことを知らせる。
   *
   * @param fileName 新しいVMファイル名
   */
  public void setFileName(String fileName) {
    this.inputFileName = fileName;
  }

  /**
   * 与えられた算術コマンドをアセンブリコードに変換し、それを書き込む
   *
   * @param command 算術コマンド
   */
  public void writeArithmetic(String command) throws IOException {
    switch (command) {
      case "add":
        this.writeArithmeticAdd();
        break;
      case "sub":
        this.writeArithmeticSub();
        break;
      case "neg":
        this.writeArithmeticNeg();
        break;
      case "eq":
        this.writeArithmeticEq();
        break;
      case "lt":
        this.writeArithmeticLt();
        break;
      case "gt":
        this.writeArithmeticGt();
        break;
      case "and":
        this.writeArithmeticAnd();
        break;
      case "or":
        this.writeArithmeticOr();
        break;
      case "not":
        this.writeArithmeticNot();
        break;
    }
  }

  /**
   * C_PUSH, C_POP コマンドをアセンブリコマンドに変換し、それを書き込む
   *
   * @param command C_PUSHまたはC_POPコマンド
   * @param segment メモリセグメント
   * @param index
   */
  public void writePushPop(String command, String segment, int index) throws IOException {
    switch (command) {
      case "push":
        switch (segment) {
          case "constant":
            this.writeOneLine("@" + index);
            this.writeOneLine("D=A");
            this.writeOneLine("@SP");
            this.writeOneLine("A=M");
            this.writeOneLine("M=D");
            this.writeOneLine("@SP");
            this.writeOneLine("M=M+1");
            break;
          case "local":
          case "argument":
          case "this":
          case "that":
            this.writePushCommand(segment, index);
            break;
          case "pointer":
            this.writePushPointerCommand(index);
            break;
          case "temp":
            this.writePushTempCommand(index);
            break;
          case "static":
            this.writePushStaticCommand(index);
            break;
        }
        break;
      case "pop":
        switch (segment) {
          case "local":
            this.writePopCommand1("LCL", index);
            break;
          case "argument":
            this.writePopCommand1("ARG", index);
            break;
          case "this":
            this.writePopCommand1("THIS", index);
            break;
          case "that":
            this.writePopCommand1("THAT", index);
            break;
          case "pointer":
            this.writePopCommand3(index);
            break;
          case "temp":
            this.writePopCommand2(5, index);
            break;
          case "static":
            this.writePopCommand4(index);
            break;
          case "constant":
            throw new RuntimeException("constractセグメントへの値の格納はできません。");
        }
    }
  }

  /**
   * 出力ファイルを閉じる
   *
   * @throws IOException
   */
  @Override
  public void close() throws IOException {
    super.close();
  }

  /**
   * VMの初期化（ブートストラップ）を行うアセンブリコードを書く。<br>
   * これは出力ファイルの先頭に配置しなければならない。
   */
  public void writeInit() {}

  /**
   * labelコマンドを行うアセンブリコードを書く
   * @param label ラベルシンボル名
   */
  public void writeLabel(String label) throws IOException {
    this.writeOneLine("(" + label + ")");
  }

  /**
   * gotoコマンドを行うアセンブリコードを書く
   * @param label 移動先アドレスを示すラベルシンボル
   */
  public void writeGoto(String label) throws IOException {
    this.writeOneLine("@" + label);
    this.writeOneLine("0;JMP");
  }

  /**
   * if-gotoコマンドを行うアセンブリコードを書く
   * @param label 移動先アドレスを示すラベルシンボル
   */
  public void writeIf(String label) throws IOException {
    this.writeOneLine("@SP");
    this.writeOneLine("AM=M-1");
    this.writeOneLine("D=M");
    this.writeOneLine("@" + label);
    this.writeOneLine("D;JNE");
  }

  /**
   * callコマンドを行うアセンブリコードを書く
   * @param functionName 呼び出す関数名
   * @param numArgs 渡す引数の個数
   */
  public void writeCall(String functionName, long numArgs) {

  }

  /**
   * returnコマンドを行うアセンブリコードを書く
   */
  public void writeReturn() throws IOException {
    // 関数の最終の結果をARG(リターン後に戻り値が入る場所)に格納する
    this.writeOneLine("@SP");
    this.writeOneLine("AM=M-1");
    this.writeOneLine("D=M");
    this.writeOneLine("@ARG");
    this.writeOneLine("A=M");
    this.writeOneLine("M=D");
    // SPを戻す
    this.writeOneLine("@ARG");
    this.writeOneLine("D=M+1");
    this.writeOneLine("@SP");
    this.writeOneLine("M=D");
    // THATの復元
    this.writeOneLine("@LCL");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M");
    this.writeOneLine("@THAT");
    this.writeOneLine("M=D");
    // THISの復元
    this.writeOneLine("@LCL");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M");
    this.writeOneLine("@THIS");
    this.writeOneLine("M=D");
    // ARGの復元
    this.writeOneLine("@LCL");
    this.writeOneLine("M=M-1"); // ここでLCLの値は元に戻る
    this.writeOneLine("A=M");
    this.writeOneLine("D=M");
    this.writeOneLine("@ARG");
    this.writeOneLine("M=D");
    // LCLの復元
    this.writeOneLine("@LCL");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M");
    this.writeOneLine("@LCL");
    this.writeOneLine("M=D");
  }

  /**
   * functionコマンドを行うアセンブリコードを書く
   *
   * @param functionName 関数名
   * @param numLocals この関数がもつローカル変数の個数
   */
  public void writeFunction(String functionName, long numLocals) throws IOException {
    // 関数の開始位置のためにラベルを宣言する
    this.writeOneLine("(" + functionName + ")");
    // ローカル変数（numLocals個）を0で初期化
    int count = 0;
    while (numLocals - count > 0) {
      // ローカル変数を全て0で初期化する
      this.writePushCommand("local", count);
      count++;
    }
  }


  private void writeOneLine(String string) throws IOException {
    this.write(string);
    this.flush();
    this.newLine();
  }

  private void writeArithmeticAdd() throws IOException {
    this.writeOneLine("@SP");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M");
    this.writeOneLine("@SP");
    this.writeOneLine("A=M-1");
    this.writeOneLine("M=D+M");
  }

  private void writeArithmeticSub() throws IOException {
    this.writeOneLine("@SP");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M");
    this.writeOneLine("@SP");
    this.writeOneLine("A=M-1");
    this.writeOneLine("M=D-M");
    this.writeOneLine("M=-M");
  }

  private void writeArithmeticNeg() throws IOException {
    this.writeOneLine("@SP");
    this.writeOneLine("A=M-1");
    this.writeOneLine("M=-M");
  }

  private void writeArithmeticEq() throws IOException {
    var count = this.writeCount;
    this.writeOneLine("@SP");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M");
    this.writeOneLine("@SP");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M-D");
    this.writeOneLine("@EQ_TRUE" + count);
    this.writeOneLine("D;JEQ");
    this.writeOneLine("@EQ_FALSE" + count);
    this.writeOneLine("D;JNE");
    this.writeOneLine("(EQ_TRUE" + count + ")");
    this.writeOneLine("@SP");
    this.writeOneLine("A=M");
    this.writeOneLine("M=-1");
    this.writeOneLine("@SP");
    this.writeOneLine("M=M+1");
    this.writeOneLine("@END" + count);
    this.writeOneLine("0;JMP");
    this.writeOneLine("(EQ_FALSE" + count + ")");
    this.writeOneLine("@SP");
    this.writeOneLine("A=M");
    this.writeOneLine("M=0");
    this.writeOneLine("@SP");
    this.writeOneLine("M=M+1");
    this.writeOneLine("@END" + count);
    this.writeOneLine("0;JMP");
    this.writeOneLine("(END" + count + ")");
    this.writeCount++;
  }

  private void writeArithmeticLt() throws IOException {
    var count = this.writeCount;
    this.writeOneLine("@SP");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M");
    this.writeOneLine("@SP");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M-D");
    this.writeOneLine("@LT_TRUE" + count);
    this.writeOneLine("D;JLT");
    this.writeOneLine("@LT_FALSE" + count);
    this.writeOneLine("D;JGE");
    this.writeOneLine("(LT_TRUE" + count + ")");
    this.writeOneLine("@SP");
    this.writeOneLine("A=M");
    this.writeOneLine("M=-1");
    this.writeOneLine("@SP");
    this.writeOneLine("M=M+1");
    this.writeOneLine("@END" + count);
    this.writeOneLine("0;JMP");
    this.writeOneLine("(LT_FALSE" + count + ")");
    this.writeOneLine("@SP");
    this.writeOneLine("A=M");
    this.writeOneLine("M=0");
    this.writeOneLine("@SP");
    this.writeOneLine("M=M+1");
    this.writeOneLine("@END" + count);
    this.writeOneLine("0;JMP");
    this.writeOneLine("(END" + count + ")");
    this.writeCount++;
  }

  private void writeArithmeticGt() throws IOException {
    var count = this.writeCount;
    this.writeOneLine("@SP");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M");
    this.writeOneLine("@SP");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M-D");
    this.writeOneLine("@GT_TRUE" + count);
    this.writeOneLine("D;JGT");
    this.writeOneLine("@GT_FALSE" + count);
    this.writeOneLine("D;JLE");
    this.writeOneLine("(GT_TRUE" + count + ")");
    this.writeOneLine("@SP");
    this.writeOneLine("A=M");
    this.writeOneLine("M=-1");
    this.writeOneLine("@SP");
    this.writeOneLine("M=M+1");
    this.writeOneLine("@END" + count);
    this.writeOneLine("0;JMP");
    this.writeOneLine("(GT_FALSE" + count + ")");
    this.writeOneLine("@SP");
    this.writeOneLine("A=M");
    this.writeOneLine("M=0");
    this.writeOneLine("@SP");
    this.writeOneLine("M=M+1");
    this.writeOneLine("@END" + count);
    this.writeOneLine("0;JMP");
    this.writeOneLine("(END" + count + ")");
    this.writeCount++;
  }

  private void writeArithmeticAnd() throws IOException {
    this.writeOneLine("@SP");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M");
    this.writeOneLine("@SP");
    this.writeOneLine("A=M-1");
    this.writeOneLine("M=D&M");
  }

  private void writeArithmeticOr() throws IOException {
    this.writeOneLine("@SP");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M");
    this.writeOneLine("@SP");
    this.writeOneLine("A=M-1");
    this.writeOneLine("M=D|M");
  }

  private void writeArithmeticNot() throws IOException {
    this.writeOneLine("@SP");
    this.writeOneLine("A=M-1");
    this.writeOneLine("M=!M");
  }

  private void writePushCommand(String segment, int index) throws IOException {
    String registerName = "";
    switch (segment) {
      case "argument":
        registerName = "ARG";
        break;
      case "local":
        registerName = "LCL";
        break;
      case "this":
      case "pointer":
        registerName = "THIS";
        break;
      case "that":
        registerName = "THAT";
        break;
    }

    this.writeOneLine("@" + index);
    this.writeOneLine("D=A");
    this.writeOneLine("@" + registerName);
    this.writeOneLine("A=M+D");
    this.writeOneLine("D=M");
    this.writeOneLine("@SP");
    this.writeOneLine("A=M");
    this.writeOneLine("M=D");
    this.writeOneLine("@SP");
    this.writeOneLine("M=M+1");
  }

  private void writePushStaticCommand(int index) throws IOException {
    int lastSlashIndex = this.inputFileName.lastIndexOf("/");
    int lastDotIndex = this.inputFileName.lastIndexOf(".");
    String vmFileName = this.inputFileName.substring(lastSlashIndex+1, lastDotIndex);

    this.writeOneLine("@" + vmFileName + "." + index);
    this.writeOneLine("D=M");
    this.writeOneLine("@SP");
    this.writeOneLine("A=M");
    this.writeOneLine("M=D");
    this.writeOneLine("@SP");
    this.writeOneLine("M=M+1");
  }

  private void writePushPointerCommand(int index) throws IOException {
    String pointerAddress = "";
    switch (index) {
      case 0:
        pointerAddress = "R3";
        break;
      case 1:
        pointerAddress = "R4";
        break;
    }
    this.writeOneLine("@" + pointerAddress);
    this.writeOneLine("D=M");
    this.writeOneLine("@SP");
    this.writeOneLine("A=M");
    this.writeOneLine("M=D");
    this.writeOneLine("@SP");
    this.writeOneLine("M=M+1");
  }

  private void writePushTempCommand(long index) throws IOException {
    String address = String.valueOf(5 + index);
    this.writeOneLine("@R" + address);
    this.writeOneLine("D=M");
    this.writeOneLine("@SP");
    this.writeOneLine("A=M");
    this.writeOneLine("M=D");
    this.writeOneLine("@SP");
    this.writeOneLine("M=M+1");
  }

  private void writePopCommand1(String baseAddressName, long index) throws IOException {
    // segmentとindexから値を格納するアドレスを特定する
    this.writeOneLine("@" + index);
    this.writeOneLine("D=A");
    this.writeOneLine("@" + baseAddressName);
    this.writeOneLine("M=M+D");
    this.writeOneLine("A=M");
    // スタックの一番上のデータを取り出す
    this.writeOneLine("@SP");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M");
    // 指定されたアドレスに値を格納
    this.writeOneLine("@" + baseAddressName);
    this.writeOneLine("A=M");
    this.writeOneLine("M=D");
    // baseAddressを元に戻す
    this.writeOneLine("@" + index);
    this.writeOneLine("D=A");
    this.writeOneLine("@" + baseAddressName);
    this.writeOneLine("M=M-D");
  }

  private void writePopCommand2(long baseAddress, long index) throws IOException {
    String addressSymbol = String.valueOf(baseAddress + index);
    this.writeOneLine("@SP");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M");

    this.writeOneLine("@R" + addressSymbol);
    this.writeOneLine("M=D");
  }

  private void writePopCommand3(int index) throws IOException {
    String pointerAddress = "";
    switch (index) {
      case 0:
        pointerAddress = "R3";
        break;
      case 1:
        pointerAddress = "R4";
        break;
    }
    this.writeOneLine("@SP");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M");
    this.writeOneLine("@" + pointerAddress);
    this.writeOneLine("M=D");
  }

  private void writePopCommand4(long index) throws IOException {
    int lastSlashIndex = this.inputFileName.lastIndexOf("/");
    int lastDotIndex = this.inputFileName.lastIndexOf(".");
    String vmFileName = this.inputFileName.substring(lastSlashIndex+1, lastDotIndex);

    this.writeOneLine("@SP");
    this.writeOneLine("M=M-1");
    this.writeOneLine("A=M");
    this.writeOneLine("D=M");
    this.writeOneLine("@" + vmFileName + "." + index);
    this.writeOneLine("M=D");
  }

}
