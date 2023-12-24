# 遊び方

![Alt Text](pieces.png){ width="320" }

- 7種類のピースがランダムに上中央から落下してきます。
- このピースを１個ずつ操作して、横一列に隙間無く並べましょう。
  - ピースのブロックを横１列に敷き詰めることができた場合、その列のブロックは消えて、上にあったブロックが下に落ちてきます。
    - 積み上がった残りのブロックはその分だけ降下しますが、下の隙間を埋めることはありません。
  - 落下するブロックは操作によって移動・回転させたり、早く落下させる事ができます。
    - 落下中のブロックが地面や他のブロックの上に着くと、暫くした後にそのブロックが固定されて、上から次のブロックが落下してきます。
- ブロックが上まで積みあがり、次のブロックが落ちてこられなくなるとゲームオーバーです。

> **ゲームモードによっては…**
> - 得点(Score)が存在するモードでは、*基本的には*１回の落下で複数のラインを作ると高得点です。
> - ラインを作っていくにつれ、難易度(Level)≒ブロック落下速度が上がることもあります。
>   - 中にはラインを作らなくてもLevelが上がるもの、ラインをまとめて作るとLevelが上がりやすいものも…
> - モードによっては、規定のライン数やLevelに達するなどでゲームクリアになります。
>
> {style="note"}



## ボタンの説明

- UP：ハードドロップ（ブロックを一瞬で落下）- カーソルを上に移動
- DOWN：ソフトドロップ（ブロックを早く落下）- カーソルを下に移動
- LEFT：ブロックを左に移動- カーソルで選択している項目の値を1つ減らす
- RIGHT：ブロックを右に移動- カーソルで選択している項目の値を1つ増やす
- A：ブロックの回転- メニュー項目の決定
- B：ブロックの逆回転- キャンセル
- C：ブロックの回転
- D：ホールド（ブロックを一時的に保管して、後で使えます）
- E：ブロックの180度回転
- F：エンディング早送り（SPEED MANIAとGARBAGE MANIAモードで使用可能）- ネットプレイで練習モードを開始／終了
- QUIT：ゲームを終了する
- PAUSE：ゲームを一時停止
- GIVE UP：タイトルに戻る
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

## ゲームモード
ゲームモードによって、目的や難易度が異なります。
また一部モードはルールに影響されない特殊な設定になる場合もあります。

[ゲームモード](GameModes.md)

## ゲームルール

選んだゲームルールに応じて操作性やブロックの見た目が変わります。
ゲームモード選択後、またはCONFIGの中にある「[RULE SELECT]」から使用するルールを変更できます。

[ゲームルール](GameRules.md)

付属のルールエディタを使うと独自のルールを作成できます。

[ルール設定項目](RuleEditor.md)

## BGMを鳴らすには

BGMは標準では付いていませんが、任意の音楽ファイルを再生できます。\
BGMの設定をするにはミュージックリストエディタ(musiclisteditor.bat)を使ってください。 対応形式はたぶん「.ogg」「.wav」「.xm」「.mod」「.aif」「.aiff」の6種類です。

巨大なoggファイルを入れるとループする時に落ちるようです。

## ネットプレイ(β版)

[できること]
- 他のプレイヤーと対戦(最大6人)
- 新しいルームを作る - すでにあるルームに入る - 簡単なチャット機能 - 観戦

[//]: # ([できないこと])

[//]: # (- その他ほとんど全部)

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

## FAQ

Q: Slick版でジョイスティックが動かない\
A: GENERAL CONFIG画面の"JOYSTICK METHOD"の設定をLWGJLに変えて、JOYSTICK SETTING画面の設定をいろいろ弄ってください。
Slick版のジョイスティックサポートはSDL版ほど良くないです。

Q: ネットプレイでレートや1人プレイの記録が保存されない\
A: 名前にトリップが入っていないと記録は保存されません。 トリップをつけるには、名前の後ろにシャープ記号（# ）とパスワードを入れてください。 （例えば、名前欄に"ABCDEF# nullpomino"
と入れて接続すると"ABCDEF
◆gN6kJVofq6"になります)
