# NullpoMino_Alt ～ぬるぽミノ改～ Version 7.7.20xx

## これって何？

Javaで作られた落ちものアクションパズルゲームもどきを
Kotlin/JVMでリメイクしたものです。 現状Slick版のみです。

## 起動方法

動作にはJava Runtime Environmentが必要です。http://www.java.com/ja/download/
また、OpenGLに対応したビデオカードが必要です。

IntelliJやmavenなどからビルドする場合のSDKはEclipse Temurin推奨です。

### Windows

- `play_slick.bat`:本体を起動します。
- `ruleeditor.bat`:ルールの作成- 編集ができるツールを起動します。
- `sequencer.bat`:リプレイファイルを開いてNEXTの順番を確認できるツールを起動します。（Zirceanさん開発）
- `musiclisteditor.bat`:ミュージックリストエディタを起動します。BGMの設定ができます。
- `netserver.bat`:NetServer（ネットプレイ用サーバー）を起動します。
- `netadmin.bat`:NetAdmin（NetServer管理ツール）を起動します。
- `airankstool.bat`:RanksAIの定石ファイルを作成するツールを起動します。（大量のメモリを必要とします）

### Linux

端末ウィンドウでNullpoMinoがあるディレクトリ（このファイルがあるディレクトリ）まで移動して 以下のコマンドを入力してEnterキーを押すとたぶん起動します。
(注:初回起動時は、最初にchmod +x <コマンド名>で起動用シェルスクリプトに実行権限を与えてください)

- Slickバージョン:`./play_slick`または`./NullpoMino`
- ルールエディタ:`./ruleeditor`
- シーケンスビューア:`./sequencer`
- ミュージックリストエディタ:`./musiclisteditor`
- ネットプレイ用サーバー:`./netserver`
- NetAdmin:`./netadmin`
- AI Ranks Tool:`./airankstool`

使用しているビデオカードやLinuxのバージョンによってはうまく動かないかもしれません。 Slickバージョン固有の問題:
3Dデスクトップ機能（Berylとか）は無効にすることをおすすめします。 一応x64でも動作するようです。

LWJGLおよびSCIMのバグ(もしくは制限)のため、play_slickシェルスクリプトはゲームを起動するときに全てのIMEを無効化します。
シェルスクリプト内のコマンドライン最初のXMODIFIERS=@im=noneは、使っているIMEがSCIM以外の場合は不要です。

もしSCIMを有効化したままゲームをプレイしたい場合は、以下のコマンドを試してください(sudoを実行できる権限が必要です):

```
sudo chmod go+r /dev/input/*
java -cp bin:NullpoMino.jar:lib/log4j-1.2.15.jar:lib/slick.jar:lib/lwjgl.jar:lib/jorbis-0.0.15.jar:lib/jogg-0.0.7.jar:lib/ibxm.jar:lib/jinput.jar -Djava.library.path=lib mu.nu.nullpo.gui.slick.NullpoMinoSlick -j
```

最初のコマンドは、すべてのプログラムがキーボード入力を直接読み取れるようにします。\
一度実行すると、次に再起動もしくはシャットダウンするまで再度実行する必要はありません。

2つ目のコマンドは、ゲームを"-j"オプションを付けて起動します。 通常、このゲームはキーボード入力をLWJGLから読み取ろうとしますが、SCIMとは相性が悪いです。\
このオプションを使用している場合、ゲームはキーボード入力をシステムから直接読み取りますので、SCIMがあっても問題なく動作するようになります。\
ただし一部認識できないキー（;など）があります。

### リソースパック- ファイル一覧

- graphics/
  - back/ graphics/back??.pngをこのフォルダに移動
    - back0.png
    - back1.png
    - back2.png ...
  - blockskin/ nullpomino7.5.0と互換性あり
    - big/b0.png
    - normal/n0.png
    - small/s0.png
  - effects/ break?_?.pngはnullpomino7.5.0と互換性なし
    -
  - frames/
- se/ 同名は「.wav」「.ogg」の順に優先
  - applause0 applause1 applause2 applause3 applause4 applause5
  - b2b_combo b2b_end b2b_start
  - bomb
  - bravo
  - cancel
  - change
  - cheer
  - combo
  - combo_pow
  - cool
  - countdown
  - crowd0 crowd1
  - cursor
  - danger
  - dead
  - dead_last
  - decide decide0 decide1 decide2
  - endingstart
  - erase0 erase1 erase2
  - excellent
  - firecracker0 firecracker1 firecracker2
  - gamelost
  - gamewon
  - garbage0 garbage1
  - gem
  - grade0 grade1 grade2 grade3 grade4
  - gradeup
  - harddrop
  - hold
  - holdfail
  - hurryup
  - initialhold
  - initialrotate
  - item_laser
  - item_spawn
  - item_trigger
  - levelstop
  - levelup
  - levelup_section
  - line1 line2 line3 line4
  - linefall linefall1
  - lock
  - matchend
  - medal1 medal2 medal3
  - move
  - movefail
  - pause
  - piece_i piece_i1 piece_i2 piece_i3 piece_i4 piece_j piece_l piece_o piece_s piece_t piece_z
  - puzzle_skip
  - puzzle_timeout
  - regret
  - rotate
  - rotfail
  - shutter
  - softdrop
  - split
  - square_g square_s
  - stageclear
  - stagefail
  - start0
  - start1
  - step
  - stgstar
  - time_pen time_rec
  - timeover
  - twist
  - twister
  - wallkick
  - warning
- jingle/ 同名は「.wav」「.ogg」の順に優先
  - excellent0
  - excellent1
  - excellent2
  - welcome 起動時
