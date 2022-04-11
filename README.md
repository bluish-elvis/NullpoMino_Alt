# NullpoMino ～ぬるぽミノ～ Version 7.7.Alt

## これって何？

Java+で作った落ちものアクションパズルゲームもどきを Kotlin/JVMでリメイクしたものです。 現状Slick版のみです。

## 起動方法

動作にはJava Runtime Environmentが必要です。http://www.java.com/ja/download/
また、OpenGLに対応したビデオカードが必要です。

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

## 遊び方

- 7種類のブロックがランダムに上中央から落下してきます。
  - 落下するブロックは操作によって移動- 回転させたり、早く落下させる事ができます。
  - 落下中のブロックが地面や他のブロックの上に着くと、暫くした後にそのブロックが固定されて、上から次のブロックが落下してきます。
- 固定されたブロックは横一列に隙間無く並べてラインを作ることで消すことができます。
  - 積み上がった残りのブロックはその分だけ降下しますが、下の隙間を埋めることはありません。
  - 一度に複数のラインを作ると高得点です。ラインを作った総数が増えると、その後のブロック落下速度が上がることがあります。
- ブロックが上まで積みあがり、次のブロックが落ちてこれなくなるとゲームオーバーです。
  - モードによっては、規定数のラインを作るなどでゲームクリアになります。

### ボタンの説明

- UP：ハードドロップ（ブロックを一瞬で落下）- カーソルを上に移動
- DOWN：ソフトドロップ（ブロックを早く落下）- カーソルを下に移動
- LEFT：ブロックを左に移動- カーソルで選択している項目の値を1つ減らす
- RIGHT：ブロックを右に移動- カーソルで選択している項目の値を1つ増やす
- A：ブロックの回転- メニュー項目の決定
- B：ブロックの逆回転- キャンセル
- C：ブロックの回転
- D：ホールド（ブロックを一時的に保管して、後で使うことができます）
- E：ブロックの180度回転
- F：エンディング早送り（SPEED MANIAとGARBAGE MANIAモードで使用可能）- ネットプレイで練習モードを開始／終了
- QUIT：ゲームを終了する
- PAUSE：ゲームを一時停止
- GIVEUP：タイトルに戻る
- RETRY：ゲームを最初からやり直す
- FRAME STEP：ポーズ中に押すと1フレームだけゲームを進める（設定で有効にしている場合）
- SCREEN SHOT：スクリーンショットをssフォルダに保存

### キー配置

- メニュー画面でのキー配置

| ボタン名        | Blockbox     | Guideline    | Classic      |
|-------------|--------------|--------------|--------------|
| UP          | Cursor Up    | Cursor Up    | Cursor Up    |
| DOWN        | Cursor Down  | Cursor Down  | Cursor Down  |
| LEFT        | Cursor Left  | Cursor Left  | Cursor Left  |
| RIGHT       | Cursor Right | Cursor Right | Cursor Right |
| A           | Enter        | Enter        | A            |
| B           | Escape       | Escape       | S            |
| C           | A            | C            | D            |
| D           | Space        | Shift        | Z            |
| E           | D            | X            | X            |
| F           | S            | V            | C            |
| QUIT        | F12          | F12          | Escape       |
| PAUSE       | F1           | F1           | F1           |
| GIVEUP      | F11          | F11          | F12          |
| RETRY       | F10          | F10          | F11          |
| FRAME STEP  | N            | N            | N            |
| SCREEN SHOT | F5           | F5           | F10          |

- ゲーム中のキー配置

| ボタン名        | Blockbox     | Guideline    | Classic      |
|-------------|--------------|--------------|--------------|
| UP          | Cursor Up    | Space        | Cursor Up    |
| DOWN        | Cursor Down  | Cursor Down  | Cursor Down  |
| LEFT        | Cursor Left  | Cursor Left  | Cursor Left  |
| RIGHT       | Cursor Right | Cursor Right | Cursor Right |
| A           | Z            | Z            | A            |
| B           | X            | Cursor Up    | S            |
| C           | A            | C            | D            |
| D           | Space        | Shift        | Z            |
| E           | D            | X            | X            |
| F           | S            | V            | C            |
| QUIT        | F12          | F12          | Escape       |
| PAUSE       | Escape       | Escape       | F1           |
| GIVEUP      | F11          | F11          | F12          |
| RETRY       | F10          | F10          | F11          |
| FRAME STEP  | N            | N            | N            |
| SCREEN SHOT | F5           | F5           | F10          |

キー配置はタイトルの「CONFIG」の中にある「[KEYBOARD SETTING]」から変更できます。

### 【設定のリセット】

設定をリセットしたいときは、以下のファイルを削除してください。

- Swing版：config/setting/swing.cfg
- Slick版：config/setting/slick.cfg
- SDL版：config/setting/sdl.cfg
- 各バージョン共通の設定：config/setting/global.cfg
- ゲームモード別の設定：config/setting/mode/***.cfg
- ゲームモード別の設定：config/setting/mode/***.cfg

### 【ゲームルール】

選んだゲームルールに応じて操作性やブロックの見た目が変わります。 CONFIGの中にある「[RULE SELECT]」から使用するルールを変更できます。 付属のルールエディタを使うと独自のルールを作成できます。 NEXT順生成アルゴリズム：

- BagRandomizer: ７種全てのブロックで１巡していく。 一番最初にSZOが出ないバージョンや、１巡中のブロックが＋１、－１、×２、×９になるバージョンも有る。
- HistoryRandomizer: 一番最初にSZOが出ないほか、一度出たブロックは最小でも4ブロックの間は出にくくなる。
- Memoryless/Nintendo:制限なし。Nintendo/GameBoyはブロックが連続しない
- FixedSequence:sequence.txtの順番を読み込む。
- DistanceWeight:全ブロックが平等に出るように都度当選確率が変動する。

### 【ゲームモード】

- MARATHON
  - 10ライン消すごとにレベルが上がる初心者向けモードです。
  - 最高レベルを「15レベル/150ラインまで」「20レベル/200ラインまで」「100レベル/999ラインまで」「無限/１回積み上がるまで」から選べます。
- MARATHON+
  - やや難易度の高い標準モードです。
  - 最高レベルを「Lv20(200ライン)」「Lv30(300ライン)」「Lv50(400ライン)」「Lv200(500ライン以上)」から選べます。
    設定したレベルをクリアすると一定時間「ボーナスレベル」に突入します。
    - Lv20でのボーナスレベルのみ無限に続きますが、置いたブロックがたまにしか表示されません。
- EXTREME MARATHON
  - MARATHONと似ていますが、ブロックの落下速度が常に最大で、地上でブロックを滑らせながら積んでいく上級者向けモードです。
  - 10ライン消す毎に、ブロックを動かせる時間が短くなっていきます。

- SHUTTLE RUN
  - ライン消去でできるだけ早く「GOAL」を減らし、次のレベルに進むことが目的のモードです。
  - 一度に複数のラインを消したり、連続でラインを消したりすると「GOAL」を効率よく減らせます。
  - ２分以内に0にするとボーナス得点が入り次のレベルに進めます。目的や制限時間が異なる5種類のゲームタイプを選べます。
    - LV15-TIME TRIAL： レベル15を突破するまでのスコアとタイムを競います。 時間切れになるまえにレベルアップすると、ボーナス得点が得られます。
    - LV15-SPEED RUN： レベル15まで遊べますが、２分以内にレベルアップしないとゲームオーバーです。
    - 10MIN-TRIAL：10分間でのスコアとレベルを競います。 ２分以内にレベルアップしなかった場合、「REGRET」と表示されてGOALがリセットされてしまいます。
    - 10MIN-SURVIVAL：10分間プレイできますが、２分以内にレベルアップしないとゲームオーバーです。
    - SPECIAL： レベルアップすると制限時間が30秒回復する耐久モードです。

- DIG CHALLENGE
  - 下からどんどんせり上がってくる邪魔ブロックをひたすら消していくモードです。邪魔ブロックを消すと高得点です。
    - NORMAL: ピースを置くまで溜まった分が一気にせり上がる
    - REALTIME: ピースを置かなくてもせり上がる

- LINE RACE
  - どれだけ早く規定ライン数を消せるか競うタイムアタックモードです。
  - 規定ライン数は20ライン, 40ライン, 100ラインの3種類から選択可能です。
- SCORE RACE
  - どれだけ早く規定スコアに到達できるかを競うタイムアタックモードです。
  - 規定スコアは10000点, 25000点, 30000点の3種類から選択可能です。
- DIG RACE
  - どれだけ早くすべての邪魔ブロックを消せるか競うタイムアタックモードです。
  - 邪魔ブロックの数は5ライン, 10ライン, 18ラインから選択可能です。
- COMBO RACE
  - 規定コンボをどれだけ早く達成できるか、または最大で何コンボできるかを競います。
  - 最大ライン数は20ライン, 40ライン, 100ライン, エンドレスから選択可能です。
  - エンドレスでコンボが途切れた場合や、最大ライン数が更新不可能になった場合はゲームオーバーになります。
- ULTRA 制限時間内にどれだけ多くの得点/ライン消しを狙えるかを競うモードです。
  - 制限時間は1～5分の5種類から選択可能です。

- SQUARE
  - 縦4x横4のサイズの正方形を作って消していくモードです。
  - 2種類以上のブロックを使って正方形を作ると銀色、1種類のブロックだけで正方形を作ると金色になります。
  - 銀色/金色にしたブロックを含んでラインを消すと高得点で、金色は銀色の２倍の特典です。
  - 3種類のゲームタイプを選べます。
    - MARATHON: エンドレス
    - SPRINT:  150点取るまでのタイムアタック
    - ULTRA:   3分間スコアアタック 基本的にどんなルールでも一応遊べますが、「SQUARE」ルールを使うと正方形を作り易い順番でブロックが落ちてきます。

- RETRO MARATHON
  - 全体的にスピードの上昇は緩やかですが、接地時に即固定されてしまうモードです。
    - TYPE A：エンドレス
    - TYPE B：25ラインを消すとクリア
    - TYPE C：スコア999999を取るとクリア
- RETRO MASTERY
  - 上級者向けになった、接地時に即固定されてしまうモードです。いかに無駄な消し方をしないかがハイスコアの鍵です。\
    「NINTENDO-R」ルールがおすすめです。
  - 200：
  - ENDLESS：
  - PRESSURE：
- RETRO MANIA
  - MARATHON(ENDLESS)とほぼ同様のゲームが遊べますが、4ライン消すか一定時間が経つとレベルが上がります。\
    「CLASSIC0」ルールがおすすめです。
- Retro Modern
  - Marathon+(Lv30)と似たゲームが遊べますが、ボーナスレベルの内容が異なり、一定の順番でブロックが落ちてきます。\
    同じライン数消しを３回行うことでボーナス点が入ります。

- Grand Festival
  - レベル300に到達するまでに稼いだ得点と打ち上げた花火の数を競うモードです。\
    スピードの上昇は緩やかで、初心者向けです。
- Grand Marathon レベル999になるまでに、スコアを稼いで高い段位を目指す中級者向けモードです。\
  レベルはブロックを置くだけで1つ上がりますが、レベルの末尾2桁が99のとき、およびレベル998ではラインを消さないと上がりません。 レベル500で最高速度になります。
- Grand Mania 高速でブロックを積んでいき、レベル999突破までの高い段位を目指す中～上級者向けモードです。\
  このモードでは段位と得点は無関係で、短時間で大量のラインを消すことで段位が上がります。 Grand Marathonと違い、レベル500からはブロック出現間隔と操作時間が短縮され始めます。
- Grand Mastery プレイ次第で最高速度が大きく上がる、中～上級者向けモードです。\
  Grand Maniaに似ていますが、レベルを速く上げるほど段位や速度がより上昇しやすくなります。

- Grand Mountain 時々灰色のブロックが下からせり上がってくるモードです。\
  レベルが上がるほどせり上がりのペースも上がってきます。
- Grand Storm 落下速度が最大の状態からスタートする上級者向けモードです。\
  レベル500を短時間で突破すると、最高速度状態でレベル999までプレイできます。
- Grand Lightning Grand Stormよりもさらに高速でブロックを積んでいく超上級者向けモードです。\
  一定時間内にレベル500を突破すると、何かが起こり始めます。
- Grand Phanthom Grand Stormモードと似ていますが、このモードでは置いたブロックが徐々に見えなくなっていきます。\
  フィールドの地形を記憶することが重要となります。
- Grand Finale 超高速、かつ過酷な条件下でプレイするモードです。\
  Grand LightningとGrand Phantom、Grand Roadsを完全クリアしたプレイヤー向けです。
- Grand Roads できるだけ速く150ラインまたは200ライン消すことが目的のモードです。\
  10ライン消すたびにレベルが上がりますが、制限時間内にレベルアップしないとゲームオーバーです。
  - 150ラインタイプ(右に行くほど難しい):
    EASY＜HARD＜20G＜ANOTHER＜EXTREME
  - 200ラインタイプ(右に行くほど難しい):
    MODERATE＜EXHAUST＜CHALLENGE＜HELL≒INSANE＜VOID

- PRACTICE 好きな速度を設定して練習ができるモードです。 出現するブロックの種類も設定できます。

- GEM MANIA フィールドに配置された宝石ブロックをできるだけ速くすべて消去することが目的のモードです。
  - 各ステージには制限時間が存在し、1分以内にクリアできないと次のステージに移ってしまいます。
  - これとは別にゲーム全体の制限時間も存在し、これが0になるとゲームオーバーです。各ステージを20秒以内にクリアすると少し回復します。

- VS-BATTLE 人間またはコンピュータと対戦するモードです。
  - 一度に複数のラインを消すとお邪魔ブロックを相手に送ることができます。\
    お邪魔ブロックで相手をゲームオーバーにさせると勝利です。

- TOOL-VS MAP EDIT このモードは厳密に言うと「ゲーム」モードではなく、VS-BATTLEとネットプレイで使用できるマップを作成できるモードです。
  [フィールド編集画面のときの操作方法]
  - Up/Down/Left/Right: カーソルを動かす
  - A: カーソル位置にブロックを置く
  - B: メニューに戻る
  - C+Left/Right: 配置するブロックの色を選ぶ
  - D: カーソル位置にあるブロックを消す

- AVALANCHE 1P (RC1)
  - 同じ色のブロックを縦か横に4つ以上繋げて消していくモードです。途中で折れ曲がっていてもOKですが、斜めにはくっつきません。
  - 空中に浮いたブロックは全て重力に従って落下します。これを利用して連鎖を決めれば高得点が入ります。
  - 選べるゲームタイプはSQUAREモードと同じです。
  - まともにプレイする場合は「AVALANCHE」ルールを使用してください。

- AVALANCHE 1P FEVER MARATHON (RC1)
  - あらかじめ簡単に連鎖できるように組まれたブロック）が積まれた状態でゲームが始まります。\ 一番長い連鎖ができると思うところにブロックを置いて、連鎖をスタートさせてください。
  - 連鎖終了後、新しい連鎖のタネが出現します。うまく連鎖できれば、次に出現する連鎖のタネが大きくなり、制限時間も増えます。
  - まともにプレイする場合は「AVALANCHE」ルールを使用してください。

- AVALANCHE VS-BATTLE (RC1)\
  - AVALANCHE 1Pモードと似たルールで対戦します。連鎖でブロックを大量に消して高得点を稼ぐほど、相手に邪魔ブロックを送り込むことができます。
  - まともにプレイする場合は「AVALANCHE」ルールを使用してください。

- AVALANCHE VS FEVER MARATHON (RC1)
  - 連鎖のタネ（あらかじめ簡単に連鎖できるように組まれたブロック）が積まれた状態でゲームが始まります。
  - 連鎖すると、HANDICAP欄の数字が減っていきます。これが0になると、相手に実際に攻撃できるようになります。
  - まともにプレイする場合は「AVALANCHE」ルールを使用してください。

- AVALANCHE VS DIG RACE (RC1)
  - 相手より先に、7色に光る宝石ブロックを消すことが目的のモードです。宝石ブロックは他のブロックの下に埋もれています。
  - 大きな連鎖をすると相手に邪魔ブロックを送ることが出来ますが、致命傷を与えるほどの攻撃力はありません。
  - まともにプレイする場合は「AVALANCHE」ルールを使用してください。

- PHYSICIAN (RC1)
  - 上から落ちてくる3色の通常ブロックを使い、あらかじめフィールド内に置かれている宝石ブロックをすべて消していくモードです。
  - ブロックは縦か横に4つ以上同じ色を並べると消えます。
  - まともにプレイする場合は「PHYSICIAN」ルールを使用してください。

- PHYSICIAN VS-BATTLE (RC1)
  - PHYSICIANモードのルールで対戦します。 相手がゲームオーバーになるか、先にウイルスを全て消すと勝利です。
  - まともにプレイする場合は「PHYSICIAN」ルールを使用してください。

- SPF VS-BATTLE (BETA)
  - 上から落ちてくるノーマルジェム（通常ブロック）を積み上げ、時々落ちてくるクラッシュジェム（宝石ブロック）を使ってノーマルジェムを消していきます。
  - 2×2以上の大きさで同色のノーマルジェムを四角形型に組み合わせると、より強力なパワージェムに変化します。
  - ノーマルジェムやパワージェムはいくらでも繋げていくことができますが、クラッシュジェムを使わない限り消すことができません。\
    クラッシュジェムを使ってジェムを消すと、相手にカウンタージェム（邪魔ブロック）を送り込むことができます。
  - まともにプレイする場合は「SPF」ルールを使用してください。

### BGMを鳴らすには

まだBGMは標準では付いていませんが、任意の音楽ファイルを再生できます。\
BGMの設定をするにはミュージックリストエディタ(musiclisteditor.bat)を使ってください。 対応形式はたぶん「.ogg」「.wav」「.xm」「.mod」「.aif」「.aiff」の6種類です。

巨大なoggファイルを入れるとループする時に落ちるようです。

<!--
## ネットプレイ(β版)

[できること]
- 他のプレイヤーと対戦(最大6人)
- 新しいルームを作る - すでにあるルームに入る - 簡単なチャット機能 - 観戦
[できないこと]
- その他ほとんど全部

### [ネットプレイのはじめ方]

ネットプレイモードに入る方法:

1. 普通にゲームを起動します。3つあるどのバージョンでもOKです。
2. トップメニューから「NETPLAY」を選択してAボタンを押します。
3. 「NullpoMino NetLobby」というウィンドウが現れます。

新しいサーバーをリストに追加する方法:

1. サーバー選択画面(NullpoMino NetLobbyというウィンドウが出現した直後の状態)で「追加」ボタンをクリックします。
2. ホスト名(またはIPアドレス)とポート番号を入れる画面が出てきます。 「ホスト名またはIPアドレス:ポート番号」の形式で入力してください。(ホスト名とポート番号をコンマ「:」で区切ります)
   サーバー側のポート番号が9200の場合は「:9200」をホスト名の後ろにつける必要はありません。
3. 入力したらOKボタンをクリックしてください。

ローカルでネットプレイを試すには、netserver.batをダブルクリックしてサーバーを起動し 「127.0.0.1」をサーバーリストに追加してください。

harddrop.comの皆さんがネットプレイサーバーを提供しています。harddrop.comの皆さんありがとう！ harddrop.com

追加したサーバーに接続する方法:

1. 名前とトリップをニックネーム欄に入力します（任意） 名前もトリップも入力しない場合は自動的に名前が「noname」になります
    - トリップは2chとかにあるような個人識別機能です。ニックネーム欄に#
   記号の後にパスワードを入れると暗号化された文字列が表示されます。 Wikipediaでのトリップの記事：
   http://ja.wikipedia.org/wiki/%E3%83%88%E3%83%AA%E3%83%83%E3%83%97_(%E9%9B%BB%E5%AD%90%E6%8E%B2%E7%A4%BA%E6%9D%BF)
2. 接続したいサーバーをリストボックスから選びます。(ダブルクリックすると即接続できます)
3. 「接続」ボタンをクリックします。画面がロビー画面に切り替わります。

新しい（対戦用の）ルームを作成する方法:

1. 画面上部にある「ルーム作成」ボタンをクリックします。
2. ルームの名前(省略可)と参加可能な最大人数を入力します。
3. OKボタンをクリックします。

1人プレイ用ルームを作成する方法:

1. 画面上部にある「1人プレイ」ボタンをクリックします。
2. プレイするモードとルールを選択します。
3. OKボタンをクリックします。

すでにあるルームに入る方法:
ルーム一覧表で入りたいルーム名をダブルクリックするだけです。 観戦だけしたい場合は、観戦したいルーム名を右クリックして、出てきた右クリックメニューから「観戦」を選びます。

OKシグナルを出す:

1. ゲームウィンドウ(普段1Pゲームを遊ぶウィンドウ)をクリックして、ゲームウィンドウに操作を移します。
2. Aボタンを押します。「OK」と自分のフィールドに出てきたら完了です。
3. その部屋にいる全員がOKシグナルを出すとゲームが始まります。

### [サーバーについて]

netserver.batをダブルクリックするとサーバーが起動しますが、この場合のポート番号はデフォルトの9200で固定です。\
他のポートに変えたい場合、およびLinuxまたはMac OS Xを使っている場合は以下のコマンドを使ってください。

Windows:
java -cp NullpoMino.jar;lib/log4j-1.2.15.jar mu.nu.nullpo.game.net.NetServer [ポート番号]
Linux/MacOS:
java -cp NullpoMino.jar:lib/log4j-1.2.15.jar mu.nu.nullpo.game.net.NetServer [ポート番号]

2番目の引数を設定することで、別のnetserver.cfgから設定を読み込むこともできます。 Windows:
java -cp NullpoMino.jar;lib/log4j-1.2.15.jar mu.nu.nullpo.game.net.NetServer [ポート番号] [netserver.cfgの場所]
Linux/MacOS:
java -cp NullpoMino.jar:lib/log4j-1.2.15.jar mu.nu.nullpo.game.net.NetServer [ポート番号] [netserver.cfgの場所]
-->

### FAQ

Q: Slick版でジョイスティックが動かない\
A: GENERAL CONFIG画面の"JOYSTICK METHOD"の設定をLWGJLに変えて、JOYSTICK SETTING画面の設定をいろいろ弄ってください。 Slick版のジョイスティックサポートはSDL版ほど良くないです。

Q: ネットプレイでレートや1人プレイの記録が保存されない\
A: 名前にトリップが入っていないと記録は保存されません。 トリップをつけるには、名前の後ろにシャープ記号（# ）とパスワードを入れてください。 （例えば、名前欄に"ABCDEF# nullpomino"と入れて接続すると"ABCDEF
◆gN6kJVofq6"になります)

## 製作- 謝辞

- 製作：
  - NullNoname ◆bzEQ7554bc (別名pbomqlu910963、元名無し) pbomqlu910963@gmail.com
  - Zircean
  - Poochy
  - Wojtek (aka dodd)
  - Spirale (olivier.vidal1 on the SVN)
  - kitaru2004
  - Shrapnel.City (aka Pineapple)
  - vic7070 (aka Digital)
  - alight
  - nightmareci
  - johnwchadwick (aka nmn)
  - prelude234 (aka awake)
  - sesalamander
  - teh_4matsy@lavabit.com (aka 4matsy)
  - delvalle.jacobo (aka clincher)
  - bob.inside (aka xlro)

  Google CodeのPeopleページ:
  http://code.google.com/p/nullpomino/people/list

このゲームは以下のツール- ライブラリ- 素材を使用しました。 この場を借りてお礼申し上げます。

- ツール
  - Eclipse 3.6 http://www.eclipse.org/
  - PictBear SE http://www20.pos.to/~sleipnir/

- ライブラリ
  - Slick - 2D Game Library based on LWJGLhttp://slick.cokeandcode.com/
  - Lightweight Java Game Library (LWJGL) http://www.lwjgl.org/
  - JOrbis -- Pure Java Ogg Vorbis Decoder http://www.jcraft.com/jorbis/
  - IBXM Java MOD/S3M/XM Player http://sites.google.com/site/mumart/
  - sdljava - Java Binding to SDL http://sdljava.sourceforge.net/
  - Simple DirectMedia Layer http://www.libsdl.org/
  - Apache log4j 1.2.15 http://logging.apache.org/log4j/1.2/index.html
  - Crypt.java (Java-based implementation of the unix crypt(3) command)http://www.cacas.org/java/gnu/tools/
  - ModePile https://github.com/0xFC963F18DC21/ModePile

- 効果音
  - ザ- マッチメイカァズ http://osabisi.sakura.ne.jp/m2/
  - TAM Music Factory http://www.tam-music.com/

- 背景 (res/graphics/oldbg)
  - ゆんフリー写真素材集 http://www.yunphoto.net/

- フォント
  - オリジナルフォント【みかちゃん】 http://www001.upp.so-net.ne.jp/mikachan/

- Also thanks to:
  - Lee
  - Burbruee
  - Steve
  - Blink
  - xlro (http://nullpo.nu.mu/)
  - vicar (http://vicar.bob.buttobi.net/)
  - SWR
  - hebo-MAI
  - gif
  - virulent
  - 0xFC963F18DC21
  - Lilia Oshisaure
  - Shots243
  - tetrisconcept.net http://www.tetrisconcept.net/
  - Hard Drop http://harddrop.com/
    - NullpoMino Topic: http://harddrop.com/forums/index.php?showtopic=2035
    - NullpoMino Guide: http://harddrop.com/forums/index.php?showtopic=2317
    - NullpoMino on HD wiki: http://harddrop.com/wiki/index.php?title=NullpoMino
  - Puyo Nexus http://www.puyonexus.net/

- Google Code Project Page: http://code.google.com/p/nullpomino/
- Github Project Page: https://github.com/nullpomino/nullpomino

# TODO

- まともな説明書を作る
- CONFIG画面の設定項目の説明を作る
- Swing版をなにかまともな別のものにする
- PRACTICEモードの設定項目を増やす
- AIを強くする
- パズルモード
- Fix CPU 100% bug of NetServer
- Reduce RAM usage
- More room customize features (Garbage pattern options, and more...)
- Detect disconnected players from server-side
- Replay support
- Manage the lag
- Password protected rooms
- ID/Password or something like that
- Various Bugfixes
- Helpful items in SCORE ATTACK mode
- Grand Order mode, Several courses of certain task within the given time limit
- More garbage pattern options for VS-BATTLE (and Netplay)
- More AI players
- Replace Swing version with something better (jME maybe?)
- Better replay selector system
- TAS detection (I didn't include it in this version because the current method had problems with music)

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
