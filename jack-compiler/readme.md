# 各コンパイルメソッドでやること
## compileClass()
- クラス名が判明するのでここでwriterを生成できる。

## compileClassVarDec()
- クラススコープのシンボルテーブルに変数を登録するだけなのでVMコマンド書き込みは発生しない。

## compileSubroutine()
- 関数を定義。 fucntion [Class.subroutine] [number_of_local_variables]
    - local変数の個数は、サブルーチン内の処理が全て明らかにならないとわかりようがないので、最後にまとめて書き込みを行う。

## 

# VMコマンド
## 算術コマンド
スタック上で算術演算と論理演算を行う。<br>
スタックから２つのデータを取り出し（pop）、そのデータに対して2変数関数を実行し、その結果をスタックに返すものと、<br>
スタックから１つのデータを取り出し（pop）、そのデータに対して1変数関数を実行し、その結果をスタックに返すものがある。

### 算術演算
- add : 加算
- sub : 減算
- neg : 符号反転
- not

### 論理演算
- eq : 等しい
- gt : 〜より大きい greater than
- lt : 〜より小さい less than
- and
- or

## メモリアクセスコマンド
- push segment index : segment[index]をスタックの上にプッシュする。
- pop segment index : スタックの一番上のデータをポップし、それをsegment[index] に格納する。

### セグメント一覧
- argument : 関数の引数を格納
- local : 関数のローカル変数を格納
- static : スタティック変数を格納
- constant : 0~32767の定数を表す擬似セグメント
- this
- that
- pointer
- temp

## プログラムフローコマンド
- goto
- if-goto
- label : gotoコマンドの移動先を示す。

## 関数呼び出しコマンド
- function f n : n個のローカル変数を持つfという名前の関数を定義する。
- call f m : fという関数を呼ぶ。m個の引数wpあらかじめスタックにプッシュしておく。
- return : 呼び出し元へリターンする。