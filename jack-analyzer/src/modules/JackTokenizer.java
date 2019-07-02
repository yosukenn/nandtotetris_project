package modules;

import static modules.data.Keyword.BOOLEAN;
import static modules.data.Keyword.CHAR;
import static modules.data.Keyword.CLASS;
import static modules.data.Keyword.CONSTRUCTOR;
import static modules.data.Keyword.DO;
import static modules.data.Keyword.ELSE;
import static modules.data.Keyword.FALSE;
import static modules.data.Keyword.FIELD;
import static modules.data.Keyword.FUNCTION;
import static modules.data.Keyword.IF;
import static modules.data.Keyword.INT;
import static modules.data.Keyword.LET;
import static modules.data.Keyword.METHOD;
import static modules.data.Keyword.NULL;
import static modules.data.Keyword.RETURN;
import static modules.data.Keyword.STATIC;
import static modules.data.Keyword.THIS;
import static modules.data.Keyword.TRUE;
import static modules.data.Keyword.VAR;
import static modules.data.Keyword.VOID;
import static modules.data.Keyword.WHILE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import modules.data.Keyword;
import modules.data.TokenType;

/**
 * トークナイザ。<br>
 * 入力ストリームから全てのコメントと空白文字を取り除き、Jack文法に従い、Jack言語のトークンへ分割する。
 */
public class JackTokenizer implements AutoCloseable {

  private String currentTokens = "";

  private BufferedReader reader;
  private int readCount = 0;
  private String currentLine = null;
  private Scanner scanner = null;

  private BufferedWriter writer;

  public JackTokenizer(String filename) throws IOException {
    this.reader = new BufferedReader(new FileReader(filename));

    int lastSlashIndexOfInputDir = filename.lastIndexOf("/");
    int lastDotIndexOfInputDir = filename.lastIndexOf(".");
    String coreName = filename.substring(lastSlashIndexOfInputDir, lastDotIndexOfInputDir);
    var writer =
        new BufferedWriter(
            new FileWriter(
                "/Users/yosukennturner/Desktop/nand2tetris/nandtotetris_project/jack-analyzer/output"
                    + coreName
                    + "T.xml"));
    this.writer = writer;
    this.writer.write("<tokens>");
    this.writer.flush();
    this.writer.newLine();
  }

  @Override
  public void close() throws IOException {
    this.writer.write("</tokens>");
    this.writer.flush();
    this.reader.close();
    this.scanner.close();
    this.writer.close();
  }

  /** 入力にまだトークンは存在するかを取得します。 */
  public boolean hasMoreTokens() {
    return this.currentTokens != null;
  }

  /**
   * 入力から次のトークンを取得し、それを現在のトークンとする。<br>
   * このルーチンは、hasMoreTokens() が true の場合のみ呼び出すことができる。<br>
   * また、最初は現トークンは設定されない。
   */
  public void advance() throws IOException {
    var readLineFlg = false;
    var readCountForAssert = readCount;

    while (true) {
      if (readCount == 0 || readLineFlg == true) {
        while (true) {
          var line = this.reader.readLine().trim();
          readCount++;
          if (line.contains("//")) {
            String[] lines = line.split("//");
            line = lines[0];
          }
          if (line != null || !line.equals("")) {
            currentLine = line;
            break;
          }
        }
      }

      if (readCountForAssert != readCount) {
        scanner.close();
        scanner = new Scanner(currentLine); // TODO このscannerが一生閉じない可能性がある問題
      }
      var tokenCandidate = scanner.next();
      if (tokenCandidate.startsWith("/**") || tokenCandidate.startsWith("/*")) {
        while (true) {
          if (scanner.hasNext()) {
            tokenCandidate = scanner.next();
          } else {
            readLineFlg = true;
            scanner.close();
            break;
          }

          if (tokenCandidate.startsWith("*/")) {
            tokenCandidate = scanner.next();
            break;
          } else {
            readLineFlg = true;
            scanner.close();
            break;
          }
        }
        if (readLineFlg == true) {
          continue;
        }
      }

      currentTokens = tokenCandidate;

      if (readLineFlg == false) {
        break;
      }
    }
    // TODO シンボル（,;()など）を１つのトークンとして判別する処理を追加する。
  }

  /** 現トークンの種類を返す。 */
  public TokenType tokenType() {
    if (checkKeywordType(currentTokens)) {
      return TokenType.KEYWORD;
    } else if (checkSymbolType(currentTokens)) {
      return TokenType.SYMBOL;
    } else if (checkIntConstType(currentTokens)) {
      return TokenType.INT_CONST;
    } else if (currentTokens.matches("[^\"\\n]")) {
      return TokenType.STRING_CONST;
    } else if (currentTokens.matches("^[a-zA-Z]+[a-zA-Z0-9]]")) {
      return TokenType.IDENTIFIER;
    } else {
      throw new RuntimeException("どのトークンタイプにも当てはまらない。");
    }
  }

  /**
   * 現トークンのキーワードを返す。<br>
   * このルーチンは、tokenType()がKEYWORDの場合のみ呼び出すことができる。
   */
  public Keyword keyword() throws IOException {
    this.writeTokenAsOneLine("keyword", currentTokens);
    switch (currentTokens) {
      case "class":
        return CLASS;
      case "method":
        return METHOD;
      case "function":
        return FUNCTION;
      case "constructor":
        return CONSTRUCTOR;
      case "int":
        return INT;
      case "boolean":
        return BOOLEAN;
      case "char":
        return CHAR;
      case "void":
        return VOID;
      case "var":
        return VAR;
      case "static":
        return STATIC;
      case "field":
        return FIELD;
      case "let":
        return LET;
      case "do":
        return DO;
      case "if":
        return IF;
      case "else":
        return ELSE;
      case "while":
        return WHILE;
      case "return":
        return RETURN;
      case "true":
        return TRUE;
      case "false":
        return FALSE;
      case "null":
        return NULL;
      case "this":
        return THIS;
      default:
        throw new IllegalStateException("現トークンが字句要素の種類が\"keyword\"でない。");
    }
  }

  /**
   * 現トークンの文字を返す。<br>
   * このルーチンは、tokenType()がSYMBOLの場合のみ呼び出すことができる。
   */
  public char symbol() throws IOException {
    String SYMBOL = "symbol";
    char currentSymbolToken = currentTokens.toCharArray()[0];
    switch (currentSymbolToken) {
      case '<':
        this.writeTokenAsOneLine(SYMBOL, "&lt;");
        break;
      case '>':
        this.writeTokenAsOneLine(SYMBOL, "&gt;");
        break;
      case '&':
        this.writeTokenAsOneLine(SYMBOL, "&amp;");
        break;
      default:
        this.writeTokenAsOneLine(SYMBOL, currentTokens);
        break;
    }
    return currentSymbolToken;
  }

  /**
   * 現トークンの識別子 identifier を返す。<br>
   * このルーチンは、tokenType() が IDENTIFIER の場合のみ呼び出すことができる。
   */
  public String identifier() throws IOException {
    this.writeTokenAsOneLine("identifier", currentTokens);
    return currentTokens;
  }

  /**
   * 現トークンの整数の値を返す。<br>
   * このルーチンは、tokenType()がINT_CONSTの場合のみ呼び出すことができる。
   */
  public int intVal() throws IOException {
    this.writeTokenAsOneLine("integerConstant", currentTokens);
    return Integer.parseInt(currentTokens);
  }

  /**
   * 現トークンの文字列を返す。<br>
   * このルーチンは、tokenType()がSTRING_CONSTの場合のみ呼び出すことができる。
   */
  public String stringVal() throws IOException {
    String currentStringToken =
        currentTokens.substring(1, currentTokens.length() + 1); // ダブルクォートを取り除く。
    this.writeTokenAsOneLine("stringConstant", currentStringToken);
    return currentTokens;
  }

  private boolean checkKeywordType(String currentTokens) {
    if (currentTokens.contains("class")
        || currentTokens.contains("constructor")
        || currentTokens.contains("function")
        || currentTokens.contains("method")
        || currentTokens.contains("field")
        || currentTokens.contains("static")
        || currentTokens.contains("var")
        || currentTokens.contains("int")
        || currentTokens.contains("char")
        || currentTokens.contains("boolean")
        || currentTokens.contains("void")
        || currentTokens.contains("true")
        || currentTokens.contains("false")
        || currentTokens.contains("null")
        || currentTokens.contains("this")
        || currentTokens.contains("let")
        || currentTokens.contains("do")
        || currentTokens.contains("if")
        || currentTokens.contains("else")
        || currentTokens.contains("while")
        || currentTokens.contains("return")) {
      return true;
    } else {
      return false;
    }
  }

  private boolean checkSymbolType(String currentTokens) {
    if (currentTokens.contains("{")
        || currentTokens.contains("}")
        || currentTokens.contains("(")
        || currentTokens.contains(")")
        || currentTokens.contains("[")
        || currentTokens.contains("]")
        || currentTokens.contains(".")
        || currentTokens.contains(",")
        || currentTokens.contains(";")
        || currentTokens.contains("+")
        || currentTokens.contains("-")
        || currentTokens.contains("*")
        || currentTokens.equals("/")
        || currentTokens.contains("&")
        || currentTokens.contains("|")
        || currentTokens.contains("<")
        || currentTokens.contains(">")
        || currentTokens.contains("=")
        || currentTokens.contains("~")) {
      return true;
    } else {
      return false;
    }
  }

  private boolean checkIntConstType(String currentTokens) {
    try {
      Short.parseShort(currentTokens);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private void writeTokenAsOneLine(String tokenType, String token) throws IOException {
    this.writer.write("<" + tokenType + "> " + token + " </" + tokenType + ">");
    this.writer.flush();
    this.writer.newLine();
  }
}
