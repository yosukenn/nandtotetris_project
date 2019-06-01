import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import modules.CodeWriter;
import modules.Parser;
import static modules.CommandType.*;

public class VMtranslator {

  /**
   * ex. VMTranslator source
   *
   * @param args .vmファイルもしくは１つ以上の.vmファイルを含むディレクトリ
   */
  public static void main(String[] args) {
    // プログラムの引数がファイルであっても、ディレクトリであっても許容する。
    // コマンドライン引数
    // ex./Users/yosukennturner/Desktop/nand2tetris/projects/07/FunctionCall/FibonacciElement

    // 読み込み対象のファイルリストの生成
    List<String> vmFileList = Collections.emptyList();
    try {
      vmFileList = Parser.createVmFileList(args[0]);
    } catch (IOException e) {
      System.out.println(e);
    }

    // ParserモジュールでVMの入力ファイルのパースを行う
    for (var vmFile : vmFileList) {
      try {
        try (Parser parser = new Parser(vmFile)) {

          // ファイル名となる文字列を生成する
          int lastSlashIndex = vmFile.lastIndexOf("/");
          int lastDotIndex = vmFile.lastIndexOf(".");
          String fileNameMaterial = vmFile.substring(lastSlashIndex + 1, lastDotIndex);

          // CodeWriterモジュールでアセンブリコードを出力ファイルへ書き込む準備を行う。
          // 入力ファイルのVMコマンドを１行ずつ読み進めながら、アセンブリコードへの変換を行い、出力ファイルへ書き込みを行う。
          try (CodeWriter codeWriter =
              new CodeWriter(
                  vmFile.substring(0, lastSlashIndex) + "/" + fileNameMaterial + ".asm")) {
            codeWriter.setFileName(vmFile);

            parser.advance();
            while (parser.hasMoreCommands()) {
              if (parser.getCurrentCommand().equals("")) {
                parser.advance();
                continue;
              }

              var commandType = parser.commandType();
              if (commandType == C_ARITHMETIC) {
                codeWriter.writeArithmetic(parser.command());
              } else if (commandType == C_PUSH || commandType == C_POP) {
                codeWriter.writePushPop(parser.command(), parser.arg1(), parser.arg2());
              } else if (commandType == C_LABEL) {
                codeWriter.writeLabel(parser.arg1());
              } else if (commandType == C_GOTO) {
                codeWriter.writeGoto(parser.arg1());
              } else if (commandType == C_IF) {
                codeWriter.writeIf(parser.arg1());
              } else if (commandType == C_FUNCTION) {
                codeWriter.writeFunction(parser.arg1(), parser.arg2());
              } else if (commandType == C_RETURN) {
                codeWriter.writeReturn();
              }

              parser.advance();
            }
          }
        }
      } catch (IOException e) {
        System.out.println(e.getMessage());
        System.out.println(e.getStackTrace());
      }
    }
  }
}
