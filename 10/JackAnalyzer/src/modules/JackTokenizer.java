package modules;

import static modules.data.Keyword.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import modules.data.Keyword;
import modules.data.TokenType;

/**
 * トークナイザ。<br>
 * 入力ストリームから全てのコメントと空白文字を取り除き、Jack文法に従い、Jack言語のトークンへ分割する。
 */
public class JackTokenizer {

  public Scanner scanner; // scannerがfinalクラスで継承できないため。

  private String currentTokens = "";
  private BufferedWriter writer;

  public JackTokenizer(String filename) throws IOException {
    this.scanner = new Scanner(filename);

    int lastSlashIndexOfInputDir = filename.lastIndexOf("/");
    int lastDotIndexOfInputDir = filename.lastIndexOf(".");
    String coreName = filename.substring(lastSlashIndexOfInputDir, lastDotIndexOfInputDir + 1);
    var writer =
        new BufferedWriter(
            new FileWriter(
                "/Users/yosukennturner/Desktop/nand2tetris/nandtotetris_project/10/output"
                    + coreName
                    + "T.xml"));
    this.writer = writer;
    this.writer.write("<tokens>");
    this.writer.flush();
    this.writer.write("\t");
    this.writer.flush();
    this.writer.newLine();
  }

  public void close() throws IOException {
    this.writer.write("</tokens>");
    this.writer.flush();
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
    this.currentTokens = this.scanner.next(); // TODO next()では改行できないかもしれない。
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
    this.writeTokenAndTab("keyword", currentTokens);
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
    char currentSymbolTokn = currentTokens.toCharArray()[0];
    switch (currentSymbolTokn) {
      case '<':
        this.writeTokenAndTab(SYMBOL, "&lt;");
        break;
      case '>':
        this.writeTokenAndTab(SYMBOL, "&gt;");
        break;
      case '&':
        this.writeTokenAndTab(SYMBOL, "&amp;");
        break;
      default:
        this.writeTokenAndTab(SYMBOL, currentTokens);
        break;
    }
    return currentSymbolTokn;
  }

  /**
   * 現トークンの識別子 identifier を返す。<br>
   * このルーチンは、tokenType() が IDENTIFIER の場合のみ呼び出すことができる。
   */
  public String identifier() {
    return currentTokens;
  }

  /**
   * 現トークンの整数の値を返す。<br>
   * このルーチンは、tokenType()がINT_CONSTの場合のみ呼び出すことができる。
   */
  public int intVal() {
    return Integer.parseInt(currentTokens);
  }

  /**
   * 現トークンの文字列を返す。<br>
   * このルーチンは、tokenType()がSTRING_CONSTの場合のみ呼び出すことができる。
   */
  public String stringVal() {
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
        || currentTokens.contains("/")
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

  private void writeTokenAndTab(String tokenType, String token) throws IOException {
    this.writer.write("<" + tokenType + "> " + token + " </" + tokenType + ">");
    this.writer.flush();
    this.writer.write("\t");
    this.writer.flush();
  }
}
