import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import modules.CompilationEngine;
import modules.JackTokenizer;

/**
 * セットアップや他モジュールの呼び出しを行うモジュール<br>
 * 実行コマンド ex. $ JackCompiler source
 */
public class JackCompiler {
  public static void main(String[] args) {

    var source = new File(args[0]);

    try {
      if (source.isDirectory()) {
        File[] jackFiles = source.listFiles();
        for (var jackFile : jackFiles) {
          if (jackFile.getName().endsWith(".jack")) {
            compile(jackFile);
          }
        }
      } else if (source.isFile()) {
        if (!source.getName().endsWith(".jack")) {
          throw new IllegalArgumentException("指定されたファイルは.jackファイルではありません。");
        }
        compile(source);
      }
    } catch (Exception e) {
      System.out.println("解析中にエラーが発生したため、プログラムを終了します。");
      System.out.println(e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * jackファイル内の全ての行を読み込み、文字列に変換します。
   *
   * @param source .jackファイル
   * @return .jackファイル内のプログラム文字列
   */
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

  /** .jackファイルをコンパイルして.vmファイルを生成します。 */
  private static void compile(File source) throws IOException, XMLStreamException {
    String jackProgram = JackCompiler.readAllProgramInJackfile(source);

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

    try (var compilationEngine =
        new CompilationEngine(
            source.getParentFile(), // 生成するvmファイルの親ディレクトリパスを指定
            tokenizerOutputFilename,
            compileEngineOutputFilename)) {
      compilationEngine.compileClass(); // 必ず最初はterminal: class
    }
  }
}
