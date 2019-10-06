package modules;

import static modules.data.Segment.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import modules.data.ArithmeticCommand;
import modules.data.IdentifierAttr;
import modules.data.Segment;

/**
 * 再帰によるトップダウン式の解析器<br>
 * 構文木であるXMLファイル生成関連のプログラムにはを付与しています。
 */
public class CompilationEngine implements AutoCloseable {

  BufferedReader reader;

  /** コンパイル対象のクラスの名前 */
  private String compiledClassName;

  /** 生成するVMファイルの親ディレクトリを表すファイルオブジェクト */
  private File parentPath;

  // xxT.wmlファイルの行内容を表すプロパティ一覧
  private static final String ELEMENT_TYPE = "elementType";
  private static final String CONTENT = "content";
  private static final String ENCLOSED_CONTENT = "enclosed_content";

  // キーワード一覧
  private static final String STATIC_KEYWORD = "static";
  private static final String FIELD_KEYWORD = "field";
  private static final String FUNCTION_KEYWORD = "function";
  private static final String CONSTRUCTOR_KEYWORD = "constructor";
  private static final String METHOD_KEYWORD = "method";

  // VMコマンド用定数
  private static final String SEGMENT = "segment";
  private static final String INDEX = "expression";
  private static final String DO_NOTHING = "do nothing";

  // OSの関数名
  private static final String MULTIPLY = "Math.multiply";
  private static final String DIVIDE = "Math.divide";

  // ラベル命名用文字列
  private static final String LABEL = "label";

  public CompilationEngine(File parentDir, String inputFile, String outputFile) throws IOException {
    this.parentPath = parentDir;

    this.reader = new BufferedReader(new FileReader(inputFile));
    var firstLine = this.reader.readLine();
    if (!firstLine.equals("<tokens>")) {
      throw new IllegalArgumentException("入力ファイルの形式が正しくありません。firstLine=" + firstLine);
    }
  }

  @Override
  public void close() throws IOException {
    this.reader.close();
  }

  public void compileClass() throws IOException {
    // シンボルテーブルの生成
    var classSymbolTable = new SymbolTable();

    var firstLine = parseXMLLine(this.reader.readLine());
    if (!firstLine.get(CONTENT).equals("class")) {
      throw new IllegalStateException();
    }

    // クラス名となるidentifier読み込み。クラス名はシンボルテーブルに記録しない。
    var secondLine = parseXMLLine(this.reader.readLine());
    this.compiledClassName = secondLine.get(CONTENT);

    // VMWriterの生成
    try (var vmWriter = new VMWriter(new File(parentPath, compiledClassName + ".vm"))) {
      // symbol"{"の読み込み
      parseXMLLine(this.reader.readLine());

      // xml要素の種類によって適切な処理を呼び出す。
      while (true) {
        var thirdLine = parseXMLLine(this.reader.readLine());
        switch (thirdLine.get(CONTENT)) {
          case STATIC_KEYWORD:
          case FIELD_KEYWORD:
            compileClassVarDec(classSymbolTable, thirdLine);
            continue;
          case FUNCTION_KEYWORD:
          case CONSTRUCTOR_KEYWORD:
          case METHOD_KEYWORD:
            compileSubroutine(thirdLine, vmWriter, classSymbolTable);
            continue;
        }
        break;
      }
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
  public void compileClassVarDec(SymbolTable classSymbolTable, Map<String, String> stringMap)
      throws IOException {
    // keyword "static", "field"の読み込み

    // データ型を表すkeywordの読み込み
    var secondLine = parseXMLLine(this.reader.readLine());

    // 変数名を表すidentifierの読み込み
    var thirdLine = parseXMLLine(this.reader.readLine());

    // identifierのシンボルテーブルへの登録。
    classSymbolTable.define(
        thirdLine.get(CONTENT),
        secondLine.get(CONTENT),
        IdentifierAttr.fromCode(stringMap.get(CONTENT)));

    var forthLine = parseXMLLine(this.reader.readLine());
    while (true) {
      if (forthLine.get(CONTENT).equals(";")) {
        break;
      } else if (forthLine.get(CONTENT).equals(",")) { // ","で区切られて複数の変数を宣言している場合

        // 変数名を表すidentifierの読み込み
        var fifthLine = parseXMLLine(this.reader.readLine());

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
  public void compileSubroutine(
      Map<String, String> stringMap, VMWriter vmWriter, SymbolTable classSymbolTable)
      throws IOException {
    // サブルーチンスコープのシンボルテーブルを作成
    var subroutineSymbolTable = new SymbolTable();
    subroutineSymbolTable.startSubroutine(compiledClassName);

    // keyword "function", "constructor", "method"の書き込み

    // データ型を表すkeywordの書き込み
    var secondLine = parseXMLLine(this.reader.readLine());

    // メソッド、ファンクション、コンストラクタ名identifierの書き込み
    var thirdLine = parseXMLLine(this.reader.readLine());

    // symbol「(」の書き込み
    var forthLine = parseXMLLine(this.reader.readLine());

    compileParameterList(subroutineSymbolTable);

    // symbol「)」の書き込み
    var fifthLine = parseXMLLine(this.reader.readLine());

    // 「{ statements }」の書き込み
    compileSubroutineBody(classSymbolTable, subroutineSymbolTable, vmWriter);

    // VMWriterを使っての関数定義コマンドとstringBufferの書き込み
    vmWriter.writeFunction(
        compiledClassName + "." + thirdLine.get(CONTENT),
        subroutineSymbolTable.varCount(IdentifierAttr.VAR));
    vmWriter.writeStringBuffer();
  }

  private void compileSubroutineBody(
      SymbolTable classSymbolTable, SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {

    // symbol「{」の書き込み
    var firstLine = parseXMLLine(this.reader.readLine());

    while (true) {
      var secondLine = parseXMLLine(this.reader.readLine());
      if (secondLine
          .get(CONTENT)
          .equals("var")) { // TODO { subroutineBody } の冒頭でvar宣言がされることを前提としているコードなので欠陥がある。
        compileVarDec(subroutineSymbolTable, secondLine);
      } else {
        compileStatements(classSymbolTable, subroutineSymbolTable, secondLine, vmWriter);
        break;
      }
    }

    // symbol「}」の書き込み
    var thirdLine = Map.of(ELEMENT_TYPE, "symbol", CONTENT, "}", ENCLOSED_CONTENT, " } ");
  }

  public void compileParameterList(SymbolTable subroutineSymbolTable) throws IOException {

    this.reader.mark(100);
    var firstLine = parseXMLLine(this.reader.readLine()); // 引数の型を表している。
    if (firstLine.get(ELEMENT_TYPE).equals("keyword")) {
      while (true) {
        // identifier(引数の変数名)を読み込み
        var secondLine = parseXMLLine(this.reader.readLine());

        // identifierをシンボルテーブルに登録する。
        subroutineSymbolTable.define(
            secondLine.get(CONTENT), firstLine.get(CONTENT), IdentifierAttr.ARG);

        // まだ引数があったらコンパイル。なかったらcompileParameterListを終了する。
        this.reader.mark(100);
        var thirdLine = parseXMLLine(this.reader.readLine());
        if (thirdLine.get(CONTENT).equals(",")) {
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
   * 一連の文をコンパイルする。<br>
   * var宣言は含まない。
   *
   * @throws IOException
   */
  public void compileStatements(
      SymbolTable classSymbolTable,
      SymbolTable subroutineSymbolTable,
      Map<String, String> firstLine,
      VMWriter vmWriter)
      throws IOException {

    int returnFlg = 0;

    var line = firstLine;
    while (true) {
      switch (line.get(CONTENT)) {
        case "do":
          compileDo(subroutineSymbolTable, line, vmWriter);
          break;
        case "let":
          compileLet(classSymbolTable, subroutineSymbolTable, vmWriter);
          break;
        case "while":
          compileWhile(classSymbolTable, subroutineSymbolTable, vmWriter);
          break;
        case "return":
          compileReturn(subroutineSymbolTable, line, vmWriter);
          returnFlg = 1;
          break;
        case "if":
          compileIf(classSymbolTable, subroutineSymbolTable, vmWriter);
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

  /** サブルーチン内のローカル変数宣言をコンパイルする。 */
  public void compileVarDec(SymbolTable subroutineSymbolTable, Map<String, String> firstLine)
      throws IOException {

    // keyword「var」の出力

    // keyword dataType の出力
    var secondLine = parseXMLLine(this.reader.readLine());

    // identifierの出力
    var thirdLine = parseXMLLine(this.reader.readLine());

    // シンボルテーブルへの登録
    subroutineSymbolTable.define(
        thirdLine.get(CONTENT), secondLine.get(CONTENT), IdentifierAttr.VAR);

    // 変数の出力
    var forthLine = parseXMLLine(this.reader.readLine());
    while (true) {
      if (forthLine.get(CONTENT).equals(";")) {
        // symbol「;」の書き込み
        break;
      } else if (forthLine.get(CONTENT).equals(",")) {

        // 変数名を表すidentifier の書き込み
        var fifthLine = parseXMLLine(this.reader.readLine());

        // シンボルテーブルへの登録
        subroutineSymbolTable.define(
            fifthLine.get(CONTENT), secondLine.get(CONTENT), IdentifierAttr.VAR);

        forthLine = parseXMLLine(this.reader.readLine());
        continue;
      }
    }
  }

  /**
   * jack言語のdoコマンドを、vmコマンドにコンパイルする。
   *
   * @param subroutineSymbolTable
   * @param firstLine keyword "do" を表す。
   * @param vmWriter
   * @throws IOException
   */
  public void compileDo(
      SymbolTable subroutineSymbolTable, Map<String, String> firstLine, VMWriter vmWriter)
      throws IOException {

    // identifier 変数名 or メソッド名 の読み込み
    var secondLine = parseXMLLine(this.reader.readLine());

    var numOfArgs = 0; // 呼び出し先の関数の引数の数

    var thirdLine = parseXMLLine(this.reader.readLine());
    if (thirdLine.get(CONTENT).equals(".")) {
      // identifier
      var forthLine = parseXMLLine(this.reader.readLine());

      // symbol"("
      var fifthLine = parseXMLLine(this.reader.readLine());

      numOfArgs = compileExpressionList(subroutineSymbolTable, vmWriter); // 引数のプッシュを済ませる。

      // symbol")"
      var sixthLine = parseXMLLine(this.reader.readLine());

      // symbol";"
      var seventhLine = parseXMLLine(this.reader.readLine());

      vmWriter.bufferCall(
          secondLine.get(CONTENT) + thirdLine.get(CONTENT) + forthLine.get(CONTENT), numOfArgs);

    } else if (thirdLine.get(CONTENT).equals("(")) {
      // メソッドの実行主体が書かれていない場合の処理(privateメソッド)

      numOfArgs = compileExpressionList(subroutineSymbolTable, vmWriter);

      // symbol")"
      var forthLine = parseXMLLine(this.reader.readLine());

      // symbol";"
      var fifthLine = parseXMLLine(this.reader.readLine());

      vmWriter.bufferCall(compiledClassName + "." + secondLine.get(CONTENT), numOfArgs);
    }
  }

  /**
   * let文をコンパイルする。<br>
   * 変数への値の代入を行うのがlet文。
   */
  public void compileLet(
      SymbolTable classSymbolTable, SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {

    // identifierの読み込み
    var firstLine = parseXMLLine(this.reader.readLine());

    var secondLine = parseXMLLine(this.reader.readLine());
    if (secondLine.get(CONTENT).equals("[")) { // TODO 配列は後でやる。
      // 配列宣言"[ iterator ]"部分のコンパイル
      compileArrayIterator(subroutineSymbolTable, secondLine, vmWriter);

      // symbol"="の読み込み
      parseXMLLine(this.reader.readLine());
    }

    compileExpression(subroutineSymbolTable, vmWriter); // 式が評価され、その結果がスタックにプッシュされる
    logicalPop(classSymbolTable, subroutineSymbolTable, vmWriter, firstLine.get(CONTENT));

    // symbol";"の読み込み
    parseXMLLine(this.reader.readLine());
  }

  /** スタックから値をポップし、与えられたシンボルに適したメモリセグメントにその値を割り当てる。 */
  private void logicalPop(
      SymbolTable classSymbolTable,
      SymbolTable subroutineSymbolTable,
      VMWriter vmWriter,
      String symbolName) {
    // サブルーチンスコープならポップ
    var subroutineSymbolKind = subroutineSymbolTable.kindOf(symbolName);
    switch (subroutineSymbolKind) {
      case ARG:
        vmWriter.bufferPop(ARG, subroutineSymbolTable.indexOf(symbolName));
      case VAR:
        vmWriter.bufferPop(LOCAL, subroutineSymbolTable.indexOf(symbolName));
      case NONE:
        break;
    }
    // クラススコープならポップ
    var classSymbolKind = classSymbolTable.kindOf(symbolName);
    switch (classSymbolKind) {
      case STATIC:
        vmWriter.bufferPop(STATIC, classSymbolTable.indexOf(symbolName));
      case FIELD:
        vmWriter.bufferPop(
            STATIC,
            classSymbolTable.indexOf(symbolName) + classSymbolTable.indexOf(symbolName) + 1);
      case NONE:
        throw new IllegalStateException("シンボルテーブルに登録されていない変数ですねぇ。");
    }
  }

  /** while文をコンパイルする。 */
  public void compileWhile(
      SymbolTable classSymbolTable, SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {

    // keyword"while"の読み込み

    // symbol"(" の読み込み
    parseXMLLine(this.reader.readLine());

    compileExpression(subroutineSymbolTable, vmWriter);

    // symbol ")" の読み込み
    parseXMLLine(this.reader.readLine());

    var firstLabelIndex = vmWriter.getCurrentLadelIndex();
    var secondLabelIndex = vmWriter.getCurrentLadelIndex();
    vmWriter.bufferLabel(LABEL + firstLabelIndex);
    vmWriter.bufferIf(LABEL + secondLabelIndex);

    // symbol"{"の読み込み
    parseXMLLine(this.reader.readLine());

    var fifthLine = parseXMLLine(this.reader.readLine());
    compileStatements(classSymbolTable, subroutineSymbolTable, fifthLine, vmWriter);

    vmWriter.bufferGoto(LABEL + firstLabelIndex);
    vmWriter.bufferLabel(LABEL + secondLabelIndex); // TODO 2週目からはループ条件がスタックにプッシュされているとは限らない。

    // symbol"}"の読み込み
  }

  /**
   * return文をコンパイルする。<br>
   * 関数の返り値をスタックのプッシュした後でreturnコマンドが実行される。
   */
  public void compileReturn(
      SymbolTable subroutineSymbolTable, Map<String, String> firstLine, VMWriter vmWriter)
      throws IOException {

    // keyword"return"

    this.reader.mark(100);
    var secondLine = parseXMLLine(this.reader.readLine());
    if (secondLine.get(ELEMENT_TYPE).equals("identifier")
        || secondLine.get(ELEMENT_TYPE).equals("keyword")) {
      this.reader.reset();
      compileExpression(subroutineSymbolTable, vmWriter);

      parseXMLLine(this.reader.readLine());

    } else if (secondLine.get(ELEMENT_TYPE).equals("symbol")) {
    }

    vmWriter.bufferReturn();
  }

  public void compileIf(
      SymbolTable classSymbolTable, SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {

    // keyword"if"

    // symbol"("の読み込み
    parseXMLLine(this.reader.readLine());

    // if条件式のコンパイル
    compileExpression(subroutineSymbolTable, vmWriter);

    // symbol")"の読み込み
    parseXMLLine(this.reader.readLine());

    // symbol"{"の読み込み

    var firstLabelIndex = vmWriter.getCurrentLadelIndex();
    var secondLabelIndex = vmWriter.getCurrentLadelIndex();
    vmWriter.bufferIf(LABEL + firstLabelIndex);
    vmWriter.bufferLabel(LABEL + firstLabelIndex);

    // statementsのコンパイル
    var forthLine = parseXMLLine(this.reader.readLine());
    compileStatements(classSymbolTable, subroutineSymbolTable, forthLine, vmWriter);

    vmWriter.bufferGoto(LABEL + secondLabelIndex);

    // symbol"}"の読み込み

    this.reader.mark(100); // 取り消せるようにマーク
    var sixthLine = parseXMLLine(this.reader.readLine());
    if (sixthLine.get(CONTENT).equals("else")) {
      // keyword"else"

      // symbol"{"の読み込み
      this.reader.readLine();

      // statementsのコンパイル
      compileStatements(
          classSymbolTable, subroutineSymbolTable, parseXMLLine(this.reader.readLine()), vmWriter);

      // symbol"}"の読み込み
    } else {
      this.reader.reset(); // TODO 読み込んだ結果"else"がないIf文だった場合、ちゃんと読み込みを取り消せられているか確認。
    }
  }

  /**
   * 式をコンパイルする。<br>
   * この結果はスタックにプッシュする。
   */
  public void compileExpression(SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {

    var resultMap1 = compileTerm(subroutineSymbolTable, vmWriter);
    if (!resultMap1.containsKey(DO_NOTHING)) {
      vmWriter.bufferPush(
          Segment.fromCode(resultMap1.get(SEGMENT)), Integer.parseInt(resultMap1.get(INDEX)));
    }

    // TODO "|", "*", "/", "+"が複数回出てくる場合が出てきたら対応を追加する。
    this.reader.mark(100);
    var nextEle = parseXMLLine(this.reader.readLine());

    /* ----------------------------------------算術演算-------------------------------------- */
    /* ----------------------------------------論理演算-------------------------------------- */
    if (nextEle.get(CONTENT).equals("+") // x + y
        || nextEle.get(CONTENT).equals("-") // x - y
        || nextEle.get(CONTENT).equals("=") // x = y
        || nextEle.get(CONTENT).equals("|")) { // x | y
      var resultMap2 = compileTerm(subroutineSymbolTable, vmWriter);
      if (!resultMap2.containsKey(DO_NOTHING)) {
        vmWriter.bufferPush(
            Segment.fromCode(resultMap2.get(SEGMENT)), Integer.parseInt(resultMap2.get(INDEX)));
      }
      vmWriter.bufferArithmetic(ArithmeticCommand.fromCode(nextEle.get(CONTENT)));

    } else if (nextEle.get(CONTENT).equals("*")) { // x * y
      var resultMap2 = compileTerm(subroutineSymbolTable, vmWriter);
      vmWriter.bufferPush(
          Segment.fromCode(resultMap2.get(SEGMENT)), Integer.parseInt(resultMap2.get(INDEX)));
      vmWriter.bufferCall(MULTIPLY, 2);

    } else if (nextEle.get(CONTENT).equals("/")) { // x / y
      var resultMap2 = compileTerm(subroutineSymbolTable, vmWriter);
      vmWriter.bufferPush(
          Segment.fromCode(resultMap2.get(SEGMENT)), Integer.parseInt(resultMap2.get(INDEX)));
      vmWriter.bufferCall(DIVIDE, 2);

    } else {
      this.reader.reset();
    }

    this.reader.mark(100);
    var secondLine = parseXMLLine(this.reader.readLine());
    /* ----------------------------------------論理演算: <, >, &-------------------------------------- */
    if (secondLine.get(CONTENT).equals("&lt;") // 不等号: <
        || secondLine.get(CONTENT).equals("&gt;")) { // 不等号: >
      var resultMap2 = compileTerm(subroutineSymbolTable, vmWriter);
      vmWriter.bufferPush(
          Segment.fromCode(resultMap2.get(SEGMENT)), Integer.parseInt(resultMap2.get(INDEX)));
      var operand = secondLine.get(CONTENT).equals("&lt;") ? "<" : ">";
      vmWriter.bufferArithmetic(ArithmeticCommand.fromCode(operand));

    } else {
      this.reader.reset();
    }

    this.reader.mark(100);
    var thirdLine = parseXMLLine(this.reader.readLine()); // and演算子: &
    if (thirdLine.get(CONTENT).equals("&amp;")) { // TODO "&"が複数出てくるパターン対策。
      var resultMap2 = compileTerm(subroutineSymbolTable, vmWriter);
      vmWriter.bufferPush(
          Segment.fromCode(resultMap2.get(SEGMENT)), Integer.parseInt(resultMap2.get(INDEX)));
      vmWriter.bufferArithmetic(ArithmeticCommand.fromCode("&"));

    } else {
      this.reader.reset();
    }
  }

  /** 式(Expression)の一部分をコンパイルする */
  public Map<String, String> compileTerm(SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {

    Map<String, String> resultMap = new HashMap<>();

    var firstLine = parseXMLLine(this.reader.readLine());
    // ---------------------シンボルテーブルに定義されている変数------------------------
    if (firstLine.get(ELEMENT_TYPE).equals("identifier")) {
      resultMap =
          Map.of(
              SEGMENT,
              subroutineSymbolTable
                  .kindOf(firstLine.get(CONTENT))
                  .getCode(), // TODO IdentifierAttrとSegmentは必ずしも一致しないので、多分ずれてくる。一旦保留。
              INDEX,
              String.valueOf(subroutineSymbolTable.indexOf(firstLine.get(CONTENT))));

      // ---------------------keyword "this"--------------------------------
    } else if (firstLine.get(CONTENT).equals("this")) {

      resultMap = Map.of(SEGMENT, ARG.getCode(), INDEX, "0");

      // ---------------------定数：integerConstant---------------------------
    } else if (firstLine.get(ELEMENT_TYPE).equals("integerConstant")) {
      resultMap = Map.of(SEGMENT, CONST.getCode(), INDEX, firstLine.get(CONTENT));

      // ---------------------文字列：stringConstant---------------------------
    } else if (firstLine.get(ELEMENT_TYPE).equals("stringConstant")) {
      // TODO どうする？おそらくOSのString.new()を使う？

      // ---------------------"true", "false"---------------------------------
    } else if (firstLine.get(CONTENT).equals("true") || firstLine.get(CONTENT).equals("false")) {
      var number = firstLine.get(CONTENT).equals("true") ? "1" : "0";
      resultMap = Map.of(SEGMENT, CONST.getCode(), INDEX, number);
    } else {
      resultMap = Map.of(DO_NOTHING, "do nothing");
    }

    // ---------------------(x + y) のような括弧で括られた式。---------------------------------
    if (firstLine.get(CONTENT).equals("(")) {
      compileExpression(subroutineSymbolTable, vmWriter);

      // ")" の読み取り
      var secondLine = parseXMLLine(this.reader.readLine());

      // ---------------------negate---------------------------------
      // ---------------------not------------------------------------
    } else if (firstLine.get(CONTENT).equals("-") || firstLine.get(CONTENT).equals("~")) {
      resultMap = compileTerm(subroutineSymbolTable, vmWriter);
      vmWriter.bufferPush(
          Segment.fromCode(resultMap.get(SEGMENT)), Integer.parseInt(resultMap.get(INDEX)));
      var commandCode = firstLine.get(CONTENT).equals("-") ? "negate" : "~";
      vmWriter.bufferArithmetic(ArithmeticCommand.fromCode(commandCode));

      return Map.of(DO_NOTHING, "do nothing"); // 何もしないのでNO_NOTHINGキーを返す。

    } else {
      this.reader.mark(100);
      var secondLine = parseXMLLine(this.reader.readLine());
      if (secondLine.get(CONTENT).equals(".")) {
        // サブルーチン呼び出し
        compileCallSubroutine(subroutineSymbolTable, secondLine, vmWriter);
        // TODO 引数として関数の返り値を渡すことがありえるのかわからないので対応を保留。

      } else if (secondLine.get(CONTENT).equals("[")) {
        // 配列宣言のコンパイル
        compileArrayIterator(subroutineSymbolTable, secondLine, vmWriter);
        // TODO 引数として配列を渡すことがありえるのかわからないので対応を保留。

      } else {
        this.reader.reset();
      }
    }

    return resultMap;
  }

  /**
   * コンマで分離された式のリスト（空の可能性もある）をコンパイルする。
   *
   * @return コンマで区切られた引数の数
   */
  public int compileExpressionList(SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {
    var expressionCount = 0; // 引数の数の初期化

    while (true) {
      this.reader.mark(100);
      var line = parseXMLLine(this.reader.readLine());

      /* -----------------------------------引数 : "this", "true", "false"------------------------------- */
      if (line.get(ELEMENT_TYPE).equals("keyword")
          /* -----------------------------------引数 : シンボルテーブルに登録されている変数------------------- */
          /* -----------------------------------引数 : x + yなどの算術演算--------------------------------- */
          /* -----------------------------------引数 : x > yなどの論理演算--------------------------------- */
          || line.get(ELEMENT_TYPE).equals("identifier")
          /* -----------------------------------引数 : 定数--------------------------------- */
          || line.get(ELEMENT_TYPE).equals("integerConstant")) {
        this.reader.reset();
        expressionCount++;
        compileExpression(subroutineSymbolTable, vmWriter);

      } else if (line.get(ELEMENT_TYPE).equals("stringConstant")) {
        /* -----------------------------------引数 : 文字列--------------------------------- */
        // TODO どうする？おそらくOSのString.new()を使う？
        this.reader.reset();
        expressionCount++;
        compileExpression(subroutineSymbolTable, vmWriter);

      } else if (line.get(CONTENT).equals("(")) {
        /* -----------------------------------引数 : (1 + 2)などの式。--------------------------------- */
        this.reader.reset();
        expressionCount++;
        compileExpression(subroutineSymbolTable, vmWriter);

        /* -----------------------------------引数がコンマ区切りで複数存在する場合--------------------------------- */
      } else if (line.get(CONTENT).equals(",")) {

        /* -----------------------------------引数リスト終わり--------------------------------- */
      } else {
        this.reader.reset();
        break;
      }
    }
    return expressionCount;
  }

  /**
   * サブルーチン呼び出し(do)の内、".( arguments... )"部分のコンパイルを行う。
   *
   * @return コンマで区切られた引数の数
   */
  private int compileCallSubroutine(
      SymbolTable subroutineSymbolTable, Map<String, String> firstLine, VMWriter vmWriter)
      throws IOException {
    // symbol「.」の出力

    // identifierの出力
    var secondLine = parseXMLLine(this.reader.readLine());

    // symbol「(」の出力
    var thirdLine = parseXMLLine(this.reader.readLine());

    var numOfExpression = compileExpressionList(subroutineSymbolTable, vmWriter);

    // symbol「)」の出力
    var sixthLine = parseXMLLine(this.reader.readLine());

    return numOfExpression;
  }

  private void compileArrayIterator(
      SymbolTable subroutineSymbolTable, Map<String, String> firstLine, VMWriter vmWriter)
      throws IOException {
    // symbol"["のコンパイル

    // 配列イテレータのコンパイル
    compileExpression(subroutineSymbolTable, vmWriter);

    // symbol「]」のコンパイル
    this.reader.readLine();
  }
}
