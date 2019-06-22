package modules;

import java.io.IOException;
import java.util.Scanner;

/**
 * トークナイザ。<br>
 * 入力ストリームから全てのコメントと空白文字を取り除き、Jack文法に従い、Jack言語のトークンへ分割する。
 */
public class JackTokenizer {

  Scanner scanner; // scannerがfinalクラスで継承できないため。

  private String currentTokens = "";

  public JackTokenizer(String filename) throws IOException {
    this.scanner = new Scanner(filename);
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


}
