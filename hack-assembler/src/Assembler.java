import modules.Code;
import modules.CommandType;
import modules.Parser;
import modules.SymbolTable;

import java.io.*;

public class Assembler {
    public static void main(String[] args) {
        // コマンド引数でasmファイルを絶対パスで受け取る ex. java -jar 06.jar /Users/yosukennturner/Desktop/nand2tetris/projects/06/add/Add.asm
        // Parserモジュールをインスタンス化してアセンブリファイルを読み込む準備をする

        // SymbolTableの初期化
        var symbolTable = new SymbolTable();

        // 1回目のパス : バイナリコードを生成せずに、シンボルテーブルだけを作成する
        try {
            try (Parser parser = new Parser(args[0])) {

                // ROMアドレスを記録していく
                long count = -1L;
                parser.advance();
                while (parser.hasMoreCommands()) {

                    if (parser.getCurrentCommand().equals("")) {
                        parser.advance();
                        continue;
                    }

                    if (parser.commandType() == CommandType.A_COMMAND || parser.commandType() == CommandType.C_COMMAND) {
                        // A命令、C命令に出くわしたたびに１ずつ加算する
                        count++;
                    } else {
                        // 擬似コマンドに出くわした時は、次のコマンドアドレスをラベルに関連づけてシンボルテーブルに保存する
                        symbolTable.addEntry(parser.symbol(), count + 1);
                    }
                    parser.advance();
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.out.println(e.getStackTrace());
        }


        // 2回目のパス : 1行ごとにパース処理を行う
        try {
            try (Parser parser = new Parser(args[0])) {

                // ファイル名となる文字列を生成する
                int lastSlashIndex = args[0].lastIndexOf("/");
                int lastDotIndex = args[0].lastIndexOf(".");
                String fileNameMaterial = args[0].substring(lastSlashIndex+1, lastDotIndex);

                // Writerを作成しバイナリコード書き込みの準備をする
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("hackFiles/" + fileNameMaterial + ".hack"));) {

                    var useableMemoryAddress = 16L;

                    // バイナリコード変換そ担うCoreモジュールの準備
                    Code code = new Code();
                    parser.advance();
                    while (parser.hasMoreCommands()) {
                        // Parserモジュールのメソッドを使ってコマンドを読む
                        if (parser.getCurrentCommand().equals("")) {
                            parser.advance();
                            continue;
                        }

                        if (parser.commandType() == CommandType.A_COMMAND) {
                            // シンボルに出くわしたとき、その変数がシンボルテーブルに登録されていたら参照し、登録されていなかったらアドレス番号を登録する
                            if (!parser.getCurrentCommand().matches("@\\d+")) {
                                if (symbolTable.contains(parser.symbol())) {
                                    parser.setCurrentCommand("@" + symbolTable.getAddress(parser.symbol()));
                                } else {
                                    if (useableMemoryAddress < 16384) {
                                        symbolTable.addEntry(parser.symbol(), useableMemoryAddress);
                                        parser.setCurrentCommand("@" + symbolTable.getAddress(parser.symbol()));
                                        useableMemoryAddress++;
                                    } else {
                                        throw new RuntimeException("使用できるRAMアドレスが存在しません。");
                                    }
                                }
                            }

                            // 現コマンドの種類がAコマンドの場合、バイナリコードを生成して書き込む
                            writer.write("0");
                            writer.write(parser.getValueOfaddressInstruction());
                            writer.flush();
                            writer.newLine();
                        } else if (parser.commandType() == CommandType.C_COMMAND) {
                            // 現コマンドの種類がCコマンドの場合、dest, comp, jump を用いて各部品のバイナリコードを生成し、引っ付けて書き込む
                            writer.write("111");
                            writer.write(code.comp(parser.comp()));
                            writer.write(code.dest(parser.dest()));
                            writer.write(code.jump(parser.jump()));
                            writer.flush();
                            writer.newLine();
                        } else if (parser.commandType() == CommandType.L_COMMAND) {
                            // 無視する
                        } else {
                            throw new RuntimeException();
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
