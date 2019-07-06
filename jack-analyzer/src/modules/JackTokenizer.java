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
public class JackTokenizer implements AutoCloseable {

  private String currentToken = "";

  private Scanner scanner;
  private BufferedWriter writer;

  public JackTokenizer(String outputFilename, String program) throws IOException {
    this.scanner = new Scanner(program);

    this.writer = new BufferedWriter(new FileWriter(outputFilename));
    this.writer.write("<tokens>");
    this.writer.newLine();
  }

  @Override
  public void close() throws IOException {
    this.writer.write("</tokens>");
    this.writer.flush();
    this.scanner.close();
    this.writer.close();
  }

  /** 入力にまだトークンは存在するかを取得します。 */
  public boolean hasMoreTokens() {
    return this.scanner.hasNext();
  }

  /**
   * 入力から次のトークンを取得し、それを現在のトークンとする。<br>
   * このルーチンは、hasMoreTokens() が true の場合のみ呼び出すことができる。<br>
   * また、最初は現トークンは設定されない。
   */
  public void advance() {
    // TODO StringConstantを読み込めるように修正する。

    var tokenCandidate = scanner.next();

    if (tokenCandidate.startsWith("/**") || tokenCandidate.startsWith("/*")) {
      while (true) {
        tokenCandidate = scanner.next();

        if (tokenCandidate.startsWith("*/")) {
          tokenCandidate = scanner.next();
          break;
        }
      }
    }
    currentToken = tokenCandidate;
  }

  /** 現トークンの種類を返す。 */
  public TokenType tokenType() {
    if (checkKeywordType(currentToken)) {
      return TokenType.KEYWORD;
    } else if (checkSymbolType(currentToken)) {
      return TokenType.SYMBOL;
    } else if (checkIntConstType(currentToken)) {
      return TokenType.INT_CONST;
    } else if (currentToken.matches("^[a-zA-Z]+[a-zA-Z0-9]*")) {
      return TokenType.IDENTIFIER;
    } else if (currentToken.matches("[^\"\\n]")) { // TODO 正規表現を修正する。
      return TokenType.STRING_CONST;
    } else {
      throw new RuntimeException("どのトークンタイプにも当てはまらない。");
    }
  }

  /**
   * 現トークンのキーワードを返す。<br>
   * このルーチンは、tokenType()がKEYWORDの場合のみ呼び出すことができる。
   */
  public Keyword keyword() throws IOException {
    this.writeTokenAsOneLine("keyword", currentToken);
    switch (currentToken) {
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
    char currentSymbolToken = currentToken.toCharArray()[0];
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
        this.writeTokenAsOneLine(SYMBOL, currentToken);
        break;
    }
    return currentSymbolToken;
  }

  /**
   * 現トークンの識別子 identifier を返す。<br>
   * このルーチンは、tokenType() が IDENTIFIER の場合のみ呼び出すことができる。
   */
  public String identifier() throws IOException {
    this.writeTokenAsOneLine("identifier", currentToken);
    return currentToken;
  }

  /**
   * 現トークンの整数の値を返す。<br>
   * このルーチンは、tokenType()がINT_CONSTの場合のみ呼び出すことができる。
   */
  public int intVal() throws IOException {
    this.writeTokenAsOneLine("integerConstant", currentToken);
    return Integer.parseInt(currentToken);
  }

  /**
   * 現トークンの文字列を返す。<br>
   * このルーチンは、tokenType()がSTRING_CONSTの場合のみ呼び出すことができる。
   */
  public String stringVal() throws IOException {
    //    String currentStringToken = currentToken.substring(1, currentToken.length()); //
    // ダブルクォートを取り除く。
    this.writeTokenAsOneLine("stringConstant", currentToken);
    return currentToken;
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
