package modules;

import static modules.data.IdentifierAttr.FIELD;
import static modules.data.IdentifierAttr.NONE;
import static modules.data.IdentifierAttr.VAR;
import static modules.data.Segment.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import modules.data.ArithmeticCommand;
import modules.data.IdentifierAttr;
import modules.data.Segment;
import modules.data.SubroutineType;

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
    parentPath = parentDir;

    reader = new BufferedReader(new FileReader(inputFile));
    var firstLine = reader.readLine();
    if (!firstLine.equals("<tokens>")) {
      throw new IllegalArgumentException("入力ファイルの形式が正しくありません。firstLine=" + firstLine);
    }
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }

  public void compileClass() throws IOException {
    // シンボルテーブルの生成
    var classSymbolTable = new SymbolTable();

    var firstLine = parseXMLLine(reader.readLine());
    if (!firstLine.get(CONTENT).equals("class")) {
      throw new IllegalStateException();
    }

    // クラス名となるidentifier読み込み。クラス名はシンボルテーブルに記録しない。
    var secondLine = parseXMLLine(reader.readLine());
    compiledClassName = secondLine.get(CONTENT);

    // VMWriterの生成
    try (var vmWriter = new VMWriter(new File(parentPath, compiledClassName + ".vm"))) {
      // symbol"{"の読み込み
      parseXMLLine(reader.readLine());

      // xml要素の種類によって適切な処理を呼び出す。
      while (true) {
        var thirdLine = parseXMLLine(reader.readLine());
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
    var secondLine = parseXMLLine(reader.readLine());

    // 変数名を表すidentifierの読み込み
    var thirdLine = parseXMLLine(reader.readLine());

    // identifierのシンボルテーブルへの登録。
    classSymbolTable.define(
        thirdLine.get(CONTENT),
        secondLine.get(CONTENT),
        IdentifierAttr.fromCode(stringMap.get(CONTENT)));

    var forthLine = parseXMLLine(reader.readLine());
    while (true) {
      if (forthLine.get(CONTENT).equals(";")) {
        break;
      } else if (forthLine.get(CONTENT).equals(",")) { // ","で区切られて複数の変数を宣言している場合

        // 変数名を表すidentifierの読み込み
        var fifthLine = parseXMLLine(reader.readLine());

        // identifierのシンボルテーブルへの登録。
        classSymbolTable.define(
            fifthLine.get(CONTENT),
            secondLine.get(CONTENT),
            IdentifierAttr.fromCode(stringMap.get(CONTENT)));

        forthLine = parseXMLLine(reader.readLine());
        continue;
      }
    }
  }

  /** メソッド、ファンクション、コンストラクタをコンパイルする。 TODO イマココ。コンストラクタのコンパイル。 */
  public void compileSubroutine(
      Map<String, String> stringMap, VMWriter vmWriter, SymbolTable classSymbolTable)
      throws IOException {

    if (stringMap.get(CONTENT).equals("constructor")) {
      var numOfField = classSymbolTable.varCount(FIELD);
      vmWriter.bufferPush(CONST, (int) numOfField);
      vmWriter.bufferCall("Memory.alloc", 1); // オブジェクト用のメモリ領域確保
      vmWriter.bufferPop(POINTER, 0); // thisのベースアドレスをオブジェクトのベースアドレスに
    } else if (stringMap.get(CONTENT).equals("method")) {
      // メソッドの場合、呼び出し元のオブジェクトは0番目の引数としてプッシュされているので、ポップしてポインタを初期化する
      vmWriter.bufferPush(ARG, 0);
      vmWriter.bufferPop(POINTER, 0);
    }

    // サブルーチンスコープのシンボルテーブルを作成
    var subroutineSymbolTable = new SymbolTable();
    subroutineSymbolTable.startSubroutine(
        compiledClassName, SubroutineType.fromCode(stringMap.get(CONTENT)));

    // データ型を表すkeywordの書き込み
    var secondLine = parseXMLLine(reader.readLine());

    // メソッド、ファンクション、コンストラクタ名identifierの書き込み
    var thirdLine = parseXMLLine(reader.readLine());

    // symbol「(」の書き込み
    var forthLine = parseXMLLine(reader.readLine());

    compileParameterList(subroutineSymbolTable);

    // symbol「)」の書き込み
    var fifthLine = parseXMLLine(reader.readLine());

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

    // symbol「{」の読み込み
    parseXMLLine(reader.readLine());

    while (true) {
      var secondLine = parseXMLLine(reader.readLine());
      if (secondLine
          .get(CONTENT)
          .equals("var")) { // TODO { subroutineBody } の冒頭でvar宣言がされることを前提としているコードなので欠陥がある。
        compileVarDec(subroutineSymbolTable, secondLine);
      } else {
        compileStatements(classSymbolTable, subroutineSymbolTable, secondLine, vmWriter);
        break;
      }
    }

    // symbol「}」の読み込み
    Map.of(ELEMENT_TYPE, "symbol", CONTENT, "}", ENCLOSED_CONTENT, " } ");
  }

  public void compileParameterList(SymbolTable subroutineSymbolTable) throws IOException {

    reader.mark(100);
    var firstLine = parseXMLLine(reader.readLine()); // 引数の型を表している。
    if (firstLine.get(ELEMENT_TYPE).equals("keyword")) {
      while (true) {
        // identifier(引数の変数名)を読み込み
        var secondLine = parseXMLLine(reader.readLine());

        // identifierをシンボルテーブルに登録する。
        subroutineSymbolTable.define(
            secondLine.get(CONTENT), firstLine.get(CONTENT), IdentifierAttr.ARG);

        // まだ引数があったらコンパイル。なかったらcompileParameterListを終了する。
        reader.mark(100);
        var thirdLine = parseXMLLine(reader.readLine());
        if (thirdLine.get(CONTENT).equals(",")) {
          firstLine = parseXMLLine(reader.readLine());
        } else if (thirdLine.get(CONTENT).equals(")")) {
          reader.reset();
          break;
        }
      }
    } else {
      reader.reset();
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
          compileDo(classSymbolTable, subroutineSymbolTable, vmWriter);
          break;
        case "let":
          compileLet(classSymbolTable, subroutineSymbolTable, vmWriter);
          break;
        case "while":
          compileWhile(classSymbolTable, subroutineSymbolTable, vmWriter);
          break;
        case "return":
          compileReturn(classSymbolTable, subroutineSymbolTable, vmWriter);
          returnFlg = 1;
          break;
        case "if":
          compileIf(classSymbolTable, subroutineSymbolTable, vmWriter);
          break;
      }
      var readLine = reader.readLine();
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
    var secondLine = parseXMLLine(reader.readLine());

    // identifierの出力
    var thirdLine = parseXMLLine(reader.readLine());

    // シンボルテーブルへの登録
    subroutineSymbolTable.define(
        thirdLine.get(CONTENT), secondLine.get(CONTENT), IdentifierAttr.VAR);

    // 変数の出力
    var forthLine = parseXMLLine(reader.readLine());
    while (true) {
      if (forthLine.get(CONTENT).equals(";")) {
        // symbol「;」の書き込み
        break;
      } else if (forthLine.get(CONTENT).equals(",")) {

        // 変数名を表すidentifier の書き込み
        var fifthLine = parseXMLLine(reader.readLine());

        // シンボルテーブルへの登録
        subroutineSymbolTable.define(
            fifthLine.get(CONTENT), secondLine.get(CONTENT), IdentifierAttr.VAR);

        forthLine = parseXMLLine(reader.readLine());
        continue;
      }
    }
  }

  /**
   * jack言語のdoコマンドを、vmコマンドにコンパイルする。
   *
   * @param subroutineSymbolTable
   * @param vmWriter
   * @throws IOException
   */
  public void compileDo(
      SymbolTable classSymbolTable, SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {

    compileCallSubroutine(classSymbolTable, subroutineSymbolTable, vmWriter);

    // symbol";"
    parseXMLLine(reader.readLine());
  }

  /**
   * サブルーチン呼び出しをコンパイルする。<br>
   * Main.main() とか main() とか。もちろん引数ありのパターンもある。
   */
  public void compileCallSubroutine(
      SymbolTable classSymbolTable, SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {
    // identifier 変数名 or メソッド名 の読み込み
    var secondLine = parseXMLLine(reader.readLine());
    var identifierName = secondLine.get(CONTENT);

    var numOfArgs = 0; // 呼び出し先の関数の引数の数

    var thirdLine = parseXMLLine(reader.readLine());
    if (thirdLine.get(CONTENT).equals(".")) {
      if (subroutineSymbolTable.kindOf(identifierName) != NONE
          || classSymbolTable.kindOf(identifierName) != NONE) {

        var index = 0;
        Segment segment = CONST;
        if (subroutineSymbolTable.kindOf(identifierName) != NONE) {
          index = subroutineSymbolTable.indexOf(identifierName);
          segment = subroutineSymbolTable.kindOf(identifierName) == VAR ? LOCAL : ARG;
          identifierName = subroutineSymbolTable.typeOf(identifierName);
        } else if (classSymbolTable.kindOf(identifierName) != NONE) {
          index = classSymbolTable.indexOf(identifierName);
          segment = classSymbolTable.kindOf(identifierName) == FIELD ? THIS : STATIC;
          identifierName = classSymbolTable.typeOf(identifierName);
        }
        // メソッド名を示すidentifier
        var forthLine = parseXMLLine(reader.readLine());
        // symbol"("
        parseXMLLine(reader.readLine());

        if (segment != CONST) {
          vmWriter.bufferPush(segment, index); // メソッドを呼び出したオブジェクトを引数としてプッシュしておく
        } else {
          throw new IllegalStateException("Illegal Segment: " + CONST);
        }

        numOfArgs =
            compileExpressionList(
                classSymbolTable, subroutineSymbolTable, vmWriter); // 引数のプッシュを済ませる。
        numOfArgs = numOfArgs + 1;

        // symbol")"
        parseXMLLine(reader.readLine());

        vmWriter.bufferCall(identifierName + "." + forthLine.get(CONTENT), numOfArgs);

        vmWriter.bufferPop(TEMP, 0); // メソッドを呼び出したオブジェクトがスタックにプッシュされたままなので、tempに逃しておく

      } else {
        // メソッド名を示すidentifier
        var forthLine = parseXMLLine(reader.readLine());
        // symbol"("
        parseXMLLine(reader.readLine());

        numOfArgs = compileExpressionList(classSymbolTable, subroutineSymbolTable, vmWriter);

        // symbol")"
        parseXMLLine(reader.readLine());

        vmWriter.bufferCall(identifierName + "." + forthLine.get(CONTENT), numOfArgs);

        if (!forthLine.get(CONTENT).equals("new")) {
          vmWriter.bufferPop(TEMP, 0); // コンストラクタ以外。なんで？
        }
      }

    } else if (thirdLine.get(CONTENT).equals("(")) {
      vmWriter.bufferPush(POINTER, 0); // 呼び出し元が書かれていない=thisオブジェクトが呼び出した
      numOfArgs = compileExpressionList(classSymbolTable, subroutineSymbolTable, vmWriter) + 1;

      // symbol")"
      parseXMLLine(reader.readLine());

      vmWriter.bufferCall(compiledClassName + "." + identifierName, numOfArgs);

      vmWriter.bufferPop(TEMP, 0); // メソッドを呼び出したオブジェクトがスタックにプッシュされたままなので、tempに逃しておく
    }
  }

  /*
  private int countThisArg() throws IOException {
    reader.mark(100);
    int thisArgCount = 0;
    while (true) {
      var token = parseXMLLine(reader.readLine()).get(CONTENT);
      if (token.equals("this")) {
        thisArgCount++;
      }

      if (token.equals(")")) {
        if (parseXMLLine(reader.readLine()).get(CONTENT).equals(";")) {
          reader.reset();
          break;
        }
      }
    }

    return thisArgCount;
  }
  */

  /**
   * let文をコンパイルする。<br>
   * 変数への値の代入を行うのがlet文。
   */
  public void compileLet(
      SymbolTable classSymbolTable, SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {

    // identifierの読み込み
    var firstLine = parseXMLLine(reader.readLine());

    var secondLine = parseXMLLine(reader.readLine());
    if (secondLine.get(CONTENT).equals("[")) {
      // symbol"["

      // 配列イテレータのコンパイル
      compileExpression(classSymbolTable, subroutineSymbolTable, vmWriter);

      // symbol"]"のコンパイル
      reader.readLine();

      vmWriter.bufferArithmetic(ArithmeticCommand.ADD);
      vmWriter.bufferPop(POINTER, 1); // 配列はthatセグメントを使う。

      // symbol"="の読み込み
      parseXMLLine(reader.readLine());

      compileExpression(classSymbolTable, subroutineSymbolTable, vmWriter);

      vmWriter.bufferPop(THAT, 0);

    } else {
      compileExpression(
          classSymbolTable, subroutineSymbolTable, vmWriter); // 式が評価され、その結果がスタックにプッシュされる

      logicalPop(classSymbolTable, subroutineSymbolTable, vmWriter, firstLine.get(CONTENT));
    }

    // symbol";"の読み込み
    parseXMLLine(reader.readLine());
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
        return;
      case VAR:
        vmWriter.bufferPop(LOCAL, subroutineSymbolTable.indexOf(symbolName));
        return;
      case NONE:
        break;
    }
    // クラススコープならポップ
    var classSymbolKind = classSymbolTable.kindOf(symbolName);
    switch (classSymbolKind) {
      case STATIC:
        vmWriter.bufferPop(STATIC, classSymbolTable.indexOf(symbolName));
        return;
      case FIELD:
        vmWriter.bufferPop(THIS, classSymbolTable.indexOf(symbolName));
        return;
      case NONE:
        throw new IllegalStateException("シンボルテーブルに登録されていない変数ですねぇ。");
    }
  }

  /** while文をコンパイルする。 */
  public void compileWhile(
      SymbolTable classSymbolTable, SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {

    // keyword"while"の読み込み

    var firstLabelIndex = vmWriter.getCurrentLadelIndex();
    vmWriter.bufferLabel(LABEL + firstLabelIndex);

    // symbol"(" の読み込み
    parseXMLLine(reader.readLine());
    compileExpression(classSymbolTable, subroutineSymbolTable, vmWriter);

    // symbol ")" の読み込み
    parseXMLLine(reader.readLine());

    var secondLabelIndex = vmWriter.getCurrentLadelIndex();
    var thirdLabelIndex = vmWriter.getCurrentLadelIndex();
    vmWriter.bufferIf(LABEL + secondLabelIndex);
    vmWriter.bufferGoto(LABEL + thirdLabelIndex);
    vmWriter.bufferLabel(LABEL + secondLabelIndex);

    // symbol"{"の読み込み
    parseXMLLine(reader.readLine());

    var fifthLine = parseXMLLine(reader.readLine());
    compileStatements(classSymbolTable, subroutineSymbolTable, fifthLine, vmWriter);

    vmWriter.bufferGoto(LABEL + firstLabelIndex);

    // symbol"}"の読み込み

    vmWriter.bufferLabel(LABEL + thirdLabelIndex);
  }

  /**
   * return文をコンパイルする。<br>
   * 関数の返り値をスタックのプッシュした後でreturnコマンドが実行される。
   */
  public void compileReturn(
      SymbolTable classSymbolTable, SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {
    reader.mark(100);
    var secondLine = parseXMLLine(reader.readLine());
    if (secondLine.get(ELEMENT_TYPE).equals("identifier")
        || secondLine.get(ELEMENT_TYPE).equals("keyword")
        || secondLine.get(ELEMENT_TYPE).equals("integerConstant")) {
      reader.reset();
      compileExpression(classSymbolTable, subroutineSymbolTable, vmWriter); // 結果はスタックの一番上にプッシュされる。

      parseXMLLine(reader.readLine());
    } else {
      vmWriter.bufferPush(CONST, 0); // voidのサブルーチンの戻り値は0
    }

    vmWriter.bufferReturn();
  }

  public void compileIf(
      SymbolTable classSymbolTable, SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {

    // keyword"if"

    // symbol"("の読み込み
    parseXMLLine(reader.readLine());

    // if条件式のコンパイル
    compileExpression(classSymbolTable, subroutineSymbolTable, vmWriter);

    // symbol")"の読み込み
    parseXMLLine(reader.readLine());

    // symbol"{"の読み込み

    var firstLabelIndex = vmWriter.getCurrentLadelIndex();
    var secondLabelIndex = vmWriter.getCurrentLadelIndex();
    var thirdLabelIndex = vmWriter.getCurrentLadelIndex();
    vmWriter.bufferIf(LABEL + firstLabelIndex);

    List<Runnable> processes = new ArrayList<>();
    processes.add(
        () ->
            vmWriter.bufferGoto(
                LABEL + thirdLabelIndex)); // else分岐のないif文の場合、この処理は不要なので処理をバッファしてあとで実行する。
    processes.add(() -> vmWriter.bufferLabel(LABEL + firstLabelIndex));

    // statementsのコンパイル
    var forthLine = parseXMLLine(reader.readLine());
    processes.add(
        () -> {
          try {
            compileStatements(classSymbolTable, subroutineSymbolTable, forthLine, vmWriter);
          } catch (IOException e) {
            System.out.println("compileStatements中に例外発生でっせ。");
            System.exit(1);
          }
        });

    processes.add(() -> vmWriter.bufferGoto(LABEL + secondLabelIndex));

    // symbol"}"の読み込み

    reader.mark(100); // 取り消せるようにマーク
    var sixthLine = parseXMLLine(reader.readLine());
    if (sixthLine.get(CONTENT).equals("else")) {
      reader.reset();

      for (var process : processes) {
        process.run();
      }

      // keyword"else"
      parseXMLLine(reader.readLine());

      vmWriter.bufferLabel(LABEL + thirdLabelIndex);

      // symbol"{"の読み込み
      reader.readLine();

      // statementsのコンパイル
      compileStatements(
          classSymbolTable, subroutineSymbolTable, parseXMLLine(reader.readLine()), vmWriter);

      vmWriter.bufferGoto(LABEL + secondLabelIndex);

      // symbol"}"の読み込み
    } else {
      reader.reset();

      for (int i = 1; i <= (processes.size() - 1); i++) {
        processes.get(i).run();
      }
    }

    vmWriter.bufferLabel(LABEL + secondLabelIndex);
  }

  /**
   * 式をコンパイルする。<br>
   * 演算結果はスタックにプッシュする。
   */
  public void compileExpression(
      SymbolTable classSymbolTable, SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {

    var resultMap1 = compileTerm(classSymbolTable, subroutineSymbolTable, vmWriter);
    if (!resultMap1.containsKey(DO_NOTHING)) {
      vmWriter.bufferPush(
          Segment.fromCode(resultMap1.get(SEGMENT)), Integer.parseInt(resultMap1.get(INDEX)));
    }

    reader.mark(100);
    var nextEle = parseXMLLine(reader.readLine());

    /* ----------------------------------------算術演算-------------------------------------- */
    /* ----------------------------------------論理演算-------------------------------------- */
    if (nextEle.get(CONTENT).equals("+") // x + y
        || nextEle.get(CONTENT).equals("-") // x - y
        || nextEle.get(CONTENT).equals("=") // x = y
        || nextEle.get(CONTENT).equals("|")) { // x | y
      var resultMap2 = compileTerm(classSymbolTable, subroutineSymbolTable, vmWriter);
      if (!resultMap2.containsKey(DO_NOTHING)) {
        vmWriter.bufferPush(
            Segment.fromCode(resultMap2.get(SEGMENT)), Integer.parseInt(resultMap2.get(INDEX)));
      }
      vmWriter.bufferArithmetic(ArithmeticCommand.fromCode(nextEle.get(CONTENT)));

    } else if (nextEle.get(CONTENT).equals("*")) { // x * y
      var resultMap2 = compileTerm(classSymbolTable, subroutineSymbolTable, vmWriter);
      vmWriter.bufferPush(
          Segment.fromCode(resultMap2.get(SEGMENT)), Integer.parseInt(resultMap2.get(INDEX)));
      vmWriter.bufferCall(MULTIPLY, 2);

    } else if (nextEle.get(CONTENT).equals("/")) { // x / y
      var resultMap2 = compileTerm(classSymbolTable, subroutineSymbolTable, vmWriter);
      vmWriter.bufferPush(
          Segment.fromCode(resultMap2.get(SEGMENT)), Integer.parseInt(resultMap2.get(INDEX)));
      vmWriter.bufferCall(DIVIDE, 2);

    } else {
      reader.reset();
    }

    reader.mark(100);
    var secondLine = parseXMLLine(reader.readLine());
    /* ----------------------------------------論理演算: <, >, &-------------------------------------- */
    if (secondLine.get(CONTENT).equals("&lt;") // 不等号: <
        || secondLine.get(CONTENT).equals("&gt;")) { // 不等号: >
      var resultMap2 = compileTerm(classSymbolTable, subroutineSymbolTable, vmWriter);
      vmWriter.bufferPush(
          Segment.fromCode(resultMap2.get(SEGMENT)), Integer.parseInt(resultMap2.get(INDEX)));
      var operand = secondLine.get(CONTENT).equals("&lt;") ? "<" : ">";
      vmWriter.bufferArithmetic(ArithmeticCommand.fromCode(operand));

    } else {
      reader.reset();
    }

    reader.mark(100);
    var thirdLine = parseXMLLine(reader.readLine()); // and演算子: &
    if (thirdLine.get(CONTENT).equals("&amp;")) {
      var resultMap2 = compileTerm(classSymbolTable, subroutineSymbolTable, vmWriter);
      if (!resultMap1.containsKey(DO_NOTHING)) {
        vmWriter.bufferPush(
            Segment.fromCode(resultMap2.get(SEGMENT)), Integer.parseInt(resultMap2.get(INDEX)));
      }
      vmWriter.bufferArithmetic(ArithmeticCommand.fromCode("&"));

    } else {
      reader.reset();
    }
  }

  /** 式(Expression)の一部分をコンパイルする */
  public Map<String, String> compileTerm(
      SymbolTable classSymbolTable, SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {

    Map<String, String> resultMap = new HashMap<>();

    reader.mark(100);
    var firstLine = parseXMLLine(reader.readLine());
    // ---------------------シンボルテーブルに定義されている変数 or 関数呼び出し------------------------
    if (firstLine.get(ELEMENT_TYPE).equals("identifier")) {
      var kind = subroutineSymbolTable.kindOf(firstLine.get(CONTENT));
      var index = 0;
      Segment segment = null;
      if (kind != NONE) {
        index = subroutineSymbolTable.indexOf(firstLine.get(CONTENT));
        switch (kind) {
          case ARG:
            segment = ARG;
            break;
          case VAR:
            segment = LOCAL;
            break;
        }
        resultMap = Map.of(SEGMENT, segment.getCode(), INDEX, String.valueOf(index));

      } else {
        kind = classSymbolTable.kindOf(firstLine.get(CONTENT));
        if (kind != NONE) {
          index = classSymbolTable.indexOf(firstLine.get(CONTENT));
          if (kind == FIELD) {
            segment = THIS;
          } else if (kind == IdentifierAttr.STATIC) {
            segment = STATIC;
          }
          resultMap = Map.of(SEGMENT, segment.getCode(), INDEX, String.valueOf(index));

        } else {
          /* -----------------------------サブルーチン呼び出し---------------------------- */
          reader.reset();
          compileCallSubroutine(classSymbolTable, subroutineSymbolTable, vmWriter);
          return Map.of(DO_NOTHING, "do nothing");
        }
      }

      // ---------------------keyword "this"--------------------------------
    } else if (firstLine.get(CONTENT).equals("this")) {

      resultMap = Map.of(SEGMENT, POINTER.getCode(), INDEX, "0");

      // ---------------------定数：integerConstant---------------------------
    } else if (firstLine.get(ELEMENT_TYPE).equals("integerConstant")) {
      resultMap = Map.of(SEGMENT, CONST.getCode(), INDEX, firstLine.get(CONTENT));

      // ---------------------文字列：stringConstant---------------------------
    } else if (firstLine.get(ELEMENT_TYPE).equals("stringConstant")) {
      var stringLength = firstLine.get(CONTENT).length();
      vmWriter.bufferPush(CONST, stringLength);
      vmWriter.bufferCall("String.new", 1);
      for (char c : firstLine.get(CONTENT).toCharArray()) {
        vmWriter.bufferPush(CONST, c);
        vmWriter.bufferCall("String.appendChar", 1);
      }
      resultMap = Map.of(DO_NOTHING, "do nothing"); // 何もしない

      // ---------------------"true", "false"---------------------------------
    } else if (firstLine.get(CONTENT).equals("true") || firstLine.get(CONTENT).equals("false")) {
      vmWriter.bufferPush(CONST, 0);

      if (firstLine.get(CONTENT).equals("true")) {
        vmWriter.bufferArithmetic(ArithmeticCommand.NOT);
      }
      resultMap = Map.of(DO_NOTHING, "do nothing");
    } else {
      resultMap = Map.of(DO_NOTHING, "do nothing");
    }

    // ---------------------(x + y) のような括弧で括られた式。---------------------------------
    if (firstLine.get(CONTENT).equals("(")) {
      compileExpression(classSymbolTable, subroutineSymbolTable, vmWriter);

      // ")" の読み取り
      parseXMLLine(reader.readLine());

      // ---------------------negate---------------------------------
      // ---------------------not------------------------------------
    } else if (firstLine.get(CONTENT).equals("-") || firstLine.get(CONTENT).equals("~")) {
      resultMap = compileTerm(classSymbolTable, subroutineSymbolTable, vmWriter);
      if (!resultMap.containsKey(DO_NOTHING)) {
        vmWriter.bufferPush(
            Segment.fromCode(resultMap.get(SEGMENT)), Integer.parseInt(resultMap.get(INDEX)));
      }
      var commandCode = firstLine.get(CONTENT).equals("-") ? "negate" : "~";
      vmWriter.bufferArithmetic(ArithmeticCommand.fromCode(commandCode));

      return Map.of(DO_NOTHING, "do nothing"); // 何もしないのでNO_NOTHINGキーを返す。

    } else {
      reader.mark(100);
      var secondLine = parseXMLLine(reader.readLine());
      if (secondLine.get(CONTENT).equals("[")) {
        // 配列宣言のコンパイル
        compileArrayIterator(classSymbolTable, subroutineSymbolTable, secondLine, vmWriter);
        // TODO 引数として配列を渡すことがありえるのかわからないので対応を保留。

      } else {
        reader.reset();
      }
    }

    return resultMap;
  }

  /**
   * コンマで分離された式のリスト（空の可能性もある）をコンパイルする。
   *
   * @return コンマで区切られた引数の数
   */
  public int compileExpressionList(
      SymbolTable classSymbolTable, SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {
    var expressionCount = 0; // 引数の数の初期化

    while (true) {
      reader.mark(100);
      var line = parseXMLLine(reader.readLine());

      /* -----------------------------------引数 : "this", "true", "false"------------------------------- */
      if (line.get(ELEMENT_TYPE).equals("keyword")
          /* -----------------------------------引数 : シンボルテーブルに登録されている変数------------------- */
          /* -----------------------------------引数 : x + yなどの算術演算--------------------------------- */
          /* -----------------------------------引数 : x > yなどの論理演算--------------------------------- */
          || line.get(ELEMENT_TYPE).equals("identifier")
          /* -----------------------------------引数 : 定数--------------------------------- */
          || line.get(ELEMENT_TYPE).equals("integerConstant")) {
        reader.reset();
        expressionCount++;
        compileExpression(classSymbolTable, subroutineSymbolTable, vmWriter);

      } else if (line.get(ELEMENT_TYPE).equals("stringConstant")) {
        /* -----------------------------------引数 : 文字列--------------------------------- */
        // TODO どうする？おそらくOSのString.new()を使う？
        reader.reset();
        expressionCount++;
        compileExpression(classSymbolTable, subroutineSymbolTable, vmWriter);

      } else if (line.get(CONTENT).equals("(")) {
        /* -----------------------------------引数 : (1 + 2)などの式。--------------------------------- */
        reader.reset();
        expressionCount++;
        compileExpression(classSymbolTable, subroutineSymbolTable, vmWriter);

        /* -----------------------------------符号付き整数--------------------------------- */
      } else if (line.get(CONTENT).equals("-")) {
        reader.reset();
        expressionCount++;
        compileExpression(classSymbolTable, subroutineSymbolTable, vmWriter);

        /* -----------------------------------引数がコンマ区切りで複数存在する場合--------------------------------- */
      } else if (line.get(CONTENT).equals(",")) {
        // continue

        /* -----------------------------------引数リスト終わり--------------------------------- */
      } else {
        reader.reset();
        break;
      }
    }
    return expressionCount;
  }

  private void compileArrayIterator(
      SymbolTable classSymbolTable,
      SymbolTable subroutineSymbolTable,
      Map<String, String> firstLine,
      VMWriter vmWriter)
      throws IOException {
    // symbol"["のコンパイル

    // 配列イテレータのコンパイル
    compileExpression(classSymbolTable, subroutineSymbolTable, vmWriter);

    // symbol「]」のコンパイル
    reader.readLine();
  }
}
