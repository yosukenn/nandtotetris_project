import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import modules.CompilationEngine;
import modules.JackTokenizer;

/**
 * セットアップや他モジュールの呼び出しを行うモジュール<br>
 * 実行コマンド ex. $ JackAnalyzer source
 */
public class JackAnalyzer {
  public static void main(String[] args) {

    var source = new File(args[0]);

    try {
      if (source.isDirectory()) {
        File[] jackFiles = source.listFiles();
        for (var jackFile : jackFiles) {
          if (jackFile.getName().endsWith(".jack")) {
            analyze(jackFile);
          }
        }
      } else if (source.isFile()) {
        if (!source.getName().endsWith(".jack")) {
          throw new IllegalArgumentException("指定されたファイルは.jackファイルではありません。");
        }
        analyze(source);
      }
    } catch (Exception e) {
      System.out.println("解析中にエラーが発生したため、プログラムを終了します。");
      System.out.println(e.getMessage());
      e.printStackTrace();
      System.exit(1);
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
            if (s.matches("[,.;()\\[\\]\\-\\+~]")) {
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
      System.out.println(e.getMessage());
      System.exit(1);
    }
    return readString.toString();
  }

  private static void analyze(File source) throws IOException, XMLStreamException {
    String jackProgram = JackAnalyzer.readAllProgramInJackfile(source);

    int lastDotIndexOfInputFile = source.getName().lastIndexOf(".");
    String coreName = source.getName().substring(0, lastDotIndexOfInputFile);
    String tokenizerOutputFilename = source.getParent() + "/" + coreName + "T.xml";
    String compileEngineOutputFilename = source.getParent() + "/" + coreName + ".xml";

    try (var jackTokenizer = new JackTokenizer(tokenizerOutputFilename, jackProgram)) {

      jackTokenizer.advance();
      while (true) {
        switch (jackTokenizer.tokenType()) {
          case KEYWORD:
            jackTokenizer.keyword();
            break;
          case SYMBOL:
            jackTokenizer.symbol();
            break;
          case INT_CONST:
            jackTokenizer.intVal();
            break;
          case IDENTIFIER:
            jackTokenizer.identifier();
            break;
          case STRING_CONST:
            jackTokenizer.stringVal();
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

    // ⑵ Xxx.xml という名前の出力ファイルを作り、それに書き込みを行う準備をする。
    try (var comlilationEngine =
        new CompilationEngine(tokenizerOutputFilename, compileEngineOutputFilename)) {
      // ⑶ 入力である JackTokenizer を出力ファイルへコンパイルするために、ConpilationEngine を用いる。
      comlilationEngine.compileClass(); // 必ず最初はtarminal: class
    }
  }
}
