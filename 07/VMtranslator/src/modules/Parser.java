package modules;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/** 一つの .vmファイルn対してパースを行うとともに、入力コマンドへのアクセスをカプセル化する。 */
public class Parser extends BufferedReader {

  private String currentCommand = "";

  public String getCurrentCommand() {
    return this.currentCommand;
  }

  /**
   * 入力ファイル/ストリームを開きパースを行う準備をする
   *
   * @param inputFile 入力ファイル
   */
  public Parser(String inputFile) throws FileNotFoundException {
    super(new FileReader(inputFile));
  }

  /**
   * 入力にまだコマンドが存在するかを判定します。
   *
   * @return 入力にまだコマンドが存在するか？
   */
  public boolean hasMoreCommands() throws IOException {
    return this.currentCommand != null;
  }

  /**
   * 入力から次のコマンドを読み、それを現在のコマンドにする。<br>
   * このメソッドは hasMoreCommands が true の場合のみ呼ばれる。 <br>
   * 最初は現コマンドは空である。
   */
  public void advance() throws IOException {
    String readLine = this.readLine();
    if (readLine == null) {
      this.currentCommand = null;
      return;
    }
    readLine = readLine.trim();
    readLine = readLine.replaceAll("\\s{2,}", "");
    String[] words = new String[2];
    if (readLine.contains("//")) {
      words = readLine.split("//");
    } else {
      words[0] = readLine;
    }
    this.currentCommand = words[0];
  }

  /** @return 現コマンドの種類 */
  public CommandType commandType() {

    if (currentCommand.equals("add") || currentCommand.equals("sub") || currentCommand.equals("neg") || currentCommand.equals("eq") || currentCommand.equals("gt") || currentCommand.equals("lt") || currentCommand.equals("and") || currentCommand.equals("or") || currentCommand.equals("not")) {
      return CommandType.C_ARITHMETIC;
    } else if (currentCommand.contains("push")) {
      return CommandType.C_PUSH;
    } else if (currentCommand.contains("pop")) {
      return CommandType.C_POP;
    } else if (currentCommand.startsWith("label")) {
      return CommandType.C_LABEL;
    } else if (currentCommand.startsWith("goto")) {
      return CommandType.C_GOTO;
    } else if (currentCommand.startsWith("if-goto")) {
      return CommandType.C_IF;
    } else if (currentCommand.startsWith("function")) {
      return CommandType.C_FUNCTION;
    } else if (currentCommand.startsWith("call")) {
      return CommandType.C_CALL;
    } else if (currentCommand.startsWith("return")) {
      return CommandType.C_RETURN;
    }
    throw new RuntimeException("該当のコマンドが見つかりません。");
  }

  public String command() {
    if (currentCommand.contains(" ")) {
      return currentCommand.split(" ")[0];
    } else {
      return currentCommand;
    }
  }

  /**
   * 現コマンドの最初の引数が返される。<br>
   * C_ARITHMETIC の場合、コマンド自体が返される。<br>
   * 現コマンドがC_RETURNの場合、このメソッドは呼ばない。
   *
   * @return 現コマンドの最初の引数
   */
  public String arg1() {
    if (currentCommand.contains(" ")) {
      return currentCommand.split(" ")[1];
    } else {
      throw new RuntimeException("対応するコマンドが存在しません。");
    }
  }

  /**
   * 現コマンドの２番目の引数が返される。<br>
   * 現コマンドがC_PUSH, C_POP, C_FUNCTION, C_CALlの場合のみ呼ぶようにする。
   *
   * @return 現コマンドの２番目の引数
   */
  public int arg2() {
    if (currentCommand.contains(" ")) {
      return Integer.parseInt(currentCommand.split(" ")[2]);
    } else {
      throw new RuntimeException("対応するコマンドが存在しません。");
    }
  }
}
