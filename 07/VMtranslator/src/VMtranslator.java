import java.io.IOException;
import java.util.Collections;
import java.util.List;
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

    try {
      // 指定されたディレクトリ名から生成するアセンブリファイル名を生成する
      int lastSlashIndexOfInputDir = args[0].lastIndexOf("/");
      String asmNameMaterial = args[0].substring(lastSlashIndexOfInputDir + 1);
      try (CodeWriter codeWriter =
          new CodeWriter(
              args[0] + "/" + asmNameMaterial + ".asm")) {
        // ParserモジュールでVMの入力ファイルのパースを行う
        for (var vmFile : vmFileList) {
          try (Parser parser = new Parser(vmFile)) {

            // ファイル名となる文字列を生成する
            int lastSlashIndex = vmFile.lastIndexOf("/");
            int lastDotIndex = vmFile.lastIndexOf(".");
            String fileNameMaterial = vmFile.substring(lastSlashIndex + 1, lastDotIndex);

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
              } else if (commandType == C_CALL) {
                codeWriter.writeCall(parser.arg1(), parser.arg2());
              }
              parser.advance();
            }
          }
        }
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
      System.out.println(e.getStackTrace());
    }
  }
}
