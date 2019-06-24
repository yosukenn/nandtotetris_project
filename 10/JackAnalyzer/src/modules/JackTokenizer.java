package modules;

import static modules.data.Keyword.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import modules.data.Keyword;
import modules.data.TokenType;

/**
 * トークナイザ。<br>
 * 入力ストリームから全てのコメントと空白文字を取り除き、Jack文法に従い、Jack言語のトークンへ分割する。
 */
public class JackTokenizer {

  public Scanner scanner; // scannerがfinalクラスで継承できないため。

  private String currentTokens = "";
  private BufferedWriter writer;

  public JackTokenizer(String filename) throws IOException {
    this.scanner = new Scanner(filename);

    int lastSlashIndexOfInputDir = filename.lastIndexOf("/");
    int lastDotIndexOfInputDir = filename.lastIndexOf(".");
    String coreName = filename.substring(lastSlashIndexOfInputDir, lastDotIndexOfInputDir + 1);
    var writer =
        new BufferedWriter(
            new FileWriter(
                "/Users/yosukennturner/Desktop/nand2tetris/nandtotetris_project/10/output"
                    + coreName
                    + "T.xml"));
    this.writer = writer;
  }

  /** 入力にまだトークンは存在するかを取得します。 */
  public boolean hasMoreTokens() {
    return this.currentTokens != null;
  }

  /**
   * 入力から次のトークンを取得し、それを現在のトークンとする。<br>
   * このルーチンは、hasMoreTokens() が true の場合のみ呼び出すことができる。<br>
   * また、最初は現トークンは設定されない。
   */
  public void advance() throws IOException {
    this.currentTokens = this.scanner.next(); // TODO next()では改行できないかもしれない。
  }

  /** 現トークンの種類を返す。 */
  public TokenType tokenType() {
    // TODO あとで実装
    return TokenType.IDENTIFIER;
  }

  /**
   * 現トークンのキーワードを返す。<br>
   * このルーチンは、tokenType()がKEYWORDの場合のみ呼び出すことができる。
   */
  public Keyword keyword() {
    // TODO あとで実装
    return CLASS;
  }

  /**
   * 現トークンの文字を返す。<br>
   * このルーチンは、tokenType()がSYMBOLの場合のみ呼び出すことができる。
   */
  public char symbol() {
    return currentTokens.toCharArray()[0];
  }

  /**
   * 現トークンの識別子 identifier を返す。<br>
   * このルーチンは、tokenType() が IDENTIFIER の場合のみ呼び出すことができる。
   */
  public String identifier() {
    return currentTokens;
  }

  /**
   * 現トークンの整数の値を返す。<br>
   * このルーチンは、tokenType()がINT_CONSTの場合のみ呼び出すことができる。
   */
  public int intVal() {
    return Integer.parseInt(currentTokens);
  }

  /**
   * 現トークンの文字列を返す。<br>
   * このルーチンは、tokenType()がSTRING_CONSTの場合のみ呼び出すことができる。
   */
  public String stringVal() {
    return currentTokens;
  }
}
