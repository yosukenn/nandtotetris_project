import java.io.IOException;
import java.security.Key;
import modules.CompilationEngine;
import modules.JackTokenizer;
import modules.data.Keyword;

/**
 * セットアップや他モジュールの呼び出しを行うモジュール<br>
 * ⑴ 入力ファイルの Xxx.jack（もしくはそれを含むディレクトリ） から、JackTokenizer を生成する。<br>
 * ⑵ Xxx.xml という名前の出力ファイルを作り、それに書き込みを行う準備をする。<br>
 * ⑶ 入力である JackTokenizer を出力ファイルへコンパイルするために、ConpilationEngine を用いる。
 */
public class JackAnalyzer {
  public static void main(String[] args) {

    // ⑴ // TODO コマンドライン引数がディレクトリでも対応できるようにする
    // 実行コマンド ex. $ JackAnalyzer source
    try {
      var jackTokenizer = new JackTokenizer(args[0]);

      // ⑵ Xxx.xml という名前の出力ファイルを作り、それに書き込みを行う準備をする。
      int lastSlashIndexOfInputDir = args[0].lastIndexOf("/");
      int lastDotIndexOfInputDir = args[0].lastIndexOf(".");
      String coreName = args[0].substring(lastSlashIndexOfInputDir, lastDotIndexOfInputDir + 1);
      var comlilationEngine =
          new CompilationEngine(
              args[0],
              "/Users/yosukennturner/Desktop/nand2tetris/nandtotetris_project/10/output"
                  + coreName
                  + ".xml");

      // ⑶ 入力である JackTokenizer を出力ファイルへコンパイルするために、ConpilationEngine を用いる。

      jackTokenizer.advance();
      while (jackTokenizer.hasMoreTokens()) {
        switch (jackTokenizer.tokenType()) {
          case KEYWORD:
            Keyword keyword = jackTokenizer.keyword();
            break;
          case SYMBOL:
            char symbol = jackTokenizer.symbol();
            break;
          case INT_CONST:
            int intConst = jackTokenizer.intVal();
            break;
          case IDENTIFIER:
            String indentifier = jackTokenizer.identifier();
            break;
          case STRING_CONST:
            String stringConst = jackTokenizer.stringVal();
            break;
        }
        jackTokenizer.advance();
      }
      jackTokenizer.close();

    } catch (IOException e) {
      System.out.println("ファイルを拓けませんでした。");
      System.out.println(e.getMessage());
    }
  }
}
