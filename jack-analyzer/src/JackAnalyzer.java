import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import modules.CompilationEngine;
import modules.JackTokenizer;
import modules.data.Keyword;

// TODO ファイル生成先をsourceディレクトリ、もしくはsourceファイルが格納されているディレクトリにする。

/** セットアップや他モジュールの呼び出しを行うモジュール */
public class JackAnalyzer {
  public static void main(String[] args) {

    // 実行コマンド ex. $ JackAnalyzer source
    var source = new File(args[0]);
    String jackProgram = JackAnalyzer.readAllProgramInJackfile(source);

    if (source.isDirectory()) {
      // TODO sourceがディレクトリの時の処理
      // .jackファイルでフィルターをかけて.jackファイルの配列を作る。
      // file を one by one でコンパイル処理をする。
      // トークナイザが出力するファイルも構文木もは.jackにつき1つ
    } else if (source.isFile()) {

      if (!source.getName().endsWith(".jack")) {
        throw new IllegalArgumentException("指定されたファイルは.jackファイルではありません。");
      }

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
        try (var comlilationEngine = new CompilationEngine(source, compileEngineOutputFilename)) {
          // ⑶ 入力である JackTokenizer を出力ファイルへコンパイルするために、ConpilationEngine を用いる。
        }

        jackTokenizer.advance();
        while (true) {
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
          if (jackTokenizer.hasMoreTokens() == true) {
            jackTokenizer.advance();
          } else {
            break;
          }
        }

      } catch (IOException e) {
        System.out.println("ファイルを開けませんでした。");
        System.out.println(e.getMessage());
      }
    }
  }

  private static String readAllProgramInJackfile(File source) {
    StringBuilder readString = new StringBuilder();
    // 1文字ずつ読んでシンボルだったら空白を挿入する。
    try (BufferedReader reader = new BufferedReader(new FileReader(source))) {
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
            if (s.matches("[,.;()\\[\\]\\-\\+]")) {
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
