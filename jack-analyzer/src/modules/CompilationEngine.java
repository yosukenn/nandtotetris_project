package modules;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/** 再帰によるトップダウン式の解析器<br> */
public class CompilationEngine implements AutoCloseable {

  XMLStreamReader reader;
  BufferedWriter writer;

  public CompilationEngine(String inputFile, String outputFile)
      throws IOException, XMLStreamException {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    this.reader = factory.createXMLStreamReader(new FileInputStream(inputFile));
    this.writer = new BufferedWriter(new FileWriter(outputFile));
  }

  @Override
  public void close() throws IOException, XMLStreamException {
    this.reader.close();
    this.writer.close();
  }

  // TODO まずは書き込みは考えずに読み込みだけ実装。
  public void compileClass() throws XMLStreamException {
    // 字句要素 keyword の class でなかったら例外。

    // <class></class> の書き出し

    // xml要素の種類によって適切な処理を呼び出す。
    while (this.reader.hasNext()) {
      int eventType = this.reader.next();
    }
  }

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
