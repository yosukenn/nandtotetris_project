import java.io.IOException;
import modules.CompilationEngine;
import modules.JackTokenizer;

/**
 * セットアップや他モジュールの呼び出しを行うモジュール<br>
 * ⑴ 入力ファイルの Xxx.jack（もしくはそれを含むディレクトリ） から、JackTokenizer を生成する。<br>
 * ⑵ Xxx.xml という名前の出力ファイルを作り、それに書き込みを行う準備をする。<br>
 * ⑶ 入力である JackTokenizer を出力ファイルへコンパイルするために、ConpilationEngine を用いる。
 */
public class JackAnalyzer {
  public static void main(String[] args) {

    // ⑴ // TODO コマンドライン引数がディレクトリでも対応できるようにする
    // ex. $ JackAnalyzer source
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

    } catch (IOException e) {
      System.out.println("ファイルを拓けませんでした。");
      System.out.println(e.getMessage());
    }
  }
}