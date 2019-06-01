import java.io.IOException;
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

    // ParserモジュールでVMの入力ファイルのパースを行う
    try {
      try (Parser parser = new Parser(args[0])) {

        // ファイル名となる文字列を生成する
        int lastSlashIndex = args[0].lastIndexOf("/");
        int lastDotIndex = args[0].lastIndexOf(".");
        String fileNameMaterial = args[0].substring(lastSlashIndex + 1, lastDotIndex);

        // CodeWriterモジュールでアセンブリコードを出力ファイルへ書き込む準備を行う。
        // 入力ファイルのVMコマンドを１行ずつ読み進めながら、アセンブリコードへの変換を行い、出力ファイルへ書き込みを行う。
        try (CodeWriter codeWriter =
            new CodeWriter(
                args[0].substring(0, lastSlashIndex) + "/" + fileNameMaterial + ".asm")) {
          codeWriter.setFileName(args[0]);

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
