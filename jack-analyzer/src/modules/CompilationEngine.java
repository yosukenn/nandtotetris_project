package modules;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/** 再帰によるトップダウン式の解析器<br> */
public class CompilationEngine implements AutoCloseable {

  BufferedWriter writer;

  public CompilationEngine(String inputFile, String outputFile) throws IOException {
    this.writer = new BufferedWriter(new FileWriter(outputFile));
  }

  @Override
  public void close() throws IOException {
    this.writer.close();
  }

  public void compileClass() {}

  public void compileClassVarDec() {}

  public void compileSubroutine() {}

  public void compileParameterList() {}

  public void compileVarDec() {}

  public void complieStatements() {}

  public void compileDo() {}

  public void compileLet() {}

  public void compileWhile() {}

  public void comileReturn() {}

  public void compileIf() {}

  public void compileExpression() {}

  public void compileTerm() {}

  public void compileExpressionList() {}
}