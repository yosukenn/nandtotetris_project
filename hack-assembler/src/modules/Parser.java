package modules;

import java.io.*;

/**
 * 各アセンブリコマンドをその基本要素に分解する。
 * 必ずクローズしてください。
 */
public class Parser extends BufferedReader {

    private String currentCommand = "";

    public String getCurrentCommand() {
        return currentCommand;
    }

    public void setCurrentCommand(String currentCommand) {
        this.currentCommand = currentCommand;
    }

    /**
     * 入力ファイル/ストリームを開きパースを行う準備をする
     *
     * @param inputFile 入力ファイル
     */
    public Parser(String inputFile) throws FileNotFoundException{
        super(new FileReader(inputFile));
    }

    /**
     * 入力にまだコマンドが存在するかを判定します。
     *
     * @return 入力にまだコマンドが存在するか？
     */
    public boolean hasMoreCommands() throws IOException{
        return this.currentCommand != null;
    }

    /**
     * 入力から次のコマンドを読み、それを現在のコマンドにする。
     * このメソッドは hasMoreCommands が true の場合のみ呼ばれる。
     * 最初は現コマンドは空である。
     */
    public void advance() throws IOException {
        String readLine = this.readLine();
        if (readLine == null) {
            this.currentCommand = null;
            return;
        }
        readLine = readLine.trim();
        readLine = readLine.replaceAll(" +", "");
        String[] words = new String[2];
        if (readLine.contains("//")) {
            words = readLine.split("//");
        } else {
            words[0] = readLine;
        }
        this.currentCommand = words[0];
    }

    /**
     * @return 現コマンドの種類
     */
    public CommandType commandType() {
        if (currentCommand.contains("@")) {
            return CommandType.A_COMMAND;
        } else if(currentCommand.startsWith("(")) {
            return CommandType.L_COMMAND;
        } else {
            return CommandType.C_COMMAND;
        }
    }

    /**
     * 現コマンド@Xxxまたは(Xxx)のXxxを返す。
     * Xxxはシンボルまたは10進数の数値である。
     * このメソッドはcommandType()がA_COMMANDまたはL_COMMANDの時だけ呼ばれる。
     * @return
     */
    public String symbol() {
        String symbol = "";
        if (this.commandType() == CommandType.A_COMMAND) {
            symbol = currentCommand.replace("@", "");
        }
        if (this.commandType() == CommandType.L_COMMAND) {
            symbol = currentCommand.replace("(", "").replace(")", "");
        }
        return symbol;
    }

    /**
     * 10進数定数を15bit2進数表現に変換します。
     * @return　10進数定数を15bit2進数表現に変換したバイナリコード
     */
    public String getValueOfaddressInstruction() {
        // "@"より後の10進数表現整数を15bit2進数表現に変換して返す
        String value = this.currentCommand.substring(1);
        long valueLong = Long.parseLong(value);
        return String.format("%015d", Long.parseLong(Long.toBinaryString(valueLong)));
    }


    /**
     * @return 現C命令の dest ニーモニック
     */
    public String dest() {
        // =,;の前
        int index;
        String dest = "";
        if (this.currentCommand.contains("=")) {
            index = this.currentCommand.indexOf("=");
            dest = this.currentCommand.substring(0, index);
        } else if (this.currentCommand.contains(";")) {
            dest = "empty";
        }
        return dest;
    }

    /**
     * @return 現C命令の comp ニーモニック
     */
    public String comp() {
        if (this.currentCommand.contains("=")) {
            // =の後
            int index = this.currentCommand.indexOf("=");
            int commandLength = this.currentCommand.length();
            return this.currentCommand.substring(index + 1, commandLength);
        } else if (this.currentCommand.contains((";"))) {
            // ;の前
            int index = this.currentCommand.indexOf(";");
            return this.currentCommand.substring(0, index);
        } else {
            return "empty";
        }
    }

    /**
     * @return 現C命令の jump ニーモニック
     */
    public String jump() {
        if (this.currentCommand.contains(";")) {
            // ;の後
            int index = this.currentCommand.indexOf(";");
            int commandLength = this.currentCommand.length();
            return this.currentCommand.substring(index + 1, commandLength);
        } else {
            return "empty";
        }
    }
}
