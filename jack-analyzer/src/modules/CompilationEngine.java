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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

    // 最後にドキュメントの生成を行う。
    createXMLFile(this.outputFile, this.document);
  }

  public void compileClass() throws IOException {
    var firstLine = parseXMLLine(this.reader.readLine());
    if (!firstLine.get(CONTENT).equals("class")) {
      throw new IllegalStateException();
    }

    // classの書き込み
    var klass = document.createElement("class");
    document.appendChild(klass);

    // keywordの書き込み
    Element keyword = document.createElement(firstLine.get(ELEMENT_TYPE));
    klass.appendChild(keyword);
    keyword.appendChild(document.createTextNode(firstLine.get(ENCLOSED_CONTENT)));

    // identifierの書き込み
    var secondLine = parseXMLLine(this.reader.readLine());
    Element identifier = document.createElement(secondLine.get(ELEMENT_TYPE));
    klass.appendChild(identifier);
    identifier.appendChild(document.createTextNode(secondLine.get(ENCLOSED_CONTENT)));

    // symbolの書き込み
    var thirdLine = parseXMLLine(this.reader.readLine());
    Element symbol = document.createElement(thirdLine.get(ELEMENT_TYPE));
    klass.appendChild(symbol);
    symbol.appendChild(document.createTextNode(thirdLine.get(ENCLOSED_CONTENT)));

    // xml要素の種類によって適切な処理を呼び出す。
    /*
    式以外の要素を扱う場合、class直下の構文は以下になる。
    classVarDec : static, field
    classVarDec : function, constructor, method
    symbol
     */
    while (true) {
      var forthLine = parseXMLLine(this.reader.readLine());
      switch (forthLine.get(CONTENT)) {
        case "static":
        case "field":
          Element classVarDec = document.createElement("classVarDec");
          klass.appendChild(classVarDec);
          compileClassVarDec(classVarDec, forthLine);
          continue;
        case "function":
        case "constructor":
        case "method":
          Element subroutineDec = document.createElement("subroutineDec");
          klass.appendChild(subroutineDec);
          compileSubroutine(subroutineDec, forthLine);
          continue;
        case "symbol":
          // TODO 最後にsymbol"}"を出力
          break;
      }
      break;
    }
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
    return map;
  }

  /** スペースを囲い文字としてつけます。 */
  private String encloseBySpace(String string) {
    return " " + string + " ";
  }

  public void compileClassVarDec(Element classVarDec, Map<String, String> stringMap)
      throws IOException {
    // keywordの書き込み
    appendChildIncludeText(classVarDec, stringMap);

    // keywordの書き込み
    var secondLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(classVarDec, secondLine);

    // identifierの書き込み
    var thirdLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(classVarDec, thirdLine);

    // symbol「;」の書き込み
    var forthLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(classVarDec, forthLine);

    // TODO symbol「,」で複数のフィールドが宣言されている場合の処理の実装。
  }

  public void compileSubroutine(Element subroutine, Map<String, String> stringMap)
      throws IOException {
    // keywordの書き込み
    appendChildIncludeText(subroutine, stringMap);

    // keywordの書き込み
    var secondLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(subroutine, secondLine);

    // identifierの書き込み
    var thirdLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(subroutine, thirdLine);

    // symbol「(」の書き込み
    var forthLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(subroutine, forthLine);

    // TODO 引数を渡されている場合の処理
    compileParameterList(subroutine);

    // symbol「)」の書き込み
    var fifthLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(subroutine, fifthLine);

    // 「{ statements」}」の書き込み
    compileSubroutineBody(subroutine);
  }

  private void compileSubroutineBody(Element subroutine) throws IOException {
    Element subroutineBody = document.createElement("subroutineBody");
    subroutine.appendChild(subroutineBody);

    // symbol「{」の書き込み
    var firstLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(subroutine, firstLine);

    // TODO 一連の文をコンパイルする処理
    compileStatements();

    // symbol「}」の書き込み
    var secondLine = parseXMLLine(this.reader.readLine());
    appendChildIncludeText(subroutine, secondLine);
  }

  public void compileParameterList(Element subtoutine) {
    // TODO 引数を渡されている場合の処理
    Element parameterList = document.createElement("parameterList");
    subtoutine.appendChild(parameterList);
  }

  public void compileVarDec() {}

  public void compileStatements() {}

  public void compileDo() {}

  public void compileLet() {}

  public void compileWhile() {}

  public void comileReturn() {}

  public void compileIf() {}

  public void compileExpression() {}

  public void compileTerm() {}

  public void compileExpressionList() {}

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

  private void appendChildIncludeText(Element parent, Map<String, String> stringLine) {
    Element element = document.createElement(stringLine.get(ELEMENT_TYPE));
    parent.appendChild(element);
    element.appendChild(document.createTextNode(stringLine.get(ENCLOSED_CONTENT)));
  }
}
