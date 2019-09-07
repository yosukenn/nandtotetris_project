package modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import modules.data.IdentifierAttr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/*
TODO 修正できる点（多分やらないけど）
 - compileXX 内でwhilu文で繰り返し処理をしなくても、再帰的にメソッドを呼び出せばもっとシンプルにかける。
 */

/** 再帰によるトップダウン式の解析器<br> */
public class CompilationEngine implements AutoCloseable {

  BufferedReader reader;

  // XML文書作成例 https://teratail.com/questions/13471
  DocumentBuilder documentBuilder = null;
  Document document;

  private File outputFile;

  private static final String ELEMENT_TYPE = "elementType";
  private static final String CONTENT = "content";
  private static final String ENCLOSED_CONTENT = "enclosed_content";

  private static final String STATIC_KEYWORD = "static";
  private static final String FIELD_KEYWORD = "field";
  private static final String FUNCTION_KEYWORD = "function";
  private static final String CONSTRUCTOR_KEYWORD = "constructor";
  private static final String METHOD_KEYWORD = "method";

  public CompilationEngine(String inputFile, String outputFile) throws IOException {
    this.reader = new BufferedReader(new FileReader(inputFile));
    var firstLine = this.reader.readLine();
    if (!firstLine.equals("<tokens>")) {
      throw new IllegalArgumentException("入力ファイルの形式が正しくありません。firstLine=" + firstLine);
    }

    try {
      this.documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      System.out.println("Document生成に失敗しました。");
      e.printStackTrace();
    }
    document = documentBuilder.newDocument();
    document.setXmlStandalone(true);

    this.outputFile = new File(outputFile);
  }

  @Override
  public void close() throws IOException {
    this.reader.close();

    // 最後に構文木をXML形式での生成する。
    createXMLFile(this.outputFile, this.document);
  }

  public void compileClass() throws IOException {
    // シンボルテーブルの生成
    var classSymbolTable = new SymbolTable();

    var firstLine = parseXMLLine(this.reader.readLine());
    if (!firstLine.get(CONTENT).equals("class")) {
      throw new IllegalStateException();
    }

    // classの書き込み
    var klass = document.createElement("class");
    document.appendChild(klass);

    // keyword"class"の書き込み
    Element keyword = document.createElement(firstLine.get(ELEMENT_TYPE));
    klass.appendChild(keyword);
    keyword.appendChild(document.createTextNode(firstLine.get(ENCLOSED_CONTENT)));

    // クラス名となるidentifierの書き込み。クラス名はシンボルテーブルに記録しない。
    var secondLine = parseXMLLine(this.reader.readLine());
    writeIdentifierForSymbolTable(klass, secondLine, "class", "defined", "not applicable", 0);

    // symbol"{"の書き込み
    var thirdLine = parseXMLLine(this.reader.readLine());
    Element symbol = document.createElement(thirdLine.get(ELEMENT_TYPE));
    klass.appendChild(symbol);
    symbol.appendChild(document.createTextNode(thirdLine.get(ENCLOSED_CONTENT)));

    // xml要素の種類によって適切な処理を呼び出す。
    while (true) {
      var forthLine = parseXMLLine(this.reader.readLine());
      switch (forthLine.get(CONTENT)) {
        case STATIC_KEYWORD:
        case FIELD_KEYWORD:
          Element classVarDec = document.createElement("classVarDec");
          klass.appendChild(classVarDec);
          compileClassVarDec(classSymbolTable, classVarDec, forthLine);
          continue;
        case FUNCTION_KEYWORD:
        case CONSTRUCTOR_KEYWORD:
        case METHOD_KEYWORD:
          Element subroutineDec = document.createElement("subroutineDec");
          klass.appendChild(subroutineDec);
          compileSubroutine(subroutineDec, forthLine);
          continue;
      }
      break;
    }
    // 最後にsymbol"}"を出力 TODO 過剰に読み取っていて"}"を出力できていないので、一旦無理やり出力している。
    //    var fifthLine = parseXMLLine(this.reader.readLine());
    //    appendChildIncludeText(klass, fifthLine);
    appendChildIncludeText(
        klass, Map.of(ELEMENT_TYPE, "symbol", CONTENT, "}", ENCLOSED_CONTENT, " } "));
  }

  private Map<String, String> parseXMLLine(String line) {
    var map = new HashMap<String, String>();

    String regex = "(<\\w+>) (\\S+) (</\\w+>)";
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(line);

    if (m.find()) {
      map.put(ELEMENT_TYPE, m.group(1).substring(1, m.group(1).length() - 1));
      map.put(CONTENT, m.group(2));
      map.put(ENCLOSED_CONTENT, encloseBySpace(m.group(2)));
    }

    if (map.get(ELEMENT_TYPE) == null
        && map.get(CONTENT) == null
        && map.get(ENCLOSED_CONTENT) == null) {
      String[] strings = line.split(" ");
      map.put(ELEMENT_TYPE, strings[0].substring(1, strings[0].length() - 1));
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(strings[1]);
      for (int i = 2; i < strings.length - 1; i++) {
        stringBuilder.append(" ");
        stringBuilder.append(strings[i]);
      }
      map.put(CONTENT, stringBuilder.toString());
      map.put(ENCLOSED_CONTENT, " " + stringBuilder.toString() + " ");
    }

    return map;
  }

  /** スペースを囲い文字としてつけます。 */
  private String encloseBySpace(String string) {
    return " " + string + " ";
  }

  /** スタティック宣言、フィールド宣言をコンパイルする。 */
  public void compileClassVarDec(
      SymbolTable classSymbolTable, Element classVarDec, Map<String, String> stringMap)
      throws IOException {
    // keyword "static", "field"の書き込み
    appendChildIncludeText(classVarDec, stringMap);

    // データ型を表すkeywordの書き込み
    var secondLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(classVarDec, secondLine);

    // 変数名を表すidentifierの書き込み
    var thirdLine = parseXMLLine(this.reader.readLine());
    writeIdentifierForSymbolTable(
        classVarDec,
        thirdLine,
        stringMap.get(CONTENT),
        "defined",
        stringMap.get(CONTENT),
        0 /* indexは一旦一律0にする */);

    // identifierのシンボルテーブルへの登録。
    classSymbolTable.define(
        thirdLine.get(CONTENT),
        secondLine.get(CONTENT),
        IdentifierAttr.fromCode(stringMap.get(CONTENT)));

    var forthLine = parseXMLLine(this.reader.readLine());
    while (true) {
      if (forthLine.get(CONTENT).equals(";")) {
        // symbol";"の書き込み
        appendChildIncludeText(classVarDec, forthLine);
        break;
      } else if (forthLine.get(CONTENT).equals(",")) { // ","で区切られて複数の変数を宣言している場合
        appendChildIncludeText(classVarDec, forthLine);

        // 変数名を表すidentifierの書き込み
        var fifthLine = parseXMLLine(this.reader.readLine());
        writeIdentifierForSymbolTable(
            classVarDec,
            fifthLine,
            stringMap.get(CONTENT),
            "defined",
            stringMap.get(CONTENT),
            0 /* indexは一旦一律0にする */);

        // identifierのシンボルテーブルへの登録。
        classSymbolTable.define(
            fifthLine.get(CONTENT),
            secondLine.get(CONTENT),
            IdentifierAttr.fromCode(stringMap.get(CONTENT)));

        forthLine = parseXMLLine(this.reader.readLine());
        continue;
      }
    }
  }

  /** メソッド、ファンクション、コンストラクタをコンパイルする。 */
  public void compileSubroutine(Element subroutine, Map<String, String> stringMap)
      throws IOException {
    // サブルーチンスコープのシンボルテーブルを作成
    var subroutineSymbolTable = new SymbolTable();
    subroutineSymbolTable.startSubroutine(); // 何もしていない。

    // keyword "function", "constructor", "method"の書き込み
    appendChildIncludeText(subroutine, stringMap);

    // データ型を表すkeywordの書き込み
    var secondLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(subroutine, secondLine);

    // メソッド、ファンクション、コンストラクタを表すidentifierの書き込み
    var thirdLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(subroutine, thirdLine);

    // symbol「(」の書き込み
    var forthLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(subroutine, forthLine);

    compileParameterList(subroutineSymbolTable, subroutine);

    // symbol「)」の書き込み
    var fifthLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(subroutine, fifthLine);

    // 「{ statements }」の書き込み TODO statements内の変数をシンボルテーブルに登録する処理を追加。
    compileSubroutineBody(subroutine);
  }

  private void compileSubroutineBody(Element subroutine) throws IOException {
    Element subroutineBody = document.createElement("subroutineBody");
    subroutine.appendChild(subroutineBody);

    // symbol「{」の書き込み
    var firstLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(subroutineBody, firstLine);

    while (true) {
      var secondLine = parseXMLLine(this.reader.readLine());
      if (secondLine.get(CONTENT).equals("var")) {
        compileVarDec(subroutineBody, secondLine);
      } else {
        compileStatements(subroutineBody, secondLine);
        break;
      }
    }

    // symbol「}」の書き込み
    var thirdLine = Map.of(ELEMENT_TYPE, "symbol", CONTENT, "}", ENCLOSED_CONTENT, " } ");
    appendChildIncludeText(subroutineBody, thirdLine);
  }

  public void compileParameterList(SymbolTable subroutineSymbolTable, Element subroutine)
      throws IOException {
    Element parameterList = document.createElement("parameterList");
    subroutine.appendChild(parameterList);

    this.reader.mark(100);
    var firstLine = parseXMLLine(this.reader.readLine());
    if (firstLine.get(ELEMENT_TYPE).equals("keyword")) {
      while (true) {
        // keyword(引数の型)を構文木に書き込む
        appendChildIncludeText(parameterList, firstLine);

        // identifier(引数の変数名)を構文木に書き込む
        var secondLine = parseXMLLine(this.reader.readLine());
        writeIdentifierForSymbolTable(
            parameterList,
            secondLine,
            "argument",
            "defined",
            "argument",
            0 /* 実行番号はシンボルテーブルを使うようになってから管理するので、一旦一律0にする。 */);

        // identifierをシンボルテーブルに登録する。
        subroutineSymbolTable.define(
            secondLine.get(CONTENT), firstLine.get(CONTENT), IdentifierAttr.ARG);

        // まだ引数があったらコンパイル。なかったらcompileParameterListを終了する。
        this.reader.mark(100);
        var thirdLine = parseXMLLine(this.reader.readLine());
        if (thirdLine.get(CONTENT).equals(",")) {
          appendChildIncludeText(parameterList, thirdLine);
          firstLine = parseXMLLine(this.reader.readLine());
        } else if (thirdLine.get(CONTENT).equals(")")) {
          this.reader.reset();
          break;
        }
      }
    } else {
      this.reader.reset();
    }
  }

  /**
   * var宣言は含まない。
   *
   * @param subroutineBody
   * @throws IOException
   */
  public void compileStatements(Element subroutineBody, Map<String, String> firstLine)
      throws IOException {
    Element statements = document.createElement("statements");
    subroutineBody.appendChild(statements);

    int returnFlg = 0;

    var line = firstLine;
    while (true) {
      switch (line.get(CONTENT)) {
        case "do":
          compileDo(statements, line);
          break;
        case "let":
          compileLet(statements, line);
          break;
        case "while":
          compileWhile(statements, line);
          break;
        case "return":
          compileReturn(statements, line);
          returnFlg = 1;
          break;
        case "if":
          compileIf(statements, line);
          break;
      }
      var readLine = this.reader.readLine();
      if (readLine == null) {
        break;
      }
      line = parseXMLLine(readLine);
      if (line.get(CONTENT).equals("}")) {
        break;
        // TODO resetしないといけないかも
      }
    }
  }

  public void compileVarDec(Element subroutineBody, Map<String, String> firstLine)
      throws IOException {
    Element varDec = document.createElement("varDec");
    subroutineBody.appendChild(varDec);

    // keyword「var」の出力
    appendChildIncludeText(varDec, firstLine);

    // keyword dataType の出力
    var secondLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(varDec, secondLine);

    // identifierの出力
    var thirdLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(varDec, thirdLine);

    // 変数の出力
    var forthLine = parseXMLLine(this.reader.readLine());
    while (true) {
      if (forthLine.get(CONTENT).equals(";")) {
        // symbol「;」の書き込み
        appendChildIncludeText(varDec, forthLine);
        break;
      } else if (forthLine.get(CONTENT).equals(",")) {
        appendChildIncludeText(varDec, forthLine);

        var fifthLine = parseXMLLine(this.reader.readLine());
        appendChildIncludeText(varDec, fifthLine);

        forthLine = parseXMLLine(this.reader.readLine());
        continue;
      }
    }
  }

  public void compileDo(Element subroutineBody, Map<String, String> firstLine) throws IOException {
    Element doStatement = document.createElement("doStatement");
    subroutineBody.appendChild(doStatement);

    // keyword「do」の出力
    appendChildIncludeText(doStatement, firstLine);

    // identifier 変数名 or メソッド名 の出力 TODO シンボルテーブルを使うようになれば不要。
    var secondLine = parseXMLLine(this.reader.readLine());
    writeIdentifierForSymbolTable(
        doStatement,
        secondLine,
        "class or subroutine",
        "used",
        "class_name or subroutine_name",
        0 /* 暫定的に0 */);

    var thirdLine = parseXMLLine(this.reader.readLine());
    if (thirdLine.get(CONTENT).equals(".")) {
      // ".()"のコンパイル
      compileCallSubroutine(doStatement, thirdLine);

      // symbol";"の出力
      var forthLine = parseXMLLine(this.reader.readLine());
      appendChildIncludeText(doStatement, forthLine);

    } else if (thirdLine.get(CONTENT).equals("(")) {
      // メソッドの実行主体が書かれていない場合の処理(privateメソッド)

      appendChildIncludeText(doStatement, thirdLine);

      compileExpressionList(doStatement);

      // symbol「)」の出力
      var forthLine = parseXMLLine(this.reader.readLine());
      appendChildIncludeText(doStatement, forthLine);

      // symbol「;」の出力
      var fifthLine = parseXMLLine(this.reader.readLine());
      appendChildIncludeText(doStatement, fifthLine);
    }
  }

  public void compileLet(Element subroutineBody, Map<String, String> firstLine) throws IOException {
    Element letStatement = document.createElement("letStatement");
    subroutineBody.appendChild(letStatement);

    // keyword"let"のコンパイル
    appendChildIncludeText(letStatement, firstLine);

    // identifierの出力
    var secondLine = parseXMLLine(this.reader.readLine());
    writeIdentifierForSymbolTable( // TODO シンボルテーブルを使うようになれば不要。
        letStatement, secondLine, "static or field", "used", "static or field", 0 /* 暫定的に0 */);

    var thirdLine = parseXMLLine(this.reader.readLine());
    if (thirdLine.get(CONTENT).equals("[")) {
      // 配列宣言"[ iterator ]"部分のコンパイル
      compileArrayIterator(letStatement, thirdLine);

      // symbol「=」のコンパイル
      appendChildIncludeText(letStatement, parseXMLLine(this.reader.readLine()));

    } else {
      // symbol「=」の出力
      appendChildIncludeText(letStatement, thirdLine);
    }

    compileExpression(letStatement);

    // symbol「;」の出力
    var forthLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(letStatement, forthLine);
  }

  public void compileWhile(Element parent, Map<String, String> firstLine) throws IOException {
    Element whileStatement = document.createElement("whileStatement");
    parent.appendChild(whileStatement);

    // keyword"while"の書き込み
    appendChildIncludeText(whileStatement, firstLine);

    // symbol"(" の書き込み
    var secondLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(whileStatement, secondLine);

    compileExpression(whileStatement);

    // symbol ")" の書き込み
    var thirdLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(whileStatement, thirdLine);

    // symbol「{」の書き込み
    var forthLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(whileStatement, forthLine);

    var fifthLine = parseXMLLine(this.reader.readLine());
    compileStatements(whileStatement, fifthLine);

    // TODO compileStatements()で行を読み込み過ぎているのを修正する必要があるが、これを修正すると崩壊するので無理やり"}"をコンパイルするようにしている。
    // symbol「}」の書き込み
    //    var sixthLine = parseXMLLine(this.reader.readLine());
    //    appendChildIncludeText(whileStatement, sixthLine);
    var closeSymbolLine = Map.of(ELEMENT_TYPE, "symbol", CONTENT, "}", ENCLOSED_CONTENT, " } ");
    appendChildIncludeText(whileStatement, closeSymbolLine);
  }

  public void compileReturn(Element parent, Map<String, String> firstLine) throws IOException {
    Element returnStatement = document.createElement("returnStatement");
    parent.appendChild(returnStatement);

    // keyword"return"のコンパイル
    appendChildIncludeText(returnStatement, firstLine);

    this.reader.mark(100);
    var secondLine = parseXMLLine(this.reader.readLine());
    if (secondLine.get(ELEMENT_TYPE).equals("identifier")
        || secondLine.get(ELEMENT_TYPE).equals("keyword")) {
      this.reader.reset();
      compileExpression(returnStatement);

      var forthLine = parseXMLLine(this.reader.readLine());
      appendChildIncludeText(returnStatement, forthLine);

    } else if (secondLine.get(ELEMENT_TYPE).equals("symbol")) {
      appendChildIncludeText(returnStatement, secondLine);
    }
  }

  public void compileIf(Element parent, Map<String, String> firstLine) throws IOException {
    Element ifStatement = document.createElement("ifStatement");
    parent.appendChild(ifStatement);

    // keyword"if"のコンパイル
    appendChildIncludeText(ifStatement, firstLine);

    // symbol"("のコンパイル
    var secondLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(ifStatement, secondLine);

    // if条件式のコンパイル
    compileExpression(ifStatement);

    // symbol")"のコンパイル
    var thirdLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(ifStatement, thirdLine);

    // symbol"{"のコンパイル
    appendChildIncludeText(ifStatement, parseXMLLine(this.reader.readLine()));

    // statementsのコンパイル
    var forthLine = parseXMLLine(this.reader.readLine());
    compileStatements(ifStatement, forthLine);

    // symbol"}"のコンパイル
    var fifthLine = Map.of(ELEMENT_TYPE, "symbol", CONTENT, "}", ENCLOSED_CONTENT, " } ");
    appendChildIncludeText(ifStatement, fifthLine);

    this.reader.mark(100); // 取り消せるようにマーク
    var sixthLine = parseXMLLine(this.reader.readLine());
    if (sixthLine.get(CONTENT).equals("else")) {
      // keyword"else"のコンパイル
      appendChildIncludeText(ifStatement, sixthLine);

      // symbol"{"のコンパイル
      appendChildIncludeText(ifStatement, parseXMLLine(this.reader.readLine()));

      // statementsのコンパイル
      compileStatements(ifStatement, parseXMLLine(this.reader.readLine()));

      // symbol"}"のコンパイル
      var seventhLine = Map.of(ELEMENT_TYPE, "symbol", CONTENT, "}", ENCLOSED_CONTENT, " } ");
      appendChildIncludeText(ifStatement, seventhLine);
    } else {
      this.reader.reset(); // TODO 読み込んだ結果"else"がないIf文だった場合、ちゃんと読み込みを取り消せられているか確認。
    }
  }

  public void compileExpression(Element parent) throws IOException {
    Element expression = document.createElement("expression");
    parent.appendChild(expression);

    compileTerm(expression);

    this.reader.mark(100);
    var nextEle = parseXMLLine(this.reader.readLine());
    if (nextEle.get(CONTENT).equals("|")
        || nextEle.get(CONTENT).equals("*")
        || nextEle.get(CONTENT).equals("/")
        || nextEle.get(CONTENT).equals("+")
        || nextEle.get(CONTENT).equals("-")
        || nextEle.get(CONTENT).equals("=")) {
      // TODO "|", "*", "/", "+"が複数回出てくる場合が出てきたら対応を追加する。
      // 複数の変数を代入する場合の場合
      appendChildIncludeText(expression, nextEle);
      compileTerm(expression);

    } else {
      this.reader.reset();
    }

    this.reader.mark(100);
    var secondLine = parseXMLLine(this.reader.readLine());
    if (secondLine.get(CONTENT).equals("&lt;")) {
      appendChildIncludeText(
          expression, Map.of(ELEMENT_TYPE, "symbol", CONTENT, "<", ENCLOSED_CONTENT, " < "));
      compileTerm(expression);
    } else if (secondLine.get(CONTENT).equals("&gt;")) {
      appendChildIncludeText(
          expression, Map.of(ELEMENT_TYPE, "symbol", CONTENT, ">", ENCLOSED_CONTENT, " > "));
      compileTerm(expression);
    } else {
      this.reader.reset();
    }

    this.reader.mark(100);
    var thirdLine = parseXMLLine(this.reader.readLine());
    if (thirdLine.get(CONTENT).equals("&amp;")) { // TODO "&"が複数出てくるパターン対策。
      appendChildIncludeText(
          expression, Map.of(ELEMENT_TYPE, "symbol", CONTENT, "&", ENCLOSED_CONTENT, " & "));
      compileTerm(expression);
    } else {
      this.reader.reset();
    }
  }

  /** 式(Expression)の一部分をコンパイルする */
  public void compileTerm(Element parent) throws IOException {
    Element term = document.createElement("term");
    parent.appendChild(term);

    var firstLine = parseXMLLine(this.reader.readLine());
    if (firstLine.get(ELEMENT_TYPE).equals("identifier")) { // TODO シンボルテーブルを使うようになれば不要。
      // class_name or valiable
      writeIdentifierForSymbolTable(
          term,
          firstLine,
          "class or argument or static or field or subroutine",
          "used",
          "var or argument or static or field",
          0 /* 暫定的に0 */);
    } else {
      appendChildIncludeText(term, firstLine);
    }

    if (firstLine.get(CONTENT).equals("(")) {
      // "( sign variable )" をコンパイルする。

      // "sign variable"
      compileExpression(term);

      // ")"
      var secondLine = parseXMLLine(this.reader.readLine());
      appendChildIncludeText(term, secondLine);

    } else if (firstLine.get(CONTENT).equals("-") || firstLine.get(CONTENT).equals("~")) {
      // 符号付き変数のコンパイル
      compileTerm(term);

    } else {
      this.reader.mark(100);
      var secondLine = parseXMLLine(this.reader.readLine());
      if (secondLine.get(CONTENT).equals(".")) {
        // サブルーチン呼び出し
        compileCallSubroutine(term, secondLine);
      } else if (secondLine.get(CONTENT).equals("[")) {
        // 配列宣言のコンパイル
        compileArrayIterator(term, secondLine);

      } else {
        this.reader.reset();
      }
    }
  }

  public void compileExpressionList(Element subroutineBody) throws IOException {
    Element expressionList = document.createElement("expressionList");
    subroutineBody.appendChild(expressionList);

    while (true) {
      this.reader.mark(100);
      var line = parseXMLLine(this.reader.readLine());
      if (line.get(ELEMENT_TYPE).equals("keyword")
          || line.get(ELEMENT_TYPE).equals("identifier")
          || line.get(ELEMENT_TYPE).equals("integerConstant")
          || line.get(ELEMENT_TYPE).equals("stringConstant")
          || line.get(CONTENT).equals("(")) {
        this.reader.reset();
        compileExpression(expressionList);
      } else if (line.get(CONTENT).equals(",")) {
        appendChildIncludeText(expressionList, line);
      } else {
        this.reader.reset();
        break;
      }
    }

    // TODO 実引数が複数ある時の処理
  }

  private static boolean createXMLFile(File file, Document document) {

    // Transformerインスタンスの生成
    Transformer transformer = null;
    try {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      transformer = transformerFactory.newTransformer();
    } catch (TransformerConfigurationException e) {
      e.printStackTrace();
      return false;
    }

    // Transformerの設定
    transformer.setOutputProperty("indent", "yes"); // 改行指定
    transformer.setOutputProperty("encoding", "UTF-8"); // エンコーディング
    transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2"); // インデントの桁指定
    transformer.setOutputProperty("method", "html"); // XML宣言部分を省略

    // XMLファイルの作成
    try {
      transformer.transform(new DOMSource(document), new StreamResult(file));
    } catch (TransformerException e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

  /** サーブルーチン呼び出しの内、".( arguments... )"部分のコンパイルを行う。 */
  private void compileCallSubroutine(Element parent, Map<String, String> firstLine)
      throws IOException {
    // symbol「.」の出力
    appendChildIncludeText(parent, firstLine);

    // identifierの出力
    var secondLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(parent, secondLine);

    // symbol「(」の出力
    var thirdLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(parent, thirdLine);

    compileExpressionList(parent);

    // symbol「)」の出力
    var sixthLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(parent, sixthLine);
  }

  /*
       // 配列宣言"[ iterator ]"部分のコンパイル
     compileArrayIterator(letStatement, thirdLine)

     // symbol"["のコンパイル
     appendChildIncludeText(letStatement, thirdLine);

     // 配列イテレータのコンパイル
     compileExpression(letStatement);

     // symbol「]」のコンパイル
     appendChildIncludeText(letStatement, parseXMLLine(this.reader.readLine()));
  */

  private void compileArrayIterator(Element parent, Map<String, String> firstLine)
      throws IOException {
    // symbol"["のコンパイル
    appendChildIncludeText(parent, firstLine);

    // 配列イテレータのコンパイル
    compileExpression(parent);

    // symbol「]」のコンパイル
    appendChildIncludeText(parent, parseXMLLine(this.reader.readLine()));
  }

  private void appendChildIncludeText(Element parent, Map<String, String> stringLine) {
    Element element = document.createElement(stringLine.get(ELEMENT_TYPE));
    parent.appendChild(element);
    element.appendChild(document.createTextNode(stringLine.get(ENCLOSED_CONTENT)));
  }

  /**
   * ソースコードに出てきた識別子に追加の情報を付与したのち、XML要素"identifier"に詰める。<br>
   * TODO シンボルテーブル使うようになれば不要。
   *
   * <ul>
   *   <li>識別子のカテゴリ
   *   <li>識別子は定義されているか、使用されているか
   *   <li>識別子の4つの属性(var, argument, static, field, not applicable)と、実行番号。
   * </ul>
   */
  private void writeIdentifierForSymbolTable(
      Element parent,
      Map<String, String> contentMap,
      String category,
      String be,
      String attribute,
      int index) {

    Element identifierEle = document.createElement(contentMap.get(ELEMENT_TYPE));
    parent.appendChild(identifierEle);

    // content element の作成とtextの設定
    var contentEle = document.createElement("content");
    identifierEle.appendChild(contentEle);
    contentEle.appendChild(document.createTextNode(contentMap.get(ENCLOSED_CONTENT)));

    // category element の作成とtextの設定
    var categoryEle = document.createElement("category");
    identifierEle.appendChild(categoryEle);
    categoryEle.appendChild(document.createTextNode(category));

    // be element の作成とtextの設定
    var beEle = document.createElement("be");
    identifierEle.appendChild(beEle);
    beEle.appendChild(document.createTextNode(be));

    // attribute element の作成とtextの設定
    var attrEle = document.createElement("attribute");
    identifierEle.appendChild(attrEle);
    attrEle.appendChild(document.createTextNode(attribute));

    // index element の作成とtextの設定
    var indexEle = document.createElement("index");
    identifierEle.appendChild(indexEle);
    indexEle.appendChild(document.createTextNode(Integer.toString(index)));
  }
}
