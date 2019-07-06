import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import modules.CompilationEngine;
import modules.JackTokenizer;
import modules.data.Keyword;

/** セットアップや他モジュールの呼び出しを行うモジュール */
public class JackAnalyzer {
  public static void main(String[] args) {

    // ⑴ // TODO コマンドライン引数がディレクトリでも対応できるようにする
    // 実行コマンド ex. $ JackAnalyzer source

    String jackProgram = JackAnalyzer.readAllProgramInJackfile(args[0]);
    System.out.println("---------------------------------------------------------");
    System.out.println(jackProgram);
    System.out.println("---------------------------------------------------------");

    int lastSlashIndexOfInputDir = args[0].lastIndexOf("/");
    int lastDotIndexOfInputDir = args[0].lastIndexOf(".");
    String coreName = args[0].substring(lastSlashIndexOfInputDir, lastDotIndexOfInputDir);
    String tokenizerOutputFilename =
        "/Users/yosukennturner/Desktop/nand2tetris/nandtotetris_project/jack-analyzer/output"
            + coreName
            + "T.xml";

    try (var jackTokenizer = new JackTokenizer(tokenizerOutputFilename, jackProgram.toString())) {

      // ⑵ Xxx.xml という名前の出力ファイルを作り、それに書き込みを行う準備をする。
      String compileEngineOutputFilename =
          "/Users/yosukennturner/Desktop/nand2tetris/nandtotetris_project/jack-analyzer/output"
              + coreName
              + "T.xml";
      try (var comlilationEngine = new CompilationEngine(args[0], compileEngineOutputFilename)) {
        // ⑶ 入力である JackTokenizer を出力ファイルへコンパイルするために、ConpilationEngine を用いる。
      }

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

    } catch (IOException e) {
      System.out.println("ファイルを開けませんでした。");
      System.out.println(e.getMessage());
    }
  }

  private static String readAllProgramInJackfile(String filename) {
    StringBuilder readString = new StringBuilder();
    // 1文字ずつ読んでシンボルだったら空白を挿入する。
    try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
      var line = reader.readLine().trim();
      while (line != null) {
        line.trim();
        if (line.contains("//")) {
          String[] lines = line.split("//");
          line = lines[0];
        }
        if (!line.equals("")) {
          String[] strArray = line.split("");
          for (String s : strArray) {
            if (s.matches("[,.;()]")) {
              readString.append(" " + s + " ");
            } else {
              readString.append(s);
            }
          }
          readString.append(" ");
        }
        line = reader.readLine();
      }

    } catch (IOException e) {
      System.out.println("ファイル読み込み中に異常を検知したため、プログラムを異常終了します。");
      System.exit(1);
    }
    return readString.toString();
  }
}
