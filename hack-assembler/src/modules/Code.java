package modules;

/**
 * アセンブリ言語のニーモニックをバイナリコードへ変換する
 */
public class Code {

    // compコード
    String a = "0";
    String c = "000000";

    /**
     * @param mnemonic destニーモニック
     * @return destニーモニックのバイナリコード
     */
    public String dest(String mnemonic) {
        // destコード
        String d1 = "0";
        String d2 = "0";
        String d3 = "0";

        if (mnemonic.equals("empty")) {
            return d1 + d2 + d3;
        }

        if (mnemonic.contains("A")) {
            d1 = "1";
        }
        if (mnemonic.contains("D")) {
            d2 = "1";
        }
        if (mnemonic.contains("M")) {
            d3 = "1";
        }
        return d1 + d2 + d3;
    }

    /**
     *
     * @param mnemonic compニーモニック
     * @return compニーモニックのバイナリコード
     */
    public String comp(String mnemonic) {
        // compコードの初期化
        a = "0";
        c = "000000";

        if (mnemonic.equals("empty")) {
            return a + c;
        }

        return createCompBinary(mnemonic);
    }

    /**
     *
     * @param mnemonic jumpニーモニック
     * @return jumpニーモニックのバイナリコード
     */
    public String jump(String mnemonic) {
        // jumpコード
        String j = "000";
        if (mnemonic.equals("empty")) {
            return j;
        }
        if (mnemonic.equals("JGT")) {
            j = "001";
        } else if (mnemonic.equals("JEQ")) {
            j = "010";
        } else if (mnemonic.equals("JGE")) {
            j = "011";
        } else if (mnemonic.equals("JLT")) {
            j = "100";
        } else if (mnemonic.equals("JNE")) {
            j = "101";
        } else if (mnemonic.equals("JLE")) {
            j = "110";
        } else if (mnemonic.equals("JMP")) {
            j = "111";
        }
        return j;
    }

    private String createCompBinary(String mnemonic) {
        // a=1の時のニーモニックをバイナリコードへマッピング
        if (mnemonic.contains("M")) {
            a = "1";
            if (mnemonic.equals("M")) {
                c = "110000";
            } else if (mnemonic.equals("!M")) {
                c = "110001";
            } else if (mnemonic.equals("-M")) {
                c = "110011";
            } else if (mnemonic.equals("M+1")) {
                c = "110111";
            } else if (mnemonic.equals("M-1")) {
                c = "110010";
            } else if (mnemonic.equals("D+M")) {
                c = "000010";
            } else if (mnemonic.equals("D-M")) {
                c = "010011";
            } else if (mnemonic.equals("M-D")) {
                c = "000111";
            } else if (mnemonic.equals("D&M")) {
                c = "000000";
            } else if (mnemonic.equals("D|M")) {
                c = "010101";
            }
        } else {
            // a=0の時のニーモニックをバイナリコードへマッピング
            if (mnemonic.matches("0")) {
                c = "101010";
            } else if (mnemonic.equals("1")) {
                c = "111111";
            } else if (mnemonic.equals("-1")) {
                c = "111010";
            } else if (mnemonic.equals("D")) {
                c = "001100";
            } else if (mnemonic.equals("A")) {
                c = "110000";
            } else if (mnemonic.equals("!D")) {
                c = "001101";
            } else if (mnemonic.equals("!A")) {
                c = "110011";
            } else if (mnemonic.equals("-D")) {
                c = "001111";
            } else if (mnemonic.equals("-A")) {
                c = "110011";
            } else if (mnemonic.equals("D+1")) {
                c = "011111";
            } else if (mnemonic.equals("A+1")) {
                c = "110111";
            } else if (mnemonic.equals("D-1")) {
                c = "001110";
            } else if (mnemonic.equals("A-1")) {
                c = "110010";
            } else if (mnemonic.equals("D+A")) {
                c = "000010";
            } else if (mnemonic.equals("D-A")) {
                c = "010011";
            } else if (mnemonic.equals("A-D")) {
                c = "000111";
            } else if (mnemonic.equals("D&A")) {
                c = "000000";
            } else if (mnemonic.equals("D|A")) {
                c = "010101";
            }
        }
        return a + c;
    }

}
