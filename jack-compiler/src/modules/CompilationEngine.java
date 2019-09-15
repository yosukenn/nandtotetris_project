package modules;

import static modules.data.Segment.ARG;
import static modules.data.Segment.CONST;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import modules.data.IdentifierAttr;
import modules.data.Segment;

/*
TODO 修正できる点（多分やらないけど）
 - compileXX 内でwhile文で繰り返し処理をしなくても、再帰的にメソッドを呼び出せばもっとシンプルにかける。
 */

/**
 * 再帰によるトップダウン式の解析器<br>
 * 構文木であるXMLファイル生成関連のプログラムには[create syntax tree]を付与しています。
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
  private static final String EXPRESSION = "expression";

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

    // [create syntax tree]classの書き込み

    // [create syntax tree]keyword"class"の書き込み

    // [create syntax tree]クラス名となるidentifierの書き込み。クラス名はシンボルテーブルに記録しない。
    var secondLine = parseXMLLine(this.reader.readLine());
    this.compiledClassName = secondLine.get(CONTENT);

    // VMWriterの生成
    try (var vmWriter = new VMWriter(new File(parentPath, compiledClassName + ".vm"))) {
      // symbol"{"の書き込み
      var thirdLine = parseXMLLine(this.reader.readLine());

      // [create syntax tree]xml要素の種類によって適切な処理を呼び出す。
      while (true) {
        var forthLine = parseXMLLine(this.reader.readLine());
        switch (forthLine.get(CONTENT)) {
          case STATIC_KEYWORD:
          case FIELD_KEYWORD:
            compileClassVarDec(classSymbolTable, forthLine);
            continue;
          case FUNCTION_KEYWORD:
          case CONSTRUCTOR_KEYWORD:
          case METHOD_KEYWORD:
            compileSubroutine(forthLine, vmWriter);
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
    // [create syntax tree]keyword "static", "field"の書き込み

    // [create syntax tree]データ型を表すkeywordの書き込み
    var secondLine = parseXMLLine(this.reader.readLine());

    // [create syntax tree]変数名を表すidentifierの書き込み
    var thirdLine = parseXMLLine(this.reader.readLine());

    // identifierのシンボルテーブルへの登録。
    classSymbolTable.define(
        thirdLine.get(CONTENT),
        secondLine.get(CONTENT),
        IdentifierAttr.fromCode(stringMap.get(CONTENT)));

    var forthLine = parseXMLLine(this.reader.readLine());
    while (true) {
      if (forthLine.get(CONTENT).equals(";")) {
        // [create syntax tree]symbol";"の書き込み
        break;
      } else if (forthLine.get(CONTENT).equals(",")) { // ","で区切られて複数の変数を宣言している場合

        // [create syntax tree]変数名を表すidentifierの書き込み
        var fifthLine = parseXMLLine(this.reader.readLine());

        // [create syntax tree]identifierのシンボルテーブルへの登録。
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
  public void compileSubroutine(Map<String, String> stringMap, VMWriter vmWriter)
      throws IOException {
    // サブルーチンスコープのシンボルテーブルを作成
    var subroutineSymbolTable = new SymbolTable();
    subroutineSymbolTable.startSubroutine(compiledClassName);

    // [create syntax tree]keyword "function", "constructor", "method"の書き込み

    // [create syntax tree]データ型を表すkeywordの書き込み
    var secondLine = parseXMLLine(this.reader.readLine());

    // [create syntax tree]メソッド、ファンクション、コンストラクタ名identifierの書き込み
    var thirdLine = parseXMLLine(this.reader.readLine());

    // [create syntax tree]symbol「(」の書き込み
    var forthLine = parseXMLLine(this.reader.readLine());

    compileParameterList(subroutineSymbolTable);

    // [create syntax tree]symbol「)」の書き込み
    var fifthLine = parseXMLLine(this.reader.readLine());

    // [create syntax tree]「{ statements }」の書き込み
    compileSubroutineBody(subroutineSymbolTable, vmWriter);

    // VMWriterを使っての関数定義コマンドとstringBufferの書き込み
    vmWriter.writeFunction(
        thirdLine.get(CONTENT), subroutineSymbolTable.varCount(IdentifierAttr.VAR));
    vmWriter.writeStringBuffer();
  }

  private void compileSubroutineBody(SymbolTable subroutineSymbolTable, VMWriter vmWriter)
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
        compileStatements(subroutineSymbolTable, secondLine, vmWriter);
        break;
      }
    }

    // symbol「}」の書き込み
    var thirdLine = Map.of(ELEMENT_TYPE, "symbol", CONTENT, "}", ENCLOSED_CONTENT, " } ");
  }

  public void compileParameterList(SymbolTable subroutineSymbolTable) throws IOException {

    this.reader.mark(100);
    var firstLine = parseXMLLine(this.reader.readLine());
    if (firstLine.get(ELEMENT_TYPE).equals("keyword")) {
      while (true) {
        // keyword(引数の型)を構文木に書き込む

        // identifier(引数の変数名)を構文木に書き込む
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
      SymbolTable subroutineSymbolTable, Map<String, String> firstLine, VMWriter vmWriter)
      throws IOException {

    int returnFlg = 0;

    var line = firstLine;
    while (true) {
      switch (line.get(CONTENT)) {
        case "do":
          compileDo(subroutineSymbolTable, line, vmWriter);
          break;
        case "let":
          compileLet(subroutineSymbolTable, line, vmWriter);
          break;
        case "while":
          compileWhile(subroutineSymbolTable, line, vmWriter);
          break;
        case "return":
          compileReturn(subroutineSymbolTable, line, vmWriter);
          returnFlg = 1;
          break;
        case "if":
          compileIf(subroutineSymbolTable, line, vmWriter);
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

  public void compileDo(
      SymbolTable subroutineSymbolTable, Map<String, String> firstLine, VMWriter vmWriter)
      throws IOException {

    // keyword「do」の出力

    // identifier 変数名 or メソッド名 の出力
    var secondLine = parseXMLLine(this.reader.readLine());

    var numOfArgs = 0; // 呼び出し先の関数の引数の数

    var thirdLine = parseXMLLine(this.reader.readLine());
    if (thirdLine.get(CONTENT).equals(".")) {
      // symbol「.」の出力

      // identifierの出力
      var forthLine = parseXMLLine(this.reader.readLine());

      // symbol「(」の出力
      var fifthLine = parseXMLLine(this.reader.readLine());

      numOfArgs = compileExpressionList(subroutineSymbolTable, vmWriter);

      // symbol「)」の出力
      var sixthLine = parseXMLLine(this.reader.readLine());

      // symbol";"の出力
      var seventhLine = parseXMLLine(this.reader.readLine());

      vmWriter.bufferCallCommand(
          secondLine.get(CONTENT) + thirdLine.get(CONTENT) + forthLine.get(CONTENT), numOfArgs);

    } else if (thirdLine.get(CONTENT).equals("(")) {
      // メソッドの実行主体が書かれていない場合の処理(privateメソッド)

      numOfArgs = compileExpressionList(subroutineSymbolTable, vmWriter);

      // symbol「)」の出力
      var forthLine = parseXMLLine(this.reader.readLine());

      // symbol「;」の出力
      var fifthLine = parseXMLLine(this.reader.readLine());

      vmWriter.bufferCallCommand(compiledClassName + "." + secondLine.get(CONTENT), numOfArgs);
    }
  }

  public void compileLet(
      SymbolTable subroutineSymbolTable, Map<String, String> firstLine, VMWriter vmWriter)
      throws IOException {

    // keyword"let"のコンパイル

    // identifierの出力
    var secondLine = parseXMLLine(this.reader.readLine());

    var thirdLine = parseXMLLine(this.reader.readLine());
    if (thirdLine.get(CONTENT).equals("[")) {
      // 配列宣言"[ iterator ]"部分のコンパイル
      compileArrayIterator(subroutineSymbolTable, thirdLine, vmWriter);

      // symbol「=」のコンパイル
      parseXMLLine(this.reader.readLine());

    } else {
      // symbol「=」の出力
    }

    compileExpression(subroutineSymbolTable, vmWriter);

    // symbol「;」の出力
    var forthLine = parseXMLLine(this.reader.readLine());
  }

  public void compileWhile(
      SymbolTable subroutineSymbolTable, Map<String, String> firstLine, VMWriter vmWriter)
      throws IOException {

    // keyword"while"の書き込み

    // symbol"(" の書き込み
    var secondLine = parseXMLLine(this.reader.readLine());

    compileExpression(subroutineSymbolTable, vmWriter);

    // symbol ")" の書き込み
    var thirdLine = parseXMLLine(this.reader.readLine());

    // symbol「{」の書き込み
    var forthLine = parseXMLLine(this.reader.readLine());

    var fifthLine = parseXMLLine(this.reader.readLine());
    compileStatements(subroutineSymbolTable, fifthLine, vmWriter);

    // TODO compileStatements()で行を読み込み過ぎているのを修正する必要があるが、これを修正すると崩壊するので無理やり"}"をコンパイルするようにしている。
    var closeSymbolLine = Map.of(ELEMENT_TYPE, "symbol", CONTENT, "}", ENCLOSED_CONTENT, " } ");
  }

  public void compileReturn(
      SymbolTable subroutineSymbolTable, Map<String, String> firstLine, VMWriter vmWriter)
      throws IOException {

    // keyword"return"のコンパイル

    this.reader.mark(100);
    var secondLine = parseXMLLine(this.reader.readLine());
    if (secondLine.get(ELEMENT_TYPE).equals("identifier")
        || secondLine.get(ELEMENT_TYPE).equals("keyword")) {
      this.reader.reset();
      compileExpression(subroutineSymbolTable, vmWriter);

      var forthLine = parseXMLLine(this.reader.readLine());

    } else if (secondLine.get(ELEMENT_TYPE).equals("symbol")) {
    }
  }

  public void compileIf(
      SymbolTable subroutineSymbolTable, Map<String, String> firstLine, VMWriter vmWriter)
      throws IOException {

    // keyword"if"のコンパイル

    // symbol"("のコンパイル
    var secondLine = parseXMLLine(this.reader.readLine());

    // if条件式のコンパイル
    compileExpression(subroutineSymbolTable, vmWriter);

    // symbol")"のコンパイル
    var thirdLine = parseXMLLine(this.reader.readLine());

    // symbol"{"のコンパイル

    // statementsのコンパイル
    var forthLine = parseXMLLine(this.reader.readLine());
    compileStatements(subroutineSymbolTable, forthLine, vmWriter);

    // symbol"}"のコンパイル
    var fifthLine = Map.of(ELEMENT_TYPE, "symbol", CONTENT, "}", ENCLOSED_CONTENT, " } ");

    this.reader.mark(100); // 取り消せるようにマーク
    var sixthLine = parseXMLLine(this.reader.readLine());
    if (sixthLine.get(CONTENT).equals("else")) {
      // keyword"else"のコンパイル

      // symbol"{"のコンパイル
      this.reader.readLine();

      // statementsのコンパイル
      compileStatements(subroutineSymbolTable, parseXMLLine(this.reader.readLine()), vmWriter);

      // symbol"}"のコンパイル
      var seventhLine = Map.of(ELEMENT_TYPE, "symbol", CONTENT, "}", ENCLOSED_CONTENT, " } ");
    } else {
      this.reader.reset(); // TODO 読み込んだ結果"else"がないIf文だった場合、ちゃんと読み込みを取り消せられているか確認。
    }
  }

  public Map<String, String> compileExpression(SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {

    var resultMap = compileTerm(subroutineSymbolTable, vmWriter);

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
      compileTerm(subroutineSymbolTable, vmWriter);

    } else {
      this.reader.reset();
    }

    this.reader.mark(100);
    var secondLine = parseXMLLine(this.reader.readLine());
    if (secondLine.get(CONTENT).equals("&lt;")) { // 不等号
      //      appendChildIncludeText(
      //          expression, Map.of(ELEMENT_TYPE, "symbol", CONTENT, "<", ENCLOSED_CONTENT, " <
      // "));
      compileTerm(subroutineSymbolTable, vmWriter);
    } else if (secondLine.get(CONTENT).equals("&gt;")) {
      //      appendChildIncludeText(
      //          expression, Map.of(ELEMENT_TYPE, "symbol", CONTENT, ">", ENCLOSED_CONTENT, " >
      // "));
      compileTerm(subroutineSymbolTable, vmWriter);
    } else {
      this.reader.reset();
    }

    this.reader.mark(100);
    var thirdLine = parseXMLLine(this.reader.readLine()); // and演算子
    if (thirdLine.get(CONTENT).equals("&amp;")) { // TODO "&"が複数出てくるパターン対策。
      //      appendChildIncludeText(
      //          expression, Map.of(ELEMENT_TYPE, "symbol", CONTENT, "&", ENCLOSED_CONTENT, " &
      // "));
      compileTerm(subroutineSymbolTable, vmWriter);
    } else {
      this.reader.reset();
    }

    return resultMap;
  }

  /** 式(Expression)の一部分をコンパイルする */
  public Map<String, String> compileTerm(SymbolTable subroutineSymbolTable, VMWriter vmWriter)
      throws IOException {

    Map<String, String> resultMap = new HashMap<>();

    var firstLine = parseXMLLine(this.reader.readLine());
    if (firstLine.get(ELEMENT_TYPE).equals("identifier")) {
      // ---------------------シンボルテーブルに定義されている変数------------------------
      resultMap =
          Map.of(
              SEGMENT,
              subroutineSymbolTable
                  .kindOf(firstLine.get(CONTENT))
                  .getCode(), // TODO IdentifierAttrとSegmentは必ずしも一致しないので、多分ずれてくる。一旦保留。
              EXPRESSION,
              String.valueOf(subroutineSymbolTable.indexOf(firstLine.get(CONTENT))));

      // ---------------------keyword "this"--------------------------------
    } else if (firstLine.get(CONTENT).equals("this")) {

      resultMap = Map.of(SEGMENT, ARG.getCode(), EXPRESSION, "0");

    } else if (firstLine.get(ELEMENT_TYPE).equals("integerConstant")) {
      // ---------------------定数：integerConstant---------------------------
      resultMap = Map.of(SEGMENT, CONST.getCode(), EXPRESSION, firstLine.get(CONTENT));

    } else if (firstLine.get(ELEMENT_TYPE).equals("stringConstant")) {
      // ---------------------文字列：stringConstant---------------------------
      // TODO どうする？一旦保留

    } else {
      // ---------------------"true", "false"---------------------------------
      var number = firstLine.get(CONTENT).equals("true") ? "1" : "0";
      resultMap = Map.of(SEGMENT, CONST.getCode(), EXPRESSION, number);
    }

    if (firstLine.get(CONTENT).equals("(")) {
      // "( sign variable )" をコンパイルする。

      // "sign variable"
      compileExpression(subroutineSymbolTable, vmWriter);

      // ")"
      var secondLine = parseXMLLine(this.reader.readLine());

    } else if (firstLine.get(CONTENT).equals("-") || firstLine.get(CONTENT).equals("~")) {
      // 符号付き変数のコンパイル
      compileTerm(subroutineSymbolTable, vmWriter);

    } else {
      this.reader.mark(100);
      var secondLine = parseXMLLine(this.reader.readLine());
      if (secondLine.get(CONTENT).equals(".")) {
        // サブルーチン呼び出し
        compileCallSubroutine(subroutineSymbolTable, secondLine, vmWriter);
      } else if (secondLine.get(CONTENT).equals("[")) {
        // 配列宣言のコンパイル
        compileArrayIterator(subroutineSymbolTable, secondLine, vmWriter);

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

    // TODO 作業中 関数呼び出しの前に引数をスタックにプッシュする。残りは論理式と算術式が引数として渡ってきたときの処理。

    while (true) {
      this.reader.mark(100);
      var line = parseXMLLine(this.reader.readLine());

      /* -----------------------------------引数あり--------------------------------- */
      if (line.get(ELEMENT_TYPE).equals("keyword")) {
        // "this", "true", "false"
        this.reader.reset();
        expressionCount++;
        var resultMap = compileExpression(subroutineSymbolTable, vmWriter);
        vmWriter.bufferPushCommand(
            Segment.fromCode(resultMap.get(SEGMENT)), Integer.parseInt(resultMap.get(EXPRESSION)));

      } else if (line.get(ELEMENT_TYPE).equals("identifier")) {
        // シンボルテーブルに登録されている変数
        this.reader.reset();
        expressionCount++;
        var resultMap = compileExpression(subroutineSymbolTable, vmWriter);
        vmWriter.bufferPushCommand(
            Segment.fromCode(resultMap.get(SEGMENT)), Integer.parseInt(resultMap.get(EXPRESSION)));

      } else if (line.get(ELEMENT_TYPE).equals("integerConstant")) {
        // 定数
        this.reader.reset();
        expressionCount++;
        var resultMap = compileExpression(subroutineSymbolTable, vmWriter);
        vmWriter.bufferPushCommand(
            Segment.fromCode(resultMap.get(SEGMENT)), Integer.parseInt(resultMap.get(EXPRESSION)));

      } else if (line.get(ELEMENT_TYPE).equals("stringConstant")) {
        // 文字列 // TODO ?
        this.reader.reset();
        expressionCount++;
        compileExpression(subroutineSymbolTable, vmWriter);

      } else if (line.get(CONTENT).equals("(")) {
        // (1 + 2)などの式。
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
