package modules;

import java.util.HashMap;
import java.util.Map;

/**
 * Hack命令中のシンボルを補完するクラス。
 */
public class SymbolTable {

    /**
     * シンボルテーブル
     */
    private Map<String, Long> symbolTable;

    public SymbolTable() {
        // 定義済みシンボルをシンボルテーブルに登録する
        this.symbolTable = new HashMap<>();
        this.symbolTable.put("SP", 0L);
        this.symbolTable.put("LCL", 1L);
        this.symbolTable.put("ARG", 2L);
        this.symbolTable.put("THIS", 3L);
        this.symbolTable.put("THAT", 4L);
        for (long i = 0; i <= 15; i++) {
            this.symbolTable.put("R" + i, i);
        }
        this.symbolTable.put("SCREEN", 16384L);
        this.symbolTable.put("KBD", 24576L);
    }

    /**
     * テーブルに (symbol, address) のペアを追加する。
     * @param symbol
     * @param address
     */
    public void addEntry(String symbol, long address) {
        this.symbolTable.put(symbol, address);
    }

    /**
     * シンボルテーブルは与えられたsymbolを含むか？
     * @param symbol
     * @return
     */
    public boolean contains(String symbol) {
        return this.symbolTable.containsKey(symbol);
    }

    /**
     * symbol に結び付けられたアドレスを返す。
     * @param symbol
     * @return
     */
    public long getAddress(String symbol) {
        return this.symbolTable.get(symbol);
    }

}
