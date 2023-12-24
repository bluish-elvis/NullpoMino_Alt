VERSION 5.00
Begin VB.Form Form1
   Appearance      =   0  'ﾌﾗｯﾄ
   BackColor       =   &H00000000&
   BorderStyle     =   1  '固定(実線)
   Caption         =   "DTET"
   ClientHeight    =   7200
   ClientLeft      =   45
   ClientTop       =   330
   ClientWidth     =   9600
   ControlBox      =   0   'False
   BeginProperty Font
      Name            =   "MS UI Gothic"
      Size            =   12
      Charset         =   128
      Weight          =   700
      Underline       =   0   'False
      Italic          =   0   'False
      Strikethrough   =   0   'False
   EndProperty
   ForeColor       =   &H00000000&
   Icon            =   "Form1.frx":0000
   KeyPreview      =   -1  'True
   LinkTopic       =   "Form1"
   MaxButton       =   0   'False
   MinButton       =   0   'False
   ScaleHeight     =   480
   ScaleMode       =   3  'ﾋﾟｸｾﾙ
   ScaleWidth      =   640
   StartUpPosition =   2  '画面の中央
   Visible         =   0   'False
   Begin VB.PictureBox Picture1
      Appearance      =   0  'ﾌﾗｯﾄ
      BackColor       =   &H00000000&
      BorderStyle     =   0  'なし
      ForeColor       =   &H00000000&
      Height          =   7200
      IMEMode         =   3  'ｵﾌ固定
      Left            =   0
      ScaleHeight     =   480
      ScaleMode       =   3  'ﾋﾟｸｾﾙ
      ScaleWidth      =   640
      TabIndex        =   0
      Top             =   0
      Width           =   9600
   End
End
Attribute VB_Name = "Form1"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False

'----------------------------------------------
'  DTET(DirectTETRIS)        2000-2003  Mihys
'----------------------------------------------

'Ver 1.00
'RUSHシステムを追加
'マニュアル「BLOCK GRAPHICS」を「BLOCK GRAPHIC」に訂正。
'トライアルにモードを表示
'ADVANCEスコア0時間切れでランキング入力を不可能に修正。

'Ver 0.57
'ウィンドウモードのVRAMをシステムメモリに変更。
'ローマ字「XN（ん）」を追加

'Ver 0.56
'トレーニング以外のミスでタイムが0になるバグを修正。

'Ver 0.55
' 「ONLINE BATTLE」を「NETWORK BATTLE」に変更。
' トレーニングのスコア表示を変更。
' ネットメッセージに辞書ファイル機能を追加。
' "rec.dtd"なしで起動可能に変更。

'Ver 0.53
' メニューのカーソル「0 or Max」時にオートリピート無効化。
' パッドの十字キーの初期設定を4方向に変更。

'Ver 0.52
' 日本以外の地域設定での文字コンバートを訂正。

'Ver 0.51
' KEYBOARDの使用可能キーにテンキーの「=」を追加。
' TRAININGのLV50以上のレベル上下のオートリピートを高速化。
' VIDEOセーブファイル名の番号を「_0x」に変更。
' VIDEOファイル名のソートの優先順位を「名前>拡張子」に変更。
' VIDEOの早送りの最高速を50倍速に変更。
' ADVANCE,JOKER途中の時間切れのライフと雪を訂正。
' ネットログの対戦中断記録の条件を開始カウントダウン以降に変更。
' コールされた側が必ずBeepを鳴らすように変更。
' 最初の1フレーム間にコールされたときのデータ受信を訂正。

'Ver 0.50
' 初リリース。

Option Explicit
Const WW& = 640, HH& = 480, BPP& = 8
Const Rg! = 3.14159265358979 / 180
Const VSizeMax& = 204800
Const Ne$ = "ABCDEFGHIJKLMNOPQRSTUVWXYZ!?&@0123456789-/+:.\"
Const FNE$ = "abcdefghijklmnopqrstuvwxyz0123456789\"
Const VTx$ = "-TDECRMGUVLXFPZAQKOWYHNIBS2417095386vzkfJshadrtybnluwegcmioqxjp+" '"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz+-"
Dim DX As DirectX7
Dim DI As DirectInput
Dim DIDM As DirectInputDevice
Dim DIDK As DirectInputDevice
Dim KS As DIKEYBOARDSTATE
Dim DIEN As DirectInputEnumDevices
Dim DIDJ(3) As DirectInputDevice
Dim JS(3) As DIJOYSTATE
Dim JS0 As DIJOYSTATE
Dim DS As DirectSound
Dim SB(15) As DirectSoundBuffer
Dim SSB(15) As DirectSoundBuffer
Dim SBD As DSBUFFERDESC
Dim WF As WAVEFORMATEX
Dim DD As DirectDraw7
Dim PrSf As DirectDrawSurface7
Dim BBSf As DirectDrawSurface7
Dim FoSf As DirectDrawSurface7
Dim BCSf As DirectDrawSurface7
Dim BDSf As DirectDrawSurface7
Dim SpSf As DirectDrawSurface7
Dim BGSf As DirectDrawSurface7
Dim BSSf(14) As DirectDrawSurface7
Dim BFSf(3) As DirectDrawSurface7
Dim StrSf(1) As DirectDrawSurface7
Dim SD As DDSURFACEDESC2
Dim CA As DDSCAPS2
Dim PrC As DirectDrawClipper
Dim MPal As DirectDrawPalette
Dim MPa(255) As PALETTEENTRY
Dim Pal As DirectDrawPalette
Dim Pa(255) As PALETTEENTRY
Dim BGPal As DirectDrawPalette
Dim BGPa(255) As PALETTEENTRY
Dim BSPal(14) As DirectDrawPalette
Dim CK As DDCOLORKEY
Dim DMP As DirectMusicPerformance
Dim DMPC As DMUS_PORTCAPS
Dim DML As DirectMusicLoader
Dim DMS As DirectMusicSegment
Dim DMSS As DirectMusicSegmentState
Dim DP As DirectPlay4
Dim DPEC As DirectPlayEnumConnections
Dim DPES As DirectPlayEnumSessions
Dim DPSD As DirectPlaySessionData
Dim DPA As DirectPlayAddress
Dim DPEP As DirectPlayEnumPlayers
Dim DPM As DirectPlayMessage

Private Type XY
X As Single
Y As Single
XX As Single
YY As Single
End Type

Private Type XYLong
X As Long
Y As Long
XX As Long
YY As Long
End Type

Dim FormCapOrg As String 'フォームキャプション
Dim WM As Boolean, WMM As Boolean 'ウィンドウモード
Dim FullScrErr As Boolean 'フルスクリーンエラー
Dim WMC As Boolean 'ウィンドウモードチェンジ
Dim WndL As Single, WndT As Single, WndWW As Single, WndHH As Single 'ウィンドウ座標
Dim WS As Long, WSS As Long '最小化
Dim ClkC As Boolean, Clk As String, ClkH As Long, ClkM As Long, ClkS As Long '現在時刻
Dim CmdFile As String '起動時読み込みファイル
Dim EscC As Long 'ESCカウンタ
Dim TV As Long 'TVモード
Dim Tic As Long 'TickCount
Dim AFS As Boolean '処理落ちフレームスキップモード
Dim RomaMx As Long, Roma(61, 5) As String, RomaM(5) As String 'ローマ字入力
Dim TokuMx As Long, Toku(499, 1) As String '特殊文字
Dim EdRule As String '末尾改行禁止処理
Dim Net As Boolean, Nett As Boolean 'ネットワークモード
Dim StrMsgLog() As String, StrMsgLogC As Long 'テキストメッセージログ
Dim TrMAdd As Boolean 'モード追加書き込み
Dim VSNetPlay As Boolean 'ネット対戦中
Dim Jpn As Boolean '日本語入力モード
Dim KeyLock As Boolean '文字入力ロック
Dim KeyBuf As String 'テキストメッセージキーバッファ
Dim StrEdit As Boolean, KeyChar As String 'テキストメッセージ
Dim SMTp As Boolean, SMMv As Boolean, SMdl As Boolean 'テキストメッセージ
Dim SMX As Long, SMY As Long 'テキストメッセージ
Dim StrMsg As String, StrMsgg As String, StrMsgUD As String, StrMsgBT As String, StrMsgB As String, StrMsgBB As String 'テキストメッセージ
Dim SMBPos As Long, SMBPoss As Long, SMBPosL As Long
Dim StrMsgOrg(255) As String, StrMsgOrgUD(255) As String, StrMsgToku(255) As Boolean, StrMsgTokuUD(255) As Boolean, StrMsgSel(2) As String 'テキストメッセージ
Dim SMSel As Long, SMSell As Long, SMPos As Long, SMPoss As Long, SMPosUD As Long, SMPosL As Long, StrMsgJ As String 'テキストメッセージ
Dim StrRsv As Long 'テキストメッセージ受信待ち
Dim NetCont As Boolean 'ネットコンティニュー
Dim ParReq As Boolean 'コンティニュー親リクエスト
Dim NetQ As Long, NetQQ As Long, NetQC As Long 'ネットクオリティ
Dim NetFldLR As Long 'ネット対戦フィールドポジション
Dim NetPBX As Long, NetPBY As Long, NetPBM As Long 'ネット用ブロック座標
Dim NetBX As Long, NetBY As Long, NetBM As Long 'ネット相手ブロック座標
Dim ParStC As Long '親参加カウンタ
Dim NetMsg As Long 'ネットメッセージ
Dim RsvNetMsg As Long '予約ネットメッセージ
Dim NetPar As Boolean '親／参加
Dim Par As Boolean '親／参加（対戦時）
Dim ParTrM As Long, ParBtNx(110) As Long, ParDBS(1) As Long '親スタートデータ
Dim PlID As Long, JID As Long, PLID1(1) As Long 'ネット参加者ID
Dim JN As String * 6, JN1(1) As String * 6 'ネット参加者名
Dim HN(80) As String * 6 'ハンドル名リスト
Dim HNN As String * 6 'ハンドル名
Dim HNMax As Long 'ハンドル名最大数
Dim HNNum As Long, HNNumm As Long 'ハンドル名
Dim MFNum As Long '文字ファイル名
Dim Midi As Long 'MIDIモード
Dim MidiMode As Long 'MIDIモード
Dim MidiPortC As Long 'MIDIポート数
Dim MidiOn As Boolean 'MIDI再生フラグ
Dim PortIndex() As Long 'MIDIポートIndex
Dim PortName() As String 'MIDIポート名
Dim Trans As Integer 'MIDI音階
Dim Src As RECT '転送元RECT
Dim Des As RECT '転送先RECT
Dim RC0 As RECT '全画面Blt用RECT(0固定)
Dim ClLst(9) As Long 'カラーリスト
Dim GMode As Long, GGMode As Long 'ゲームモード
Dim GEnd As Boolean 'ゲームサブ終了
Private Type OpBXYSrc
X As Long
Y As Long
End Type
Dim OpB(191) As OpBXYSrc, OpBRR As OpBXYSrc 'オープニングブロック
Dim OpBPos(47) As Single 'オープニングブロックポジション
Dim Tit As Boolean, TC As Long 'タイトル
Dim TTL As Long 'タイトル時間制限
Dim TitBGC As Long 'タイトル背景カウンタ
Dim TitBGF As Long, TitBGFC As Long 'タイトル背景フェード
Dim TitY As Single 'タイトル座標
Dim R As Single, R1 As Single, R2 As Single '多目的変数
Dim RR As Long, RR1 As Long, RR2 As Long '多目的変数
Dim RRR As String, RRR1 As String, RRR2 As String '多目的変数
Dim FL(3) As Long 'フィールド枠線
Dim I As Long, J As Long, U As Long, Y As Long, T As Long 'カウンタ
Dim FS As Boolean '処理落ちフレームスキップ
Dim AppDir As String 'アプリパス
Dim EffDir As String 'データファイルパス
Dim VidDir As String 'ビデオファイルパス
Dim SysC As Long 'システム時間（現実時間）
Dim GmC As Long 'ゲーム時間
Dim SpdR As Long '処理スピード
Dim SpdE As Long, SpdE1 As Long, SpdE2 As Long '処理スピード用タイムカウンタ
Dim E As Long, E1 As Long, E2 As Long 'タイムカウンタ
Dim TE As Long, TEE As Long, TE1 As Long, TE2 As Long 'タイムカウンタ
Dim Trn As Boolean 'トレーニング
Dim TA As Boolean 'タイムアタック
Dim Fnl As Boolean 'ファイナル
Dim OfBt As Boolean 'オフィシャルマッチ
Dim PlFin As Long, PlFC As Long, PlFCh As Boolean, PlF As Long 'プレイ終了（ブロック消去）
Dim FE As Boolean 'フィールドエディット
Dim FEX As Long, FEY As Long 'フィールドエディット座標
Dim FECh As Boolean, FEChh As Long 'フィールドエディット重なりチェック
Dim CsrL As Boolean, CsrR As Boolean, CsrU As Boolean, CsrD As Boolean, CsrA As Boolean, CsrB As Boolean, CsrS As Boolean '一般カーソルキー
Dim CsrLL As Long, CsrRR As Long, CsrUU As Long, CsrDD As Long, CsrAA As Long, CsrBB As Long, CsrSS As Long '一般カーソルキー
Dim CsrL1(4) As Boolean, CsrR1(4) As Boolean, CsrU1(4) As Boolean, CsrD1(4) As Boolean, CsrA1(4) As Boolean, CsrB1(4) As Boolean, CsrS1(4) As Boolean '個別カーソルキー
Dim CsrLL1(4) As Long, CsrRR1(4) As Long, CsrUU1(4) As Long, CsrDD1(4) As Long, CsrAA1(4) As Long, CsrBB1(4) As Long, CsrSS1(4) As Long '個別カーソルキー
Dim CsrAAP As Long, CsrBBP As Long, CsrSSP As Long '個別カーソルキー認識
Dim CPos As Long, CCPos As Long 'カーソル座標
Dim VSA As Long '対戦動作モード
Dim VSR As Long '対戦順位
Dim VSLiv As Boolean '相打ち判定
Dim CX As Single, CY As Single, CXX As Single, CYY As Single 'カーソル表示座標
Dim JC As Long 'パッド接続数
Dim MnJ As Long 'CONFIGパッド番号
Dim MnPB As Long, MnPBB As Long 'CONFIGパッドボタン
Dim PArw(3) As Byte 'コントローラカーソル斜めキー（0:無効 1:横優先 2:縦優先 3:斜め）
Dim PBS(3) As Byte 'コントローラボタンポーズ
Dim PBL(3) As Byte 'コントローラボタンL
Dim PBR(3) As Byte 'コントローラボタンR
Dim KArwL As Byte, KArwR As Byte, KArwU As Byte, KArwD As Byte 'キーボードカーソルキー
Dim KBS As Byte, KBL As Byte, KBR As Byte 'キーボードボタン
Dim NC As Long, NCC As Long 'ネット接続人数
Dim SsC As Long 'セッション人数
Dim CC As Long, CCC As Long '文字カラーカウンタ
Dim VSLv(2, 3) As Long '対戦レベルデータ
Dim Bn(3) As Long 'スコア加算定数
Dim BS(6) As String * 32 'ブロック座標データ（仮）
Dim B(6, 3, 3, 1) As Long 'ブロック座標データ
Dim BCNm(1, 7) As Long 'ブロック番号色
Dim BR(7) As Long 'ブロックランダム
Dim NxDC As Long 'NEXTアニメカウンタ
Dim PlC As Long 'プレーヤーカウンタ
Dim PL As Long 'プレーヤー
Dim TrM As Long, TrMN(4) As String 'トライアルモード
Dim BD As Long 'ボードカラー
Dim BC As Long 'ブロックカラー
Dim BG As Long '背景
Dim BGMem As Boolean, BGMemm As Boolean '背景システムメモリ
Dim BAni As Boolean 'ブロックアニメ
Dim BSm As Boolean 'ブロックスムーズ移動
Dim Zanzo As Boolean 'ブロック残像
Dim MNx As Long 'マルチNEXT
Dim OBPT(1) As Long 'オフィシャルマッチプレーヤーデバイス
Dim GFin As Long '切り替えモード
Dim VSFinC As Long '対戦終了カウンタ
Private Type ScRnk
Nm As String * 6
SC As Long
Li As Long
End Type
Dim SRnk(29) As ScRnk 'SCORE ATTACKランキング
Private Type TmRnk
Nm As String * 6
Tm As Long
Li As Long
End Type
Dim TRnk(29) As TmRnk 'TIME ATTACKランキング
Private Type FnRnk
Nm As String * 6
Lv As Long
Tm As Long
End Type
Dim FRnk(9) As FnRnk 'FINALランキング
Private Type LsRnk
Nm As String * 6
Liv As Long
Tm As Long
End Type
Dim LRnk(9) As LsRnk 'FURTHESTランキング
Dim TrRnk As Long, TdRnk As Long 'トライアル順位
Dim TrNm As String * 6, TrNmm As String * 6, NmPos As Long, Nm As Long 'トライアル名前
Dim FNm As String * 8 'ファイル名前
Dim St As Long 'レベル(/5)
Dim Stt As Long 'レベル(/5)比較
Dim LvL As Long '各プレーヤーレベル比較
Dim FOC As Long 'フェードアウトカウンタ
Dim FIC As Long 'フェードインカウンタ
Dim FAC As Long 'フェードアニメカウンタ
Dim FF As Boolean 'フェード完了フラグ
Dim LS(49) As Long 'レベルスピード
Dim Pau As Long 'ポーズ
Dim TimInc As Boolean '時間増加
Dim PWC As Long, VEWC As Long 'ポーズカウンタ
Dim DAni As Long 'ダメージアニメカウンタ
Dim LAni As Long 'ライフアニメカウンタ
Dim BtNx(776) As Long '対戦用NEXT
Dim DBS(9) As Long '攻撃ブロックスペース
Dim Hitt As Long '攻撃量計算
Private Type CPEmo
DP As Long '下置き係数
LR As Long '壁寄り係数
QD As Boolean 'クイックドロップ
Spd As Long '連打スピード
SRt As Boolean '回転タイミング
Wt As Long '時間待ち
End Type
Dim CPE(25) As CPEmo 'CP性格
Dim AI(139) As String * 6 'CP思考ルーチンデータ（仮）
Dim CPA(6, 3, 5, 4) As Long 'CP思考ルーチンデータ
Dim NoBGM As Boolean 'BGMミュート
Dim Mut As Boolean 'ミュート
Private Type SPlay
On As Boolean 'サウンドスイッチ
Fre As Long '再生速度
Pan As Long 'パン
AB As Boolean '再生可能
DbC As Boolean 'ダブル
End Type
Dim Sou(15) As SPlay 'サウンド
Dim SVol As Long 'サウンドボリュームレベル(0-10)
Dim SPan As Long 'サウンドパンレベル(0-10)
Dim VPC As Long 'ビデオメニューページ
Dim VFC As Long 'ビデオファイルカウンタ
Dim VFN As String, VFNL() As String 'ビデオファイル名
Dim VFS As Long 'ビデオファイル名ソート
Dim VFD() As Long 'ビデオスコア
Dim VM As Long 'ビデオモード
Dim VD As Long, VDC As Long, VDD As Long, VDS As String 'ビデオデータ
Dim VCDS As String 'ビデオチェックデータ
Dim VP As Long, VPP As Long, SVP As Long 'ビデオ位置
Dim VSize As Long 'ビデオサイズ
Dim VDSc As Long 'ビデオデータスコア（タイム）
Dim VDLi As Long 'ビデオデータライン（レベル）
Dim VDTrM As Long 'ビデオデータモード
Dim VDTA As Boolean 'ビデオデータタイムアタック
Dim VDFnl As Boolean 'ビデオデータFINAL
Dim VDRush As Boolean 'ビデオデータモード
Dim VS As Long 'ビデオスピード
Dim VSC As Long 'ビデオスピードカウンタ
Dim VKX1 As Single, VKY1 As Single 'ビデオキー座標
Dim VKX2 As Single, VKY2 As Single 'ビデオキー座標
Dim VKX3 As Single, VKY3 As Single 'ビデオキー座標
Dim VKA As Long, VKB As Long 'ビデオキーボタン
Dim FFi As Integer 'フリーファイル
Dim Fade As Boolean, ScrFC As Long '画面フェード
Dim TFC 'タイトルフェードカウンタ
Dim Mn As Long, Mnn As Long 'メニューID
Dim MnCapLen As Long 'メニュー文字長さ
Dim TCPOS As Long 'タイトルカーソル座標
Dim TCX As Single, TCY As Single, TCXX As Single, TCYY As Single 'タイトルカーソル表示座標
Dim TtA As Single 'タイトルメニュー角度
Dim TtPos(4) As XY 'タイトルメニュー表示座標
Dim PadC As Long 'メニューパッド
Private Type TXY
V As Boolean
X As Single
Y As Single
XX As Single
YY As Single
End Type
Dim TxPos(108) As TXY 'テキスト表示座標
Dim Tx(108) As String '文字
Private Type MenuDB
Cap As String 'キャプション
StN As Long '開始番号
MNs As Long 'メニュー数
Obs As Long 'オブジェクト数
X As Long 'X座標
Y As Long 'Y座標
End Type
Dim MnDB(22) As MenuDB 'メニュー文字データベース
Dim TxMx As Long 'テキスト番号最大値
Dim MnMx As Long 'メニュー番号最大値

Dim ScLiv(2) As Long 'SCORE ATTACKライフ
Dim ScLv(2) As Long 'SCORE ATTACK最高レベル
Dim TmLiv(2) As Long 'TIME ATTACKライフ
Dim FnLv As Long 'FINAL最高レベル
Dim LsLiv As Long 'FURTHEST最高レベル

Dim DTCl As Long, DTLiv As Long 'DTET攻略率
Dim CPMax As Long 'CP最大グレード
Dim OBMax As Long 'オフィシャルマッチ最大レベル
Dim Chk1 As Long, Chk2 As Long 'セーブデータチェックサム
Dim RecCh As Boolean 'レコードデータチェック
Dim RecNew As Boolean 'レコードデータ新規作成

Private Type EnvDat
Midi As Long
SVol As Byte
SPan As Byte
BG As Byte
BAni As Boolean
BSm As Boolean
Zanzo As Boolean
BC As Byte
BD As Byte
MNx As Byte
ClkC As Boolean
TV As Byte
AFS As Boolean
WM As Boolean
BGMem As Boolean
KArwL As Byte
KArwR As Byte
KArwU As Byte
KArwD As Byte
KBS As Byte
KBL As Byte
KBR As Byte
Net As Boolean
HNN As String * 6
NetQ As Byte
Jpn As Boolean
NetFldLR As Byte
End Type
Dim EvDt As EnvDat 'セーブデータ（環境）

Private Type RecDat
SRnk(14) As ScRnk
TRnk(14) As TmRnk
FRnk(4) As FnRnk
LRnk(4) As LsRnk
ScLiv(2) As Byte
ScLv(2) As Integer
TmLiv(2) As Byte
FnLv As Integer
LsLiv As Byte
Chk As Long
End Type
Dim RcDt As RecDat 'セーブデータ（レコード）

Dim PT(3) As Long 'プレーヤーデバイス
Dim Ch1(3) As Long, Chh1(3) As Long 'チェック
Dim ChN1(3) As Boolean 'チェック
Dim Rush1(3) As Boolean, RushC1(3) As Long 'RUSHシステム
Dim RushAniC As Long 'ラッシュアニメカウンタ
Dim SB1(3) As Long 'スコア加算
Dim CB1(3) As Long 'コンボボーナスカウンタ
Dim CBLv1(3) As Long 'コンボ時レベル
Dim RCPG1(3) As Long 'CPグレード（記憶）
Dim CPG1(3) As Long 'CPグレード
Dim SCPG1(3) As Long 'CPグレード（一時）
Dim CWC1(3) As Long 'CP経過時間
Dim CCWC1(3) As Long 'CP動作完了経過時間
Dim CMWC1(3) As Long 'CP強制決定時間
Dim Sc1(3) As Long 'スコア
Dim Li1(3) As Long 'ライン
Dim Lv1(3) As Long 'レベル
Dim SLv1(3) As Long '初期レベル
Dim NL1(3) As Long '次レベルライン
Dim NF1(3) As Long '数字フラッシュ時間
Dim CS1(3) As Long, MCS1(3) As Long, CSM1(3) As Long, CSX1(3) As Long, CS2M1(3) As Long, CS2X1(3) As Long '思考ルーチンスコア
Dim BXX1(3) As Long, BYY1(3) As Long '思考中座標
Dim InpL1(3) As Boolean, InpR1(3) As Boolean, InpU1(3) As Boolean, InpD1(3) As Boolean 'カーソルキー
Dim InpLL1(3) As Long, InpRR1(3) As Long, InpUU1(3) As Long, InpDD1(3) As Long 'カーソルキー
Dim InpLLL1(3) As Long, InpRRR1(3) As Long 'カーソルキー
Dim InpA1(3) As Boolean, InpB1(3) As Boolean, InpS1(3) As Boolean 'ボタン
Dim InpAA1(3) As Long, InpBB1(3) As Long, InpSS1(3) As Long 'ボタン
Dim BF1(3, 15, 25) As Long 'フィールド
Dim FE1(3, 3) As Long 'フィールド消去Y座標
Dim Nx1(3, 25) As Long 'NEXT
Dim NxC1(3) As Long '対戦用NEXTカウンタ
Dim DBSC1(3) As Long '攻撃ブロック隙間カウンタ
Dim Hit1(3) As Long '攻撃量
Dim Dmg1(3) As Long 'ダメージ
Dim DmC1(3) As Long 'ダメージカウンタ
Dim NetDmg1(3) As Long 'ネット対戦ダメージ消去
Dim BA1(3) As Long 'ブロック動作モード
Dim BAA1(3) As Long 'ブロック動作モード（一時）
Dim BT1(3) As Long, BX1(3) As Long, BY1(3) As Long, BM1(3) As Long 'ブロック情報
Dim BMX1(3) As Long, BMM1(3) As Long 'ブロック情報
Dim BYF1(3) As Long 'ブロック残像始点Y座標
Dim RBX1(3, 19) As Long, RBY1(3, 19) As Long, RBM1(3, 19) As Long, RAC1(3, 19) As Long, RBSC1(3, 19) As Long, RInc1(3, 19) As Long, RBYF1(3, 19) As Long 'RUSHブロック残像情報
Dim LBX1(3, 19) As Long, LBY1(3, 19) As Long, LBM1(3, 19) As Long, LAC1(3, 19) As Long, LBSC1(3, 19) As Long, LInc1(3, 19) As Long, LBYF1(3, 19) As Long 'RUSHブロック残像情報2
Dim LBT1(3) As Long 'ブロック向き2
Dim LBA1(3) As Long 'ブロックモード2
Dim LRushC1(3) As Long 'Rush数2
Dim CLB1(3, 19) As Boolean 'ブロック位置チェック
Dim BSC1(3) As Long, Inc1(3) As Long 'ブロックスピード
Dim ASC1(3) As Long '硬直スピード
Dim AC1(3) As Long '接触時間カウンタ
Dim BWC1(3) As Long 'ウェイトカウンタ
Dim DB1(3) As Long 'ドロップボーナス
Dim WCa1(3) As Boolean '硬直キャンセル
Dim DCa1(3) As Boolean '下キャンセル
Dim Liv1(3) As Long 'ライフ
Dim SLiv1(3) As Long '初期ライフ(ネット対戦)
Dim FT(3) As Long '残り時間
Dim CT(3) As Long '経過時間
Dim RT(3) As Long 'ラップ
Dim St1(3) As Long 'ストック
Dim CPos1(3) As Long 'カーソル座標
Dim VSR1(3) As Long '対戦順位
Dim VSLivC1(3) As Boolean 'ネット相打ち判定

Private Type EAni
V As Long
A As Long
AF As Long
X As Single
Y As Single
XX As Single
YY As Single
XXX As Single
YYY As Single
End Type
Dim EA(159) As EAni
Dim EAC As Long
'（海）
Dim Sea(29) As Single
Dim SeaA As Long
'（アルファベット）
Private Type AB
X As Single
Y As Single
R As Single
R2 As Single
End Type
Dim ABG As AB
'（スターダスト）
Dim StD(59) As Single
'（キラキラ振り子）
Dim FC As Long
Dim FX As Single
Dim FY As Single
Dim KrI As Long
Private Type Kira
V As Boolean
A As Long
X As Single
Y As Single
XX As Single
YY As Single
End Type
Dim Kr(35) As Kira
Dim FSX As Long
'（オーロラ）
Dim Crs As Single
Dim CrsF As Single
Dim CrsFF As Single
'（紫空）
Dim PSkD(7) As Long
Dim PSkS1(7) As Long
Dim PSkS2(7) As Long
Dim PSkP As Single
'（カーペット）
Dim CptG As XY
'（ダイヤ）
Dim Dia As XY
Dim DiaR As Single
'（レーザー）
Private Type Laser
C As Long
X As Single
Y As Single
R As Long
RR As Long
End Type
Dim Lsr(59) As Laser
Dim LsrSY As Long
'（水晶）
Private Type Crystal
A As Long
AA As Long
X As Long
Y As Single
YY As Single
End Type
Dim Crys(15) As Crystal
Dim CrysSY As Long
'レベル50-199（超空間）
Dim Spa As Single
'レベル200（電流）
Dim EdG As XYLong
'レベルMAX
Dim LdG As XYLong

Private Sub Form_KeyDown(KeyCode As Integer, Shift As Integer)
On Error GoTo CE
If KeyCode = vbKeyF4 Then
If Shift And vbAltMask Then
Unload Me
Else
If WS <> 1 Then WM = Not WM
End If
End If
If KeyCode = vbKeyF5 Then If Form1.WindowState <> 1 Then Form1.WindowState = 1 Else Form1.WindowState = 2 + WM * 2
If Len(KeyBuf) < 80 Then
If KeyCode = vbKeyReturn Then KeyBuf = KeyBuf + Chr$(13)
If KeyCode = vbKeyRight Then KeyBuf = KeyBuf + Chr$(28)
If KeyCode = vbKeyLeft Then KeyBuf = KeyBuf + Chr$(29)
If KeyCode = vbKeyUp Then KeyBuf = KeyBuf + Chr$(30)
If KeyCode = vbKeyDown Then KeyBuf = KeyBuf + Chr$(31)
If KeyCode = vbKeyDelete Then KeyBuf = KeyBuf + Chr$(127)
End If
Exit Sub

CE:
'Beep
End Sub

Private Sub Form_KeyPress(KeyAscii As Integer)
On Error GoTo CE
If Len(KeyBuf) < 80 And (KeyAscii = 8 Or KeyAscii = 27 Or KeyAscii >= 32 And KeyAscii <= 126 Or KeyAscii >= 161 And KeyAscii <= 223) Then KeyBuf = KeyBuf + Chr$(KeyAscii)
Exit Sub

CE:
'Beep
End Sub

Private Sub Form_Load() 'DTET
If App.PrevInstance Then End
StrMsgLogC = -1

On Error GoTo CE
Randomize
FormCapOrg = Form1.Caption
AppDir = App.Path: If Right$(AppDir, 1) <> "\" Then AppDir = AppDir + "\"
EffDir = AppDir + "effect\"
VidDir = AppDir + "video\"
RecNew = False
CmdFile = Command$
If CmdFile <> vbNullString$ Then
If Left$(UCase$(CmdFile), 2 - (Len(CmdFile) > 2)) = "/N" + Space$(-(Len(CmdFile) > 2)) Then
RecNew = True
CmdFile = Mid$(CmdFile, 3)
While Left$(CmdFile, 1) = " ": CmdFile = Mid$(CmdFile, 2): Wend
End If
If InStr(CmdFile, Chr$(34)) = 1 And InStr(2, CmdFile, Chr$(34)) > 0 Then
CmdFile = Mid$(CmdFile, 2, InStr(2, CmdFile, Chr$(34)) - 2)
Else
If InStr(CmdFile, Chr$(9)) > 0 Then CmdFile = Left$(CmdFile, InStr(CmdFile, Chr$(9)) - 1)
If InStr(CmdFile, " ") > 0 Then CmdFile = Left$(CmdFile, InStr(CmdFile, " ") - 1)
End If
End If
If CmdFile <> vbNullString$ And XDir(CmdFile, vbHidden) <> vbNullString Then
If Not VCheck(CmdFile) Then CmdFile = vbNullString$
Else
CmdFile = vbNullString$
End If

For I = 0 To 3: PArw(I) = 0: PBS(I) = 2: PBL(I) = 0: PBR(I) = 1: Next I

EnvLoad
RecLoad
TokuLoad

WMM = WM: BGMemm = BGMem

RomaM(1) = "A": RomaM(2) = "I": RomaM(3) = "U": RomaM(4) = "E": RomaM(5) = "O"
I = 0
Roma(I, 0) = "LTS": Roma(I, 1) = "": Roma(I, 2) = "": Roma(I, 3) = "っ": Roma(I, 4) = "": Roma(I, 5) = "": I = I + 1
Roma(I, 0) = "XTS": Roma(I, 1) = "": Roma(I, 2) = "": Roma(I, 3) = "っ": Roma(I, 4) = "": Roma(I, 5) = "": I = I + 1
Roma(I, 0) = "BY": Roma(I, 1) = "びゃ": Roma(I, 2) = "びぃ": Roma(I, 3) = "びゅ": Roma(I, 4) = "びぇ": Roma(I, 5) = "びょ": I = I + 1
Roma(I, 0) = "CH": Roma(I, 1) = "ちゃ": Roma(I, 2) = "ち": Roma(I, 3) = "ちゅ": Roma(I, 4) = "ちぇ": Roma(I, 5) = "ちょ": I = I + 1
Roma(I, 0) = "CY": Roma(I, 1) = "ちゃ": Roma(I, 2) = "ちぃ": Roma(I, 3) = "ちゅ": Roma(I, 4) = "ちぇ": Roma(I, 5) = "ちょ": I = I + 1
Roma(I, 0) = "DH": Roma(I, 1) = "でゃ": Roma(I, 2) = "でぃ": Roma(I, 3) = "でゅ": Roma(I, 4) = "でぇ": Roma(I, 5) = "でょ": I = I + 1
Roma(I, 0) = "DW": Roma(I, 1) = "どぁ": Roma(I, 2) = "どぃ": Roma(I, 3) = "どぅ": Roma(I, 4) = "どぇ": Roma(I, 5) = "どぉ": I = I + 1
Roma(I, 0) = "DY": Roma(I, 1) = "ぢゃ": Roma(I, 2) = "ぢぃ": Roma(I, 3) = "ぢゅ": Roma(I, 4) = "ぢぇ": Roma(I, 5) = "ぢょ": I = I + 1
Roma(I, 0) = "FW": Roma(I, 1) = "ふぁ": Roma(I, 2) = "ふぃ": Roma(I, 3) = "ふぅ": Roma(I, 4) = "ふぇ": Roma(I, 5) = "ふぉ": I = I + 1
Roma(I, 0) = "FY": Roma(I, 1) = "ふゃ": Roma(I, 2) = "ふぃ": Roma(I, 3) = "ふゅ": Roma(I, 4) = "ふぇ": Roma(I, 5) = "ふょ": I = I + 1
Roma(I, 0) = "GW": Roma(I, 1) = "ぐぁ": Roma(I, 2) = "ぐぃ": Roma(I, 3) = "ぐぅ": Roma(I, 4) = "ぐぇ": Roma(I, 5) = "ぐぉ": I = I + 1
Roma(I, 0) = "GY": Roma(I, 1) = "ぎゃ": Roma(I, 2) = "ぎぃ": Roma(I, 3) = "ぎゅ": Roma(I, 4) = "ぎぇ": Roma(I, 5) = "ぎょ": I = I + 1
Roma(I, 0) = "HY": Roma(I, 1) = "ひゃ": Roma(I, 2) = "ひぃ": Roma(I, 3) = "ひゅ": Roma(I, 4) = "ひぇ": Roma(I, 5) = "ひょ": I = I + 1
Roma(I, 0) = "JY": Roma(I, 1) = "じゃ": Roma(I, 2) = "じぃ": Roma(I, 3) = "じゅ": Roma(I, 4) = "じぇ": Roma(I, 5) = "じょ": I = I + 1
'Roma(I, 0) = "KW": Roma(I, 1) = "くぁ": Roma(I, 2) = "": Roma(I, 3) = "": Roma(I, 4) = "": Roma(I, 5) = "": I = I + 1
Roma(I, 0) = "KY": Roma(I, 1) = "きゃ": Roma(I, 2) = "きぃ": Roma(I, 3) = "きゅ": Roma(I, 4) = "きぇ": Roma(I, 5) = "きょ": I = I + 1
Roma(I, 0) = "LK": Roma(I, 1) = "ヵ": Roma(I, 2) = "": Roma(I, 3) = "": Roma(I, 4) = "ヶ": Roma(I, 5) = "": I = I + 1
Roma(I, 0) = "LT": Roma(I, 1) = "": Roma(I, 2) = "": Roma(I, 3) = "っ": Roma(I, 4) = "": Roma(I, 5) = "": I = I + 1
Roma(I, 0) = "LW": Roma(I, 1) = "ゎ": Roma(I, 2) = "": Roma(I, 3) = "": Roma(I, 4) = "": Roma(I, 5) = "": I = I + 1
Roma(I, 0) = "LY": Roma(I, 1) = "ゃ": Roma(I, 2) = "ぃ": Roma(I, 3) = "ゅ": Roma(I, 4) = "ぇ": Roma(I, 5) = "ょ": I = I + 1
Roma(I, 0) = "MY": Roma(I, 1) = "みゃ": Roma(I, 2) = "みぃ": Roma(I, 3) = "みゅ": Roma(I, 4) = "みぇ": Roma(I, 5) = "みょ": I = I + 1
Roma(I, 0) = "NY": Roma(I, 1) = "にゃ": Roma(I, 2) = "にぃ": Roma(I, 3) = "にゅ": Roma(I, 4) = "にぇ": Roma(I, 5) = "にょ": I = I + 1
Roma(I, 0) = "PY": Roma(I, 1) = "ぴゃ": Roma(I, 2) = "ぴぃ": Roma(I, 3) = "ぴゅ": Roma(I, 4) = "ぴぇ": Roma(I, 5) = "ぴょ": I = I + 1
Roma(I, 0) = "QW": Roma(I, 1) = "くぁ": Roma(I, 2) = "くぃ": Roma(I, 3) = "くぅ": Roma(I, 4) = "くぇ": Roma(I, 5) = "くぉ": I = I + 1
Roma(I, 0) = "QY": Roma(I, 1) = "くゃ": Roma(I, 2) = "くぃ": Roma(I, 3) = "くゅ": Roma(I, 4) = "くぇ": Roma(I, 5) = "くょ": I = I + 1
Roma(I, 0) = "RY": Roma(I, 1) = "りゃ": Roma(I, 2) = "りぃ": Roma(I, 3) = "りゅ": Roma(I, 4) = "りぇ": Roma(I, 5) = "りょ": I = I + 1
Roma(I, 0) = "SH": Roma(I, 1) = "しゃ": Roma(I, 2) = "し": Roma(I, 3) = "しゅ": Roma(I, 4) = "しぇ": Roma(I, 5) = "しょ": I = I + 1
Roma(I, 0) = "SW": Roma(I, 1) = "すぁ": Roma(I, 2) = "すぃ": Roma(I, 3) = "すぅ": Roma(I, 4) = "すぇ": Roma(I, 5) = "すぉ": I = I + 1
Roma(I, 0) = "SY": Roma(I, 1) = "しゃ": Roma(I, 2) = "しぃ": Roma(I, 3) = "しゅ": Roma(I, 4) = "しぇ": Roma(I, 5) = "しょ": I = I + 1
Roma(I, 0) = "TH": Roma(I, 1) = "てゃ": Roma(I, 2) = "てぃ": Roma(I, 3) = "てゅ": Roma(I, 4) = "てぇ": Roma(I, 5) = "てょ": I = I + 1
Roma(I, 0) = "TS": Roma(I, 1) = "つぁ": Roma(I, 2) = "つぃ": Roma(I, 3) = "つ": Roma(I, 4) = "つぇ": Roma(I, 5) = "つぉ": I = I + 1
Roma(I, 0) = "TW": Roma(I, 1) = "とぁ": Roma(I, 2) = "とぃ": Roma(I, 3) = "とぅ": Roma(I, 4) = "とぇ": Roma(I, 5) = "とぉ": I = I + 1
Roma(I, 0) = "TY": Roma(I, 1) = "ちゃ": Roma(I, 2) = "ちぃ": Roma(I, 3) = "ちゅ": Roma(I, 4) = "ちぇ": Roma(I, 5) = "ちょ": I = I + 1
Roma(I, 0) = "VY": Roma(I, 1) = "ヴゃ": Roma(I, 2) = "ヴぃ": Roma(I, 3) = "ヴゅ": Roma(I, 4) = "ヴぇ": Roma(I, 5) = "ヴょ": I = I + 1
Roma(I, 0) = "WH": Roma(I, 1) = "うぁ": Roma(I, 2) = "うぃ": Roma(I, 3) = "う": Roma(I, 4) = "うぇ": Roma(I, 5) = "うぉ": I = I + 1
Roma(I, 0) = "XK": Roma(I, 1) = "ヵ": Roma(I, 2) = "": Roma(I, 3) = "": Roma(I, 4) = "ヶ": Roma(I, 5) = "": I = I + 1
Roma(I, 0) = "XT": Roma(I, 1) = "": Roma(I, 2) = "": Roma(I, 3) = "っ": Roma(I, 4) = "": Roma(I, 5) = "": I = I + 1
Roma(I, 0) = "XW": Roma(I, 1) = "ゎ": Roma(I, 2) = "": Roma(I, 3) = "": Roma(I, 4) = "": Roma(I, 5) = "": I = I + 1
Roma(I, 0) = "XY": Roma(I, 1) = "ゃ": Roma(I, 2) = "ぃ": Roma(I, 3) = "ゅ": Roma(I, 4) = "ぇ": Roma(I, 5) = "ょ": I = I + 1
Roma(I, 0) = "ZY": Roma(I, 1) = "じゃ": Roma(I, 2) = "じぃ": Roma(I, 3) = "じゅ": Roma(I, 4) = "じぇ": Roma(I, 5) = "じょ": I = I + 1
Roma(I, 0) = "B": Roma(I, 1) = "ば": Roma(I, 2) = "び": Roma(I, 3) = "ぶ": Roma(I, 4) = "べ": Roma(I, 5) = "ぼ": I = I + 1
Roma(I, 0) = "C": Roma(I, 1) = "か": Roma(I, 2) = "し": Roma(I, 3) = "く": Roma(I, 4) = "せ": Roma(I, 5) = "こ": I = I + 1
Roma(I, 0) = "D": Roma(I, 1) = "だ": Roma(I, 2) = "ぢ": Roma(I, 3) = "づ": Roma(I, 4) = "で": Roma(I, 5) = "ど": I = I + 1
Roma(I, 0) = "F": Roma(I, 1) = "ふぁ": Roma(I, 2) = "ふぃ": Roma(I, 3) = "ふ": Roma(I, 4) = "ふぇ": Roma(I, 5) = "ふぉ": I = I + 1
Roma(I, 0) = "G": Roma(I, 1) = "が": Roma(I, 2) = "ぎ": Roma(I, 3) = "ぐ": Roma(I, 4) = "げ": Roma(I, 5) = "ご": I = I + 1
Roma(I, 0) = "H": Roma(I, 1) = "は": Roma(I, 2) = "ひ": Roma(I, 3) = "ふ": Roma(I, 4) = "へ": Roma(I, 5) = "ほ": I = I + 1
Roma(I, 0) = "J": Roma(I, 1) = "じゃ": Roma(I, 2) = "じ": Roma(I, 3) = "じゅ": Roma(I, 4) = "じぇ": Roma(I, 5) = "じょ": I = I + 1
Roma(I, 0) = "K": Roma(I, 1) = "か": Roma(I, 2) = "き": Roma(I, 3) = "く": Roma(I, 4) = "け": Roma(I, 5) = "こ": I = I + 1
Roma(I, 0) = "L": Roma(I, 1) = "ぁ": Roma(I, 2) = "ぃ": Roma(I, 3) = "ぅ": Roma(I, 4) = "ぇ": Roma(I, 5) = "ぉ": I = I + 1
Roma(I, 0) = "M": Roma(I, 1) = "ま": Roma(I, 2) = "み": Roma(I, 3) = "む": Roma(I, 4) = "め": Roma(I, 5) = "も": I = I + 1
Roma(I, 0) = "N": Roma(I, 1) = "な": Roma(I, 2) = "に": Roma(I, 3) = "ぬ": Roma(I, 4) = "ね": Roma(I, 5) = "の": I = I + 1
Roma(I, 0) = "P": Roma(I, 1) = "ぱ": Roma(I, 2) = "ぴ": Roma(I, 3) = "ぷ": Roma(I, 4) = "ぺ": Roma(I, 5) = "ぽ": I = I + 1
Roma(I, 0) = "Q": Roma(I, 1) = "くぁ": Roma(I, 2) = "くぃ": Roma(I, 3) = "く": Roma(I, 4) = "くぇ": Roma(I, 5) = "くぉ": I = I + 1
Roma(I, 0) = "R": Roma(I, 1) = "ら": Roma(I, 2) = "り": Roma(I, 3) = "る": Roma(I, 4) = "れ": Roma(I, 5) = "ろ": I = I + 1
Roma(I, 0) = "S": Roma(I, 1) = "さ": Roma(I, 2) = "し": Roma(I, 3) = "す": Roma(I, 4) = "せ": Roma(I, 5) = "そ": I = I + 1
Roma(I, 0) = "T": Roma(I, 1) = "た": Roma(I, 2) = "ち": Roma(I, 3) = "つ": Roma(I, 4) = "て": Roma(I, 5) = "と": I = I + 1
Roma(I, 0) = "V": Roma(I, 1) = "ヴぁ": Roma(I, 2) = "ヴぃ": Roma(I, 3) = "ヴ": Roma(I, 4) = "ヴぇ": Roma(I, 5) = "ヴぉ": I = I + 1
Roma(I, 0) = "W": Roma(I, 1) = "わ": Roma(I, 2) = "うぃ": Roma(I, 3) = "う": Roma(I, 4) = "うぇ": Roma(I, 5) = "を": I = I + 1
Roma(I, 0) = "X": Roma(I, 1) = "ぁ": Roma(I, 2) = "ぃ": Roma(I, 3) = "ぅ": Roma(I, 4) = "ぇ": Roma(I, 5) = "ぉ": I = I + 1
Roma(I, 0) = "Y": Roma(I, 1) = "や": Roma(I, 2) = "い": Roma(I, 3) = "ゆ": Roma(I, 4) = "いぇ": Roma(I, 5) = "よ": I = I + 1
Roma(I, 0) = "Z": Roma(I, 1) = "ざ": Roma(I, 2) = "じ": Roma(I, 3) = "ず": Roma(I, 4) = "ぜ": Roma(I, 5) = "ぞ": I = I + 1
Roma(I, 0) = "": Roma(I, 1) = "あ": Roma(I, 2) = "い": Roma(I, 3) = "う": Roma(I, 4) = "え": Roma(I, 5) = "お": I = I + 1
RomaMx = I - 1

EdRule = " ,.!?、。！？"

ClLst(0) = RGB(255, 0, 0)
ClLst(1) = RGB(255, 128, 0)
ClLst(2) = RGB(255, 255, 0)
ClLst(3) = RGB(0, 255, 0)
ClLst(4) = RGB(0, 255, 255)
ClLst(5) = RGB(0, 0, 255)
ClLst(6) = RGB(255, 0, 255)
ClLst(7) = RGB(128, 128, 128)
ClLst(8) = RGB(192, 128, 255)
ClLst(9) = RGB(192, 255, 128)

BS(0) = "02122232202122230212223210111213"
BS(1) = "11211222112112221121122211211222"
BS(2) = "01112112100111121102122210112112"
BS(3) = "01111222201121120111122210011102"
BS(4) = "11210212101121221121021200011112"
BS(5) = "01112122101102120102122210201112"
BS(6) = "01112102001011122102122210111222"

BCNm(0, 0) = RGB(255, 0, 0): BCNm(0, 1) = RGB(255, 255, 0): BCNm(0, 2) = RGB(0, 255, 255): BCNm(0, 3) = RGB(0, 255, 0)
BCNm(0, 4) = RGB(255, 0, 255): BCNm(0, 5) = RGB(0, 0, 255): BCNm(0, 6) = RGB(255, 128, 0): BCNm(0, 7) = RGB(255, 255, 255)
BCNm(1, 0) = RGB(192, 0, 0): BCNm(1, 1) = RGB(192, 192, 0): BCNm(1, 2) = RGB(0, 192, 192): BCNm(1, 3) = RGB(0, 192, 0)
BCNm(1, 4) = RGB(192, 0, 192): BCNm(1, 5) = RGB(0, 0, 192): BCNm(1, 6) = RGB(192, 96, 0): BCNm(1, 7) = RGB(192, 192, 192)

For I = 0 To 6: For U = 0 To 3: For Y = 0 To 3: For T = 0 To 1
B(I, U, Y, T) = Val(Mid$(BS(I), U * 8 + Y * 2 + T + 1, 1))
Next T, Y, U, I
AI(0) = "000000"
AI(1) = "022220"
AI(2) = "2****2"
AI(3) = "088880"
AI(4) = "000000"
AI(5) = "002*20"
AI(6) = "005*50"
AI(7) = "006*60"
AI(8) = "007*70"
AI(9) = "000800"
AI(10) = "000000"
AI(11) = "022220"
AI(12) = "2****2"
AI(13) = "088880"
AI(14) = "000000"
AI(15) = "02*200"
AI(16) = "05*500"
AI(17) = "06*600"
AI(18) = "07*700"
AI(19) = "008000"
AI(20) = "002200"
AI(21) = "01**10"
AI(22) = "02**20"
AI(23) = "009900"
AI(24) = "000000"
AI(25) = "002200"
AI(26) = "01**10"
AI(27) = "02**20"
AI(28) = "009900"
AI(29) = "000000"
AI(30) = "002200"
AI(31) = "01**10"
AI(32) = "02**20"
AI(33) = "009900"
AI(34) = "000000"
AI(35) = "002200"
AI(36) = "01**10"
AI(37) = "02**20"
AI(38) = "009900"
AI(39) = "000000"
AI(40) = "032300"
AI(41) = "2***20"
AI(42) = "08*800"
AI(43) = "006000"
AI(44) = "000000"
AI(45) = "06*200"
AI(46) = "2**200"
AI(47) = "08*400"
AI(48) = "009000"
AI(49) = "000000"
AI(50) = "002000"
AI(51) = "05*500"
AI(52) = "1***10"
AI(53) = "077700"
AI(54) = "000000"
AI(55) = "02*600"
AI(56) = "02**20"
AI(57) = "04*800"
AI(58) = "009000"
AI(59) = "000000"
AI(60) = "022000"
AI(61) = "2**200"
AI(62) = "05**20"
AI(63) = "006700"
AI(64) = "000000"
AI(65) = "002*10"
AI(66) = "02**20"
AI(67) = "03*900"
AI(68) = "008000"
AI(69) = "000000"
AI(70) = "022000"
AI(71) = "2**200"
AI(72) = "05**20"
AI(73) = "006700"
AI(74) = "000000"
AI(75) = "02*100"
AI(76) = "2**200"
AI(77) = "3*9000"
AI(78) = "080000"
AI(79) = "000000"
AI(80) = "002200"
AI(81) = "02**20"
AI(82) = "2**500"
AI(83) = "076000"
AI(84) = "000000"
AI(85) = "01*200"
AI(86) = "02**20"
AI(87) = "009*30"
AI(88) = "000800"
AI(89) = "000000"
AI(90) = "002200"
AI(91) = "02**20"
AI(92) = "2**500"
AI(93) = "076000"
AI(94) = "000000"
AI(95) = "1*2000"
AI(96) = "2**200"
AI(97) = "09*300"
AI(98) = "008000"
AI(99) = "000000"
AI(100) = "022200"
AI(101) = "2***20"
AI(102) = "078*40"
AI(103) = "000600"
AI(104) = "000000"
AI(105) = "04*100"
AI(106) = "08*300"
AI(107) = "3**500"
AI(108) = "077000"
AI(109) = "000000"
AI(110) = "020000"
AI(111) = "1*4800"
AI(112) = "4***20"
AI(113) = "066600"
AI(114) = "000000"
AI(115) = "01**10"
AI(116) = "02*800"
AI(117) = "02*400"
AI(118) = "009000"
AI(119) = "000000"
AI(120) = "022200"
AI(121) = "2***20"
AI(122) = "4*8700"
AI(123) = "060000"
AI(124) = "000000"
AI(125) = "1**100"
AI(126) = "08*200"
AI(127) = "04*200"
AI(128) = "009000"
AI(129) = "000000"
AI(130) = "000200"
AI(131) = "084*10"
AI(132) = "2***40"
AI(133) = "066600"
AI(134) = "000000"
AI(135) = "01*400"
AI(136) = "03*800"
AI(137) = "05**30"
AI(138) = "007700"
AI(139) = "000000"
For I = 0 To 6: For U = 0 To 3: For Y = 0 To 5: For T = 0 To 4
If Mid$(AI(I * 20 + U * 5 + T), Y + 1, 1) <> "*" Then CPA(I, U, Y, T) = Val(Mid$(AI(I * 20 + U * 5 + T), Y + 1, 1)) Else CPA(I, U, Y, T) = -100
Next T, Y, U, I
TrMN(0) = "NORMAL": TrMN(1) = "HARD": TrMN(2) = "ADVANCE": TrMN(3) = "JOKER": TrMN(4) = "FURTHEST"
LS(0) = 5: LS(1) = 8: LS(2) = 10: LS(3) = 12: LS(4) = 16
LS(5) = 20: LS(6) = 24: LS(7) = 30: LS(8) = 36: LS(9) = 42
LS(10) = 48: LS(11) = 54: LS(12) = 60: LS(13) = 66: LS(14) = 72
LS(15) = 80: LS(16) = 88: LS(17) = 96: LS(18) = 108: LS(19) = 120
LS(20) = 48: LS(21) = 60: LS(22) = 75: LS(23) = 90: LS(24) = 105
LS(25) = 120: LS(26) = 140: LS(27) = 160: LS(28) = 180: LS(29) = 200
LS(30) = 240: LS(31) = 280: LS(32) = 320: LS(33) = 360: LS(34) = 420
LS(35) = 480: LS(36) = 600: LS(37) = 720: LS(38) = 840: LS(39) = 960
LS(40) = 240: LS(41) = 320: LS(42) = 480: LS(43) = 640: LS(44) = 800
LS(45) = 960: LS(46) = 1200: LS(47) = 1440: LS(48) = 1920: LS(49) = 2400
Bn(0) = 40: Bn(1) = 100: Bn(2) = 300: Bn(3) = 1200
VSLv(0, 0) = 0: VSLv(0, 1) = 20: VSLv(0, 2) = 30
VSLv(1, 0) = 0: VSLv(1, 1) = 20: VSLv(1, 2) = 40: VSLv(1, 3) = 50
VSLv(2, 0) = 0: VSLv(2, 1) = 20: VSLv(2, 2) = 40: VSLv(2, 3) = 200
With CPE(0) 'デモンストレーション
.DP = 8: .LR = -8: .QD = False: .Spd = 5: .SRt = True: .Wt = 30
End With
With CPE(1) '最弱
.DP = 85: .LR = 36: .QD = False: .Spd = 16: .SRt = False: .Wt = 110
End With
With CPE(2) '壁置き
.DP = 28: .LR = 32: .QD = False: .Spd = 12: .SRt = False: .Wt = 95
End With
With CPE(3) '中央置き
.DP = 22: .LR = -16: .QD = False: .Spd = 8: .SRt = False: .Wt = 85
End With
With CPE(4) 'ノーマル
.DP = 18: .LR = -10: .QD = False: .Spd = 6: .SRt = False: .Wt = 80
End With
With CPE(5) '回転入れ
.DP = 4: .LR = 1: .QD = False: .Spd = 1: .SRt = False: .Wt = 75
End With
With CPE(6) 'クイックドロップ
.DP = 12: .LR = 0: .QD = True: .Spd = 1: .SRt = False: .Wt = 70
End With
With CPE(7) '2次回転
.DP = 2: .LR = -6: .QD = True: .Spd = 1: .SRt = True: .Wt = 65
End With
With CPE(8) '2次回転
.DP = 7: .LR = -4: .QD = True: .Spd = 1: .SRt = True: .Wt = 60
End With
With CPE(9) '2次回転
.DP = 14: .LR = -10: .QD = True: .Spd = 1: .SRt = True: .Wt = 55
End With

With CPE(10) 'オールラウンド
.DP = 8: .LR = -12: .QD = True: .Spd = 0: .SRt = True: .Wt = 51
End With
With CPE(11) 'オールラウンド
.DP = 10: .LR = -14: .QD = True: .Spd = 0: .SRt = True: .Wt = 48
End With
With CPE(12) 'オールラウンド
.DP = 12: .LR = -15: .QD = True: .Spd = 0: .SRt = True: .Wt = 45
End With
With CPE(13) 'オールラウンド
.DP = 14: .LR = -16: .QD = True: .Spd = 0: .SRt = True: .Wt = 42
End With
With CPE(14) 'オールラウンド
.DP = 16: .LR = -15: .QD = True: .Spd = 0: .SRt = True: .Wt = 39
End With
With CPE(15) 'オールラウンド
.DP = 18: .LR = -13: .QD = True: .Spd = 0: .SRt = True: .Wt = 36
End With
With CPE(16) 'マスター
.DP = 17: .LR = -11: .QD = True: .Spd = 0: .SRt = True: .Wt = 33
End With
With CPE(17) 'マスター
.DP = 15: .LR = -10: .QD = True: .Spd = 0: .SRt = True: .Wt = 30
End With
With CPE(18) 'マスター
.DP = 13: .LR = -9: .QD = True: .Spd = 0: .SRt = True: .Wt = 27
End With
With CPE(19) 'マスター
.DP = 11: .LR = -8: .QD = True: .Spd = 0: .SRt = True: .Wt = 24
End With
With CPE(20) 'マスター
.DP = 9: .LR = -7: .QD = True: .Spd = 0: .SRt = True: .Wt = 21
End With
With CPE(21) 'マスター
.DP = 6: .LR = -6: .QD = True: .Spd = 0: .SRt = True: .Wt = 18
End With

With CPE(22) 'スペシャル
.DP = 4: .LR = -5: .QD = True: .Spd = 0: .SRt = True: .Wt = 15
End With
With CPE(23) 'スペシャル
.DP = 4: .LR = -6: .QD = True: .Spd = 0: .SRt = True: .Wt = 10
End With
With CPE(24) 'スペシャル
.DP = 4: .LR = -7: .QD = True: .Spd = 0: .SRt = True: .Wt = 5
End With
With CPE(25) 'スペシャル
.DP = 4: .LR = -8: .QD = True: .Spd = 0: .SRt = True: .Wt = 1
End With

VDS = String$(VSizeMax + 256, 92)

For I = 15 To 29: SRnk(I).Nm = "______": SRnk(I).SC = 0: SRnk(I).Li = 0: Next I
For I = 15 To 29: TRnk(I).Nm = "______": TRnk(I).Tm = 60000: TRnk(I).Li = 100: Next I
For I = 5 To 9: FRnk(I).Nm = "______": FRnk(I).Lv = 200: FRnk(I).Tm = 60000: Next I
For I = 5 To 9: LRnk(I).Nm = "______": LRnk(I).Liv = 1: LRnk(I).Tm = 60000: Next I

TrNmm = String$(6, "\")

ReDim StrMsgLog(0)
TrMAdd = False

Set DX = New DirectX7
Set DI = DX.DirectInputCreate()

'キーボードデバイスの作成
Set DIDK = DI.CreateDevice("GUID_SysKeyboard")
DIDK.SetCommonDataFormat DIFORMAT_KEYBOARD
DIDK.SetCooperativeLevel Form1.hWnd, DISCL_BACKGROUND Or DISCL_NONEXCLUSIVE
DIDK.Acquire

'ジョイスティックデバイスの作成
With JS0
.X = 32768: .Y = 32768: For I = 0 To 31: .buttons(I) = 0: Next I
End With
Set DIEN = DI.GetDIEnumDevices(DIDEVTYPE_JOYSTICK, DIEDFL_ATTACHEDONLY)
JC = DIEN.GetCount: If JC > -RecCh * 4 Then JC = -RecCh * 4
For I = 0 To JC - 1
Set DIDJ(I) = DI.CreateDevice(DIEN.GetItem(I + 1).GetGuidInstance)
DIDJ(I).SetCommonDataFormat DIFORMAT_JOYSTICK
DIDJ(I).SetCooperativeLevel Form1.hWnd, DISCL_BACKGROUND Or DISCL_EXCLUSIVE
DIDJ(I).Acquire
Next I

'ミュージックの設定
MidiPortC = 0: NoBGM = False
On Error GoTo CBGM
Set DMP = DX.DirectMusicPerformanceCreate
On Error GoTo CE
If Not NoBGM Then
DMP.Init Nothing, Form1.hWnd
ReDim PortName(0), PortIndex(0)
PortName(0) = "OFF"
MidiPortC = 0
For I = 1 To DMP.GetPortCount
DMP.GetPortCaps I, DMPC
If DMPC.lClass = DMUS_PC_OUTPUTCLASS Then
MidiPortC = MidiPortC + 1
ReDim Preserve PortIndex(MidiPortC), PortName(MidiPortC): PortIndex(MidiPortC) = I: PortName(MidiPortC) = DMP.GetPortName(I)
End If
Next I
MidiMode = 0: If Midi < 0 Or Midi > MidiPortC Then Midi = MidiPortC
Trans = 0
MidiOn = False
End If

'サウンドの作成
SouCreate

'画面の変更
Set DD = DX.DirectDrawCreate("")
WndL = Form1.Left: WndT = Form1.Top: WndWW = Form1.Width: WndHH = Form1.Height
Form1.Appearance = 0
Form1.BackColor = &H0
Form1.WindowState = 2 + WM * 2
Form1.Show

If Not WM Then
DD.SetCooperativeLevel Form1.hWnd, DDSCL_EXCLUSIVE Or DDSCL_FULLSCREEN Or DDSCL_ALLOWMODEX
FullScrErr = False
On Error GoTo CDisp
DD.SetDisplayMode WW, HH, BPP, 60, DDSDM_DEFAULT
On Error GoTo CE
If FullScrErr Then
FullScrErr = False
On Error GoTo CDisp
DD.SetDisplayMode WW, HH, BPP, 0, DDSDM_DEFAULT
On Error GoTo CE
If FullScrErr Then WM = True
End If
End If
If WM Then
DD.SetCooperativeLevel Picture1.hWnd, DDSCL_NORMAL
End If

'マウスデバイスの作成
If Not WM Then
Set DIDM = DI.CreateDevice("GUID_SysMouse")
DIDM.SetCommonDataFormat DIFORMAT_MOUSE
DIDM.SetCooperativeLevel Form1.hWnd, DISCL_FOREGROUND Or DISCL_EXCLUSIVE
DIDM.Acquire
End If

'サーフェイス作成
SfCreate

'ネットワーク作成
If RecCh And Not Nett And Net Then NetCreate: NameCreate
Nett = Net
NC = 0: NCC = 0
StrMsgJ = ""
KeyLock = False
ParStC = -1

'------------------ プログラム開始 ------------------
WS = Form1.WindowState: WSS = WS

For I = 0 To 3: RCPG1(I) = 2 + I: Next I
For I = 0 To 1: OBPT(I) = 0: Next I
MnJ = 0
EscC = 0
CC = 0: CCC = 0: LAni = 0: RushAniC = 0
GMode = 6
Do
DTClRate
GGMode = GMode
Select Case GMode
Case 6 'オープニング
Opening
Case 5 'タイトルメニュー
Title
Case 2 'デバッグメニュー
DebugMenu
Case 1 'トライアル
Trial
Case 3 '2P対戦
VS2P
Case 4 '4P対戦
VS4P
Case 7 'ネット対戦
VSNet
Case Else
GMode = 0
End Select
PrSf.BltColorFill RC0, 0

Loop..<GMode = 0

Unload Me

Exit Sub

CBGM:
NoBGM = True
Resume Next

CDisp:
FullScrErr = True
Resume Next

CE:
'Beep
Unload Me

End Sub

Function XDir(ByVal PathName As String, Optional ByVal At As VbFileAttribute = vbNormal) As String 'ファイル存在
On Error GoTo CE
XDir = Dir$(PathName, At)
Exit Function
CE:
XDir = vbNullString$
End Function

Function XStrConv(St As String, Conv As VbStrConv) '文字コンバート
On Error GoTo CE
XStrConv = StrConv(St, Conv)
Exit Function
CE:
XStrConv = St
End Function

Function RvInStr(ByVal StStr As String, ByVal SchStr As String) As Long '右からInStr
On Error GoTo CE
Dim RR As Long, RR1 As Long
RR = InStr(StStr, SchStr)
RR1 = RR
While RR1 > 0
RR1 = InStr(RR + 1, StStr, SchStr)
If RR1 > 0 Then RR = RR1
Wend
RvInStr = RR
Exit Function
CE:
RvInStr = 0
End Function

Private Sub Form_Unload(Cancel As Integer) '終了処理
On Error GoTo CE
Dim I As Long
NetDestroy
SfDestroy
Set DIDM = Nothing
Set DD = Nothing
For I = 15 To 0 Step -1: Set SSB(I) = Nothing: Set SB(I) = Nothing: Next I
Set DS = Nothing
For I = 3 To 0 Step -1: Set DIDJ(I) = Nothing: Next I
Set DMSS = Nothing
Set DMS = Nothing
Set DML = Nothing
Set DMP = Nothing
Set DIEN = Nothing
Set DIDK = Nothing
Set DI = Nothing
Set DX = Nothing
EnvSave
If RecCh Then RecSave: NetLogCr: HTMLCr
Form1.Refresh
CE:
DoEvents
End
End Sub

Sub NetCreate() 'ネットワーク作成
On Error GoTo CE
Dim RR As Long, I As Long
Set DP = DX.DirectPlayCreate("")
Set DPEC = DP.GetDPEnumConnections("", DPCONNECTION_DIRECTPLAY)
RR = 0
For I = 1 To DPEC.GetCount
If InStr(UCase$(DPEC.GetName(I)), "TCP/IP") Then RR = I
Next I
Set DPA = DPEC.GetAddress(RR)
DP.InitializeConnection DPA
Set DPSD = DP.CreateSessionData
DPSD.SetGuidApplication "{3D3FBB20-10B9-11d7-968B-0800460222F0}" '{ADE312C0-AD7F-11D6-968B-0800460222F0}
DPSD.SetSessionName "DTET"
DPSD.SetMaxPlayers 2
Exit Sub

CE:
'Beep
End Sub

Sub NameCreate() 'ハンドル名作成
On Error GoTo CE
DPSD.SetFlags DPSESSION_MIGRATEHOST
DP.Open DPSD, DPOPEN_CREATE
PlID = DP.CreatePlayer(HNN, "DTET-" + HNN, 0, DPPLAYER_SERVERPLAYER)
NetPar = True
Exit Sub

CE:
'Beep
End Sub

Sub JoinCreate() '参加者名作成
On Error GoTo CE
SsC = 0
On Error GoTo CSessions
Set DPES = DP.GetDPEnumSessions(DPSD, 0, DPENUMSESSIONS_AVAILABLE)
On Error GoTo CE
SsC = DPES.GetCount
If SsC > 0 Then
Set DPSD = DPES.GetItem(1)
DPSD.SetFlags 0
DP.Open DPSD, DPOPEN_JOIN
PlID = DP.CreatePlayer(HNN, "DTET-" + HNN, 0, DPPLAYER_DEFAULT)
End If
Set DPES = Nothing
NetPar = False
Exit Sub

CSessions:
On Error GoTo CE
Exit Sub

CE:
End Sub

Sub NetDestroy() 'ネットワーク解放
On Error GoTo CE
Set DPSD = Nothing
Set DPA = Nothing
Set DPEC = Nothing
Set DP = Nothing
Exit Sub

CE:
'Beep
End Sub

Sub SessionCheck() '接続人数チェック
On Error GoTo CE
Dim I As Long
Set DPEP = DP.GetDPEnumPlayers("", DPENUMPLAYERS_GROUP)
NC = DPEP.GetCount
For I = 0 To NC - 1: PLID1(I) = DPEP.GetDPID(I + 1): JN1(I) = DPEP.GetShortName(I + 1): Next I
If NC >= 2 Then
For I = 0 To 1
If PLID1(I) <> PlID Then JID = PLID1(I): JN = JN1(I)
Next I
Else
JN = vbNullString$
End If
Set DPEP = Nothing
Exit Sub
CE:
End Sub

Private Sub StrMsgLogAdd(ByVal Msg As String) 'テキストメッセージログ追加
On Error GoTo CE
StrMsgLogC = StrMsgLogC + 1
ReDim Preserve StrMsgLog(StrMsgLogC)
If Msg <> "" Then StrMsgLog(StrMsgLogC) = "[" + CStr(Now) + "] " + Msg Else StrMsgLog(StrMsgLogC) = ""
Exit Sub

CE:
'Beep
End Sub

Sub StrMsgInit() 'テキストメッセージ初期化
On Error GoTo CE
Dim I As Long
StrMsgBT = StrMsgB
For I = 0 To 255: StrMsgOrg(I) = "": StrMsgToku(I) = False: Next I
StrMsgB = "": StrMsg = "": KeyBuf = "": SMBPos = 0: SMPos = 0
KeyLock = True
SMSel = -2
StrMsgDraw 0
SMXY
Exit Sub

CE:
'Beep
End Sub

Sub StrMsgEdit(ByVal Pan As Long) 'テキストメッセージ編集
On Error GoTo CE
Dim I As Long, U As Long, Y As Long, LpFin As Boolean, RR As Long, RR1 As Long, RR2 As Long, Ch As Boolean, RRR As String
Dim PadB As Boolean
StrMsgBB = StrMsgB: StrMsgg = StrMsg: SMSell = SMSel
SMBPoss = SMBPos: SMPoss = SMPos
SMTp = False: SMMv = False: SMdl = False

If KeyLock Then KeyBuf = Mid$(KeyBuf, 2): KeyLock = False

PadB = False
For I = 1 To JC: PadB = PadB Or (CsrBB1(I) = 1): Next I
If PadB Then StrEdit = False: KeyInitAll: StrMsg = "": SMPos = 0: For I = 0 To 255: StrMsgOrg(I) = "": StrMsgToku(I) = False: Next I: StrMsgB = "": SMBPos = 0: Sou(12).Pan = Pan: Sou(12).On = True

While Not PadB And Len(KeyBuf) > 0
KeyChar = Left$(KeyBuf, 1): KeyBuf = Mid$(KeyBuf, 2)

SMSell = SMSel
If SMSell = -2 Then '一般入力中
If Asc(KeyChar) >= 32 And Asc(KeyChar) <> 127 Then
If Jpn And Asc(KeyChar) <> 32 Then
SMSel = -1: SMSell = SMSel
Else
RR = False
RRR = Left$(StrMsgB, SMBPos) + KeyChar + Mid$(StrMsgB, SMBPos + 1)
SMBPosLim RRR
If Len(RRR) <= SMBPosL Then StrMsgB = RRR: SMBPos = SMBPos + 1: SMTp = True: Sou(1).Pan = Pan: Sou(1).On = True
End If
End If
If Asc(KeyChar) = 8 And SMBPos > 0 Then StrMsgB = Left$(StrMsgB, SMBPos - 1) + Mid$(StrMsgB, SMBPos + 1): SMBPos = SMBPos - 1: SMdl = True: Sou(12).Pan = Pan: Sou(12).On = True
If Asc(KeyChar) = 127 And SMBPos < Len(StrMsgB) Then StrMsgB = Left$(StrMsgB, SMBPos) + Mid$(StrMsgB, SMBPos + 2): SMdl = True: Sou(12).Pan = Pan: Sou(12).On = True
If Asc(KeyChar) = 29 And SMBPos > 0 Then SMBPos = SMBPos - 1: SMMv = True: Sou(5).Pan = Pan: Sou(5).On = True
If Asc(KeyChar) = 28 And SMBPos < Len(StrMsgB) Then SMBPos = SMBPos + 1: SMMv = True: Sou(5).Pan = Pan: Sou(5).On = True
If Asc(KeyChar) = 30 And SMBPos > 0 Then SMBPos = 0: SMMv = True: Sou(5).Pan = Pan: Sou(5).On = True
If Asc(KeyChar) = 31 And SMBPos < Len(StrMsgB) Then SMBPos = Len(StrMsgB): SMMv = True: Sou(5).Pan = Pan: Sou(5).On = True
If Asc(KeyChar) = 13 Then StrEdit = False: KeyInitAll: EscC = 2: If StrMsgB = "" Then Sou(12).Pan = Pan: Sou(12).On = True Else Sou(0).Pan = Pan: Sou(0).On = True
If Asc(KeyChar) = 27 Then StrEdit = False: KeyInitAll: EscC = 2: StrMsgB = "": SMBPos = 0: Sou(12).Pan = Pan: Sou(12).On = True
End If

If SMSell >= 0 Then '変換中
If Asc(KeyChar) = 31 Or Asc(KeyChar) = 32 Then SMSel = SMSel + 1 + (SMSel >= 2) * 3: StrMsg = StrMsgSel(SMSel): SMTp = True: Sou(1).Pan = Pan: Sou(1).On = True
If Asc(KeyChar) = 30 Then SMSel = SMSel - 1 - (SMSel <= 0) * 3: StrMsg = StrMsgSel(SMSel): SMTp = True: Sou(1).Pan = Pan: Sou(1).On = True
If Asc(KeyChar) = 8 Or Asc(KeyChar) = 27 Then SMSel = -1: StrMsg = StrMsgSel(2): SMdl = True: Sou(12).Pan = Pan: Sou(12).On = True
RR = False
If Asc(KeyChar) = 13 Then StrMsgB = Left$(StrMsgB, SMBPos) + StrMsg + Mid$(StrMsgB, SMBPos + 1): SMBPos = SMBPos + Len(StrMsg): StrMsg = "": SMPos = 0: For I = 0 To 255: StrMsgOrg(I) = "": StrMsgToku(I) = False: Next I: SMSel = -2: RR = True: SMTp = True: Sou(1).Pan = Pan: Sou(1).On = True
If Asc(KeyChar) > 32 And Asc(KeyChar) <> 127 Then StrMsgB = Left$(StrMsgB, SMBPos) + StrMsg + Mid$(StrMsgB, SMBPos + 1): SMBPos = SMBPos + Len(StrMsg): StrMsg = "": SMPos = 0: For I = 0 To 255: StrMsgOrg(I) = "": StrMsgToku(I) = False: Next I: SMSel = -1: SMSell = SMSel: RR = True: SMTp = True: Sou(1).Pan = Pan: Sou(1).On = True
If RR Then
SMBPosLim StrMsgB
If Len(StrMsgB) > SMBPosL Then StrMsgB = Left$(StrMsgB, SMBPosL): If SMBPos > SMBPosL Then SMBPos = SMBPosL
End If
End If

If SMSell = -1 Then '日本語入力中
If Asc(KeyChar) > 32 And Asc(KeyChar) <> 127 And Len(StrMsg) < 240 Then
StrMsgUD = StrMsg: SMPosUD = SMPos: For I = 0 To 255: StrMsgOrgUD(I) = StrMsgOrg(I): StrMsgTokuUD(I) = StrMsgToku(I): Next I
StrMsg = Left$(StrMsg, SMPos) + KeyChar + Mid$(StrMsg, SMPos + 1)
SMPos = SMPos + 1
For Y = 255 To SMPos Step -1: StrMsgOrg(Y) = StrMsgOrg(Y - 1): StrMsgToku(Y) = StrMsgToku(Y - 1): Next Y: StrMsgOrg(SMPos - 1) = KeyChar: StrMsgToku(SMPos - 1) = False
For I = 1 To 5
If UCase$(Mid$(StrMsg, SMPos, 1)) = RomaM(I) Then
For U = 0 To RomaMx
If SMPos > Len(Roma(U, 0)) Then
If UCase$(Mid$(StrMsg, SMPos - Len(Roma(U, 0)), Len(Roma(U, 0)))) = Roma(U, 0) And Roma(U, I) <> "" Then
Ch = False: For Y = 1 To Len(Roma(U, 0)): Ch = Ch Or StrMsgToku(SMPos - 1 - Y): Next Y
If Not Ch Then
RR2 = Len(Roma(U, 0))
RR = (Len(Roma(U, 0)) - Len(Roma(U, I))) + 1
For Y = 1 To Len(Roma(U, 0)): StrMsgOrg(SMPos - Len(Roma(U, 0)) - 1) = StrMsgOrg(SMPos - Len(Roma(U, 0)) - 1) + StrMsgOrg(SMPos - Len(Roma(U, 0)) - 1 + Y): Next Y
StrMsgOrg(SMPos - Len(Roma(U, 0)) - 1) = StrMsgOrg(SMPos - Len(Roma(U, 0)) - 1) + Space$(Len(Roma(U, I)) - 1): StrMsgToku(SMPos - Len(Roma(U, 0)) - 1) = False: StrMsg = Left$(StrMsg, SMPos - 1 - Len(Roma(U, 0))) + Roma(U, I) + Mid$(StrMsg, SMPos + 1): SMPos = SMPos + Len(Roma(U, I)) - Len(Roma(U, 0)) - 1
For Y = SMPos To 255 - RR: StrMsgOrg(Y) = StrMsgOrg(Y + RR): StrMsgToku(Y) = StrMsgToku(Y + RR): Next Y
I = 5: U = RomaMx
End If
End If
End If
Next U
End If
Next I

If SMPos > 1 Then If Not StrMsgToku(SMPos - 2) And InStr("BCDFGHJKLMPQRSTVWXYZ", UCase$(Mid$(StrMsg, SMPos, 1))) > 0 And UCase$(Mid$(StrMsg, SMPos, 1)) = UCase$(Mid$(StrMsg, SMPos - 1, 1)) Then StrMsg = Left$(StrMsg, SMPos - 2) + "っ" + Mid$(StrMsg, SMPos)
If SMPos > 1 Then If Not StrMsgToku(SMPos - 2) And InStr("NX", UCase$(Mid$(StrMsg, SMPos - 1, 1))) > 0 And UCase$(Mid$(StrMsg, SMPos, 1)) = "N" Then For I = SMPos - 1 To 254: StrMsgOrg(I) = StrMsgOrg(I + 1): StrMsgToku(I) = StrMsgToku(I + 1): Next I: StrMsgOrg(SMPos - 2) = Mid$(StrMsg, SMPos - 1, 2): StrMsgToku(SMPos - 2) = False: StrMsg = Left$(StrMsg, SMPos - 2) + "ん" + Mid$(StrMsg, SMPos + 1): SMPos = SMPos - 1
If SMPos > 1 Then If Not StrMsgToku(SMPos - 2) And UCase$(Mid$(StrMsg, SMPos - 1, 1)) = "N" And UCase$(Mid$(StrMsg, SMPos, 1)) <> "Y" Then StrMsg = Left$(StrMsg, SMPos - 2) + "ん" + Mid$(StrMsg, SMPos)

If Asc(Mid$(StrMsg, SMPos, 1)) >= 161 And Asc(Mid$(StrMsg, SMPos, 1)) <= 223 Then StrMsg = Left$(StrMsg, SMPos - 1) + XStrConv(Mid$(StrMsg, SMPos, 1), vbWide Or vbHiragana) + Mid$(StrMsg, SMPos + 1)
If SMPos > 1 Then If Not StrMsgToku(SMPos - 2) And InStr("かきくけこさしすせそたちつてとはひふへほ", UCase$(Mid$(StrMsg, SMPos - 1, 1))) > 0 And Mid$(StrMsg, SMPos, 1) = "゛" Then StrMsgOrg(SMPos - 2) = StrMsgOrg(SMPos - 2) + StrMsgOrg(SMPos - 1): StrMsgToku(SMPos - 2) = False: For I = SMPos - 1 To 254: StrMsgOrg(I) = StrMsgOrg(I + 1): StrMsgToku(I) = StrMsgToku(I + 1): Next I: StrMsg = Left$(StrMsg, SMPos - 2) + Chr$(Asc(Mid$(StrMsg, SMPos - 1)) + 1) + Mid$(StrMsg, SMPos + 1): SMPos = SMPos - 1
If SMPos > 1 Then If Not StrMsgToku(SMPos - 2) And InStr("はひふへほ", UCase$(Mid$(StrMsg, SMPos - 1, 1))) > 0 And Mid$(StrMsg, SMPos, 1) = "゜" Then StrMsgOrg(SMPos - 2) = StrMsgOrg(SMPos - 2) + StrMsgOrg(SMPos - 1): StrMsgToku(SMPos - 2) = False: For I = SMPos - 1 To 254: StrMsgOrg(I) = StrMsgOrg(I + 1): StrMsgToku(I) = StrMsgToku(I + 1): Next I: StrMsg = Left$(StrMsg, SMPos - 2) + Chr$(Asc(Mid$(StrMsg, SMPos - 1)) + 2) + Mid$(StrMsg, SMPos + 1): SMPos = SMPos - 1

For I = 0 To TokuMx '特殊文字
RR = Len(Toku(I, 0))
If SMPos > RR - 1 Then
If Mid$(StrMsg, SMPos - RR + 1, RR) = Toku(I, 0) Then
RR1 = 0: For U = SMPos - RR To SMPos - 1: RR1 = RR1 - StrMsgToku(U): Next U
If RR1 <= 0 Then
Ch = (Right$(StrMsgOrg(SMPos - RR), 1) = " "): If Ch Then StrMsgOrg(SMPos - RR) = Left$(StrMsgOrg(SMPos - RR), Len(StrMsgOrg(SMPos - RR)) - 1)
For U = RR - 1 + Ch To 1 Step -1: StrMsgOrg(SMPos - RR) = StrMsgOrg(SMPos - RR) + Left$(StrMsgOrg(SMPos - U), Len(StrMsgOrg(SMPos - U)) + (Right$(StrMsgOrg(SMPos - U), 1) = " ")): U = U + (Right$(StrMsgOrg(SMPos - U), 1) = " "): Next U: StrMsgToku(SMPos - RR) = (RR > 1 Or Len(Toku(I, 1)) > 1)
RR1 = Len(Toku(I, 1)) - Len(Toku(I, 0)) '変換前と変換後の文字サイズを比較
If RR1 < 0 Then For U = SMPos + RR1 To 255 + RR1: StrMsgOrg(U) = StrMsgOrg(U - RR1): StrMsgToku(U) = StrMsgToku(U - RR1): Next U: For U = 256 + RR1 To 255: StrMsgOrg(U) = "": StrMsgToku(U) = False: Next U
If RR1 > 0 Then For U = 255 To SMPos + RR1 Step -1: StrMsgOrg(U) = StrMsgOrg(U - RR1): StrMsgToku(U) = StrMsgToku(U - RR1): Next U
If Len(Toku(I, 1)) > 1 Then '特殊文字が2字以上
StrMsgOrg(SMPos - RR) = StrMsgOrg(SMPos - RR) + " ": StrMsgToku(SMPos - RR) = True
For U = SMPos - RR + 1 To SMPos - RR - 2 + Len(Toku(I, 1)): StrMsgOrg(U) = Right$(StrMsgOrg(SMPos - RR), 2): StrMsgToku(U) = True: Next U: StrMsgOrg(U) = Mid$(StrMsgOrg(SMPos - RR), Len(StrMsgOrg(SMPos - RR)) - 1, 1): StrMsgToku(U) = True
End If
StrMsg = Left$(StrMsg, SMPos - RR) + Toku(I, 1) + Mid$(StrMsg, SMPos + 1): SMPos = SMPos - RR + Len(Toku(I, 1))
End If
End If
End If
Next I

SMPosLim
If Len(StrMsg) <= SMPosL Then SMTp = True: Sou(1).Pan = Pan: Sou(1).On = True Else StrMsg = StrMsgUD: SMPos = SMPosUD: For I = 0 To 255: StrMsgOrg(I) = StrMsgOrgUD(I): StrMsgToku(I) = StrMsgTokuUD(I): Next I

Else
If Asc(KeyChar) = 8 And SMPos > 0 Then 'バックスペース
RR1 = 0: LpFin = True
While SMPos - 2 - RR1 >= 0 And LpFin
If Right$(StrMsgOrg(SMPos - 2 - RR1), 1) = " " Then RR1 = RR1 + 1 Else LpFin = False
Wend
For I = SMPos - 1 - RR1 To 254 - RR1: StrMsgOrg(I) = StrMsgOrg(I + 1 + RR1): StrMsgToku(I) = StrMsgToku(I + 1 + RR1): Next I: For I = 255 - RR1 To 255: StrMsgOrg(I) = "": StrMsgToku(I) = False: Next I: StrMsg = Left$(StrMsg, SMPos - 1 - RR1) + Mid$(StrMsg, SMPos + 1)
SMPos = SMPos - 1 - RR1
SMdl = True: Sou(12).Pan = Pan: Sou(12).On = True
End If
If Asc(KeyChar) = 127 And SMPos < Len(StrMsg) Then 'デリート
RR1 = 0: While Right$(StrMsgOrg(SMPos + RR1), 1) = " ": RR1 = RR1 + 1: Wend
For I = SMPos To 254 - RR1: StrMsgOrg(I) = StrMsgOrg(I + 1 + RR1): StrMsgToku(I) = StrMsgToku(I + 1 + RR1): Next I: For I = 255 - RR1 To 255: StrMsgOrg(I) = "": StrMsgToku(I) = False: Next I: StrMsg = Left$(StrMsg, SMPos) + Mid$(StrMsg, SMPos + 2 + RR1)
SMdl = True: Sou(12).Pan = Pan: Sou(12).On = True
End If
If Asc(KeyChar) = 29 And SMPos > 0 Then SMPos = SMPos - 1: SMMv = True: Sou(5).Pan = Pan: Sou(5).On = True: If SMPos > 0 Then While SMPos > 0 And Right$(StrMsgOrg(SMPos - 1 - (SMPos <= 0)), 1) = " ": SMPos = SMPos - 1: Wend
If Asc(KeyChar) = 28 And SMPos < Len(StrMsg) Then SMPos = SMPos + 1: SMMv = True: Sou(5).Pan = Pan: Sou(5).On = True: While Right$(StrMsgOrg(SMPos - 1 - (SMPos <= 0)), 1) = " ": SMPos = SMPos + 1: Wend
If Asc(KeyChar) = 30 And SMPos > 0 Then SMPos = 0: SMMv = True: Sou(5).Pan = Pan: Sou(5).On = True
If Asc(KeyChar) = 31 And SMPos < Len(StrMsg) Then SMPos = Len(StrMsg): SMMv = True: Sou(5).Pan = Pan: Sou(5).On = True
End If
If Asc(KeyChar) = 32 Then 'スペース
StrMsgSel(0) = XStrConv(StrMsg, vbKatakana)
StrMsgSel(2) = StrMsg
StrMsgSel(1) = ""
For I = 0 To 255
If I = 0 Then StrMsgSel(1) = StrMsgSel(1) + Left$(StrMsgOrg(I), Len(StrMsgOrg(I)) + (Right$(StrMsgOrg(I), 1) = " "))
If I > 0 Then If Right$(StrMsgOrg(I - 1), 1) <> " " Then StrMsgSel(1) = StrMsgSel(1) + Left$(StrMsgOrg(I), Len(StrMsgOrg(I)) + (Right$(StrMsgOrg(I), 1) = " "))
Next I
SMSel = 0: StrMsg = StrMsgSel(SMSel): SMTp = True: Sou(1).Pan = Pan: Sou(1).On = True
End If
If Asc(KeyChar) = 13 Then 'リターン
StrMsgB = Left$(StrMsgB, SMBPos) + StrMsg + Mid$(StrMsgB, SMBPos + 1): SMBPos = SMBPos + Len(StrMsg): StrMsg = "": SMPos = 0: For I = 0 To 255: StrMsgOrg(I) = "": StrMsgToku(I) = False: Next I: SMSel = -2: SMTp = True: Sou(1).Pan = Pan: Sou(1).On = True
SMBPosLim StrMsgB
If Len(StrMsgB) > SMBPosL Then StrMsgB = Left$(StrMsgB, SMBPosL): If SMBPos > SMBPosL Then SMBPos = SMBPosL
End If
If Asc(KeyChar) = 27 Then StrMsg = "": SMPos = 0: For I = 0 To 255: StrMsgOrg(I) = "": StrMsgToku(I) = False: Next I: SMSel = -2: SMdl = True: Sou(12).Pan = Pan: Sou(12).On = True
If StrMsg = "" Then SMSel = -2: SMSell = SMSel
End If

Wend

If StrMsgB <> StrMsgBB Or StrMsg <> StrMsgg Or SMSel <> SMSell Or SMPos <> SMPoss Or SMBPos <> SMBPoss Then
StrMsgDraw 0
SMXY
End If
'If StrMsgB <> StrMsgBB Or StrMsg <> StrMsgg Or SMSel <> SMSell Then
'StrMsgDraw 0
'SMXY
'Else
'If SMPos <> SMPoss Or SMBPos <> SMBPoss Then SMXY
'End If

Exit Sub

CE:
'Beep
End Sub

Sub StrMsgDraw(ByVal P As Long) 'テキストメッセージ表示
On Error GoTo CE
Dim RRR As String, RR As Long, RR1 As Long, RR11 As Long, RR2 As Long, RR3 As Long
StrSf(P).BltColorFill RC0, 0
If P = 0 Then '自分
'変換部分前
RRR = Left$(StrMsgB, SMBPos): RR1 = 0: RR2 = 0: RR3 = 0
While Len(RRR) > 0 And RR1 < 7
RR = 0
While RR < Len(RRR) And Picture1.TextWidth(Left$(RRR, RR + 1)) <= 136 - (InStr(EdRule, Mid$(RRR, RR + 1, 1)) > 0) * Picture1.TextWidth(Mid$(RRR, RR + 1, 1)): RR = RR + 1: Wend
RR2 = Picture1.TextWidth(Left$(RRR, RR)): RR3 = RR2
If RR > 0 Then
If RR1 < 6 Then
StrSf(P).SetForeColor RGB(0, 0, 128)
StrSf(P).DrawText 5, RR1 * 24 + 2, Left$(RRR, RR), False
StrSf(P).SetForeColor RGB(255, 255, 255)
StrSf(P).DrawText 4, RR1 * 24, Left$(RRR, RR), False
StrSf(P).SetForeColor ClLst(1)
StrSf(P).DrawText 4, RR1 * 24 + 1, Left$(RRR, RR), False
End If
RRR = Mid$(RRR, RR + 1)
End If
RR1 = RR1 + 1
Wend
'変換部分
RRR = StrMsg: RR1 = RR1 + (RR1 > 0): RR11 = RR1
While Len(RRR) > 0 And RR1 < 7
RR = 0
While RR < Len(RRR) And Picture1.TextWidth(Left$(RRR, RR + 1)) <= 136 - RR2 - (InStr(EdRule, Mid$(RRR, RR + 1, 1)) > 0) * Picture1.TextWidth(Mid$(RRR, RR + 1, 1)): RR = RR + 1: Wend
RR3 = RR2 + Picture1.TextWidth(Left$(RRR, RR))
If RR > 0 Then
If RR1 < 6 Then
StrSf(P).SetForeColor RGB(255, 0, 255)
StrSf(P).SetFillColor RGB(0, 0, 192)
StrSf(P).DrawBox RR2 + 3, RR1 * 24, RR3 + 4, RR1 * 24 + Picture1.TextHeight(Left$(RRR, RR)) + 6
StrSf(P).SetForeColor RGB(0, 0, 128)
StrSf(P).DrawText RR2 + 5, RR1 * 24 + 4, Left$(RRR, RR), False
StrSf(P).SetForeColor RGB(255, 255, 255)
StrSf(P).DrawText RR2 + 4, RR1 * 24 + 2, Left$(RRR, RR), False
StrSf(P).SetForeColor ClLst(1 - (SMSel >= 0) * (SMSel + 1))
StrSf(P).DrawText RR2 + 4, RR1 * 24 + 3, Left$(RRR, RR), False
End If
RRR = Mid$(RRR, RR + 1)
End If
RR1 = RR1 + 1: RR2 = 0
Wend
'変換部分後
RRR = Mid$(StrMsgB, SMBPos + 1): RR1 = RR1 + (RR1 > RR11)
While Len(RRR) > 0 And RR1 < 7
RR = 0
While RR < Len(RRR) And Picture1.TextWidth(Left$(RRR, RR + 1)) <= 136 - RR3 - (InStr(EdRule, Mid$(RRR, RR + 1, 1)) > 0) * Picture1.TextWidth(Mid$(RRR, RR + 1, 1)): RR = RR + 1: Wend
If RR > 0 Then
If RR1 < 6 Then
StrSf(P).SetForeColor RGB(0, 0, 128)
StrSf(P).DrawText RR3 + 5, RR1 * 24 + 2, Left$(RRR, RR), False
StrSf(P).SetForeColor RGB(255, 255, 255)
StrSf(P).DrawText RR3 + 4, RR1 * 24, Left$(RRR, RR), False
StrSf(P).SetForeColor ClLst(1)
StrSf(P).DrawText RR3 + 4, RR1 * 24 + 1, Left$(RRR, RR), False
End If
RRR = Mid$(RRR, RR + 1)
End If
RR1 = RR1 + 1: RR3 = 0
Wend
Else '対戦相手
RRR = StrMsgJ: RR1 = 0
While Len(RRR) > 0 And RR1 < 6
RR = 0
While RR < Len(RRR) And Picture1.TextWidth(Left$(RRR, RR + 1)) <= 136 - (InStr(EdRule, Mid$(RRR, RR + 1, 1)) > 0) * Picture1.TextWidth(Mid$(RRR, RR + 1, 1)): RR = RR + 1: Wend
If RR > 0 Then
StrSf(P).SetForeColor RGB(0, 0, 128)
StrSf(P).DrawText 5, RR1 * 24 + 2, Left$(RRR, RR), False
StrSf(P).SetForeColor RGB(255, 255, 255)
StrSf(P).DrawText 4, RR1 * 24, Left$(RRR, RR), False
StrSf(P).SetForeColor ClLst(1)
StrSf(P).DrawText 4, RR1 * 24 + 1, Left$(RRR, RR), False
RRR = Mid$(RRR, RR + 1)
End If
RR1 = RR1 + 1
Wend
End If
Exit Sub

CE:
'Beep
End Sub

Sub SMBPosLim(Text As String) 'メッセージ文字制限
On Error GoTo CE
Dim RRR As String, RR As Long, RR1 As Long
SMBPosL = 0
RRR = Text: RR1 = 0
While Len(RRR) > 0 And RR1 < 6
RR = 0
While RR < Len(RRR) And Picture1.TextWidth(Left$(RRR, RR + 1)) <= 136 - (InStr(EdRule, Mid$(RRR, RR + 1, 1)) > 0) * Picture1.TextWidth(Mid$(RRR, RR + 1, 1)): RR = RR + 1: Wend
If RR > 0 Then
RRR = Mid$(RRR, RR + 1)
SMBPosL = SMBPosL + RR
End If
RR1 = RR1 + 1
Wend
Exit Sub

CE:
'Beep
End Sub

Sub SMPosLim() 'メッセージ（変換）文字制限
On Error GoTo CE
Dim RRR As String, RR As Long, RR1 As Long, RR2 As Long
SMPosL = 0
'変換部分前
RRR = Left$(StrMsgB, SMBPos): RR1 = 0: RR2 = 0
While Len(RRR) > 0 And RR1 < 6
RR = 0
While RR < Len(RRR) And Picture1.TextWidth(Left$(RRR, RR + 1)) <= 136 - (InStr(EdRule, Mid$(RRR, RR + 1, 1)) > 0) * Picture1.TextWidth(Mid$(RRR, RR + 1, 1)): RR = RR + 1: Wend
RR2 = Picture1.TextWidth(Left$(RRR, RR))
If RR > 0 Then
RRR = Mid$(RRR, RR + 1)
End If
RR1 = RR1 + 1
Wend
'変換部分
RRR = StrMsg: RR1 = RR1 + (RR1 > 0)
While Len(RRR) > 0 And RR1 < 6
RR = 0
While RR < Len(RRR) And Picture1.TextWidth(Left$(RRR, RR + 1)) <= 136 - RR2 - (InStr(EdRule, Mid$(RRR, RR + 1, 1)) > 0) * Picture1.TextWidth(Mid$(RRR, RR + 1, 1)): RR = RR + 1: Wend
If RR > 0 Then
RRR = Mid$(RRR, RR + 1)
SMPosL = SMPosL + RR
End If
RR1 = RR1 + 1: RR2 = 0
Wend
Exit Sub

CE:
'Beep
End Sub

Sub SMXY() 'メッセージ文字カーソル指定
On Error GoTo CE
Dim RRR As String, RR As Long, RR1 As Long, RR2 As Long, RR3 As Long
Dim SMBPosC As Long, SMPosC As Long
SMX = 4: SMY = 0: SMBPosC = SMBPos: SMPosC = SMPos
'変換部分前
RRR = Left$(StrMsgB, SMBPos): RR1 = 0: RR2 = 0
While Len(RRR) > 0 And RR1 < 6
RR = 0
While RR < Len(RRR) And Picture1.TextWidth(Left$(RRR, RR + 1)) <= 136 - (InStr(EdRule, Mid$(RRR, RR + 1, 1)) > 0) * Picture1.TextWidth(Mid$(RRR, RR + 1, 1)): RR = RR + 1: Wend
RR2 = Picture1.TextWidth(Left$(RRR, RR))
If RR > 0 Then
If SMBPosC >= 1 Then SMX = 4 + Picture1.TextWidth(Left$(RRR, SMBPosC)): SMY = RR1 * 24
SMBPosC = SMBPosC - RR
RRR = Mid$(RRR, RR + 1)
End If
RR1 = RR1 + 1
Wend
'変換部分
RRR = StrMsg: RR1 = RR1 + (RR1 > 0): RR = 0
If Len(RRR) > 0 Then SMY = SMY + 2
While Len(RRR) > 0 And RR1 < 6
RR3 = RR: RR = 0
While RR < Len(RRR) And Picture1.TextWidth(Left$(RRR, RR + 1)) <= 136 - RR2 - (InStr(EdRule, Mid$(RRR, RR + 1, 1)) > 0) * Picture1.TextWidth(Mid$(RRR, RR + 1, 1)): RR = RR + 1: Wend
If RR > 0 Then
If SMPosC >= -(RR3 > 0) Then SMX = RR2 + 4 + Picture1.TextWidth(Left$(RRR, SMPosC)): SMY = 2 + RR1 * 24
SMPosC = SMPosC - RR
RRR = Mid$(RRR, RR + 1)
End If
RR1 = RR1 + 1: RR2 = 0
Wend
Exit Sub

CE:
'Beep
End Sub

Sub SouCreate() 'サウンドの作成
On Error GoTo CE
Dim I As Long
Dim Er As Boolean
'サウンドの設定
Set DS = DX.DirectSoundCreate("")
DS.SetCooperativeLevel Form1.hWnd, DSSCL_NORMAL
'サウンドバッファの作成
SBD.lFlags = DSBCAPS_STATIC Or DSBCAPS_STICKYFOCUS Or DSBCAPS_CTRLVOLUME Or DSBCAPS_CTRLFREQUENCY Or DSBCAPS_CTRLPAN
For I = 0 To 14
Er = False
On Error GoTo CSou
Set SB(I) = DS.CreateSoundBufferFromFile(EffDir + "S" + Mid$(Str$(I), 2) + ".WAV", SBD, WF)
Set SSB(I) = DS.CreateSoundBufferFromFile(EffDir + "S" + Mid$(Str$(I), 2) + ".WAV", SBD, WF)
On Error GoTo CE
If Not Er Then Sou(I).AB = True Else Sou(I).AB = False
Next I
Exit Sub

CSou:
Er = True
Resume Next

CE:
'Beep
End Sub

Sub SfCreate() 'サーフェイス作成
On Error GoTo CE
Dim I As Long, U As Long, RRR As String
Dim FSz As Currency

'パレットの設定
If Not WM Then
Set Pal = DD.LoadPaletteFromBitmap(EffDir + "PAL.BMP")
Pal.GetEntries 0, 256, Pa
For I = 0 To 127: MPa(I).red = 0: MPa(I).green = 0: MPa(I).blue = 0: MPa(I).flags = 0: Next I
For I = 128 To 255: MPa(I) = Pa(I): Next I
Set MPal = DD.CreatePalette(DDPCAPS_ALLOW256 Or DDPCAPS_8BIT, MPa)
End If

'プライマリの作成
If WM Then
SD.lFlags = DDSD_CAPS
SD.ddsCaps.lCaps = DDSCAPS_PRIMARYSURFACE Or -WM * DDSCAPS_SYSTEMMEMORY
Set PrSf = DD.CreateSurface(SD)
Set PrC = DD.CreateClipper(0)
PrC.SetHWnd Picture1.hWnd
PrSf.SetClipper PrC
Else
SD.lFlags = DDSD_CAPS Or DDSD_BACKBUFFERCOUNT
SD.ddsCaps.lCaps = DDSCAPS_FLIP Or DDSCAPS_COMPLEX Or DDSCAPS_PRIMARYSURFACE Or -WM * DDSCAPS_SYSTEMMEMORY
SD.lBackBufferCount = 1
Set PrSf = DD.CreateSurface(SD)
PrSf.SetPalette MPal
PrSf.BltColorFill RC0, 0
End If

'バックバッファのアタッチ
If WM Then
SD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
SD.lWidth = 640: SD.lHeight = 480
SD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
Set BBSf = DD.CreateSurface(SD)
BBSf.BltColorFill RC0, 0
Else
CA.lCaps = DDSCAPS_BACKBUFFER
Set BBSf = PrSf.GetAttachedSurface(CA)
If Not WM Then BBSf.SetPalette MPal
BBSf.BltColorFill RC0, 0
End If

'背景セット（オフスクリーン）の作成
If BGMemm Then
BGCreate
End If

'フォント（オフスクリーン）の作成
SD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
SD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
SD.lWidth = 640: SD.lHeight = 320
If XDir(EffDir + "FONTS.BMP", vbHidden) <> vbNullString$ Then
Set FoSf = DD.CreateSurfaceFromFile(EffDir + "FONTS.BMP", SD)
Else
Set FoSf = DD.CreateSurface(SD)
FoSf.BltColorFill RC0, 0
FSz = Picture1.Font.Size
Picture1.Font.Size = 12
FoSf.SetFont Picture1.Font
For I = 0 To 9
FoSf.setDrawWidth 1
For U = 0 To 68
RRR = Chr$(32 + U - (U > 64) * 26)
FoSf.SetForeColor RGB(0, 0, 192)
FoSf.DrawText (U Mod 40) * 16 + 9 - Int(Picture1.TextWidth(RRR) / 2 + 0.5), I * 32 + Int(U / 40) * 16, RRR, False
FoSf.SetForeColor ClLst(I)
FoSf.DrawText (U Mod 40) * 16 + 8 - Int(Picture1.TextWidth(RRR) / 2 + 0.5), I * 32 + Int(U / 40) * 16, RRR, False
Next U
FoSf.SetForeColor RGB(255, 255, 255)
FoSf.SetFillColor ClLst(I)
FoSf.DrawBox 464, I * 32 + 21, 480, I * 32 + 27
FoSf.DrawBox 469, I * 32 + 16, 475, I * 32 + 32
FoSf.SetForeColor ClLst(I)
FoSf.DrawBox 469, I * 32 + 22, 475, I * 32 + 26
FoSf.DrawBox 470, I * 32 + 21, 474, I * 32 + 27
FoSf.SetForeColor RGB(255, 255, 255)
FoSf.DrawEllipse 480, I * 32 + 16, 496, I * 32 + 32
FoSf.setDrawWidth 2
FoSf.SetForeColor RGB(0, 0, 192)
FoSf.SetFillColor RGB(0, 0, 0)
FoSf.DrawEllipse 498, I * 32 + 19, 503, I * 32 + 31
FoSf.DrawLine 505, I * 32 + 19, 505, I * 32 + 30
FoSf.DrawLine 510, I * 32 + 23, 505, I * 32 + 27
FoSf.DrawLine 507, I * 32 + 26, 510, I * 32 + 30
FoSf.SetForeColor ClLst(I)
FoSf.DrawEllipse 497, I * 32 + 19, 502, I * 32 + 31
FoSf.DrawLine 504, I * 32 + 19, 504, I * 32 + 30
FoSf.DrawLine 509, I * 32 + 23, 504, I * 32 + 27
FoSf.DrawLine 506, I * 32 + 26, 509, I * 32 + 30
FoSf.setDrawWidth 1
FoSf.SetFillColor ClLst(I)
FoSf.DrawBox 519, I * 32 + 16, 521, I * 32 + 32
FoSf.DrawBox 517, I * 32 + 16, 523, I * 32 + 17
FoSf.DrawBox 518, I * 32 + 17, 522, I * 32 + 18
FoSf.DrawBox 518, I * 32 + 30, 522, I * 32 + 31
FoSf.DrawBox 517, I * 32 + 31, 523, I * 32 + 32
Next I
Picture1.Font.Size = FSz
End If
CK.low = 0: CK.high = 0
FoSf.SetColorKey DDCKEY_SRCBLT, CK
If Not WM Then FoSf.SetPalette MPal

'フォントの作成
BBSf.SetFont Picture1.Font

'スプライト（オフスクリーン）の作成
SD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
SD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
SD.lWidth = 384: SD.lHeight = 232
If XDir(EffDir + "SP.BMP", vbHidden) <> vbNullString$ Then
Set SpSf = DD.CreateSurfaceFromFile(EffDir + "SP.BMP", SD)
Else
Set SpSf = DD.CreateSurface(SD)
SpSf.BltColorFill RC0, 0
SpSf.setDrawWidth 1
SpSf.SetForeColor RGB(128, 0, 0): SpSf.SetFillColor RGB(128, 0, 0): SpSf.DrawEllipse 0, 0, 64, 64
SpSf.SetForeColor RGB(192, 0, 0): SpSf.SetFillColor RGB(192, 0, 0): SpSf.DrawEllipse 1, 1, 62, 62
SpSf.SetForeColor RGB(255, 0, 0): SpSf.SetFillColor RGB(255, 0, 0): SpSf.DrawEllipse 2, 2, 58, 58
SpSf.SetForeColor RGB(255, 64, 64): SpSf.SetFillColor RGB(255, 64, 64): SpSf.DrawEllipse 4, 4, 52, 52
SpSf.SetForeColor RGB(255, 128, 128): SpSf.SetFillColor RGB(255, 128, 128): SpSf.DrawEllipse 12, 11, 22, 21
SpSf.SetForeColor RGB(255, 255, 255): SpSf.SetFillColor RGB(255, 255, 255): SpSf.DrawEllipse 13, 12, 21, 20
SpSf.SetForeColor RGB(64, 64, 64): SpSf.SetFillColor RGB(64, 64, 64): SpSf.DrawEllipse 68, 4, 124, 60
SpSf.SetForeColor RGB(96, 96, 96): SpSf.SetFillColor RGB(96, 96, 96): SpSf.DrawEllipse 69, 5, 123, 59
SpSf.SetForeColor RGB(128, 128, 128): SpSf.SetFillColor RGB(128, 128, 128): SpSf.DrawEllipse 71, 7, 121, 57
SpSf.SetForeColor RGB(192, 192, 192): SpSf.SetFillColor RGB(192, 192, 192): SpSf.DrawEllipse 74, 10, 118, 54
SpSf.SetForeColor RGB(128, 128, 128): SpSf.SetFillColor RGB(128, 128, 128): SpSf.DrawEllipse 78, 14, 114, 50
SpSf.SetForeColor RGB(96, 96, 96): SpSf.SetFillColor RGB(96, 96, 96): SpSf.DrawEllipse 81, 17, 111, 47
SpSf.SetForeColor RGB(64, 64, 64): SpSf.SetFillColor RGB(64, 64, 64): SpSf.DrawEllipse 83, 19, 109, 45
SpSf.SetForeColor RGB(255, 128, 128): SpSf.SetFillColor RGB(255, 128, 128): SpSf.DrawEllipse 128, 48, 136, 56
SpSf.SetForeColor RGB(255, 255, 255): SpSf.SetFillColor RGB(255, 255, 255): SpSf.DrawEllipse 129, 49, 135, 55
SpSf.SetForeColor RGB(0, 64, 0): SpSf.SetFillColor RGB(0, 64, 0): SpSf.DrawEllipse 128, 0, 176, 48
SpSf.SetForeColor RGB(0, 128, 0): SpSf.SetFillColor RGB(0, 128, 0): SpSf.DrawEllipse 129, 1, 175, 47
SpSf.SetForeColor RGB(0, 192, 0): SpSf.SetFillColor RGB(0, 192, 0): SpSf.DrawEllipse 130, 2, 174, 46
SpSf.SetForeColor RGB(0, 255, 0): SpSf.SetFillColor RGB(0, 255, 0): SpSf.DrawEllipse 131, 3, 173, 45
SpSf.SetForeColor RGB(0, 128, 0): SpSf.SetFillColor RGB(0, 128, 0): SpSf.DrawEllipse 176, 0, 224, 48
SpSf.SetForeColor RGB(0, 192, 0): SpSf.SetFillColor RGB(0, 192, 0): SpSf.DrawEllipse 177, 1, 223, 47
SpSf.SetForeColor RGB(0, 255, 0): SpSf.SetFillColor RGB(0, 255, 0): SpSf.DrawEllipse 178, 2, 222, 46
SpSf.SetForeColor RGB(64, 255, 64): SpSf.SetFillColor RGB(64, 255, 64): SpSf.DrawEllipse 224, 0, 272, 48
SpSf.SetForeColor RGB(128, 255, 128): SpSf.SetFillColor RGB(128, 255, 128): SpSf.DrawEllipse 225, 1, 271, 47
SpSf.SetForeColor RGB(192, 255, 192): SpSf.SetFillColor RGB(192, 255, 192): SpSf.DrawEllipse 226, 2, 270, 46
SpSf.SetForeColor RGB(255, 255, 255): SpSf.SetFillColor RGB(255, 255, 255): SpSf.DrawEllipse 227, 3, 269, 45
SpSf.SetForeColor RGB(128, 255, 128): SpSf.SetFillColor RGB(128, 255, 128): SpSf.DrawEllipse 272, 0, 320, 48
SpSf.SetForeColor RGB(192, 255, 192): SpSf.SetFillColor RGB(192, 255, 192): SpSf.DrawEllipse 273, 1, 319, 47
SpSf.SetForeColor RGB(255, 255, 255): SpSf.SetFillColor RGB(255, 255, 255): SpSf.DrawEllipse 274, 2, 318, 46
SpSf.SetForeColor RGB(192, 255, 192): SpSf.SetFillColor RGB(192, 255, 192): SpSf.DrawEllipse 320, 0, 368, 48
SpSf.SetForeColor RGB(255, 255, 255): SpSf.SetFillColor RGB(255, 255, 255): SpSf.DrawEllipse 321, 1, 367, 47
SpSf.SetForeColor RGB(255, 128, 0): SpSf.SetFillColor RGB(255, 128, 0): SpSf.DrawEllipse 0, 64, 128, 192
SpSf.SetForeColor RGB(255, 255, 0): SpSf.SetFillColor RGB(255, 255, 0): SpSf.DrawEllipse 3, 67, 125, 189
SpSf.SetForeColor RGB(255, 255, 255): SpSf.SetFillColor RGB(255, 255, 255): SpSf.DrawEllipse 6, 70, 122, 186
SpSf.SetForeColor RGB(128, 255, 255): SpSf.SetFillColor RGB(128, 255, 255): SpSf.DrawEllipse 10, 74, 118, 182
SpSf.SetForeColor RGB(0, 255, 255): SpSf.SetFillColor RGB(0, 255, 255): SpSf.DrawEllipse 13, 77, 115, 179
SpSf.SetForeColor RGB(0, 0, 0): SpSf.SetFillColor RGB(0, 0, 0): SpSf.DrawEllipse 16, 80, 112, 176
SpSf.SetForeColor RGB(0, 255, 255): SpSf.SetFillColor RGB(0, 255, 255): SpSf.DrawEllipse 128, 64, 256, 192
SpSf.SetForeColor RGB(128, 255, 255): SpSf.SetFillColor RGB(128, 255, 255): SpSf.DrawEllipse 131, 67, 253, 189
SpSf.SetForeColor RGB(255, 255, 255): SpSf.SetFillColor RGB(255, 255, 255): SpSf.DrawEllipse 134, 70, 250, 186
SpSf.SetForeColor RGB(255, 255, 0): SpSf.SetFillColor RGB(255, 255, 0): SpSf.DrawEllipse 138, 74, 246, 182
SpSf.SetForeColor RGB(255, 128, 0): SpSf.SetFillColor RGB(255, 128, 0): SpSf.DrawEllipse 141, 77, 243, 179
SpSf.SetForeColor RGB(0, 0, 0): SpSf.SetFillColor RGB(0, 0, 0): SpSf.DrawEllipse 144, 80, 240, 176
SpSf.SetForeColor RGB(128, 0, 128): SpSf.SetFillColor RGB(128, 0, 128): SpSf.DrawEllipse 260, 68, 380, 188
SpSf.SetForeColor RGB(128, 128, 255): SpSf.SetFillColor RGB(128, 128, 255): SpSf.DrawEllipse 264, 72, 376, 184
SpSf.SetForeColor RGB(0, 0, 255): SpSf.SetFillColor RGB(0, 0, 255): SpSf.DrawEllipse 268, 76, 372, 180
SpSf.SetForeColor RGB(0, 0, 0): SpSf.SetFillColor RGB(0, 0, 0): SpSf.DrawEllipse 272, 80, 368, 176
For I = 0 To 7
SpSf.SetForeColor RGB(64, 64, 64): SpSf.SetFillColor RGB(64, 64, 64): SpSf.DrawBox I * 40, 192, I * 40 + 40, 232
SpSf.SetForeColor RGB(96, 96, 96): SpSf.SetFillColor RGB(96, 96, 96): SpSf.DrawBox I * 40 + 2, 194, I * 40 + 38, 230
SpSf.SetForeColor RGB(128, 128, 128): SpSf.SetFillColor RGB(128, 128, 128): SpSf.DrawBox I * 40 + 4, 196, I * 40 + 36, 228
SpSf.SetForeColor RGB(192, 192, 192): SpSf.SetFillColor RGB(192, 192, 192): SpSf.DrawBox I * 40 + 6, 198, I * 40 + 34, 226
SpSf.SetForeColor RGB(128, 128, 128): SpSf.SetFillColor RGB(128, 128, 128): SpSf.DrawBox I * 40 + 8, 200, I * 40 + 32, 224
SpSf.SetForeColor RGB(96, 96, 96): SpSf.SetFillColor RGB(96, 96, 96): SpSf.DrawBox I * 40 + 10, 202, I * 40 + 30, 222
SpSf.SetForeColor RGB(64, 64, 64): SpSf.SetFillColor RGB(64, 64, 64): SpSf.DrawBox I * 40 + 12, 204, I * 40 + 28, 220
Next I
End If
CK.low = 0: CK.high = 0
SpSf.SetColorKey DDCKEY_SRCBLT, CK
If Not WM Then SpSf.SetPalette MPal

Exit Sub

CE:
'Beep
End Sub

Sub BGCreate() '背景セット作成
On Error GoTo CE
Dim I As Long, U As Long
Dim RRR As String

For I = 0 To 14
RRR = EffDir + "BG" + Mid$(Str$(I), 2) + ".BMP"
If Not WM Then
If XDir(RRR, vbHidden) <> vbNullString$ Then
Set BSPal(I) = DD.LoadPaletteFromBitmap(RRR)
BSPal(I).GetEntries 0, 256, BGPa
Else
For U = 0 To 255: BGPa(U).red = 0: BGPa(U).green = 0: BGPa(U).blue = 0: Next U
Set BSPal(I) = DD.CreatePalette(DDPCAPS_ALLOW256 Or DDPCAPS_8BIT, BGPa)
End If
For U = 0 To 127: MPa(U) = BGPa(U): Next U
MPal.SetEntries 0, 256, MPa
End If
SD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
SD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or DDSCAPS_SYSTEMMEMORY
SD.lWidth = 640: SD.lHeight = 480
If XDir(RRR, vbHidden) <> vbNullString$ Then
Set BSSf(I) = DD.CreateSurfaceFromFile(RRR, SD)
Else
Set BSSf(I) = DD.CreateSurface(SD)
BSSf(I).BltColorFill RC0, 0
End If
If Not WM Then BSSf(I).SetPalette MPal
Next I
Exit Sub

CE:
'Beep
End Sub

Sub BGDestroy() '背景解放
On Error GoTo CE
Dim I As Long
For I = 14 To 0 Step -1: Set BSSf(I) = Nothing: Set BSPal(I) = Nothing: Next I
Exit Sub

CE:
'Beep
End Sub

Sub SfDestroy() 'サーフェイス解放
On Error GoTo CE
Dim I As Long
For I = 3 To 0 Step -1
If I < 2 Then Set StrSf(I) = Nothing
Set BFSf(I) = Nothing
Next I
Set BCSf = Nothing
Set BDSf = Nothing
Set BGSf = Nothing
Set SpSf = Nothing
Set FoSf = Nothing
BGDestroy
Set BBSf = Nothing
Set PrC = Nothing
Set PrSf = Nothing
Set MPal = Nothing
Set BGPal = Nothing
Set Pal = Nothing
Exit Sub

CE:
'Beep
End Sub

Sub BGInit() '背景初期化
On Error GoTo CE
Dim I As Long

FAC = -(BG >= 2) * Int(Rnd * 64)
If BG >= 2 Then
'（海）
SeaA = 0
For I = 0 To 29: Sea(I) = Rnd * 640: Next I
'（アルファベット）
With ABG
.R = Rnd * 360: .R2 = Rnd * 360: .X = Rnd * 640: .Y = Rnd * 480
End With
'（スターダスト）
For I = 0 To 59: StD(I) = Rnd * 152: Next I
'（キラキラ振り子）
FSX = Rnd * 640
FC = 140: FX = 0: FY = 0
KrI = 0
For I = 0 To 35
With Kr(I)
.V = False
End With
Next I
'（オーロラ）
Crs = Rnd * 360
CrsF = Rnd * 640
CrsFF = Rnd * 360
'（紫空）
PSkD(0) = 0: PSkD(1) = 24: PSkD(2) = 72: PSkD(3) = 144
PSkD(4) = 240: PSkD(5) = 336: PSkD(6) = 408: PSkD(7) = 456
PSkS1(0) = 0: PSkS2(0) = 24
PSkS1(1) = 48: PSkS2(1) = 48
PSkS1(2) = 144: PSkS2(2) = 72
PSkS1(3) = 288: PSkS2(3) = 96
PSkS1(4) = 288: PSkS2(4) = 96
PSkS1(5) = 144: PSkS2(5) = 72
PSkS1(6) = 48: PSkS2(6) = 48
PSkS1(7) = 0: PSkS2(7) = 24
PSkP = Rnd
'（カーペット）
With CptG
.X = Rnd * 640: .Y = Rnd * 480
End With
'（ダイヤ）
With Dia
DiaR = Rnd * 360: .X = Rnd * 640: .Y = Rnd * 480
End With
'（レーザー）
LsrSY = Rnd * 240
For I = 0 To 59
If (I Mod 3) = 0 Then
With Lsr(I)
.C = (Int(I / 2) Mod 2): .X = Rnd * 704 - 32: .Y = Rnd * 544 - 32: .R = I * 2 + 1: .RR = 1 - (I Mod 2) * 2
End With
Else
Lsr(I) = Lsr(Int(I / 3) * 3)
End If
Next I
'（水晶）
CrysSY = Rnd * 240
For I = 0 To 15
With Crys(I)
.A = Int(Rnd * 20): .AA = (1 + Int(Rnd * 2)) * (1 + (TrM = 2) * (I Mod 2) * 2): .X = Int(Rnd * 800) - 80: .Y = Rnd * 640 - 80: .YY = .AA * 4 + Rnd * 4 * (1 + (TrM = 2) * (I Mod 2) * 2)
End With
Next I
'レベル50-199（超空間）
Spa = 0
'レベル200（電流）
With EdG
.X = Int(Rnd * 640): .Y = Int(Rnd * 480)
End With
'レベルMAX
With LdG
.X = 0: .Y = 0
End With

End If
Exit Sub

CE:
'Beep
End Sub

Sub BGD(ByVal TrM As Long, ByVal Lev As Long) '背景アニメ
On Error GoTo CE
Dim I As Long, R As Single, R1 As Single, R2 As Single

If BG >= 1 Then
If BG >= 2 Then FAC = FAC + 1 + (FAC >= 63) * 64
If FOC < 19 Or FIC >= 10 Then
If BG >= 2 Then
Select Case Stt
Case 0 '（海）
R = Sea(0)
If R > 640 Then R = R - 640 Else If R < 0 Then R = R + 640
With Src
.Left = R: .Top = 0: .Right = 640: .Bottom = .Top + 240
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = R: .Bottom = .Top + 240
End With
If Not FS Then BBSf.BltFast 640 - R, 0, BGSf, Src, DDBLTFAST_WAIT
For I = 0 To 29
If TrM < 2 Then
R = Sea(I) + Sin((SeaA + 1.28 ^ (32 - I)) * Rg) * (1.15 ^ I) * (1 + (TrM = 0) * 0.4)
If R > 640 Then R = R - 640 Else If R < 0 Then R = R + 640
With Src
.Left = R: .Top = 240 + I * 8: .Right = 640: .Bottom = .Top + 8
End With
If Not FS Then BBSf.BltFast 0, 240 + I * 8, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 240 + I * 8: .Right = R: .Bottom = .Top + 8
End With
If Not FS Then BBSf.BltFast 640 - R, 240 + I * 8, BGSf, Src, DDBLTFAST_WAIT
Else
R = Sin((SeaA + 1.3 ^ (32 - I)) * Rg) * (1.15 ^ I - 1)
With Src
.Left = Sea(I): .Top = 240 + I * 5 + R: .Right = 640: .Bottom = .Top + 8
End With
If Not FS Then BBSf.BltFast 0, 240 + I * 8, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 240 + I * 5 + R: .Right = Sea(I): .Bottom = .Top + 8
End With
If Not FS Then BBSf.BltFast 640 - Sea(I), 240 + I * 8, BGSf, Src, DDBLTFAST_WAIT
End If
Sea(I) = Sea(I) + (I - 2.5) * (0.03 - (TrM > 0) * 0.08)
If Sea(I) > 640 Then Sea(I) = Sea(I) - 640 Else If Sea(I) < 0 Then Sea(I) = Sea(I) + 640
Next I
SeaA = SeaA + 2 + (SeaA >= 358) * 360
Case 1 '（アルファベット）
With ABG
.X = .X + Sin(.R * Rg) * (3 + TrM * 5): .Y = .Y - Cos(.R * Rg) * (3 + TrM * 5)
If .X < 0 Then .X = .X + 640
If .X >= 640 Then .X = .X - 640
If .Y < 0 Then .Y = .Y + 480
If .Y >= 480 Then .Y = .Y - 480
.R = .R + 0.06 + TrM * 0.02 - (TrM >= 2) * (1 + Sin(.R2 * Rg)) * 0.6
If .R >= 360 Then .R = .R - 360
If .R < 0 Then .R = .R + 360
If TrM >= 2 Then .R2 = .R2 + 2.2: If .R2 >= 360 Then .R2 = .R2 - 360
End With
With Src
.Left = ABG.X: .Top = ABG.Y: .Right = 640: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = ABG.Y: .Right = ABG.X: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 640 - ABG.X, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = ABG.X: .Top = 0: .Right = 640: .Bottom = ABG.Y
End With
If Not FS Then BBSf.BltFast 0, 480 - ABG.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = ABG.X: .Bottom = ABG.Y
End With
If Not FS Then BBSf.BltFast 640 - ABG.X, 480 - ABG.Y, BGSf, Src, DDBLTFAST_WAIT
'For I = 0 To 77
'With A(I)
'If .X < 100 Then .XXX = 0.1
'If .X > 524 Then .XXX = -0.1
'If .Y < 100 Then .YYY = 0.1
'If .Y > 364 Then .YYY = -0.1
'.XX = .XX + .XXX: .YY = .YY + .YYY
'If Abs(.XX) > .MXX Then .XX = .MXX * Sgn(.XX): .XXX = 0
'If Abs(.YY) > .MYY Then .YY = .MYY * Sgn(.YY): .YYY = 0
'.X = .X + .XX: .Y = .Y + .YY
'End With
'With Src
'.Left = 256 + (I Mod 8) * 16: .Top = Int(I / 8) * 16: .Right = .Left + 16: .Bottom = .Top + 16
'End With
'BltClip A(I).X, A(I).Y, SpSf, Src
'Next I

Case 8 '（スターダスト）
For I = 0 To 59
StD(I) = StD(I) + (I * 0.1 - 2.95) * (1 + (TrM >= 2) * 2)
If StD(I) < 0 Then StD(I) = StD(I) + 152
If StD(I) >= 152 Then StD(I) = StD(I) - 152
With Src
.Left = 0: .Top = (I Mod 3) * 160 + StD(I): .Right = .Left + 640: .Bottom = .Top + 8
End With
If Not FS Then BBSf.BltFast 0, I * 8, BGSf, Src, DDBLTFAST_WAIT
Next I

'With Src
'.Left = 0: .Top = 0: .Right = .Left + 640: .Bottom = .Top + 480
'End With
'If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
Case 3 '（キラキラ振り子）
FSX = FSX - 5: If FSX < 0 Then FSX = FSX + 640
With Src
.Left = FSX: .Top = 0: .Right = 640: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
If Not FS Then BBSf.BltFast 0, 240, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = FSX: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 640 - FSX, 0, BGSf, Src, DDBLTFAST_WAIT
If Not FS Then BBSf.BltFast 640 - FSX, 240, BGSf, Src, DDBLTFAST_WAIT
For I = 0 To 35
With Kr(I)
If .V Then
.A = .A + 1 + (.A >= 5) * 6
.YY = .YY - 0.1
.X = .X + .XX: .Y = .Y + .YY
End If
End With
Next I
For I = 0 To 35
If Kr(I).V Then
With Src
.Left = 16 + Kr(I).A * 16: .Top = 368: .Right = .Left + 16: .Bottom = .Top + 16
End With
BltClip Kr(I).X + (Rnd * 20 - 10) * TrM, Kr(I).Y + (Rnd * 20 - 10) * TrM, BGSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next I
FC = FC + 2 + (FC >= 358) * 360
FX = Sin(Sin(FC * Rg) * 35 * Rg): FY = Cos(Sin(FC * Rg) * 35 * Rg)
KrI = KrI + 1 + (KrI = 35) * 36
With Kr(KrI)
.V = True: .A = 0: R1 = Rnd * 360: R2 = Rnd * 64
.X = 312 + FX * 540 + Sin(R1 * Rg) * R2: .Y = -188 + FY * 540 + Cos(R1 * Rg) * R2
.XX = -4 + Rnd * 8: .YY = -4 + Rnd * 8
End With
For I = 11 To 46 + (TrM < 2) * 4
If I < 31 Or I > 37 Then
With Src
.Left = 0: .Top = 368: .Right = .Left + 16: .Bottom = .Top + 16
End With
BltClip 312 + FX * (I * 16 + (TrM = 2) * (FC Mod 15)), -188 + FY * (I * 16 + (TrM = 2) * (FC Mod 15)), BGSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next I
With Src
.Left = 0: .Top = 240: .Right = .Left + 128: .Bottom = .Top + 128
End With
BltClip 256 + FX * 540, -244 + FY * 540, BGSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Case 4 '（クリスタル）
For I = 0 To 79
R = CrsF + Sin((Crs + I * 17) * Rg) * 7 * (TrM + 1)
If R > 640 Then R = R - 640 Else If R < 0 Then R = R + 640
With Src
.Left = R: .Top = I * 8: .Right = 640: .Bottom = .Top + 8
End With
If Not FS Then BBSf.BltFast 0, I * 8, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = I * 8: .Right = R: .Bottom = .Top + 8
End With
If Not FS Then BBSf.BltFast 640 - R, I * 8, BGSf, Src, DDBLTFAST_WAIT
Next I
Crs = Crs + 3.5: If Crs >= 360 Then Crs = Crs - 360
If TrM >= 2 Then
CrsF = CrsF + Sin(CrsFF * Rg) * 2
If CrsF < 0 Then CrsF = CrsF + 640
If CrsF >= 640 Then CrsF = CrsF - 640
CrsFF = CrsFF + 0.2: If CrsFF >= 360 Then CrsFF = CrsFF - 360
End If
Case 2 '（紫空）
For I = 0 To 7
R = (I Mod 2) + PSkP * 2: If R >= 2 Then R = R - 2
If R < 1 Then
With Src
.Left = 0: .Top = PSkS1(I) + R * PSkS2(I): .Right = .Left + 640: .Bottom = .Top + PSkS2(I)
End With
If Not FS Then BBSf.BltFast 0, PSkD(I), BGSf, Src, DDBLTFAST_WAIT
Else
With Src
.Left = 0: .Top = PSkS1(I) + R * PSkS2(I): .Right = .Left + 640: .Bottom = PSkS1(I) + PSkS2(I) * 2
End With
If Not FS Then BBSf.BltFast 0, PSkD(I), BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = PSkS1(I): .Right = .Left + 640: .Bottom = .Top + (R - 1) * PSkS2(I)
End With
If Not FS Then BBSf.BltFast 0, PSkD(I) + PSkS2(I) * (2 - R), BGSf, Src, DDBLTFAST_WAIT
End If
Next I
If TrM = 0 Then PSkP = PSkP - 0.01: If PSkP < 0 Then PSkP = PSkP + 1
If TrM = 1 Then PSkP = PSkP - 0.03: If PSkP < 0 Then PSkP = PSkP + 1
If TrM = 2 Then PSkP = PSkP + 0.05: If PSkP >= 1 Then PSkP = PSkP - 1

Case 6 '（カーペット）
With CptG
If TrM = 0 Then .X = .X - 16: .Y = .Y + 1
If TrM = 1 Then .Y = .Y - 15
If TrM = 2 Then .X = .X - 16: .Y = .Y + 15
If .X < 0 Then .X = .X + 640
If .X >= 640 Then .X = .X - 640
If .Y < 0 Then .Y = .Y + 480
If .Y >= 480 Then .Y = .Y - 480
End With
With Src
.Left = CptG.X: .Top = CptG.Y: .Right = 640: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = CptG.Y: .Right = CptG.X: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 640 - CptG.X, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = CptG.X: .Top = 0: .Right = 640: .Bottom = CptG.Y
End With
If Not FS Then BBSf.BltFast 0, 480 - CptG.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = CptG.X: .Bottom = CptG.Y
End With
If Not FS Then BBSf.BltFast 640 - CptG.X, 480 - CptG.Y, BGSf, Src, DDBLTFAST_WAIT

Case 5 '（ダイヤ）
With Dia
If TrM = 0 Then .X = .X - 8: .Y = .Y + 8
If TrM = 1 Then .X = .X + 12: .Y = .Y - 12
If TrM = 2 Then .X = .X + Sin(DiaR * Rg) * 10: .Y = .Y - Cos(DiaR * Rg) * 10
If .X < 0 Then .X = .X + 320
If .X >= 320 Then .X = .X - 320
If .Y < 0 Then .Y = .Y + 240
If .Y >= 240 Then .Y = .Y - 240
If TrM = 2 Then DiaR = DiaR + 0.2: If DiaR >= 360 Then DiaR = DiaR - 360
End With
With Src
.Left = Dia.X: .Top = Dia.Y: .Right = 320: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = Dia.Y: .Right = Dia.X: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 320 - Dia.X, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = Dia.X: .Top = 0: .Right = 320: .Bottom = Dia.Y
End With
If Not FS Then BBSf.BltFast 0, 240 - Dia.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = Dia.X: .Bottom = Dia.Y
End With
If Not FS Then BBSf.BltFast 320 - Dia.X, 240 - Dia.Y, BGSf, Src, DDBLTFAST_WAIT

With Src
.Left = 640 - Dia.X: .Top = Dia.Y: .Right = 640: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 320, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 320: .Top = Dia.Y: .Right = 640 - Dia.X: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 320 + Dia.X, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 640 - Dia.X: .Top = 0: .Right = 640: .Bottom = Dia.Y
End With
If Not FS Then BBSf.BltFast 320, 240 - Dia.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 320: .Top = 0: .Right = 640 - Dia.X: .Bottom = Dia.Y
End With
If Not FS Then BBSf.BltFast 320 + Dia.X, 240 - Dia.Y, BGSf, Src, DDBLTFAST_WAIT

With Src
.Left = Dia.X: .Top = 480 - Dia.Y: .Right = 320: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 0, 240, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 480 - Dia.Y: .Right = Dia.X: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 320 - Dia.X, 240, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = Dia.X: .Top = 240: .Right = 320: .Bottom = 480 - Dia.Y
End With
If Not FS Then BBSf.BltFast 0, 240 + Dia.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 240: .Right = Dia.X: .Bottom = 480 - Dia.Y
End With
If Not FS Then BBSf.BltFast 320 - Dia.X, 240 + Dia.Y, BGSf, Src, DDBLTFAST_WAIT

With Src
.Left = 640 - Dia.X: .Top = 480 - Dia.Y: .Right = 640: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 320, 240, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 320: .Top = 480 - Dia.Y: .Right = 640 - Dia.X: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 320 + Dia.X, 240, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 640 - Dia.X: .Top = 240: .Right = 640: .Bottom = 480 - Dia.Y
End With
If Not FS Then BBSf.BltFast 320, 240 + Dia.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 320: .Top = 240: .Right = 640 - Dia.X: .Bottom = 480 - Dia.Y
End With
If Not FS Then BBSf.BltFast 320 + Dia.X, 240 + Dia.Y, BGSf, Src, DDBLTFAST_WAIT

Case 7 '（レーザー）
LsrSY = LsrSY - 4 - TrM * 3: If LsrSY < 0 Then LsrSY = LsrSY + 240
With Src
.Left = 0: .Top = 480 - LsrSY: .Right = 640: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 240: .Right = 640: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 0, LsrSY, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 240: .Right = 640: .Bottom = 480 - LsrSY
End With
If Not FS Then BBSf.BltFast 0, 240 + LsrSY, BGSf, Src, DDBLTFAST_WAIT

For I = 0 To 59
If (I Mod 3) < 2 Then
Lsr(I) = Lsr(I + 1)
Else
With Lsr(I)
If TrM = 2 Then .R = .R + .RR
If .R < 0 Then .R = .R + 360
If .R >= 360 Then .R = .R - 360
.X = .X + Sin(.R * 3 * Rg) * 18: .Y = .Y - Cos(.R * 3 * Rg) * 18
If .X < -32 Then .X = .X + 672: .RR = -.RR: .C = 1 - .C
If .X >= 640 Then .X = .X - 672: .RR = -.RR: .C = 1 - .C
If .Y < -32 Then .Y = .Y + 512: .RR = -.RR: .C = 1 - .C
If .Y >= 480 Then .Y = .Y - 512: .RR = -.RR: .C = 1 - .C
End With
End If
With Src
.Left = (Lsr(I).R Mod 20) * 32: .Top = Lsr(I).C * 96 + (Int(Lsr(I).R / 20) Mod 3) * 32: .Right = .Left + 32: .Bottom = .Top + 32
End With
BltClip Lsr(I).X, Lsr(I).Y, BGSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I

Case 9 '（水晶）
CrysSY = CrysSY - 1: If CrysSY < 0 Then CrysSY = CrysSY + 240
With Src
.Left = 0: .Top = 240 - CrysSY: .Right = 640: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = 640: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 0, CrysSY, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = 640: .Bottom = 240 - CrysSY
End With
If Not FS Then BBSf.BltFast 0, 240 + CrysSY, BGSf, Src, DDBLTFAST_WAIT
For I = 0 To 15
With Crys(I)
.A = .A + .AA
If .A >= 20 Then .A = .A - 20
If .A < 0 Then .A = .A + 20
.Y = .Y + .YY
If .Y < -80 Or .Y >= 560 Then .A = Int(Rnd * 20): .AA = (1 + Int(Rnd * 2)) * (1 + (TrM = 2) * (I Mod 2) * 2): .X = Int(Rnd * 800) - 80: .Y = Rnd * 80 - 160 - (TrM = 2) * (I Mod 2) * 640: .YY = .AA * 4 + Rnd * 4 * (1 + (TrM = 2) * (I Mod 2) * 2)
End With
With Src
.Left = (Crys(I).A Mod 8) * 80: .Top = 240 + Int(Crys(I).A / 8) * 80: .Right = .Left + 80: .Bottom = .Top + 80
End With
BltClip Crys(I).X, Crys(I).Y, BGSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I

Case 10 'レベル50-199（超空間）
For I = 0 To 59
With Src
.Left = 0: .Top = 30 + I * 7 + Sin((Spa + I * 6) * Rg) * 28: .Right = .Left + 640: .Bottom = .Top + 8
End With
If Not FS Then BBSf.BltFast 0, I * 8, BGSf, Src, DDBLTFAST_WAIT
Next I
Spa = Spa + 1 + Lev * 0.03: If Spa >= 360 Then Spa = Spa - 360

Case 11 'レベル200（電流）
With EdG
.X = .X - 73: If .X < 0 Then .X = .X + 640
.Y = .Y - 72 - TA * 6: If .Y < 0 Then .Y = .Y + 480
End With
With Src
.Left = EdG.X: .Top = EdG.Y: .Right = 640: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = EdG.Y: .Right = EdG.X: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 640 - EdG.X, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = EdG.X: .Top = 0: .Right = 640: .Bottom = EdG.Y
End With
If Not FS Then BBSf.BltFast 0, 480 - EdG.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = EdG.X: .Bottom = EdG.Y
End With
If Not FS Then BBSf.BltFast 640 - EdG.X, 480 - EdG.Y, BGSf, Src, DDBLTFAST_WAIT

Case 13 'レベルMAX
With LdG
.X = TrM * 40 * (8 - ((FAC * 3) Mod 8))
.Y = (1 - TrM) * FAC * 5: If .Y < 0 Then .Y = .Y + 480
End With
With Src
.Left = LdG.X: .Top = LdG.Y: .Right = 640: .Bottom = 320
End With
BltClip 0, -80, BGSf, Src, DDBLTFAST_WAIT
BltClip 0, 240, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = LdG.Y: .Right = LdG.X: .Bottom = 320
End With
BltClip 640 - LdG.X, -80, BGSf, Src, DDBLTFAST_WAIT
BltClip 640 - LdG.X, 240, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = LdG.X: .Top = 0: .Right = 640: .Bottom = LdG.Y
End With
BltClip 0, 240 - LdG.Y, BGSf, Src, DDBLTFAST_WAIT
BltClip 0, 560 - LdG.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = LdG.X: .Bottom = LdG.Y
End With
BltClip 640 - LdG.X, 240 - LdG.Y, BGSf, Src, DDBLTFAST_WAIT
BltClip 640 - LdG.X, 560 - LdG.Y, BGSf, Src, DDBLTFAST_WAIT

End Select
Else 'アニメなし
Select Case Stt

Case 8 '（スターダスト）
With Src
.Left = 0: .Top = 0: .Right = 640: .Bottom = 152
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
If Not FS Then BBSf.BltFast 0, 152, BGSf, Src, DDBLTFAST_WAIT
If Not FS Then BBSf.BltFast 0, 304, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = 640: .Bottom = 24
End With
If Not FS Then BBSf.BltFast 0, 456, BGSf, Src, DDBLTFAST_WAIT

Case 3 '（キラキラ振り子）
With Src
.Left = 0: .Top = 0: .Right = 640: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
If Not FS Then BBSf.BltFast 0, 240, BGSf, Src, DDBLTFAST_WAIT

Case 7 '（レーザー）
With Src
.Left = 0: .Top = 240: .Right = 640: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
If Not FS Then BBSf.BltFast 0, 240, BGSf, Src, DDBLTFAST_WAIT

Case 9 '（水晶）
With Src
.Left = 0: .Top = 0: .Right = 640: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
If Not FS Then BBSf.BltFast 0, 240, BGSf, Src, DDBLTFAST_WAIT

Case Else 'その他
If Not FS Then BBSf.BltFast 0, 0, BGSf, RC0, DDBLTFAST_WAIT

End Select
End If
End If
St = Int(Lev / 5)
If Lev >= 50 And Lev < 200 Then St = 10
If Lev >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 Then St = 11 - (Not (Fnl Or TA)) * 2
If St <> Stt Then FF = True
If FF Then BGF Lev

Else
If Not FS Then BBSf.BltColorFill RC0, 0
End If
Exit Sub

CE:
'Beep
End Sub

Sub BGF(ByVal Lev As Long) '背景フェード
On Error GoTo CE
Dim I As Long, U As Long, RR As Long, RR1 As Long

If FOC < 30 Then
FOC = FOC + 1
RR = FAC
RR1 = FOC + (FOC > 20) * (FOC - 20)
For I = 0 To 11: For U = 0 To 1
With Src
.Left = 0: .Top = 232 - RR1 * 2: .Right = 40 * (8 - ((RR * 3) Mod 8)): .Bottom = 232
End With
If Not FS Then BBSf.BltFast U * 320 + 320 - 40 * (8 - ((RR * 3) Mod 8)), I * 40 + 40 - RR1 * 2, SpSf, Src, DDBLTFAST_WAIT
With Src
.Left = 40 * (8 - ((RR * 3) Mod 8)): .Top = 232 - RR1 * 2: .Right = 320: .Bottom = 232
End With
If Not FS Then BBSf.BltFast U * 320, I * 40 + 40 - RR1 * 2, SpSf, Src, DDBLTFAST_WAIT
Next U, I
Else
If FIC = 0 Then
Stt = Int(Lev / 5)
If Lev >= 50 And Lev < 200 Then Stt = 10
If Lev >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 Then Stt = 11 - (Not (Fnl Or TA)) * 2
BGCr Stt
End If
FIC = FIC + 1
RR = FAC
RR1 = (FIC - 10) * -(FIC >= 10)
For I = 0 To 11: For U = 0 To 1
With Src
.Left = 0: .Top = 192: .Right = 40 * (8 - ((RR * 3) Mod 8)): .Bottom = 232 - RR1 * 2
End With
If Not FS Then BBSf.BltFast U * 320 + 320 - 40 * (8 - ((RR * 3) Mod 8)), I * 40, SpSf, Src, DDBLTFAST_WAIT
With Src
.Left = 40 * (8 - ((RR * 3) Mod 8)): .Top = 192: .Right = 320: .Bottom = 232 - RR1 * 2
End With
If Not FS Then BBSf.BltFast U * 320, I * 40, SpSf, Src, DDBLTFAST_WAIT
Next U, I
If FIC >= 30 Then FOC = 0: FIC = 0: FF = False
End If
Exit Sub

CE:
'Beep
End Sub

Sub BDCr(ByVal BD As Long) 'ボード作成
On Error GoTo CE
Dim I As Long, U As Long, RR As Long, RR1 As Long

Dim SD As DDSURFACEDESC2
SD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
SD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
SD.lWidth = 288 + (GGMode = 4) * 72: SD.lHeight = 480 + (GGMode = 4) * 120
If XDir(EffDir + "BD" + Mid$(Str$(BD), 2) + ".BMP", vbHidden) <> vbNullString$ Then
Set BDSf = DD.CreateSurfaceFromFile(EffDir + "BD" + Mid$(Str$(BD), 2) + ".BMP", SD)
Else
RR = 16 + (GGMode = 4) * 4: RR1 = 4 + (GGMode = 4)
Set BDSf = DD.CreateSurface(SD)
BDSf.setDrawWidth 1
BDSf.BltColorFill RC0, 0
BDSf.SetForeColor RGB(256, 128, 0)
BDSf.SetFillColor RGB(256, 256, 0)
For I = 0 To 9: BDSf.DrawBox RR * (2 + I), RR1, RR * (2 + I + 1), RR: Next I
BDSf.SetForeColor RGB(255, 255, 255)
BDSf.SetFillColor RGB(128, 128, 128)
BDSf.DrawBox 0, 0, RR * 2, RR * 23
BDSf.DrawBox RR * 12, 0, RR * 14, RR * 23
BDSf.DrawBox RR * 2, RR * 22, RR * 12, RR * 23
BDSf.SetForeColor RGB(128, 128, 128)
BDSf.DrawBox RR * 2 - 1, RR * 22 + 1, RR * 12 + 1, RR * 23 - 1
BDSf.SetForeColor RGB(192, 192, 192)
BDSf.SetFillColor RGB(192, 192, 192)
BDSf.DrawBox RR * 10, RR * 23, RR * 11, RR * 23 + 1 - (GGMode <> 4)
BDSf.DrawBox RR * 10, RR * 23 + RR1, RR * 10 + 1 - (GGMode <> 4), RR * 24 + RR1

For I = 0 To 5
BDSf.SetForeColor ClLst(Int(I / 2))
BDSf.SetFillColor ClLst(Int(I / 2))
BDSf.DrawEllipse RR * 14 + 3, RR * I + 1, RR * 16 - 3, RR * (I + 1) - 2
Next I
For I = 0 To 5
BDSf.SetForeColor ClLst(4 + Int(I / 3))
BDSf.SetFillColor RGB(192, 192, 192)
BDSf.SetFillColor ClLst(4 + Int(I / 3))
BDSf.DrawEllipse RR * 14 + 3, RR * (I + 6) + 1, RR * 16 - 3, RR * (I + 7) - 1
Next I

BDSf.setDrawWidth RR1 + 1
BDSf.SetForeColor RGB(192, 0, 192)
For I = 0 To 9: For U = 0 To 4
BDSf.DrawLine RR * 17, RR * (I * 2 + 1), RR * 17 + Sin((U * 72 + I * 7.2) * Rg) * RR1 * 3, RR * (I * 2 + 1) + Cos((U * 72 + I * 7.2) * Rg) * RR1 * 3
Next U, I
BDSf.setDrawWidth RR1 - 1
BDSf.SetForeColor RGB(255, 0, 255)
For I = 0 To 9: For U = 0 To 4
BDSf.DrawLine RR * 17, RR * (I * 2 + 1), RR * 17 + Sin((U * 72 + I * 7.2) * Rg) * RR1 * 3, RR * (I * 2 + 1) + Cos((U * 72 + I * 7.2) * Rg) * RR1 * 3
Next U, I

End If
CK.low = 0: CK.high = 0
BDSf.SetColorKey DDCKEY_SRCBLT, CK
If Not WM Then BDSf.SetPalette MPal
Exit Sub

CE:
'Beep
End Sub

Sub BCCr(ByVal BC As Long) 'ブロック作成
On Error GoTo CE
Dim I As Long, U As Long, RR As Long

Dim SD As DDSURFACEDESC2
SD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
SD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
If XDir(EffDir + "BC" + Mid$(Str$(BC), 2) + ".BMP", vbHidden) <> vbNullString$ Then
SD.lWidth = 128 + (GGMode = 4) * 32: SD.lHeight = 160 + (GGMode = 4) * 40: Set BCSf = DD.CreateSurfaceFromFile(EffDir + "BC" + Mid$(Str$(BC), 2) + ".BMP", SD)
Else
SD.lWidth = 128 + (GGMode = 4) * 32: SD.lHeight = 160 + (GGMode = 4) * 40: Set BCSf = DD.CreateSurface(SD): BCSf.BltColorFill RC0, 0
RR = 16 + (GGMode = 4) * 4
BCSf.setDrawWidth 1
For I = 0 To 1: For U = 0 To 7
BCSf.SetForeColor RGB(255, 255, 255)
BCSf.SetFillColor BCNm(I, U)
BCSf.DrawBox U * RR + 1, I * RR + 1, U * RR + (RR - 1), I * RR + (RR - 1)
Next U, I
For I = 0 To 6: For U = 0 To 3
BCSf.SetForeColor RGB(255, 255, 255)
BCSf.SetFillColor BCNm(0, I)
BCSf.DrawBox Int(I / 4) * (RR * 4) + B(I, 0, U, 0) * RR + 1, (I Mod 4) * RR * 2 + B(I, 0, U, 1) * RR + (RR + 1), Int(I / 4) * (RR * 4) + B(I, 0, U, 0) * RR + (RR - 1), (I Mod 4) * (RR * 2) + B(I, 0, U, 1) * RR + (RR * 2 - 1)
Next U, I
End If
CK.low = 0: CK.high = 0
BCSf.SetColorKey DDCKEY_SRCBLT, CK
If Not WM Then BCSf.SetPalette MPal
Exit Sub

CE:
'Beep
End Sub

Sub BGCrTit() '背景作成（タイトル）
On Error GoTo CE
Dim I As Long, RR As Long
Dim FSz As Currency

If Not WM Then
Set BGPal = Nothing: Set BGSf = Nothing
If XDir(EffDir + "BGTIT.BMP", vbHidden) <> vbNullString$ Then
Set BGPal = DD.LoadPaletteFromBitmap(EffDir + "BGTIT.BMP")
BGPal.GetEntries 0, 256, BGPa
Else
For U = 0 To 255: BGPa(U).red = 0: BGPa(U).green = 0: BGPa(U).blue = 0: Next U
Set BGPal = DD.CreatePalette(DDPCAPS_ALLOW256 Or DDPCAPS_8BIT, BGPa)
End If
For I = 0 To 127: MPa(I) = BGPa(I): Next I
MPal.SetEntries 0, 256, MPa
End If
SD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
SD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
SD.lWidth = 480: SD.lHeight = 400
If XDir(EffDir + "BGTIT.BMP", vbHidden) <> vbNullString$ Then
Set BGSf = DD.CreateSurfaceFromFile(EffDir + "BGTIT.BMP", SD)
Else
Set BGSf = DD.CreateSurface(SD)
BGSf.BltColorFill RC0, 0
FSz = Picture1.Font.Size
Picture1.Font.Size = 132
BGSf.SetFont Picture1.Font
RR = Int(Picture1.TextWidth("DTET") / 2)
For I = 0 To 7
BGSf.SetForeColor RGB(144 + I * 16, 32 + I * 32, 4 * I ^ 2)
BGSf.DrawText 236 - RR + I, 7 - I, "DTET", False
BGSf.SetForeColor RGB(128, 4 * I ^ 2, 32 + I * 32)
BGSf.DrawText 236 - RR + I, 167 - I, "DTET", False
Next I
Picture1.Font.Size = FSz
BGSf.SetFont Picture1.Font

End If
CK.low = 0: CK.high = 0
BGSf.SetColorKey DDCKEY_SRCBLT, CK
If Not WM Then BGSf.SetPalette MPal
Exit Sub

CE:
'Beep
End Sub

Sub BGCr(ByVal BGSt As Long) '背景作成
On Error GoTo CE
Dim I As Long
Dim RRR As String
Dim BGSD As DDSURFACEDESC2
Select Case BGSt
Case 10
If Fnl Then BGSt = 13
Case 11
If Fnl Then BGSt = 14 Else BGSt = 12
Case 13
BGSt = 11
End Select
If BG >= 1 Then
If Not BGMem Then
RRR = EffDir + "BG" + Mid$(Str$(BGSt), 2) + ".BMP"
If Not WM Then
Set BGPal = Nothing: Set BGSf = Nothing
If XDir(RRR, vbHidden) <> vbNullString$ Then
Set BGPal = DD.LoadPaletteFromBitmap(RRR)
BGPal.GetEntries 0, 256, BGPa
Else
For U = 0 To 255: BGPa(U).red = 0: BGPa(U).green = 0: BGPa(U).blue = 0: Next U
Set BGPal = DD.CreatePalette(DDPCAPS_ALLOW256 Or DDPCAPS_8BIT, BGPa)
End If
For I = 0 To 127: MPa(I) = BGPa(I): Next I
MPal.SetEntries 0, 256, MPa
Else
Set BGSf = Nothing
End If
BGSD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
BGSD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
BGSD.lWidth = 640: BGSD.lHeight = 480
If XDir(RRR, vbHidden) <> vbNullString$ Then
Set BGSf = DD.CreateSurfaceFromFile(RRR, BGSD)
Else
Set BGSf = DD.CreateSurface(BGSD)
BGSf.BltColorFill RC0, 0
End If
CK.low = 0: CK.high = 0
BGSf.SetColorKey DDCKEY_SRCBLT, CK
If Not WM Then BGSf.SetPalette MPal

Else 'BGメモリー
If Not WM Then
Set BGPal = Nothing: Set BGPal = BSPal(BGSt)
BGPal.GetEntries 0, 256, BGPa
For I = 0 To 127: MPa(I) = BGPa(I): Next I
MPal.SetEntries 0, 256, MPa
End If
If BGSf Is Nothing Then
BGSD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
BGSD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
BGSD.lWidth = 640: BGSD.lHeight = 480
Set BGSf = DD.CreateSurface(BGSD)
CK.low = 0: CK.high = 0
BGSf.SetColorKey DDCKEY_SRCBLT, CK
End If
If Not WM Then BGSf.SetPalette MPal
With Src
.Left = 0: .Top = 0: .Right = 640: .Bottom = 480
End With
BGSf.BltFast 0, 0, BSSf(BGSt), Src, DDBLTFAST_WAIT
End If
End If
Exit Sub

CE:
'Beep
End Sub

Sub EAni() '消去アニメ
On Error GoTo CE
Dim I As Long

For I = 0 To 159
With EA(I)
If .V Then
.XX = .XX + .XXX
.YY = .YY + .YYY: .YY = .YY * 0.95
.X = .X + .XX: .Y = .Y + .YY
If .V = 2 Then .AF = .AF + 1 + (.AF >= 3) * 4
If .X <= -16 Or .X > 688 Or .Y <= -16 Or .Y >= 496 Then .V = 0
End If
End With
Next I
For I = 0 To 159
If EA(I).V Then
With Src
If EA(I).AF < 2 Then
.Left = EA(I).A * 16: .Top = 16: .Right = .Left + 16: .Bottom = .Top + 16
Else
.Left = 112: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
End If
End With
BltClip EA(I).X, EA(I).Y, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next I
Exit Sub

CE:
End Sub

Sub EAni4() '4人用消去アニメ
On Error GoTo CE
Dim I As Long

For I = 0 To 159
With EA(I)
If .V Then
.XX = .XX + .XXX
.YY = .YY + .YYY: .YY = .YY * 0.95
.X = .X + .XX: .Y = .Y + .YY
If .V = 2 Then .AF = .AF + 1 + (.AF >= 3) * 4
If .X <= -12 Or .X > 652 Or .Y <= -12 Or .Y >= 492 Then .V = 0
End If
End With
Next I
For I = 0 To 159
If EA(I).V Then
With Src
If EA(I).AF < 2 Then
.Left = EA(I).A * 12: .Top = 12: .Right = .Left + 12: .Bottom = .Top + 12
Else
.Left = 84: .Top = 0: .Right = .Left + 12: .Bottom = .Top + 12
End If
End With
BltClip EA(I).X, EA(I).Y, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next I
Exit Sub

CE:
End Sub
Sub NxD() '1人用NEXTアニメ
On Error GoTo CE
Dim I As Long

For I = 24 To 0 Step -1
NxDC = BWC1(PL) - I * 8

If NxDC >= -8 And NxDC < 72 Then
With Src
.Left = Int(Nx1(PL, I) / 4) * 64: .Top = 32 + (Nx1(PL, I) Mod 4) * 32: .Right = .Left + 64: .Bottom = .Top + 32
End With
BltClip 0, NxDC * 6 + 16, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If

If NxDC >= 72 And NxDC < 104 Then
With Src
.Left = Int(Nx1(PL, I) / 4) * 64: .Top = 32 + (Nx1(PL, I) Mod 4) * 32: .Right = .Left + 64: .Bottom = .Top + 32
End With
BltClip (NxDC - 72) * 10, 448, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If

If NxDC >= 104 And NxDC < 176 Then
With Src
.Left = Int(Nx1(PL, I) / 4) * 64: .Top = 32 + (Nx1(PL, I) Mod 4) * 32: .Right = .Left + 64: .Bottom = .Top + 32
End With
BltClip 320, 448 - (NxDC - 104) * 6, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If

If NxDC >= 176 And NxDC < 192 Then
With Src
.Left = Int(Nx1(PL, I) / 4) * 64: .Top = 32 + (Nx1(PL, I) Mod 4) * 32: .Right = .Left + 64: .Bottom = .Top + 32
End With
BltClip 320 - (NxDC - 176) * 10, 16, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If

Next I
Exit Sub

CE:
'Beep
End Sub

Sub NxD2() '2人用NEXTアニメ
On Error GoTo CE
Dim I As Long

For I = 24 To 0 Step -1
NxDC = BWC1(PL) - I * 8

If NxDC >= 96 And NxDC < 176 Then
With Src
.Left = Int(Nx1(PL, I) / 4) * 64: .Top = 32 + (Nx1(PL, I) Mod 4) * 32: .Right = .Left + 64: .Bottom = .Top + 32
End With
BltClip PL * 576, 448 - (NxDC - 104) * 6, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If

If NxDC >= 176 And NxDC < 192 Then
With Src
.Left = Int(Nx1(PL, I) / 4) * 64: .Top = 32 + (Nx1(PL, I) Mod 4) * 32: .Right = .Left + 64: .Bottom = .Top + 32
End With
BltClip PL * 576 - (NxDC - 176) * 10 * (PL * 2 - 1), 16, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If

Next I
Exit Sub

CE:
'Beep
End Sub

Sub NxD4() '4人用NEXTアニメ
On Error GoTo CE
Dim I As Long

For I = 24 To 0 Step -1
NxDC = BWC1(PL) - I * 10

If NxDC >= 144 And NxDC < 192 Then
With Src
.Left = Int(Nx1(PL, I) / 4) * 48: .Top = 24 + (Nx1(PL, I) Mod 4) * 24: .Right = .Left + 48: .Bottom = .Top + 24
End With
BltClip 80 + PL * 144, -48 + (NxDC - 144) * 3, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If

Next I
Exit Sub

CE:
'Beep
End Sub

Private Sub PlayMusic(ByVal Fname As String, Optional ByVal LoopPos As Long, Optional ByVal LoopEnd As Long, Optional ByVal Transpose As Integer) 'BGM演奏
On Error GoTo CE
Dim I As Long
If NoBGM Or (Not MidiOn And Fname = vbNullString$) Then Exit Sub
MidiOn = False
If MidiMode > 0 Then DMP.Stop DMS, DMSS, 0, 0
If XDir(EffDir + Fname + ".MID", vbHidden) = vbNullString$ Then Fname = vbNullString$
If Fname <> vbNullString$ Then
 If MidiMode <> Midi Or Transpose <> Trans Then Set DMSS = Nothing: Set DMS = Nothing: Set DML = Nothing: Set DMP = Nothing: Set DMP = DX.DirectMusicPerformanceCreate: DMP.Init Nothing, Form1.hWnd
 If Midi > 0 Then
  If MidiMode <> Midi Or Transpose <> Trans Then
   Set DML = DX.DirectMusicLoaderCreate
   DMP.SetPort PortIndex(Midi), 1: DMP.SetMasterAutoDownload True
  End If
  DMP.SetMasterVolume 0: DMP.SetMasterTempo 0
  For I = 0 To 8: DMP.SendTransposePMSG 0, 0, I, Transpose: Next I
  Set DMS = DML.LoadSegment(EffDir + Fname + ".MID")
  If LoopEnd <= 0 Then LoopEnd = DMS.GetLength
  DMS.SetLoopPoints (LoopPos - 1) * (-(LoopPos > 0)), LoopEnd
  DMS.SetRepeats (LoopPos >= 0)
  Set DMSS = DMP.PlaySegment(DMS, 0, 0)
  MidiOn = True
 End If
 MidiMode = Midi: Trans = Transpose
End If
Exit Sub

CE:
'Beep
End Sub

Private Sub MidiTempo(ByVal MTempo As Single)
On Error GoTo CE
If NoBGM Then Exit Sub
If MidiOn Then DMP.SetMasterTempo MTempo
Exit Sub

CE:
'Beep
End Sub

Private Sub StringD(ByVal Tx As Integer, ByVal TY As Integer, ByVal TC As Long, ByVal TT As String, ByVal LR As Boolean, Optional ByVal XL As Long) '文字表示
On Error GoTo CE
If TC < 0 Then TC = 7
Dim LT As Long, I As Long, At As Long, XLL As Single
Dim RR As Long
LT = Len(TT)
For I = 0 To LT - 1
At = Asc(Mid$(TT, I + 1, 1))
Select Case At
Case 32 To 96 '大文字英数・記号
If TC < 10 Then
With Src
.Left = ((At - 32) Mod 40) * 16: .Top = Int((At - 32) / 40) * 16 + TC * 32: .Right = .Left + 16: .Bottom = .Top + 16
End With
Else
With Src
.Left = ((At - 32) Mod 40) * 16: .Top = Int((At - 32) / 40) * 16 + ((I - (TC > 10) * Int(CCC / 2)) Mod 7) * 32: .Right = .Left + 16: .Bottom = .Top + 16
End With
End If
If XL > 0 And LT > XL Then XLL = 16 * (XL - 1) / (LT - 1) Else XLL = 16
BltClip Tx + LR * (LT + (LT > XL) * (LT - XL)) * 16 + I * XLL, TY, FoSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Case 97 To 122 '小文字
If TC < 10 Then
With Src
.Left = ((At - 64) Mod 40) * 16: .Top = Int((At - 64) / 40) * 16 + TC * 32: .Right = .Left + 16: .Bottom = .Top + 16
End With
Else
With Src
.Left = ((At - 64) Mod 40) * 16: .Top = Int((At - 64) / 40) * 16 + ((I - (TC > 10) * Int(CCC / 2)) Mod 7) * 32: .Right = .Left + 16: .Bottom = .Top + 16
End With
End If
If XL > 0 And LT > XL Then XLL = 16 * (XL - 1) / (LT - 1) Else XLL = 16
BltClip Tx + LR * (LT + (LT > XL) * (LT - XL)) * 16 + I * XLL, TY, FoSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Case 123 To 126 '記号
If TC < 10 Then
With Src
.Left = ((At - 58) Mod 40) * 16: .Top = Int((At - 58) / 40) * 16 + TC * 32: .Right = .Left + 16: .Bottom = .Top + 16
End With
Else
With Src
.Left = ((At - 58) Mod 40) * 16: .Top = Int((At - 58) / 40) * 16 + ((I - (TC > 10) * Int(CCC / 2)) Mod 7) * 32: .Right = .Left + 16: .Bottom = .Top + 16
End With
End If
If XL > 0 And LT > XL Then XLL = 16 * (XL - 1) / (LT - 1) Else XLL = 16
BltClip Tx + LR * (LT + (LT > XL) * (LT - XL)) * 16 + I * XLL, TY, FoSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Case Else 'その他
If XL > 0 And LT > XL Then XLL = 16 * (XL - 1) / (LT - 1) Else XLL = 16
RR = Int(Picture1.TextWidth(Mid$(TT, I + 1, 1)) / 2 + 0.5)
BBSf.SetForeColor RGB(0, 0, 128)
BBSf.DrawText Tx + LR * (LT + (LT > XL) * (LT - XL)) * 16 + I * XLL + 9 - RR, TY + 2, Mid$(TT, I + 1, 1), False
BBSf.SetForeColor RGB(255, 255, 255)
BBSf.DrawText Tx + LR * (LT + (LT > XL) * (LT - XL)) * 16 + I * XLL + 9 - RR, TY + 1, Mid$(TT, I + 1, 1), False
If TC < 10 Then
BBSf.SetForeColor ClLst(TC)
End If
If TC >= 10 Then
BBSf.SetForeColor ClLst((I - (TC > 10) * Int(CCC / 2)) Mod 7)
End If
BBSf.DrawText Tx + LR * (LT + (LT > XL) * (LT - XL)) * 16 + I * XLL + 8 - RR, TY + 1, Mid$(TT, I + 1, 1), False
End Select
Next I
Exit Sub

CE:
'Beep
End Sub

Private Sub TextD(ByVal Tx As Integer, ByVal TY As Integer, ByVal TC As Long, ByVal TT As Long, ByVal LR As Boolean, Optional ByVal XL As Long) '数字表示
On Error GoTo CE
If TC < 0 Then TC = 7
Dim T As String, LT As Long, I As Long, XLL As Single
T = Mid$(Str$(TT), 2 + (TT < 0))
LT = Len(T)
For I = LT - 1 To 0 Step -1
If Mid$(T, I + 1, 1) = "-" Then
If TC < 10 Then
With Src
.Left = 208: .Top = TC * 32: .Right = .Left + 16: .Bottom = .Top + 16
End With
Else
If LR Then
With Src
.Left = 208: .Top = ((LT - I - (TC > 10) * Int(CCC / 2) - 1) Mod 7) * 32: .Right = .Left + 16: .Bottom = .Top + 16
End With
Else
With Src
.Left = 208: .Top = ((I - (TC > 10) * Int(CCC / 2)) Mod 7) * 32: .Right = .Left + 16: .Bottom = .Top + 16
End With
End If
End If
Else
If TC < 10 Then
With Src
.Left = (Asc(Mid$(T, I + 1, 1)) - 48) * 16 + 256: .Top = TC * 32: .Right = .Left + 16: .Bottom = .Top + 16
End With
Else
If LR Then
With Src
.Left = (Asc(Mid$(T, I + 1, 1)) - 48) * 16 + 256: .Top = ((LT - I - (TC > 10) * Int(CCC / 2) - 1) Mod 7) * 32: .Right = .Left + 16: .Bottom = .Top + 16
End With
Else
With Src
.Left = (Asc(Mid$(T, I + 1, 1)) - 48) * 16 + 256: .Top = ((I - (TC > 10) * Int(CCC / 2)) Mod 7) * 32: .Right = .Left + 16: .Bottom = .Top + 16
End With
End If
End If
End If
If XL > 0 And LT > XL Then XLL = 16 * (XL - 1) / (LT - 1) Else XLL = 16
BltClip Tx + LR * (LT + (LT > XL) * (LT - XL)) * 16 + I * XLL, TY, FoSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
Exit Sub

CE:
'Beep
End Sub

Private Sub CursorD(ByVal CX As Long, ByVal CY As Long, ByVal CuC As Long, ByVal CT As Long) 'カーソル表示
On Error GoTo CE
If CuC = 10 Then CuC = Int(CCC / 2)
If CuC > 10 Then CuC = 8 + (Int(CCC / 7) Mod 2)
With Src
.Left = 464 + CT * 16: .Top = 16 + CuC * 32: .Right = .Left + 16: .Bottom = .Top + 16
End With
If Not FS Then BltClip CX, CY, FoSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Exit Sub

CE:
'Beep
End Sub

Private Sub OkD(ByVal CX As Long, ByVal CY As Long, ByVal CuC As Long) 'OK表示
On Error GoTo CE
If CuC > 10 Then CuC = Int(CCC / 2)
With Src
.Left = 496: .Top = 16 + CuC * 32: .Right = .Left + 16: .Bottom = .Top + 16
End With
If Not FS Then BltClip CX, CY, FoSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Exit Sub

CE:
'Beep
End Sub

Private Sub BltClip(ByVal BCX As Long, ByVal BCY As Long, BCSpSf As DirectDrawSurface7, BCRR As RECT, ByVal TrFrags As CONST_DDBLTFASTFLAGS) 'BltClip
On Error GoTo CE
Dim BCR As RECT
BCR = BCRR
With BCR
If BCX < 0 Then .Left = .Left - BCX: BCX = 0
If BCX - .Left + .Right > WW Then .Right = .Left + WW - BCX
If BCY < 0 Then .Top = .Top - BCY: BCY = 0
If BCY - .Top + .Bottom > HH Then .Bottom = .Top + HH - BCY
End With
If Not FS Then BBSf.BltFast BCX, BCY, BCSpSf, BCR, TrFrags
Exit Sub

CE:
'Beep
End Sub

Sub KeyInit() 'キー初期化
On Error GoTo CE
CsrLL = 2: CsrRR = 2: CsrUU = 2: CsrDD = 2: CsrAA = 2: CsrBB = 2: CsrSS = 2
Exit Sub

CE:
'Beep
End Sub

Sub KeyInitAll() '一般キー初期化
On Error GoTo CE
Dim I As Long

For I = 0 To JC
CsrLL1(I) = 2: CsrRR1(I) = 2: CsrUU1(I) = 2: CsrDD1(I) = 2: CsrAA1(I) = 2: CsrBB1(I) = 2: CsrSS1(I) = 2
Next I
CsrLL = 2: CsrRR = 2: CsrUU = 2: CsrDD = 2: CsrAA = 2: CsrBB = 2: CsrSS = 2
CsrAAP = 0: CsrBBP = 0: CsrSSP = 0
Exit Sub

CE:
'Beep
End Sub

Sub KeyScan(ByVal PT As Long) 'キー入力
On Error GoTo CE
Select Case PT
Case 0 'キーボード
DIDK.GetDeviceStateKeyboard KS
CsrL = KS.Key(DIK_LEFT) > 0 Or KS.Key(KArwL) > 0
CsrR = KS.Key(DIK_RIGHT) > 0 Or KS.Key(KArwR) > 0
CsrU = KS.Key(DIK_UP) > 0 Or KS.Key(KArwU) > 0
CsrD = KS.Key(DIK_DOWN) > 0 Or KS.Key(KArwD) > 0
CsrA = KS.Key(DIK_SPACE) > 0 Or KS.Key(DIK_RETURN) > 0 Or KS.Key(DIK_NUMPADENTER) > 0 Or KS.Key(KBL) > 0
CsrB = KS.Key(DIK_BACK) > 0 Or KS.Key(KBR) > 0
CsrS = KS.Key(DIK_TAB) > 0 Or KS.Key(KBS) > 0
Case 1 To 4 'パッド
On Error Resume Next
JS(PT - 1) = JS0
DIDJ(PT - 1).Poll: DIDJ(PT - 1).GetDeviceStateJoystick JS(PT - 1)
On Error GoTo CE
CsrL = (JS(PT - 1).X < 16384)
CsrR = (JS(PT - 1).X > 49152)
CsrU = (JS(PT - 1).Y < 16384)
CsrD = (JS(PT - 1).Y > 49152)
CsrA = JS(PT - 1).buttons(PBL(PT - 1)) > 64
CsrB = JS(PT - 1).buttons(PBR(PT - 1)) > 64
CsrS = JS(PT - 1).buttons(PBS(PT - 1)) > 64
End Select
If CsrL And CsrR Then CsrL = False: CsrR = False
If CsrU And CsrD Then CsrU = False: CsrD = False
If (CsrL Or CsrR) And (CsrU Or CsrD) Then CsrL = False: CsrR = False: CsrU = False: CsrD = False
If CsrL Then CsrLL = CsrLL + 1 + (CsrLL >= 19) * 5 Else CsrLL = 0
If CsrR Then CsrRR = CsrRR + 1 + (CsrRR >= 19) * 5 Else CsrRR = 0
If CsrU Then CsrUU = CsrUU + 1 + (CsrUU >= 19) * 5 Else CsrUU = 0
If CsrD Then CsrDD = CsrDD + 1 + (CsrDD >= 19) * 5 Else CsrDD = 0
If CsrA Then CsrAA = CsrAA + 1 + (CsrAA >= 2) Else CsrAA = 0
If CsrB Then CsrBB = CsrBB + 1 + (CsrBB >= 2) Else CsrBB = 0
If CsrS Then CsrSS = CsrSS + 1 + (CsrSS >= 2) Else CsrSS = 0
If CsrAA = 1 And CsrBB = 1 Then CsrBB = 0
If CsrAA = 1 And CsrSS = 1 Then CsrSS = 0
If CsrBB = 1 And CsrSS = 1 Then CsrSS = 0
Exit Sub

CE:
'Beep
End Sub

Sub KeyScanAll() '一般キー入力
On Error GoTo CE
Dim I As Long
CsrAAP = -1: CsrBBP = -1: CsrSSP = -1
For I = 0 To JC
Select Case I
Case 0 'キーボード
DIDK.GetDeviceStateKeyboard KS
CsrL1(I) = KS.Key(DIK_LEFT) > 0 Or KS.Key(KArwL) > 0
CsrR1(I) = KS.Key(DIK_RIGHT) > 0 Or KS.Key(KArwR) > 0
CsrU1(I) = KS.Key(DIK_UP) > 0 Or KS.Key(KArwU) > 0
CsrD1(I) = KS.Key(DIK_DOWN) > 0 Or KS.Key(KArwD) > 0
CsrA1(I) = KS.Key(DIK_SPACE) > 0 Or KS.Key(DIK_RETURN) > 0 Or KS.Key(DIK_NUMPADENTER) > 0 Or KS.Key(KBL) > 0
CsrB1(I) = KS.Key(DIK_BACK) > 0 Or KS.Key(KBR) > 0
CsrS1(I) = KS.Key(DIK_TAB) > 0 Or KS.Key(KBS) > 0
Case 1 To 4 'パッド
On Error Resume Next
JS(I - 1) = JS0
DIDJ(I - 1).Poll: DIDJ(I - 1).GetDeviceStateJoystick JS(I - 1)
On Error GoTo CE
CsrL1(I) = (JS(I - 1).X < 16384)
CsrR1(I) = (JS(I - 1).X > 49152)
CsrU1(I) = (JS(I - 1).Y < 16384)
CsrD1(I) = (JS(I - 1).Y > 49152)
CsrA1(I) = JS(I - 1).buttons(PBL(I - 1)) > 64
CsrB1(I) = JS(I - 1).buttons(PBR(I - 1)) > 64
CsrS1(I) = JS(I - 1).buttons(PBS(I - 1)) > 64
End Select
If CsrL1(I) And CsrR1(I) Then CsrL1(I) = False: CsrR1(I) = False
If CsrU1(I) And CsrD1(I) Then CsrU1(I) = False: CsrD1(I) = False
If (CsrL1(I) Or CsrR1(I)) And (CsrU1(I) Or CsrD1(I)) Then CsrL1(I) = False: CsrR1(I) = False: CsrU1(I) = False: CsrD1(I) = False
If CsrL1(I) Then CsrLL1(I) = CsrLL1(I) + 1 + (CsrLL1(I) >= 19) * 5 Else CsrLL1(I) = 0
If CsrR1(I) Then CsrRR1(I) = CsrRR1(I) + 1 + (CsrRR1(I) >= 19) * 5 Else CsrRR1(I) = 0
If CsrU1(I) Then CsrUU1(I) = CsrUU1(I) + 1 + (CsrUU1(I) >= 19) * 5 Else CsrUU1(I) = 0
If CsrD1(I) Then CsrDD1(I) = CsrDD1(I) + 1 + (CsrDD1(I) >= 19) * 5 Else CsrDD1(I) = 0
If CsrA1(I) Then CsrAA1(I) = CsrAA1(I) + 1 + (CsrAA1(I) >= 2) Else CsrAA1(I) = 0
If CsrB1(I) Then CsrBB1(I) = CsrBB1(I) + 1 + (CsrBB1(I) >= 2) Else CsrBB1(I) = 0
If CsrS1(I) Then CsrSS1(I) = CsrSS1(I) + 1 + (CsrSS1(I) >= 2) Else CsrSS1(I) = 0
If CsrAA1(I) = 1 And CsrBB1(I) = 1 Then CsrBB1(I) = 0
If CsrAA1(I) = 1 And CsrSS1(I) = 1 Then CsrSS1(I) = 0
If CsrBB1(I) = 1 And CsrSS1(I) = 1 Then CsrSS1(I) = 0
CsrAAP = CsrAAP + (CsrAAP = -1) * (CsrAA1(I) = 1) * (I + 1)
CsrBBP = CsrBBP + (CsrBBP = -1) * (CsrBB1(I) = 1) * (I + 1)
CsrSSP = CsrSSP + (CsrSSP = -1) * (CsrSS1(I) = 1) * (I + 1)
Next I
CsrL = False: CsrR = False: CsrU = False: CsrD = False: CsrA = False: CsrB = False: CsrS = False:
For I = 0 To JC
CsrL = CsrL Or CsrL1(I)
CsrR = CsrR Or CsrR1(I)
CsrU = CsrU Or CsrU1(I)
CsrD = CsrD Or CsrD1(I)
CsrA = CsrA Or CsrA1(I)
CsrB = CsrB Or CsrB1(I)
CsrS = CsrS Or CsrS1(I)
Next I
If CsrL And CsrR Then CsrL = False: CsrR = False
If CsrU And CsrD Then CsrU = False: CsrD = False
If (CsrL Or CsrR) And (CsrU Or CsrD) Then CsrL = False: CsrR = False: CsrU = False: CsrD = False
If CsrL Then CsrLL = CsrLL + 1 + (CsrLL >= 19) * 5 Else CsrLL = 0
If CsrR Then CsrRR = CsrRR + 1 + (CsrRR >= 19) * 5 Else CsrRR = 0
If CsrU Then CsrUU = CsrUU + 1 + (CsrUU >= 19) * 5 Else CsrUU = 0
If CsrD Then CsrDD = CsrDD + 1 + (CsrDD >= 19) * 5 Else CsrDD = 0
If CsrA Then CsrAA = CsrAA + 1 + (CsrAA >= 2) Else CsrAA = 0
If CsrB Then CsrBB = CsrBB + 1 + (CsrBB >= 2) Else CsrBB = 0
If CsrS Then CsrSS = CsrSS + 1 + (CsrSS >= 2) Else CsrSS = 0
If CsrAA = 1 And CsrBB = 1 Then CsrBB = 0
If CsrAA = 1 And CsrSS = 1 Then CsrSS = 0
If CsrBB = 1 And CsrSS = 1 Then CsrSS = 0
Exit Sub

CE:
'Beep
End Sub

Sub BRndInit() 'ブロックランダム初期化
On Error GoTo CE
Dim RR As Long, RR1 As Long
Dim I As Long
For I = 0 To 6: BR(I) = I: Next I
For I = 0 To 5
RR1 = I + Int(Rnd * (7 - I))
RR = BR(I): BR(I) = BR(RR1): BR(RR1) = RR
Next I
Exit Sub

CE:
'Beep
End Sub

Sub BRndInitP(Nx As Long) 'ブロックランダム初期化（パラレル）
On Error GoTo CE
Dim RR As Long, RR1 As Long
Dim I As Long
For I = 0 To 5: BR(I) = I - (I >= Nx): Next I
For I = 0 To 4
RR1 = I + Int(Rnd * (6 - I))
RR = BR(I): BR(I) = BR(RR1): BR(RR1) = RR
Next I
BR(6) = Nx
Exit Sub

CE:
'Beep
End Sub

Function BRnd() As Long 'ブロックランダム
On Error GoTo CE
Dim RR As Long, RR1 As Long, RR2 As Long
Dim I As Long
If Fnl Then
RR1 = Int(Rnd * 63)
Select Case RR1
Case 0 To 31
RR = 0
Case 32 To 47
RR = 1
Case 48 To 55
RR = 2
Case 56 To 59
RR = 3
Case 60 To 61
RR = 4
Case Else
RR = 5
End Select
Else
RR1 = Int(Rnd * 21)
Select Case RR1
Case 0 To 5
RR = 0
Case 6 To 10
RR = 1
Case 11 To 14
RR = 2
Case 15 To 17
RR = 3
Case 18 To 19
RR = 4
Case Else
RR = 5
End Select
End If
BRnd = BR(RR): BR(7) = BR(RR)
For I = RR To 6
BR(I) = BR(I + 1)
Next I
Exit Function

CE:
'Beep
End Function

Sub FChange() 'フレームチェンジ
On Error GoTo CE
Dim BDCh As Boolean, BCCh As Boolean, BFCh(3) As Boolean, StrCh(1) As Boolean, MsgCh(1) As Boolean, BGCh As Boolean
Dim I As Long, U As Long, Y As Long, RR As Long, RRR As String, Ch As Long

'For I = 0 To 19
'StringD 16, I * 16, 0, StrMsgOrg(I) + "<" + Str$(-StrMsgToku(I)), False
'Next I
'StringD 0, SMPos * 16 - 8, 11, ">", False
'TextD 32, 320, 0, SMPos, True, 2
'TextD 80, 320, 0, SMBPos, True, 2

If WS <> 1 And WMM Then WndL = Form1.Left: WndT = Form1.Top
Do
DoEvents
WSS = WS: WS = Form1.WindowState

If (WMM Xor WM) Or (WSS <> 1 And WS = 1) Then
WMC = True
BDCh = (BDSf Is Nothing)
BCCh = (BCSf Is Nothing)
For I = 0 To 3
BFCh(I) = (BFSf(I) Is Nothing)
If I < 2 Then StrCh(I) = (StrSf(I) Is Nothing)
Next I
BGCh = (BGSf Is Nothing)
BBSf.BltColorFill RC0, 0
If Not WMM Then PrSf.BltColorFill RC0, 0
SfDestroy
DD.RestoreDisplayMode
DD.SetCooperativeLevel Form1.hWnd, DDSCL_NORMAL
Set DIDM = Nothing
End If

If WS = 1 Then
NCC = NC: NC = 0: For I = 0 To 1: PLID1(I) = 0: JN1(I) = vbNullString$: Next I
If Net Then 'ネットワーク
SessionCheck
If NC >= 2 Then RRR = String$(3, 62 + NetPar * 2) + " " + Left$(JN, InStr(JN, " ") - 1 - (InStr(JN, " ") = 0) * 7) + " - " + FormCapOrg Else RRR = FormCapOrg
If Form1.Caption <> RRR Then Form1.Caption = RRR
If NCC < 2 And NC >= 2 Then Beep: StrMsgLogAdd "": StrMsgLogAdd "#: " + Left$(HNN, InStr(HNN, " ") - 1 - (InStr(HNN, " ") = 0) * 7) + " " + String$(3, 62 + NetPar * 2) + " " + Left$(JN, InStr(JN, " ") - 1 - (InStr(JN, " ") = 0) * 7): TrMAdd = True
If NCC >= 2 And NC < 2 Then NetDestroy: NetCreate: NameCreate: ParStC = -1: If GGMode = 7 Then GFin = 2
End If
End If

If (WS <> 1 And (WMM Xor WM)) Or (WSS = 1 And WS <> 1) Then
Form1.WindowState = 2 + WM * 2
If WM Then Form1.Left = WndL: Form1.Top = WndT: Form1.Width = WndWW: Form1.Height = WndHH
Form1.Caption = FormCapOrg
'画面の変更
If Not WM Then
DD.SetCooperativeLevel Form1.hWnd, DDSCL_EXCLUSIVE Or DDSCL_FULLSCREEN Or DDSCL_ALLOWMODEX
FullScrErr = False
On Error GoTo CDisp
DD.SetDisplayMode WW, HH, BPP, 60, DDSDM_DEFAULT
On Error GoTo CE
If FullScrErr Then
FullScrErr = False
On Error GoTo CDisp
DD.SetDisplayMode WW, HH, BPP, 0, DDSDM_DEFAULT
On Error GoTo CE
If FullScrErr Then WM = True
End If
End If
If WM Then
DD.SetCooperativeLevel Picture1.hWnd, DDSCL_NORMAL
End If
'マウスデバイスの作成
If Not WM Then
Set DIDM = DI.CreateDevice("GUID_SysMouse")
DIDM.SetCommonDataFormat DIFORMAT_MOUSE
DIDM.SetCooperativeLevel Form1.hWnd, DISCL_FOREGROUND Or DISCL_EXCLUSIVE
DIDM.Acquire
End If
'サーフェイス作成
SfCreate
If Not BDCh Then 'ボード（オフスクリーン）の作成
BDCr BD
End If
If Not BCCh Then 'ブロック（オフスクリーン）の作成
If GGMode <> 5 Then BCCr BC Else BCCr 1
End If
For I = 0 To 3
If Not BFCh(I) Then 'フィールド（オフスクリーン）の作成
SD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
SD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
SD.lWidth = 160 + (GGMode = 4) * 40: SD.lHeight = 352 + (GGMode = 4) * 88
Set BFSf(I) = DD.CreateSurface(SD)
CK.low = 0: CK.high = 0
BFSf(I).SetColorKey DDCKEY_SRCBLT, CK
If Not WM Then BFSf(I).SetPalette MPal
BFSf(I).BltColorFill RC0, 0
For U = 1 To 22
Ch = 0
For Y = 3 To 12
If BF1(I, Y, U) >= 1 And BF1(I, Y, U) <= 8 Then Ch = Ch + 1
Next Y
If Ch < 10 Then
For Y = 0 To 9
If BF1(I, Y + 3, U) >= 1 And BF1(I, Y + 3, U) <= 8 Then
With Src
.Left = (BF1(I, Y + 3, U) - 1) * (16 + (GGMode = 4) * 4): .Top = 16 + (GGMode = 4) * 4: .Right = .Left + 16 + (GGMode = 4) * 4: .Bottom = .Top + 16 + (GGMode = 4) * 4
End With
BFSf(I).BltFast Y * (16 + (GGMode = 4) * 4), (U - 1) * (16 + (GGMode = 4) * 4), BCSf, Src, DDBLTFAST_WAIT
End If
Next Y
End If
Next U
End If
If I < 2 Then
If Not StrCh(I) Then 'テキスト（オフスクリーン）の作成
SD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
SD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
SD.lWidth = 160: SD.lHeight = 144
Set StrSf(I) = DD.CreateSurface(SD)
CK.low = 0: CK.high = 0
StrSf(I).SetColorKey DDCKEY_SRCBLT, CK
If Not WM Then StrSf(I).SetPalette MPal
StrSf(I).SetFont Picture1.Font
StrMsgDraw I
End If
End If
Next I
If Not BGCh Then If GGMode <> 5 Then BGCr Stt Else BGCrTit '背景の作成
End If

WMM = WM
Loop While WS = 1

'画面フェードイン／アウト
If Fade Then 'フェードイン
If ScrFC < 80 Then
FadeIn ScrFC
End If
Else 'フェードアウト
If ScrFC < 80 Then
FadeOut ScrFC
Else
If Not FS Then BBSf.BltColorFill RC0, 0
End If
End If
If ScrFC < 80 Then ScrFC = ScrFC + 1

If Tit Then 'タイトル
'If TC = 440 Then Fade = True: ScrFC = 0
If TC = 1140 Then Fade = False: ScrFC = 0
If TC = 1220 Then GMode = 4: GEnd = True: OfBt = False: TrM = 1: Fade = True: ScrFC = 0
If TC = 1845 Then Fade = False: ScrFC = 0
If TC = 1925 Then
Fnl = -Int(Rnd * (1 - (DTCl >= 900)))
If Fnl Then TrM = 2: TA = -Int(Rnd * (1 - (FnLv >= 300))) Else TrM = Int(Rnd * (2 - (ScLiv(0) > 0 And ScLiv(1) > 0))): TA = True
GMode = 1: Trn = False: GEnd = True: Fade = True: ScrFC = 0
End If
If TC = 2760 Then Fade = False: ScrFC = 0
If TC = 2850 Then GMode = 5: GEnd = True

'TextD 0, 0, 0, TC, False, 11

KeyScanAll
If CsrAA = 1 Or CsrSS = 1 Then PlayMusic vbNullString$: GMode = 5: GEnd = True
TC = TC + 1
End If

On Error Resume Next

NCC = NC: NC = 0: For I = 0 To 1: PLID1(I) = 0: JN1(I) = vbNullString$: Next I
If Net Then 'ネットワーク
SessionCheck
If NC >= 2 Then StringD 448, 0, 11, String$(3, 62 + NetPar * 2) + " " + JN, False, 10
If NCC < 2 And NC >= 2 Then Beep: StrMsgLogAdd "": StrMsgLogAdd "#: " + Left$(HNN, InStr(HNN, " ") - 1 - (InStr(HNN, " ") = 0) * 7) + " " + String$(3, 62 + NetPar * 2) + " " + Left$(JN, InStr(JN, " ") - 1 - (InStr(JN, " ") = 0) * 7): TrMAdd = True
End If

If Not Net Or NC < 2 Then ParStC = -1

If Net And NC >= 2 And GGMode <> 7 And ParStC < 0 Then '親参加手続き開始
While DP.GetMessageCount(PlID) > 0: Set DPM = DP.Receive(JID, PlID, DPRECEIVE_ALL): Set DPM = Nothing: Wend
Set DPM = DP.CreateMessage: DPM.WriteLong 2147483647: DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing
ParStC = 0
End If

If Net And GGMode <> 7 And ParStC >= 0 Then '親参加手続き
While DP.GetMessageCount(PlID) > 0 And ParStC < 115
Set DPM = DP.Receive(JID, PlID, DPRECEIVE_ALL): RR = DPM.ReadLong: Set DPM = Nothing
If ParStC = 1 Then NetQQ = (RR Mod 6): ParTrM = (Int(RR / 6) Mod 3)
If ParStC >= 2 And ParStC < 113 Then ParBtNx(ParStC - 2) = RR
If ParStC >= 113 And ParStC < 115 Then ParDBS(ParStC - 113) = RR
If RR = 2147483647 Or ParStC > 0 Then ParStC = ParStC + 1
If ParStC >= 115 And TrMAdd And StrMsgLogC >= 0 Then StrMsgLog(StrMsgLogC) = StrMsgLog(StrMsgLogC) + " (" + TrMN(ParTrM) + ")": TrMAdd = False
Wend
End If
On Error GoTo CE

'StringD 0, 0, 0, JN1(0), False
'StringD 0, 16, 0, JN1(1), False
'TextD 128, 0, 0, PLID1(0), False
'TextD 128, 16, 0, PLID1(1), False
'TextD 128, 32, 0, PlID, False

If ClkC Then '時刻表示
Clk = Time$
ClkH = Hour(Clk): ClkM = Minute(Clk): ClkS = Second(Clk)
StringD 0, 0, 5 - ((ClkM Mod 30) = 0 And ClkS < 2) * 6, Space$(-(ClkH < 10)) + Mid$(Str$(ClkH), 2) + ":" + Left$("0", -(ClkM < 10)) + Mid$(Str$(ClkM), 2) + Left$(Chr$(39) + Left$("0", -(ClkS < 10)) + Mid$(Str$(ClkS), 2) + " ", -(GGMode = 5) * 4) + Space$(8 + (GGMode = 5) * 4), False, 12
End If

CC = CC + 1 + (CC >= 44) * 45
CCC = CCC + 1 + (CCC >= 13) * 14
LAni = LAni + 1 + (LAni >= 9) * 10
RushAniC = RushAniC + 1 + (RushAniC >= 159) * 160

'フリッピング
With Src
.Left = 0: .Top = 0: .Right = 640: .Bottom = 480
End With
If WM Then DX.GetWindowRect Picture1.hWnd, Des Else Des.Left = 0: Des.Top = 0
Des.Right = Des.Left + 640: Des.Bottom = Des.Top + 480
Select Case TV - (WM And TV = 0)
Case 0 'フリップモード
Tic = DX.TickCount: TE1 = Tic - Int(Tic / 65536) * 65536
If Not FS Then PrSf.Flip BBSf, DDFLIP_WAIT
Case 1 'TVモード
Tic = DX.TickCount: TE1 = Tic - Int(Tic / 65536) * 65536
If Not FS Then While DD.GetVerticalBlankStatus = 1: Wend: While DD.GetVerticalBlankStatus = 0: Wend: PrSf.Blt Des, BBSf, RC0, DDBLT_WAIT
Case 2 To 3 'Otherモード
Do
Tic = DX.TickCount: TE2 = Tic - Int(Tic / 65536) * 65536: TE = TE2 - TE1: TE1 = TE2: If TE < 0 Then TE = TE + 65536
TEE = TEE + TE * 3
Loop While TEE < 50
If Not FS Then PrSf.Blt Des, BBSf, RC0, DDBLT_WAIT
TEE = TEE - 50 + (TEE >= 100) * (TEE - 100)
End Select
Tic = Int(Timer * 1000): SpdE2 = Tic - Int(Tic / 60000) * 60000: SpdE = SpdE2 - SpdE1: SpdE1 = SpdE2: If SpdE < 0 Then SpdE = SpdE + 60000
Tic = DX.TickCount: E2 = Tic - Int(Tic / 65536) * 65536: E = E2 - E1: E1 = E2: If E < 0 Then E = E + 65536
FS = Not FS And (TV = 3 Or AFS And E >= 25)
'サウンド
For I = 0 To 15
If Sou(I).On Then
Sou(I).On = False
If Not Mut And SVol > 0 And Sou(I).AB Then
If Sou(I).DbC Then
SSB(I).Stop: SSB(I).SetCurrentPosition 0: SSB(I).SetVolume -3000 + SVol * 300: SSB(I).SetFrequency Sou(I).Fre: SSB(I).SetPan Fix(Sou(I).Pan * (SPan / 2)): SSB(I).Play DSBPLAY_DEFAULT
Else
SB(I).Stop: SB(I).SetCurrentPosition 0: SB(I).SetVolume -3000 + SVol * 300: SB(I).SetFrequency Sou(I).Fre: SB(I).SetPan Fix(Sou(I).Pan * (SPan / 2)): SB(I).Play DSBPLAY_DEFAULT
End If
Sou(I).DbC = Not Sou(I).DbC
End If
End If
Next I
DIDK.GetDeviceStateKeyboard KS
If KS.Key(DIK_ESCAPE) <> 0 Then EscC = EscC + 1 + (EscC >= 2) Else EscC = 0
If EscC = 1 Then
If Not StrEdit Then If GGMode <> 5 Or Mn = 0 Then PlayMusic vbNullString$: GFin = 2: GMode = -(GGMode <> 5) * 5: GEnd = True
End If
Exit Sub

CDisp:
FullScrErr = True
Resume Next
Exit Sub

CE:
'Beep
End Sub

Sub FadeIn(ByVal FadeC As Long) '画面フェードイン
On Error GoTo CE
Dim I As Long, RR As Long

If FadeC < 80 Then
For I = 0 To 19
If (I Mod 2) = 0 Then
RR = 480 - ((40 - FadeC + I * 2) ^ 2) * 0.3
If FadeC - I * 2 < 40 Then
If RR < 0 Then RR = 0
With Src
.Left = I * 32: .Top = 480 - RR: .Right = .Left + 32: .Bottom = 480
End With
If Not FS Then BBSf.BltFast I * 32, 0, BBSf, Src, DDBLTFAST_WAIT
With Des
.Left = I * 32: .Top = RR: .Right = .Left + 32: .Bottom = 480
End With
If Not FS Then BBSf.BltColorFill Des, 0
End If
Else
RR = ((40 - FadeC + I * 2) ^ 2) * 0.3
If FadeC - I * 2 < 40 Then
If RR > 480 Then RR = 480
With Src
.Left = I * 32: .Top = 0: .Right = .Left + 32: .Bottom = 480 - RR
End With
If Not FS Then BBSf.BltFast I * 32, RR, BBSf, Src, DDBLTFAST_WAIT
With Des
.Left = I * 32: .Top = 0: .Right = .Left + 32: .Bottom = RR
End With
If Not FS Then BBSf.BltColorFill Des, 0
End If
End If
Next I
End If
Exit Sub

CE:
'Beep
End Sub

Sub FadeOut(ByVal FadeC As Long) '画面フェードアウト
On Error GoTo CE
Dim I As Long, RR As Long

If FadeC < 80 Then
For I = 0 To 19
If (I Mod 2) = 0 Then
RR = 480 - ((FadeC - I * 2) ^ 2) * 0.3
If FadeC - I * 2 >= 0 Then
If RR < 0 Then RR = 0
With Src
.Left = I * 32: .Top = 480 - RR: .Right = .Left + 32: .Bottom = 480
End With
If Not FS Then BBSf.BltFast I * 32, 0, BBSf, Src, DDBLTFAST_WAIT
With Des
.Left = I * 32: .Top = RR: .Right = .Left + 32: .Bottom = 480
End With
If Not FS Then BBSf.BltColorFill Des, 0
End If
Else
RR = ((FadeC - I * 2) ^ 2) * 0.3
If FadeC - I * 2 >= 0 Then
If RR > 480 Then RR = 480
With Src
.Left = I * 32: .Top = 0: .Right = .Left + 32: .Bottom = 480 - RR
End With
If Not FS Then BBSf.BltFast I * 32, RR, BBSf, Src, DDBLTFAST_WAIT
With Des
.Left = I * 32: .Top = 0: .Right = .Left + 32: .Bottom = RR
End With
If Not FS Then BBSf.BltColorFill Des, 0
End If
End If
Next I
Else
If Not FS Then BBSf.BltColorFill RC0, 0
End If
Exit Sub

CE:
'Beep
End Sub

Sub CPAI(ByVal BMM As Long) 'CP思考ルーチン
On Error GoTo CE
Dim I As Long, U As Long, Y As Long, T As Long, P As Long, P1 As Long, P2 As Long
If BMM = 0 Then MCS1(PL) = -10000
For BXX1(PL) = 1 To 11
CS1(PL) = -9999
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BXX1(PL) + B(Nx1(PL, 0), BMM, I, 0), B(Nx1(PL, 0), BMM, I, 1) + 1) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BYY1(PL) = 1
Do
BYY1(PL) = BYY1(PL) + 1
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BXX1(PL) + B(Nx1(PL, 0), BMM, I, 0), BYY1(PL) + B(Nx1(PL, 0), BMM, I, 1)) > 0 Then Ch1(PL) = 1
Next I
Loop While Ch1(PL) = 0
BYY1(PL) = BYY1(PL) - 1
If GMode <> 4 And CPE(CPG1(PL)).Spd <= 1 Then P1 = 0: P2 = 3 Else P1 = BMM: P2 = BMM
For P = P1 To P2: For Y = BYY1(PL) To BYY1(PL) - (GMode <> 4 And CPE(CPG1(PL)).SRt And P <> BMM And (Nx1(PL, 0) <> 0 And Nx1(PL, 0) <> 3 And Nx1(PL, 0) <> 4) Or (P Mod 2) <> (BMM Mod 2)): For T = BXX1(PL) + (BXX1(PL) > 1) To BXX1(PL) - (BXX1(PL) < 11)
CS1(PL) = 0
For I = 0 To 4: For U = 0 To 5
CS1(PL) = CS1(PL) - (BF1(PL, T - 1 + U, Y + I) > 0) * CPA(Nx1(PL, 0), P, U, I) * 10
Next U, I
CS1(PL) = CS1(PL) + Y * CPE(CPG1(PL)).DP + (Y < 8) * (120 + (8 - Y) * 30) + (Y < 2) * 600
CS1(PL) = CS1(PL) + Abs(T - 6) * CPE(CPG1(PL)).LR
CS1(PL) = CS1(PL) - (P = BMM) * 3 - (T = BXX1(PL)) * 5 - (Y = BYY1(PL)) * 15
CS1(PL) = CS1(PL) - ((Nx1(PL, 0) = 0 Or Nx1(PL, 0) = 3 Or Nx1(PL, 0) = 4) And T <= 6 And P = 3) * 8
If CS1(PL) > MCS1(PL) Then MCS1(PL) = CS1(PL): CSM1(PL) = BMM: CSX1(PL) = BXX1(PL): CS2M1(PL) = P: CS2X1(PL) = T
Next T, Y, P
End If
Next BXX1(PL)
Exit Sub

CE:
'Beep
End Sub

Sub DTClRate() '攻略率
On Error GoTo CE
Dim I As Long
DTLiv = 0: CPMax = 9: DTCl = 0

For I = 0 To 2
DTLiv = DTLiv + ScLiv(I) + TmLiv(I)
Next I
DTLiv = DTLiv - (FnLv >= 300) + LsLiv

If ScLiv(0) > 0 Then CPMax = CPMax + 1
If ScLiv(0) >= 5 Then CPMax = CPMax + 1
If ScLiv(1) > 0 Then CPMax = CPMax + 1
If ScLiv(1) >= 5 Then CPMax = CPMax + 1
If ScLiv(2) > 0 Then CPMax = CPMax + 1
If ScLiv(2) >= 5 Then CPMax = CPMax + 1
If TmLiv(0) > 0 Then CPMax = CPMax + 1
If TmLiv(0) >= 3 Then CPMax = CPMax + 1
If TmLiv(1) > 0 Then CPMax = CPMax + 1
If TmLiv(1) >= 3 Then CPMax = CPMax + 1
If TmLiv(2) > 0 Then CPMax = CPMax + 1
If TmLiv(2) >= 3 Then CPMax = CPMax + 1
If FnLv >= 300 Then CPMax = CPMax + 1
If FnLv > 300 Then CPMax = CPMax + 1
If LsLiv > 0 Then CPMax = CPMax + 1
If LsLiv >= 10 Then CPMax = CPMax + 1

'LIVES(SCORE ATTACK,TIME ATTACK)
For I = 0 To 2
If ScLiv(I) >= 5 Then DTCl = DTCl + 100 Else DTCl = DTCl + ScLiv(I) * 20
If TmLiv(I) >= 3 Then DTCl = DTCl + 60 Else DTCl = DTCl + TmLiv(I) * 20
Next I
'LEVEL(SCORE ATTACK)
If ScLv(0) < 10 Then DTCl = DTCl + ScLv(0) * 4
If ScLv(0) >= 10 And ScLv(0) < 20 Then DTCl = DTCl + 40 + (ScLv(0) - 10) * 6
If ScLv(0) >= 20 And ScLv(0) < 25 Then DTCl = DTCl + 100
If ScLv(0) >= 25 And ScLv(0) < 30 Then DTCl = DTCl + 100 + (ScLv(0) - 25) * 8
If ScLv(0) >= 30 Then DTCl = DTCl + 140
If ScLv(1) < 20 Then DTCl = DTCl + ScLv(1) * 2
If ScLv(1) >= 20 And ScLv(1) < 25 Then DTCl = DTCl + 40
If ScLv(1) >= 25 And ScLv(1) < 40 Then DTCl = DTCl + 40 + (ScLv(1) - 25) * 4
If ScLv(1) >= 40 And ScLv(1) < 45 Then DTCl = DTCl + 100
If ScLv(1) >= 45 And ScLv(1) < 50 Then DTCl = DTCl + 100 + (ScLv(1) - 45) * 8
If ScLv(1) >= 50 Then DTCl = DTCl + 140
If ScLv(2) < 20 Then DTCl = DTCl + ScLv(2) * 2
If ScLv(2) >= 20 And ScLv(2) < 25 Then DTCl = DTCl + 40
If ScLv(2) >= 25 And ScLv(2) < 40 Then DTCl = DTCl + 40 + (ScLv(2) - 25) * 2
If ScLv(2) >= 40 And ScLv(2) < 45 Then DTCl = DTCl + 70
If ScLv(2) >= 45 And ScLv(2) < 50 Then DTCl = DTCl + 70 + (ScLv(2) - 45) * 2
If ScLv(2) >= 50 And ScLv(2) < 200 Then DTCl = DTCl + 80 + Int((ScLv(2) - 50) * 0.4)
If ScLv(2) >= 200 Then DTCl = DTCl + 140
'LEVEL(FINAL)
If FnLv >= 200 And FnLv < 300 Then DTCl = DTCl + (FnLv - 200)
If FnLv >= 300 Then DTCl = DTCl + 100
If FnLv > 300 Then DTCl = DTCl + 50
'LIVES(FURTHEST)
If LsLiv >= 10 Then DTCl = DTCl + 200 Else DTCl = DTCl + LsLiv * 20

Exit Sub

CE:
'Beep
End Sub

Sub TokuLoad() '特殊文字ロード
On Error GoTo CE
Dim I As Long, U As Long, Str0 As String, Str1 As Long, Str2 As Long, SpcPos As Long, StrCh As Boolean
TokuMx = -1
If RecCh Then
If XDir(AppDir + "DIC.TXT", vbHidden) = vbNullString$ Then
I = 0
Toku(I, 0) = "!": Toku(I, 1) = "！": I = I + 1
Toku(I, 0) = "?": Toku(I, 1) = "？": I = I + 1
Toku(I, 0) = "[": Toku(I, 1) = "「": I = I + 1
Toku(I, 0) = "]": Toku(I, 1) = "」": I = I + 1
Toku(I, 0) = "-": Toku(I, 1) = "ー": I = I + 1
Toku(I, 0) = "~": Toku(I, 1) = "〜": I = I + 1
Toku(I, 0) = ".": Toku(I, 1) = "。": I = I + 1
Toku(I, 0) = ",": Toku(I, 1) = "、": I = I + 1
Toku(I, 0) = "/": Toku(I, 1) = "・": I = I + 1
Toku(I, 0) = "、、、": Toku(I, 1) = "…": I = I + 1
Toku(I, 0) = "。。。": Toku(I, 1) = "…": I = I + 1
Toku(I, 0) = "^(": Toku(I, 1) = "【": I = I + 1
Toku(I, 0) = "^)": Toku(I, 1) = "】": I = I + 1
Toku(I, 0) = "^「": Toku(I, 1) = "『": I = I + 1
Toku(I, 0) = "^」": Toku(I, 1) = "』": I = I + 1
Toku(I, 0) = "^ほし": Toku(I, 1) = "☆": I = I + 1
Toku(I, 0) = "^まる": Toku(I, 1) = "○": I = I + 1
Toku(I, 0) = "^おんぷ": Toku(I, 1) = "♪": I = I + 1
Toku(I, 0) = "^しかく": Toku(I, 1) = "□": I = I + 1
Toku(I, 0) = "^さんかく": Toku(I, 1) = "△": I = I + 1
TokuMx = I - 1
FFi = FreeFile
Open AppDir + "dic.txt" For Output As #FFi
For I = 0 To TokuMx
If I > 0 Then If Toku(I, 1) <> Toku(I - 1, 1) Then Print #FFi,
If I > 0 Then
If Toku(I, 1) <> Toku(I - 1, 1) Then Print #FFi, "[" + Toku(I, 1) + "]"
Else
Print #FFi, "[" + Toku(I, 1) + "]"
End If
Print #FFi, "(" + Toku(I, 0) + ")"
Next I
Close #FFi
Else
FFi = FreeFile
Open AppDir + "DIC.TXT" For Input As #FFi
I = 0: StrCh = False
While I < 500 And Not EOF(FFi)
Line Input #FFi, Str0
For U = 0 To 32: SpcPos = InStr(Str0, Chr$(U)): While SpcPos > 0: Str0 = Left$(Str0, SpcPos - 1) + Mid$(Str0, SpcPos + 1): SpcPos = InStr(Str0, Chr$(U)): Wend: Next U
If Not StrCh Then '単語判定,読み追加判定
If Len(Str0) > 2 And Left$(Str0, 1) = "[" And Right$(Str0, 1) = "]" Then Toku(I, 1) = Mid$(Str0, 2, Len(Str0) - 2): StrCh = True
If Not StrCh Then If I > 0 And Len(Str0) > 2 And Left$(Str0, 1) = "(" And Right$(Str0, 1) = ")" Then Toku(I, 1) = Toku(I - 1, 1): Toku(I, 0) = Mid$(Str0, 2, Len(Str0) - 2): I = I + 1: StrCh = False
Else '読み判定,単語新規判定
If Len(Str0) > 2 And Left$(Str0, 1) = "(" And Right$(Str0, 1) = ")" Then Toku(I, 0) = Mid$(Str0, 2, Len(Str0) - 2): I = I + 1: StrCh = False
If StrCh Then If Len(Str0) > 2 And Left$(Str0, 1) = "[" And Right$(Str0, 1) = "]" Then Toku(I, 1) = Mid$(Str0, 2, Len(Str0) - 2) Else StrCh = True
End If
Wend
TokuMx = I - 1
Close #FFi
End If
End If
Exit Sub

CE:
'Beep
End Sub

Sub EnvLoad() '環境データロード
On Error GoTo CE
Dim I As Long
If XDir(AppDir + "ENV.DTD", vbHidden) = vbNullString$ Or RecNew Then
With EvDt
.Midi = -1: .SVol = 10: .SPan = 2
.BG = 2: .BAni = True: .BSm = True: .Zanzo = True: .BC = 1: .BD = 3: .MNx = 4: .ClkC = False
.TV = 2: .AFS = True: .WM = False: .BGMem = False
.KArwL = DIK_NUMPAD4: .KArwR = DIK_NUMPAD6: .KArwU = DIK_NUMPAD8: .KArwD = DIK_NUMPAD2
.KBL = DIK_Z: .KBR = DIK_X: .KBS = DIK_C
.Net = False: .HNN = "GUEST": .NetQ = 2: .Jpn = True: .NetFldLR = 0
End With
Else
FFi = FreeFile
Open AppDir + "ENV.DTD" For Binary Access Read As #FFi
Get #FFi, , EvDt
Close #FFi
End If
With EvDt
Midi = .Midi: SVol = (.SVol Mod 11): SPan = (.SPan Mod 11)
BG = .BG: BAni = .BAni: BSm = .BSm: Zanzo = .Zanzo: BC = (.BC Mod 4): BD = (.BD Mod 4): MNx = (.MNx Mod 5): ClkC = .ClkC
TV = (.TV Mod 4): AFS = .AFS: WM = .WM: BGMem = .BGMem
KArwL = .KArwL: KArwR = .KArwR: KArwU = .KArwU: KArwD = .KArwD
KBL = .KBL: KBR = .KBR: KBS = .KBS
Net = .Net: HNN = .HNN: NetQ = (.NetQ Mod 6): Jpn = .Jpn: NetFldLR = (.NetFldLR Mod 3)
End With
Exit Sub

CE:
'Beep
End Sub

Sub RecLoad() 'レコードデータロード
On Error GoTo CE
Dim I As Long, RecErr As Boolean
RecCh = False
If XDir(AppDir + "REC.DTD", vbHidden) <> vbNullString$ Then
FFi = FreeFile
Open AppDir + "REC.DTD" For Binary Access Read As #FFi
Get #FFi, , RcDt
Close #FFi
Chk1 = 0: Chk2 = 0
With RcDt
For I = 0 To 14: Chk1 = Chk1 + (.SRnk(I).SC Mod 6144): Chk1 = (Chk1 Mod 6144): Chk1 = Chk1 + (.SRnk(I).Li Mod 6144): Chk1 = (Chk1 Mod 6144): Next I
For I = 0 To 14: Chk1 = Chk1 + (.TRnk(I).Tm Mod 6144): Chk1 = (Chk1 Mod 6144): Chk1 = Chk1 + (.TRnk(I).Li Mod 6144): Chk1 = (Chk1 Mod 6144): Next I
For I = 0 To 4: Chk1 = Chk1 + (.FRnk(I).Lv Mod 6144): Chk1 = (Chk1 Mod 6144): Chk1 = Chk1 + (.FRnk(I).Tm Mod 6144): Chk1 = (Chk1 Mod 6144): Next I
For I = 0 To 4: Chk1 = Chk1 + (.LRnk(I).Liv Mod 6144): Chk1 = (Chk1 Mod 6144): Chk1 = Chk1 + (.LRnk(I).Tm Mod 6144): Chk1 = (Chk1 Mod 6144): Next I
For I = 0 To 2: Chk2 = Chk2 + (.ScLiv(I) Mod 6144): Chk2 = (Chk2 Mod 6144): Chk2 = Chk2 + (.ScLv(I) Mod 6144): Chk2 = (Chk2 Mod 6144): Next I
For I = 0 To 2: Chk2 = Chk2 + (.TmLiv(I) Mod 6144): Chk2 = (Chk2 Mod 6144): Next I
Chk2 = Chk2 + (.FnLv Mod 6144): Chk2 = (Chk2 Mod 6144)
Chk2 = Chk2 + (.LsLiv Mod 6144): Chk2 = (Chk2 Mod 6144)
If .Chk = Chk2 * 6144 + Chk1 And .FnLv >= 50 Then RecCh = (Not RecNew) Else RecErr = True
End With
End If
If Not RecCh Then
With RcDt
For I = 0 To 4: .SRnk(I).Nm = "______": .SRnk(I).SC = 30000 - I * 5000: .SRnk(I).Li = 50 - I * 5: Next I
For I = 0 To 4: .SRnk(5 + I).Nm = "______": .SRnk(5 + I).SC = 200000 - I * 40000: .SRnk(5 + I).Li = 150 - I * 25: Next I
For I = 0 To 4: .SRnk(10 + I).Nm = "______": .SRnk(10 + I).SC = 1000000 - I * 200000: .SRnk(10 + I).Li = 300 - I * 45: Next I
For I = 0 To 4: .TRnk(I).Nm = "______": .TRnk(I).Tm = 30000 + I * 6000: .TRnk(I).Li = 100: Next I
For I = 0 To 4: .TRnk(5 + I).Nm = "______": .TRnk(5 + I).Tm = 24000 + I * 3000: .TRnk(5 + I).Li = 100: Next I
For I = 0 To 4: .TRnk(10 + I).Nm = "______": .TRnk(10 + I).Tm = 18000 + I * 1500: .TRnk(10 + I).Li = 100: Next I
For I = 0 To 4: .FRnk(I).Nm = "______": .FRnk(I).Lv = 200: .FRnk(I).Tm = 60000: Next I
For I = 0 To 4: .LRnk(I).Nm = "______": .LRnk(I).Liv = 1: .LRnk(I).Tm = 60000: Next I
For I = 0 To 2: .ScLiv(I) = 0: .ScLv(I) = 0: Next I
For I = 0 To 2: .TmLiv(I) = 0: Next I
.FnLv = 50
.LsLiv = 0
End With
RecCh = Not RecErr '= RecNew
End If
With RcDt
For I = 0 To 14: SRnk(I) = .SRnk(I): Next I
For I = 0 To 14: TRnk(I) = .TRnk(I): Next I
For I = 0 To 4: FRnk(I) = .FRnk(I): Next I
For I = 0 To 4: LRnk(I) = .LRnk(I): Next I
For I = 0 To 2: ScLiv(I) = .ScLiv(I): ScLv(I) = .ScLv(I): Next I
For I = 0 To 2: TmLiv(I) = .TmLiv(I): Next I
FnLv = .FnLv
LsLiv = .LsLiv
End With
Exit Sub

CE:
'Beep
End Sub

Sub EnvSave() '環境データセーブ
On Error GoTo CE
Dim At As VbFileAttribute
Dim I As Long
If XDir(AppDir + "ENV.DTD", vbHidden) <> vbNullString$ Then
At = GetAttr(AppDir + "ENV.DTD"): If At And vbReadOnly Then SetAttr AppDir + "ENV.DTD", At And Not vbReadOnly
End If
With EvDt
.Midi = Midi: .SVol = SVol: .SPan = SPan
.BG = BG: .BAni = BAni: .BSm = BSm: .Zanzo = Zanzo: .BC = BC: .BD = BD: .MNx = MNx: .ClkC = ClkC
.TV = TV: .AFS = AFS: .WM = WM: .BGMem = BGMem
.KArwL = KArwL: .KArwR = KArwR: .KArwU = KArwU: .KArwD = KArwD
.KBL = KBL: .KBR = KBR: .KBS = KBS
.Net = Net: .HNN = HNN: .NetQ = NetQ: .Jpn = Jpn: .NetFldLR = NetFldLR
End With
FFi = FreeFile
Open AppDir + "env.dtd" For Binary Access Write As #FFi
Put #FFi, , EvDt
Close #FFi
Exit Sub

CE:
'Beep
End Sub

Sub RecSave() 'レコードデータセーブ
On Error GoTo CE
Dim At As VbFileAttribute
Dim I As Long
If XDir(AppDir + "REC.DTD", vbHidden) <> vbNullString$ Then
At = GetAttr(AppDir + "REC.DTD"): If At And vbReadOnly Then SetAttr AppDir + "REC.DTD", At And Not vbReadOnly
End If
With RcDt
For I = 0 To 14: .SRnk(I) = SRnk(I): Next I
For I = 0 To 14: .TRnk(I) = TRnk(I): Next I
For I = 0 To 4: .FRnk(I) = FRnk(I): Next I
For I = 0 To 4: .LRnk(I) = LRnk(I): Next I
For I = 0 To 2: .ScLiv(I) = ScLiv(I): .ScLv(I) = ScLv(I): Next I
For I = 0 To 2: .TmLiv(I) = TmLiv(I): Next I
.FnLv = FnLv
.LsLiv = LsLiv
Chk1 = 0: Chk2 = 0
For I = 0 To 14: Chk1 = Chk1 + (.SRnk(I).SC Mod 6144): Chk1 = (Chk1 Mod 6144): Chk1 = Chk1 + (.SRnk(I).Li Mod 6144): Chk1 = (Chk1 Mod 6144): Next I
For I = 0 To 14: Chk1 = Chk1 + (.TRnk(I).Tm Mod 6144): Chk1 = (Chk1 Mod 6144): Chk1 = Chk1 + (.TRnk(I).Li Mod 6144): Chk1 = (Chk1 Mod 6144): Next I
For I = 0 To 4: Chk1 = Chk1 + (.FRnk(I).Lv Mod 6144): Chk1 = (Chk1 Mod 6144): Chk1 = Chk1 + (.FRnk(I).Tm Mod 6144): Chk1 = (Chk1 Mod 6144): Next I
For I = 0 To 4: Chk1 = Chk1 + (.LRnk(I).Liv Mod 6144): Chk1 = (Chk1 Mod 6144): Chk1 = Chk1 + (.LRnk(I).Tm Mod 6144): Chk1 = (Chk1 Mod 6144): Next I
For I = 0 To 2: Chk2 = Chk2 + (.ScLiv(I) Mod 6144): Chk2 = (Chk2 Mod 6144): Chk2 = Chk2 + (.ScLv(I) Mod 6144): Chk2 = (Chk2 Mod 6144): Next I
For I = 0 To 2: Chk2 = Chk2 + (.TmLiv(I) Mod 6144): Chk2 = (Chk2 Mod 6144): Next I
Chk2 = Chk2 + (.FnLv Mod 6144): Chk2 = (Chk2 Mod 6144)
Chk2 = Chk2 + (.LsLiv Mod 6144): Chk2 = (Chk2 Mod 6144)
.Chk = Chk2 * 6144 + Chk1
End With
FFi = FreeFile
Open AppDir + "rec.dtd" For Binary Access Write As #FFi
Put #FFi, , RcDt
Close #FFi
Exit Sub

CE:
'Beep
End Sub

Function VCheck(ByVal Fname As String) As Boolean 'ビデオチェック
On Error GoTo CE
Dim I As Long, RR As Long, VSt As Boolean, StP As Long, VEd As Boolean, EdP As Long, SpP As Long
Dim VDSS As String, VDSSS As String
VCheck = False
VCDS = vbNullString$
FFi = FreeFile
Open Fname For Input As #FFi
VSt = False: VEd = False: I = 1
While Not EOF(FFi) And Not VEd And I <= 25
Do
Line Input #FFi, VDSS
If Not VSt Then StP = InStr(VDSS, "{"): If StP > 0 Then VDSS = Mid$(VDSS, StP): VSt = True
If Not VEd Then EdP = InStr(VDSS, "}"): If EdP > 0 Then VDSS = Left$(VDSS, EdP): VEd = True
Loop While Not EOF(FFi) And Not VSt
SpP = InStr(VDSS, " ")
While SpP > 0
VDSS = Left$(VDSS, SpP - 1) + Mid$(VDSS, SpP + 1)
SpP = InStr(VDSS, " ")
Wend
VCDS = VCDS + VDSS
I = I + Len(VDSS)
Wend
Close #FFi
If I > 25 Then
RR = 0
For I = 0 To 4
RR = RR * 64: RR = RR + Abs(InStr(VTx, Mid$(VCDS, 6 - I, 1)) - 1)
Next I
VDSc = RR
RR = 0
For I = 0 To 2
RR = RR * 64: RR = RR + Abs(InStr(VTx, Mid$(VCDS, 9 - I, 1)) - 1)
Next I
VDLi = RR
RR = 1228 + VDSc + VDLi * 37 'NoRUSH
If Mid$(VCDS, 10, 1) = Mid$(VTx, (RR Mod 53) + 1, 1) And Mid$(VCDS, 11, 1) = Mid$(VTx, (Int(RR / 53) Mod 53) + 1, 1) Then
VDTrM = Abs((InStr(VTx, Mid$(VCDS, 12, 1)) - 1) Mod 3)
VDTA = -Abs(Int((InStr(VTx, Mid$(VCDS, 12, 1)) - 1) / 3) Mod 2)
VDFnl = -Abs(Int((InStr(VTx, Mid$(VCDS, 12, 1)) - 1) / 6) Mod 2)
VDRush = False
If VDTrM >= 0 And VDTrM <= 2 And VDTA >= -1 And VDTA <= 0 And VDFnl >= -1 And VDFnl <= 0 And Not (VDFnl And VDTrM < 2) Then VCheck = True
Else
RR = 1229 + VDSc + VDLi * 37 'RUSH
If Mid$(VCDS, 10, 1) = Mid$(VTx, (RR Mod 53) + 1, 1) And Mid$(VCDS, 11, 1) = Mid$(VTx, (Int(RR / 53) Mod 53) + 1, 1) Then
VDTrM = Abs((InStr(VTx, Mid$(VCDS, 12, 1)) - 1) Mod 3)
VDTA = -Abs(Int((InStr(VTx, Mid$(VCDS, 12, 1)) - 1) / 3) Mod 2)
VDFnl = -Abs(Int((InStr(VTx, Mid$(VCDS, 12, 1)) - 1) / 6) Mod 2)
VDRush = True
If VDTrM >= 0 And VDTrM <= 2 And VDTA >= -1 And VDTA <= 0 And VDFnl >= -1 And VDFnl <= 0 And Not (VDFnl And VDTrM < 2) Then VCheck = True
End If
'Else
'FFi = FreeFile
'Open AppDir + "fail.txt" For Append As #FFi
'Print #FFi, Fname + ": " + Mid$(VCDS, 10, 2) + " " + Mid$(VTx, (RR Mod 53) + 1, 1) + Mid$(VTx, (Int(RR / 53) Mod 53) + 1, 1)
'Close #FFi
End If
End If
Exit Function

CE:
'Beep
End Function

Sub VLoad(ByVal Fname As String) 'ビデオロード
On Error GoTo CE
Dim I As Long, VSt As Boolean, StP As Long, VEd As Boolean, EdP As Long, SpP As Long
Dim VDSS As String
If XDir(Fname, vbHidden) <> vbNullString$ Then
FFi = FreeFile
Open Fname For Input As #FFi
VSt = False: VEd = False: I = 1
While Not EOF(FFi) And Not VEd
Do
Line Input #FFi, VDSS
If Not VSt Then StP = InStr(VDSS, "{"): If StP > 0 Then VDSS = Mid$(VDSS, StP): VSt = True
If Not VEd Then EdP = InStr(VDSS, "}"): If EdP > 0 Then VDSS = Left$(VDSS, EdP): VEd = True
Loop While Not EOF(FFi) And Not VSt
SpP = InStr(VDSS, " ")
While SpP > 0
VDSS = Left$(VDSS, SpP - 1) + Mid$(VDSS, SpP + 1)
SpP = InStr(VDSS, " ")
Wend
If I + Len(VDSS) - 1 <= VSizeMax + 128 Then
Mid$(VDS, I, Len(VDSS)) = VDSS
I = I + Len(VDSS)
End If
Wend
Close #FFi
VSize = I
Else
'Beep
End If
Exit Sub

CE:
'Beep
End Sub

Sub VSave(ByVal Fname As String) 'ビデオセーブ
On Error GoTo CE
Dim I As Long, RR As Long
Dim FN As String
If XDir(Left$(VidDir, Len(VidDir) - 1), vbDirectory Or vbHidden) = vbNullString Then MkDir Left$(VidDir, Len(VidDir) - 1)
RR = InStr(Fname, "\"): If RR <> 0 Then Fname = Left$(Fname, RR - 1)
FN = VidDir + Fname + ".dtv"
If XDir(FN, vbHidden) <> vbNullString$ Then
I = 1
Do
I = I + 1
FN = VidDir + Fname + "_" + String$(-(I < 10), "0") + Mid$(Str$(I), 2) + ".dtv"
Loop While XDir(FN, vbHidden) <> vbNullString$
End If
FFi = FreeFile
Open FN For Output As #FFi
I = 1
While I <= SVP
Print #FFi, Mid$(VDS, I, 64 + (SVP - I < 63) * (I - SVP + 63))
I = I + 64
Wend
Close #FFi
Exit Sub

CE:
'Beep
End Sub

Sub TxMove(ByVal T1 As Long, ByVal T2 As Long) 'テキスト移動
On Error GoTo CE
Dim I As Long, RR As Long
For I = T1 To T2
With TxPos(I)
RR = Int(Rnd * 2)
If RR = 0 Then .XX = -416 + Rnd * 1072: .YY = -32 + Int(Rnd * 2) * 528 Else .XX = -416 + Int(Rnd * 2) * 1072: .YY = -32 + Rnd * 528
End With
Next I
Exit Sub

CE:
'Beep
End Sub

Sub CsrMove() 'カーソル移動
On Error GoTo CE
Dim RR As Long
RR = Int(Rnd * 2)
If RR = 0 Then CXX = -32 + Rnd * 688: CYY = -32 + Int(Rnd * 2) * 528 Else CXX = -32 + Int(Rnd * 2) * 688: CYY = -32 + Rnd * 528
Exit Sub

CE:
'Beep
End Sub

Sub BMove() 'ブロック回転・移動
On Error GoTo CE
Dim I As Long

BMX1(PL) = BX1(PL): BMM1(PL) = BM1(PL)
If InpAA1(PL) = 1 Then
If InpBB1(PL) < 2 Then '左ボタン左回転
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 0), BY1(PL) + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BM1(PL) = BM1(PL) - 1 - (BM1(PL) <= 0) * 4: InpAA1(PL) = 2: Sou(1).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) - 1 + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 0), BY1(PL) + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BX1(PL) = BX1(PL) - 1: BM1(PL) = BM1(PL) - 1 - (BM1(PL) <= 0) * 4: InpAA1(PL) = 2: Sou(1).On = True: Sou(5).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + 1 + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 0), BY1(PL) + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BX1(PL) = BX1(PL) + 1: BM1(PL) = BM1(PL) - 1 - (BM1(PL) <= 0) * 4: InpAA1(PL) = 2: Sou(1).On = True: Sou(5).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 0), BY1(PL) + 1 + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
AC1(PL) = 0: BSC1(PL) = 0: BY1(PL) = BY1(PL) + 1: BM1(PL) = BM1(PL) - 1 - (BM1(PL) <= 0) * 4: InpAA1(PL) = 2: Sou(1).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) - 1 + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 0), BY1(PL) + 1 + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
AC1(PL) = 0: BSC1(PL) = 0: BY1(PL) = BY1(PL) + 1: BX1(PL) = BX1(PL) - 1: BM1(PL) = BM1(PL) - 1 - (BM1(PL) <= 0) * 4: InpAA1(PL) = 2: Sou(1).On = True: Sou(5).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + 1 + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 0), BY1(PL) + 1 + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
AC1(PL) = 0: BSC1(PL) = 0: BY1(PL) = BY1(PL) + 1: BX1(PL) = BX1(PL) + 1: BM1(PL) = BM1(PL) - 1 - (BM1(PL) <= 0) * 4: InpAA1(PL) = 2: Sou(1).On = True: Sou(5).On = True
End If
End If
End If
End If
End If
End If
Else '左ボタン右回転
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 0), BY1(PL) + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BM1(PL) = BM1(PL) + 1 + (BM1(PL) >= 3) * 4: InpAA1(PL) = 2: Sou(1).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) - 1 + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 0), BY1(PL) + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BX1(PL) = BX1(PL) - 1: BM1(PL) = BM1(PL) + 1 + (BM1(PL) >= 3) * 4: InpAA1(PL) = 2: Sou(1).On = True: Sou(5).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + 1 + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 0), BY1(PL) + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BX1(PL) = BX1(PL) + 1: BM1(PL) = BM1(PL) + 1 + (BM1(PL) >= 3) * 4: InpAA1(PL) = 2: Sou(1).On = True: Sou(5).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 0), BY1(PL) + 1 + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
AC1(PL) = 0: BSC1(PL) = 0: BY1(PL) = BY1(PL) + 1: BM1(PL) = BM1(PL) + 1 + (BM1(PL) >= 3) * 4: InpAA1(PL) = 2: Sou(1).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) - 1 + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 0), BY1(PL) + 1 + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
AC1(PL) = 0: BSC1(PL) = 0: BY1(PL) = BY1(PL) + 1: BX1(PL) = BX1(PL) - 1: BM1(PL) = BM1(PL) + 1 + (BM1(PL) >= 3) * 4: InpAA1(PL) = 2: Sou(1).On = True: Sou(5).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + 1 + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 0), BY1(PL) + 1 + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
AC1(PL) = 0: BSC1(PL) = 0: BY1(PL) = BY1(PL) + 1: BX1(PL) = BX1(PL) + 1: BM1(PL) = BM1(PL) + 1 + (BM1(PL) >= 3) * 4: InpAA1(PL) = 2: Sou(1).On = True: Sou(5).On = True
End If
End If
End If
End If
End If
End If
End If
End If
If InpBB1(PL) = 1 Then
If InpAA1(PL) < 2 Then '右ボタン右回転
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 0), BY1(PL) + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BM1(PL) = BM1(PL) + 1 + (BM1(PL) >= 3) * 4: InpBB1(PL) = 2: Sou(1).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + 1 + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 0), BY1(PL) + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BX1(PL) = BX1(PL) + 1: BM1(PL) = BM1(PL) + 1 + (BM1(PL) >= 3) * 4: InpBB1(PL) = 2: Sou(1).On = True: Sou(5).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) - 1 + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 0), BY1(PL) + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BX1(PL) = BX1(PL) - 1: BM1(PL) = BM1(PL) + 1 + (BM1(PL) >= 3) * 4: InpBB1(PL) = 2: Sou(1).On = True: Sou(5).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 0), BY1(PL) + 1 + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
AC1(PL) = 0: BSC1(PL) = 0: BY1(PL) = BY1(PL) + 1: BM1(PL) = BM1(PL) + 1 + (BM1(PL) >= 3) * 4: InpBB1(PL) = 2: Sou(1).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + 1 + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 0), BY1(PL) + 1 + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
AC1(PL) = 0: BSC1(PL) = 0: BY1(PL) = BY1(PL) + 1: BX1(PL) = BX1(PL) + 1: BM1(PL) = BM1(PL) + 1 + (BM1(PL) >= 3) * 4: InpBB1(PL) = 2: Sou(1).On = True: Sou(5).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) - 1 + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 0), BY1(PL) + 1 + B(BT1(PL), BM1(PL) + 1 + (BM1(PL) >= 3) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
AC1(PL) = 0: BSC1(PL) = 0: BY1(PL) = BY1(PL) + 1: BX1(PL) = BX1(PL) - 1: BM1(PL) = BM1(PL) + 1 + (BM1(PL) >= 3) * 4: InpBB1(PL) = 2: Sou(1).On = True: Sou(5).On = True
End If
End If
End If
End If
End If
End If
Else '右ボタン左回転
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 0), BY1(PL) + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BM1(PL) = BM1(PL) - 1 - (BM1(PL) <= 0) * 4: InpBB1(PL) = 2: Sou(1).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + 1 + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 0), BY1(PL) + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BX1(PL) = BX1(PL) + 1: BM1(PL) = BM1(PL) - 1 - (BM1(PL) <= 0) * 4: InpBB1(PL) = 2: Sou(1).On = True: Sou(5).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) - 1 + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 0), BY1(PL) + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BX1(PL) = BX1(PL) - 1: BM1(PL) = BM1(PL) - 1 - (BM1(PL) <= 0) * 4: InpBB1(PL) = 2: Sou(1).On = True: Sou(5).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 0), BY1(PL) + 1 + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
AC1(PL) = 0: BSC1(PL) = 0: BY1(PL) = BY1(PL) + 1: BM1(PL) = BM1(PL) - 1 - (BM1(PL) <= 0) * 4: InpBB1(PL) = 2: Sou(1).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + 1 + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 0), BY1(PL) + 1 + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
AC1(PL) = 0: BSC1(PL) = 0: BY1(PL) = BY1(PL) + 1: BX1(PL) = BX1(PL) + 1: BM1(PL) = BM1(PL) - 1 - (BM1(PL) <= 0) * 4: InpBB1(PL) = 2: Sou(1).On = True: Sou(5).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) - 1 + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 0), BY1(PL) + 1 + B(BT1(PL), BM1(PL) - 1 - (BM1(PL) <= 0) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
AC1(PL) = 0: BSC1(PL) = 0: BY1(PL) = BY1(PL) + 1: BX1(PL) = BX1(PL) - 1: BM1(PL) = BM1(PL) - 1 - (BM1(PL) <= 0) * 4: InpBB1(PL) = 2: Sou(1).On = True: Sou(5).On = True
End If
End If
End If
End If
End If
End If
End If
End If
If BT1(PL) <> 0 And BT1(PL) <> 3 And BT1(PL) <> 4 And InpAA1(PL) = 1 And InpBB1(PL) = 1 Then '両ボタン半回転
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL) + 2 + (BM1(PL) >= 2) * 4, I, 0), BY1(PL) + B(BT1(PL), BM1(PL) + 2 + (BM1(PL) >= 2) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BM1(PL) = BM1(PL) + 2 + (BM1(PL) >= 2) * 4: InpAA1(PL) = 2: InpBB1(PL) = 2: Sou(1).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) - 1 + B(BT1(PL), BM1(PL) + 2 + (BM1(PL) >= 2) * 4, I, 0), BY1(PL) + B(BT1(PL), BM1(PL) + 2 + (BM1(PL) >= 2) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BX1(PL) = BX1(PL) - 1: BM1(PL) = BM1(PL) + 2 + (BM1(PL) >= 2) * 4: InpAA1(PL) = 2: InpBB1(PL) = 2: Sou(1).On = True: Sou(5).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + 1 + B(BT1(PL), BM1(PL) + 2 + (BM1(PL) >= 2) * 4, I, 0), BY1(PL) + B(BT1(PL), BM1(PL) + 2 + (BM1(PL) >= 2) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BX1(PL) = BX1(PL) + 1: BM1(PL) = BM1(PL) + 2 + (BM1(PL) >= 2) * 4: InpAA1(PL) = 2: InpBB1(PL) = 2: Sou(1).On = True: Sou(5).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL) + 2 + (BM1(PL) >= 2) * 4, I, 0), BY1(PL) + 1 + B(BT1(PL), BM1(PL) + 2 + (BM1(PL) >= 2) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
AC1(PL) = 0: BSC1(PL) = 0: BY1(PL) = BY1(PL) + 1: BM1(PL) = BM1(PL) + 2 + (BM1(PL) >= 2) * 4: InpAA1(PL) = 2: InpBB1(PL) = 2: Sou(1).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) - 1 + B(BT1(PL), BM1(PL) + 2 + (BM1(PL) >= 2) * 4, I, 0), BY1(PL) + 1 + B(BT1(PL), BM1(PL) + 2 + (BM1(PL) >= 2) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
AC1(PL) = 0: BSC1(PL) = 0: BY1(PL) = BY1(PL) + 1: BX1(PL) = BX1(PL) - 1: BM1(PL) = BM1(PL) + 2 + (BM1(PL) >= 2) * 4: InpAA1(PL) = 2: InpBB1(PL) = 2: Sou(1).On = True: Sou(5).On = True
Else
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + 1 + B(BT1(PL), BM1(PL) + 2 + (BM1(PL) >= 2) * 4, I, 0), BY1(PL) + 1 + B(BT1(PL), BM1(PL) + 2 + (BM1(PL) >= 2) * 4, I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
AC1(PL) = 0: BSC1(PL) = 0: BY1(PL) = BY1(PL) + 1: BX1(PL) = BX1(PL) + 1: BM1(PL) = BM1(PL) + 2 + (BM1(PL) >= 2) * 4: InpAA1(PL) = 2: InpBB1(PL) = 2: Sou(1).On = True: Sou(5).On = True
End If
End If
End If
End If
End If
End If
End If
If InpLLL1(PL) >= 15 + (TrM > 0) * 5 + Fnl * (2 - TA * 2) Or InpLL1(PL) = 1 Then '左移動
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0) - 1, BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then BX1(PL) = BX1(PL) - 1: InpLL1(PL) = 2: Sou(5).On = True
End If
If InpRRR1(PL) >= 15 + (TrM > 0) * 5 + Fnl * (2 - TA * 2) Or InpRR1(PL) = 1 Then '右移動
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0) + 1, BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then BX1(PL) = BX1(PL) + 1: InpRR1(PL) = 2: Sou(5).On = True
End If

Exit Sub

CE:
'Beep
End Sub

Sub Ranking()
On Error GoTo CE
Dim I As Long, RR As Long, RR1 As Long, RR2 As Long
If VM = 1 Then
If Not (TA Or Fnl) Then RR1 = Sc1(PL) Else RR1 = CT(PL)
RR = RR1
For I = 0 To 4
Mid$(VDS, 2 + I, 1) = Mid$(VTx, (RR Mod 64) + 1, 1): RR = Int(RR / 64)
Next I
If Not Fnl Then RR2 = Li1(PL) Else If Not TA Then RR2 = Lv1(PL) Else RR2 = Liv1(PL)
RR = RR2
For I = 0 To 2
Mid$(VDS, 7 + I, 1) = Mid$(VTx, (RR Mod 64) + 1, 1): RR = Int(RR / 64)
Next I
VDRush = Rush1(PL)
RR = 1228 - Rush1(PL) + RR1 + RR2 * 37
Mid$(VDS, 10, 1) = Mid$(VTx, (RR Mod 53) + 1, 1)
Mid$(VDS, 11, 1) = Mid$(VTx, (Int(RR / 53) Mod 53) + 1, 1)

Mid$(VDS, VPP, 1) = Mid$(VTx, VDD + 1, 1): Mid$(VDS, VPP + 1, 1) = Mid$(VTx, VDC + 1, 1): Mid$(VDS, VP, 1) = "}": SVP = VP
VSize = VP + 1
End If

If Not (TA Or Fnl) Then 'SCORE ATTACK
TrRnk = 5
For I = 4 To 0 Step -1
If Sc1(PL) > SRnk(TrM * 5 + I).SC Or (Sc1(PL) = SRnk(TrM * 5 + I).SC And Li1(PL) > SRnk(TrM * 5 + I).Li) Then TrRnk = I
Next I
TdRnk = 5
For I = 4 To 0 Step -1
If Sc1(PL) > SRnk(15 + TrM * 5 + I).SC Or (Sc1(PL) = SRnk(15 + TrM * 5 + I).SC And Li1(PL) > SRnk(15 + TrM * 5 + I).Li) Then TdRnk = I
Next I
End If
If TA And Not Fnl Then 'TIME ATTACK
TrRnk = 5
For I = 4 To 0 Step -1
If CT(PL) < TRnk(TrM * 5 + I).Tm Or (CT(PL) = TRnk(TrM * 5 + I).Tm And Li1(PL) > TRnk(TrM * 5 + I).Li) Then TrRnk = I
Next I
TdRnk = 5
For I = 4 To 0 Step -1
If CT(PL) < TRnk(15 + TrM * 5 + I).Tm Or (CT(PL) = TRnk(15 + TrM * 5 + I).Tm And Li1(PL) > TRnk(15 + TrM * 5 + I).Li) Then TdRnk = I
Next I
End If
If Not TA And Fnl Then 'FINAL
TrRnk = 5
For I = 4 To 0 Step -1
If Lv1(PL) > FRnk(I).Lv Or (Lv1(PL) = FRnk(I).Lv And CT(PL) < FRnk(I).Tm) Then TrRnk = I
Next I
TdRnk = 5
For I = 4 To 0 Step -1
If Lv1(PL) > FRnk(5 + I).Lv Or (Lv1(PL) = FRnk(5 + I).Lv And CT(PL) < FRnk(5 + I).Tm) Then TdRnk = I
Next I
End If
If TA And Fnl Then 'FURTHEST
TrRnk = 5
For I = 4 To 0 Step -1
If Liv1(PL) > LRnk(I).Liv Or (Liv1(PL) = LRnk(I).Liv And CT(PL) < LRnk(I).Tm) Then TrRnk = I
Next I
TdRnk = 5
For I = 4 To 0 Step -1
If Liv1(PL) > LRnk(5 + I).Liv Or (Liv1(PL) = LRnk(5 + I).Liv And CT(PL) < LRnk(5 + I).Tm) Then TdRnk = I
Next I
End If
If VM >= 2 Or SpdR < 900 Then TrRnk = 5: If VM = 1 Then VM = 0
Exit Sub

CE:
'Beep
End Sub

Sub Trial() '--------トライアル--------
On Error GoTo CE
GEnd = False
FS = False
PL = 0: Pau = -(Trn): CPos = 1
Fade = True: ScrFC = 0
WMC = False
KeyInitAll

If VM = 1 Then Mid$(VDS, 1, 1) = "{" 'ビデオレコード開始
If VM >= 1 And VM <= 3 Then VP = 12

If VM = 2 Or VM = 3 Then
TrM = Abs((InStr(VTx, Mid$(VDS, VP, 1)) - 1) Mod 3)
TA = -Abs(Int((InStr(VTx, Mid$(VDS, VP, 1)) - 1) / 3) Mod 2)
Fnl = -Abs(Int((InStr(VTx, Mid$(VDS, VP, 1)) - 1) / 6) Mod 2): VP = VP + 1
End If
If VM = 1 Then
Mid$(VDS, VP, 1) = Mid$(VTx, (-Fnl) * 6 + (-TA) * 3 + (TrM) + 1, 1): VP = VP + 1
End If

PT(PL) = 0
If Not TA Then SLv1(PL) = -Fnl * 50 Else SLv1(PL) = 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 - Fnl * 800  'DEL
If Tit Then PT(PL) = 5

'背景パレット初期化
If Not WM Then
For I = 0 To 127
With MPa(I)
.red = 0: .green = 0: .blue = 0
End With
Next I
If Not FS Then MPal.SetEntries 0, 256, MPa
End If

'ボード（オフスクリーン）の作成
BDCr BD

'ブロック（オフスクリーン）の作成
BCCr BC

'フィールド（オフスクリーン）の作成
SD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
SD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
SD.lWidth = 160: SD.lHeight = 352
Set BFSf(PL) = DD.CreateSurface(SD)
CK.low = 0: CK.high = 0
BFSf(PL).SetColorKey DDCKEY_SRCBLT, CK
If Not WM Then BFSf(PL).SetPalette MPal
BFSf(PL).BltColorFill RC0, 0

BRndInit
Lv1(PL) = SLv1(PL): Liv1(PL) = 5 + Trn * 5 + Fnl * (4 + TA * 9) + (Not Fnl And TA) * 2
Rush1(PL) = ((VM = 2 Or VM = 3) And VDRush)

VKX1 = 416: VKY1 = 336: VKX2 = VKX1: VKY2 = VKY1: VKX3 = VKX1: VKY3 = VKY1
VKA = 1: VKB = 1
FE = False
PWC = 0
NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50))
Sc1(PL) = 0: SB1(PL) = 0: CB1(PL) = 0: Li1(PL) = 0: St1(PL) = 0
NF1(PL) = 0: BA1(PL) = -Tit: BWC1(PL) = 0: WCa1(PL) = False: DCa1(PL) = False
LBA1(PL) = 0

TimInc = False
FT(PL) = 18000& - (TrM >= 1) * 12000 + (TrM = 2) * 18000
CT(PL) = 0: RT(PL) = 0
GFin = 0
InpL1(PL) = False: InpR1(PL) = False: InpLL1(PL) = 0: InpRR1(PL) = 0: InpLLL1(PL) = 0: InpRRR1(PL) = 0
InpU1(PL) = False: InpD1(PL) = False: InpUU1(PL) = 0: InpDD1(PL) = 0
InpA1(PL) = False: InpB1(PL) = False: InpS1(PL) = False: InpAA1(PL) = 0: InpBB1(PL) = 0: InpSS1(PL) = 1
For I = 0 To 25: For U = 0 To 15: BF1(PL, U, I) = 0: Next U, I
For I = 0 To 22: BF1(PL, 2, I) = 9: BF1(PL, 13, I) = 9: Next I
For I = 3 To 12: BF1(PL, I, 0) = 9: BF1(PL, I, 23) = 9: Next I
If VM = 2 Or VM = 3 Then
For I = 0 To 12
Nx1(PL, I * 2) = Int((InStr(VTx, Mid$(VDS, VP, 1)) - 1) / 7) Mod 7: If Nx1(PL, I * 2) < 0 Then Nx1(PL, I * 2) = 0
Nx1(PL, I * 2 + 1) = (InStr(VTx, Mid$(VDS, VP, 1)) - 1) Mod 7: If Nx1(PL, I * 2 + 1) < 0 Then Nx1(PL, I * 2 + 1) = 0
VP = VP + 1
Next I
Else
For I = 0 To 25: Nx1(PL, I) = BRnd: Next I
If VM = 1 Then
For I = 0 To 12: Mid$(VDS, VP, 1) = Mid$(VTx, Nx1(PL, I * 2) * 7 + Nx1(PL, I * 2 + 1) + 1, 1): VP = VP + 1: Next I
End If
End If

BGInit
St = Int((Lv1(PL) + (Lv1(PL) > 50) * (Lv1(PL) - 50)) / 5)
If Lv1(PL) >= 50 And Lv1(PL) < 200 Then St = 10
If Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 Then St = 11 - (Not (Fnl Or TA)) * 2
Stt = St
If BG >= 1 Then BGCr Stt ' And Stt < 12
SysC = 500: GmC = 6000: SpdR = 1200
FOC = 0: FIC = 0: FF = False
For I = 0 To 15: Sou(I).Fre = 22050: Sou(I).Pan = 0: Sou(I).On = False: Next I
EAC = 0: For I = 0 To 159: EA(I).V = False: Next I
PlFin = 0: PlFC = 0: PlF = 4
If VM = 2 Or VM = 3 Then
VS = 4: VSC = 0
End If

If VM >= 1 And VM <= 3 Then VDD = 0: VDC = -(VM = 1) * 63: VPP = VP

If Not Trn Then Sou(8).Fre = 22050: Sou(8).On = True
If Not (Tit Or VM = 3) Then
If Trn Then
PlayMusic "TRAINING", 3072, 101376
Else
If Fnl Then
If TA Then
PlayMusic "FINAL1", 15360, 181248
Else
PlayMusic "FINAL0", 9216, 113664
End If
Else
If TA Then
If TrM = 0 Then PlayMusic "TIME0", 9216, 104448
If TrM = 1 Then PlayMusic "TIME1", 15360, 113664
If TrM = 2 Then PlayMusic "TIME2", 12288, 110592
Else
If TrM = 0 Then PlayMusic "TRIAL0", 3072, 248832
If TrM = 1 Then PlayMusic "TRIAL1", 4608, 158208
If TrM = 2 Then PlayMusic "TRIAL2", 9216, 168960
End If
End If
End If
End If

'----メインループ----
Do..<GEnd

If GFin >= 1 Then
KeyScanAll
If Fade Then Fade = False: ScrFC = 0
If GFin = 2 And (CsrAA = 1 Or CsrBB = 1) Then ScrFC = 80
If ScrFC = 80 Then PlayMusic vbNullString$: GMode = 5: GEnd = True
End If
If GFin < 2 Then

If VM = 3 Then 'ビデオファイル名登録
KeyScanAll
If (CsrLL = 1 Or CsrLL = 15 Or CsrLL = 18) And NmPos < 8 Then Nm = Nm - 1 - (Nm <= 0) * (36 - (NmPos > 0)): Sou(5).Pan = 0: Sou(5).On = True
If (CsrRR = 1 Or CsrRR = 15 Or CsrRR = 18) And NmPos < 8 Then Nm = Nm + 1 + (Nm >= 35 - (NmPos > 0)) * (Nm + 1): Sou(5).Pan = 0: Sou(5).On = True
If CsrBB = 1 And VEWC >= 30 Then
If NmPos > 0 Then NmPos = NmPos - 1: Nm = InStr(FNE, Mid$(FNm, NmPos + 1, 1)) - 1: Mid$(FNm, NmPos + 1, 1) = "\": Sou(12).Pan = 0: Sou(12).On = True Else If Nm <> 36 Then Nm = 36: Sou(12).Pan = 0: Sou(12).On = True
End If
If CsrAA = 1 And VEWC >= 30 Then
If Nm < 36 Then
Mid$(FNm, NmPos + 1, 1) = Mid$(FNE, Nm + 1, 1): NmPos = NmPos + 1: Sou(0).Pan = 0: Sou(0).On = True: If NmPos = 8 Then Nm = 36
Else
If NmPos > 0 Then
If NmPos < 8 Then Mid$(FNm, NmPos + 1, 8 - NmPos) = String$(8 - NmPos, "\")
VSave FNm
VM = 2 - (Pau = -6 Or Pau = -7) * 2: Sou(4).Pan = 0: Sou(4).On = True
Else
GFin = 2
End If
End If
End If
VEWC = VEWC + 1 + (VEWC >= 30)
End If

If VM = 2 Then 'ビデオモード
If GFin = 0 Then
KeyScanAll
If (CsrLL = 1 Or CsrLL = 15) And VS > 0 Then VS = VS - 1 + (VS > 4) + (VS > 12) * 2 + (VS > 20) * 6 + (VS > 40) * 10 + (VS > 120) * 20: Sou(5).Pan = 0: Sou(5).On = True
If (CsrRR = 1 Or CsrRR = 15) And VS < 200 Then VS = VS + 1 - (VS >= 4) - (VS >= 12) * 2 - (VS >= 20) * 6 - (VS >= 40) * 10 - (VS >= 120) * 20: Sou(5).Pan = 0: Sou(5).On = True
If CsrUU > 0 And VS <> 4 Then VS = 4: Sou(5).Pan = 0: Sou(5).On = True
If (CsrDD = 1 Or CsrDD = 15) Then Sou(5).Pan = 0: Sou(5).On = True: If VS = 0 Then VSC = 0 Else VS = 0
If CsrBB = 1 Or CsrSS = 1 Then VM = 4: InpSS1(PL) = 1: Pau = PL + 1: CPos = -(Not RecCh): BRndInitP Nx1(PL, 25): Sou(14).On = True
Else
If CsrAA = 1 Or CsrBB = 1 Then ScrFC = 80
End If
End If
While VSC >= 0

If Pau <> 0 Then 'ポーズ
If VM <> 3 Then KeyScanAll
If Pau < 0 Then '特殊処理

Select Case Pau
Case -1 'SCORE ATTACK,TIME ATTACKゲームオーバー
PWC = PWC + 1
If CsrAA = 1 And PWC >= 60 And PWC < 120 Then PWC = 120
If PWC = 120 Then
If Sc1(PL) <= 0 And Li1(PL) <= 0 Or TA Then
GFin = 2
Else
Ranking
If TrRnk < 5 Or TdRnk < 5 Then Pau = -2: PWC = 0: TrNm = TrNmm: RR = InStr(TrNm, "\"): NmPos = RR - 1 - (RR = 0) * 7: Nm = -(RR = 0 Or RR > 1) * 45 Else Pau = -5: CPos = -(VM <> 1): PWC = 0
End If
End If

Case -2 '名前入力
If (CsrLL = 1 Or CsrLL = 15 Or CsrLL = 18) And NmPos < 6 Then Nm = Nm - 1 - (Nm <= 0) * (45 - (NmPos > 0)): Sou(5).Pan = 0: Sou(5).On = True
If (CsrRR = 1 Or CsrRR = 15 Or CsrRR = 18) And NmPos < 6 Then Nm = Nm + 1 + (Nm >= 44 - (NmPos > 0)) * (Nm + 1): Sou(5).Pan = 0: Sou(5).On = True
If CsrBB = 1 And PWC >= 30 Then
If NmPos > 0 Then NmPos = NmPos - 1: Nm = InStr(Ne, Mid$(TrNm, NmPos + 1, 1)) - 1: Mid$(TrNm, NmPos + 1, 1) = "\": Sou(12).Pan = 0: Sou(12).On = True Else If Nm <> 45 Then Nm = 45: Sou(12).Pan = 0: Sou(12).On = True
End If
If CsrAA = 1 And PWC >= 30 Then
If Nm < 45 Then
Mid$(TrNm, NmPos + 1, 1) = Mid$(Ne, Nm + 1, 1): NmPos = NmPos + 1: Sou(0).Pan = 0: Sou(0).On = True: If NmPos = 6 Then Nm = 45
Else
If NmPos > 0 Then
If NmPos < 6 Then Mid$(TrNm, NmPos + 1, 6 - NmPos) = String$(6 - NmPos, "\")
If Not (TA Or Fnl) Then 'SCORE ATTACK
For I = 4 To TrRnk + 1 Step -1: SRnk(TrM * 5 + I) = SRnk(TrM * 5 + I - 1): Next I
If TrRnk < 5 Then SRnk(TrM * 5 + TrRnk).Nm = TrNm: SRnk(TrM * 5 + TrRnk).SC = Sc1(PL): SRnk(TrM * 5 + TrRnk).Li = Li1(PL)
For I = 4 To TdRnk + 1 Step -1: SRnk(15 + TrM * 5 + I) = SRnk(15 + TrM * 5 + I - 1): Next I
If TdRnk < 5 Then SRnk(15 + TrM * 5 + TdRnk).Nm = TrNm: SRnk(15 + TrM * 5 + TdRnk).SC = Sc1(PL): SRnk(15 + TrM * 5 + TdRnk).Li = Li1(PL)
End If
If Not Fnl And TA Then 'TIME ATTACK
For I = 4 To TrRnk + 1 Step -1: TRnk(TrM * 5 + I) = TRnk(TrM * 5 + I - 1): Next I
If TrRnk < 5 Then TRnk(TrM * 5 + TrRnk).Nm = TrNm: TRnk(TrM * 5 + TrRnk).Tm = CT(PL): TRnk(TrM * 5 + TrRnk).Li = Li1(PL)
For I = 4 To TdRnk + 1 Step -1: TRnk(15 + TrM * 5 + I) = TRnk(15 + TrM * 5 + I - 1): Next I
If TdRnk < 5 Then TRnk(15 + TrM * 5 + TdRnk).Nm = TrNm: TRnk(15 + TrM * 5 + TdRnk).Tm = CT(PL): TRnk(15 + TrM * 5 + TdRnk).Li = Li1(PL)
End If
If Fnl And Not TA Then 'FINAL
For I = 4 To TrRnk + 1 Step -1: FRnk(I) = FRnk(I - 1): Next I
If TrRnk < 5 Then FRnk(TrRnk).Nm = TrNm: FRnk(TrRnk).Lv = Lv1(PL): FRnk(TrRnk).Tm = CT(PL)
For I = 4 To TdRnk + 1 Step -1: FRnk(5 + I) = FRnk(5 + I - 1): Next I
If TdRnk < 5 Then FRnk(5 + TdRnk).Nm = TrNm: FRnk(5 + TdRnk).Lv = Lv1(PL): FRnk(5 + TdRnk).Tm = CT(PL)
End If
If Fnl And TA Then 'FURTHEST
For I = 4 To TrRnk + 1 Step -1: LRnk(I) = LRnk(I - 1): Next I
If TrRnk < 5 Then LRnk(TrRnk).Nm = TrNm: LRnk(TrRnk).Liv = Liv1(PL): LRnk(TrRnk).Tm = CT(PL)
For I = 4 To TdRnk + 1 Step -1: LRnk(5 + I) = LRnk(5 + I - 1): Next I
If TdRnk < 5 Then LRnk(5 + TdRnk).Nm = TrNm: LRnk(5 + TdRnk).Liv = Liv1(PL): LRnk(5 + TdRnk).Tm = CT(PL)
End If
TrNmm = TrNm: Sou(4).Pan = 0: Sou(4).On = True
Else
Sou(12).Pan = 0: Sou(12).On = True
End If
Pau = -5: CPos = -(VM <> 1): PWC = 0
End If
End If
PWC = PWC + 1 + (PWC >= 30)

Case -3, -4 'SCORE ATTACKタイムアップ,FINALタイムアップ,TIME ATTACK終了
PWC = PWC + 1
If PWC = 20 Then Sc1(PL) = Sc1(PL) + SB1(PL) * ((1 + CB1(PL)) * 0.5): Sc1(PL) = Sc1(PL) + (Sc1(PL) > 1000000000) * (Sc1(PL) - 1000000000): CB1(PL) = 0: SB1(PL) = 0
If CsrAA = 1 And PWC >= 60 And PWC < 180 Then PWC = 180
If PWC = 180 Then
If Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 Then Sc1(PL) = Sc1(PL) + Int(Sc1(PL) / 5 * (Liv1(PL))): Sc1(PL) = Sc1(PL) + (Sc1(PL) > 1000000000) * (Sc1(PL) - 1000000000)
If Not Fnl And Not TA And Sc1(PL) <= 0 And Li1(PL) <= 0 Then
GFin = 2
Else
Ranking
If TrRnk < 5 Or TdRnk < 5 Then Pau = -2: PWC = 0: TrNm = TrNmm: RR = InStr(TrNm, "\"): NmPos = RR - 1 - (RR = 0) * 7: Nm = -(RR = 0 Or RR > 1) * 45 Else Pau = -5: CPos = -(VM <> 1): PWC = 0
End If
End If

Case -5 'ビデオセーブ
PWC = PWC + 1 + (PWC >= 30)
If CsrUU = 1 Or CsrDD = 1 Then CPos = 1 - CPos: Sou(5).On = True
If PWC >= 30 Then
If VM = 1 And CPos = 0 And CsrAA = 1 Then
If Not (TA And Fnl) And TrM = 0 Then FNm = "scorenml"
If Not (TA And Fnl) And TrM = 1 Then FNm = "scorehrd"
If Not (TA And Fnl) And TrM = 2 Then FNm = "scoreadv"
If TA And Not Fnl And TrM = 0 Then FNm = "timenml\"
If TA And Not Fnl And TrM = 1 Then FNm = "timehrd\"
If TA And Not Fnl And TrM = 2 Then FNm = "timeadv\"
If Not TA And Fnl Then FNm = "joker\\\"
If TA And Fnl Then FNm = "furthest"
RR = InStr(FNm, "\"): NmPos = RR - 1 - (RR = 0) * 9: Nm = -(RR = 0 Or RR > 1) * 36: VM = 3: Fade = False: ScrFC = 80: VEWC = 0: GEnd = True
End If
If CPos = 1 And CsrAA = 1 Then GFin = 2
End If

Case -6 'ビデオ終了
If PWC < 30 Then
PWC = PWC + 1
Else
If VM <> 3 Then Pau = -7: PWC = 0: CPos = 0
End If

Case -7 'リプレイ
PWC = PWC + 1 + (PWC >= 30)
If CsrUU = 1 Or CsrDD = 1 Then CPos = 1 - CPos: Sou(5).On = True
If PWC >= 30 Then
If CPos = 0 And CsrAA = 1 Then VM = 2: Fade = False: ScrFC = 80: GEnd = True
If CPos = 1 And CsrAA = 1 Then GFin = 2
End If

End Select

Else '通常ポーズ
If Trn Then 'トレーニング
If CsrUU = 1 Then CPos = CPos - 1 - (CPos <= 0) * 4: Sou(5).Pan = 0: Sou(5).On = True
If CsrDD = 1 Then CPos = CPos + 1 + (CPos >= 3) * 4: Sou(5).Pan = 0: Sou(5).On = True
If CPos = 1 Then
If (CsrLL = 1 Or CsrLL = 15 Or CsrLL = 17 Or CsrLL = 19 Or Lv1(PL) >= 50 And (CsrLL = 16 Or CsrLL = 18)) And Lv1(PL) > 0 Then Lv1(PL) = Lv1(PL) - 1: CT(PL) = 0: Sc1(PL) = 0: SB1(PL) = 0: Li1(PL) = 0: CB1(PL) = 0: Sou(5).Pan = 0: Sou(5).On = True
If (CsrRR = 1 Or CsrRR = 15 Or CsrRR = 17 Or CsrRR = 19 Or Lv1(PL) >= 50 And (CsrRR = 16 Or CsrRR = 18)) And Lv1(PL) < ScLv(TrM) - (ScLv(TrM) < 10) * (10 - ScLv(TrM)) Then Lv1(PL) = Lv1(PL) + 1: CT(PL) = 0: Sc1(PL) = 0: SB1(PL) = 0: Li1(PL) = 0: CB1(PL) = 0: Sou(5).Pan = 0: Sou(5).On = True
End If
If CsrSS = 1 Then Pau = 0
If CPos = 0 And CsrAA = 1 Then Pau = 0
If BA1(PL) <> 9 And CPos = 2 And CsrAA = 1 Then Pau = 0: FE = True: KeyInitAll: CsrAA = 3: CsrBB = 3: FEX = 4: FEY = 12
If CPos = 3 And CsrAA = 1 Then GFin = 2
If Pau = 0 And Not FE Then
If Lv1(PL) >= 50 Then Inc1(PL) = 6000 Else Inc1(PL) = LS(Lv1(PL)): If Inc1(PL) <= 0 Then Inc1(PL) = 1
ASC1(PL) = 30 + (Lv1(PL) >= 50 And Lv1(PL) < 200) * Int((Lv1(PL) - 50) / 10) + (Lv1(PL) >= 200 And Lv1(PL) < 250) * Int((Lv1(PL) - 200) / 5) + (Lv1(PL) >= 250 And Lv1(PL) < 300) * (10 + Int((Lv1(PL) - 250) / 10)) + (Lv1(PL) >= 300 And Lv1(PL) < 500) * (15 + Int((Lv1(PL) - 300) / 50)) + (Lv1(PL) >= 500) * 19
End If
Else
If CsrUU = 1 Then CPos = CPos - 1 - (CPos <= 0 - (Not RecCh)) * (2 - (VM = 4) + (Not RecCh)): Sou(5).Pan = 0: Sou(5).On = True
If CsrDD = 1 Then CPos = CPos + 1 + (CPos >= 1 - (VM = 4)) * (2 - (VM = 4) + (Not RecCh)): Sou(5).Pan = 0: Sou(5).On = True
If RecCh And CsrSS = 1 Then Pau = 0
If CPos = 0 And CsrAA = 1 Then Pau = 0
If VM = 4 And CPos = 1 And CsrAA = 1 Then VM = 2: Fade = False: ScrFC = 80: GEnd = True
If CPos = 1 - (VM = 4) And CsrAA = 1 Then GFin = 2
End If
End If
Else
If FE Then 'フィールドエディット
KeyScanAll
If (CsrLL = 1 Or CsrLL = 15 Or CsrLL = 18) Then '左移動
If FEX > 0 Then
FEX = FEX - 1: Sou(5).Pan = 0: Sou(5).On = True
Else
FECh = False
If BA1(PL) >= 3 And BA1(PL) <= 5 Then
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0) + 1, BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = 9 Then FECh = True
Next I
End If
End If
End If
If (CsrRR = 1 Or CsrRR = 15 Or CsrRR = 18) Then '右移動
If FEX < 9 Then
FEX = FEX + 1: Sou(5).Pan = 0: Sou(5).On = True
Else
FECh = False
If BA1(PL) >= 3 And BA1(PL) <= 5 Then
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0) - 1, BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = 9 Then FECh = True
Next I
End If
End If
End If
If (CsrUU = 1 Or CsrUU = 15 Or CsrUU = 18) And FEY > 1 Then FEY = FEY - 1: Sou(5).Pan = 0: Sou(5).On = True
If (CsrDD = 1 Or CsrDD = 15 Or CsrDD = 18) And FEY < 22 Then FEY = FEY + 1: Sou(5).Pan = 0: Sou(5).On = True
FECh = False 'ブロックセット
For I = 0 To 3
If FEX = BX1(PL) + B(BT1(PL), BM1(PL), I, 0) - 3 And FEY = BY1(PL) + B(BT1(PL), BM1(PL), I, 1) Then FECh = True
Next I
If BA1(PL) < 3 Or BA1(PL) > 5 Or Not FECh Then
FEChh = 0
For I = 0 To 9
If BF1(PL, 3 + I, FEY) > 0 Then FEChh = FEChh + 1
Next I
If FEChh < 9 And BF1(PL, 3 + FEX, FEY) = 0 And (CsrBB < 1 Or CsrBB > 2) And CsrAA >= 1 And CsrAA <= 2 Then 'セット
BF1(PL, 3 + FEX, FEY) = 8
With Src
.Left = 112: .Top = 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
BFSf(PL).BltFast FEX * 16, FEY * 16 - 16, BCSf, Src, DDBLTFAST_WAIT
Sou(0).Pan = 0: Sou(0).On = True
CT(PL) = 0: Sc1(PL) = 0: SB1(PL) = 0: Li1(PL) = 0: CB1(PL) = 0
End If
If FEChh < 10 And BF1(PL, 3 + FEX, FEY) <> 0 And (CsrAA < 1 Or CsrAA > 2) And CsrBB >= 1 And CsrBB <= 2 Then '消去
BF1(PL, 3 + FEX, FEY) = 0
With Des
.Left = FEX * 16: .Top = FEY * 16 - 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
BFSf(PL).BltColorFill Des, 0
Sou(12).Pan = 0: Sou(12).On = True
CT(PL) = 0: Sc1(PL) = 0: SB1(PL) = 0: Li1(PL) = 0: CB1(PL) = 0
End If
End If
If CsrSS = 1 Then FE = False: Pau = PL + 1: KeyInitAll: CPos = 2
Else
'キー入力
If VM < 2 Or VM > 3 Then '非ビデオモード
DIDK.GetDeviceStateKeyboard KS
Select Case PT(PL)
Case 0 To 4 'キーボード・パッド
InpL1(PL) = KS.Key(DIK_LEFT) > 0 Or KS.Key(KArwL) > 0
InpR1(PL) = KS.Key(DIK_RIGHT) > 0 Or KS.Key(KArwR) > 0
InpU1(PL) = KS.Key(DIK_UP) > 0 Or KS.Key(KArwU) > 0
InpD1(PL) = KS.Key(DIK_DOWN) > 0 Or KS.Key(KArwD) > 0
InpA1(PL) = KS.Key(DIK_SPACE) > 0 Or KS.Key(KBL) > 0
InpB1(PL) = KS.Key(DIK_RETURN) > 0 Or KS.Key(DIK_NUMPADENTER) > 0 Or KS.Key(KBR) > 0
InpS1(PL) = KS.Key(DIK_TAB) > 0 Or KS.Key(KBS) > 0
For I = 0 To JC - 1
On Error Resume Next
JS(I) = JS0
DIDJ(I).Poll: DIDJ(I).GetDeviceStateJoystick JS(I)
On Error GoTo CE
InpL1(PL) = InpL1(PL) Or (JS(I).X < 16384 And ((PArw(I) Mod 2) >= 1 Or JS(I).Y >= 16384 And JS(I).Y <= 49152))
InpR1(PL) = InpR1(PL) Or (JS(I).X > 49152 And ((PArw(I) Mod 2) >= 1 Or JS(I).Y >= 16384 And JS(I).Y <= 49152))
InpU1(PL) = InpU1(PL) Or (JS(I).Y < 16384 And ((PArw(I) Mod 4) >= 2 Or JS(I).X >= 16384 And JS(I).X <= 49152))
InpD1(PL) = InpD1(PL) Or (JS(I).Y > 49152 And ((PArw(I) Mod 4) >= 2 Or JS(I).X >= 16384 And JS(I).X <= 49152))
InpA1(PL) = InpA1(PL) Or JS(I).buttons(PBL(I)) > 64
InpB1(PL) = InpB1(PL) Or JS(I).buttons(PBR(I)) > 64
InpS1(PL) = InpS1(PL) Or JS(I).buttons(PBS(I)) > 64
Next I
Case 5 'CP
InpL1(PL) = False: InpR1(PL) = False: InpA1(PL) = False: InpB1(PL) = False: InpU1(PL) = False: InpD1(PL) = False
If (BA1(PL) = 1 And BWC1(PL) > 3) Or BA1(PL) = 2 Or BA1(PL) = 3 Then
If (Not CPE(CPG1(PL)).SRt) Or (BX1(PL) >= CSX1(PL) - 1 And BX1(PL) <= CSX1(PL) + 1) Then
If BM1(PL) = (CSM1(PL) + 2) + (CSM1(PL) >= 2) * 4 Then InpA1(PL) = True: InpB1(PL) = True
If BM1(PL) = (CSM1(PL) + 1) + (CSM1(PL) >= 3) * 4 Then InpA1(PL) = True
If BM1(PL) = (CSM1(PL) - 1) - (CSM1(PL) <= 0) * 4 Then InpB1(PL) = True
End If
If BM1(PL) = CSM1(PL) Then InpA1(PL) = False: InpB1(PL) = False
If CPE(CPG1(PL)).SRt And (BT1(PL) = 2 Or BT1(PL) = 5 Or BT1(PL) = 6) And (CSX1(PL) < 5 Or CSX1(PL) > 7) Then
If BX1(PL) = 6 Then InpA1(PL) = True: InpB1(PL) = True
If (BX1(PL) = 5 Or BX1(PL) = 7) Then InpA1(PL) = False: InpB1(PL) = False
End If
If CPE(CPG1(PL)).Spd = 0 And Not (BT1(PL) = 1 Or BT1(PL) = 3 Or BT1(PL) = 4) And (AC1(PL) >= 11 And AC1(PL) <= 13 And CCWC1(PL) = 0) Then InpA1(PL) = (BX1(PL) < CSX1(PL)) Xor (BT1(PL) <> 2): InpB1(PL) = (BX1(PL) > CSX1(PL)) Xor (BT1(PL) <> 2)
If BA1(PL) = 3 Then CWC1(PL) = CWC1(PL) + 1
If CWC1(PL) > CPE(CPG1(PL)).Spd - 1 Then
If BX1(PL) > CSX1(PL) Then InpL1(PL) = True
If BX1(PL) < CSX1(PL) Then InpR1(PL) = True
If CWC1(PL) > CPE(CPG1(PL)).Spd Then CWC1(PL) = 0
End If
If BM1(PL) = CSM1(PL) And BX1(PL) = CSX1(PL) Then
If BA1(PL) = 3 And AC1(PL) > 0 And CCWC1(PL) > Int(CPE(CPG1(PL)).Wt / 2) And (CSM1(PL) <> CS2M1(PL) Or CSX1(PL) <> CS2X1(PL)) Then CSM1(PL) = CS2M1(PL): CSX1(PL) = CS2X1(PL): CCWC1(PL) = Int(CPE(CPG1(PL)).Wt / 2)
CCWC1(PL) = CCWC1(PL) + 1 + (CCWC1(PL) > CPE(CPG1(PL)).Wt): If CCWC1(PL) > CPE(CPG1(PL)).Wt Then If ((TrM > 0 And CPE(CPG1(PL)).QD) Or TrM = 2) And CSM1(PL) = CS2M1(PL) And CSX1(PL) = CS2X1(PL) Then InpU1(PL) = True Else InpD1(PL) = (Lv1(PL) < 30 Or CSM1(PL) = CS2M1(PL) And CSX1(PL) = CS2X1(PL))
End If
If BX1(PL) = BMX1(PL) And BM1(PL) = BMM1(PL) Then
CMWC1(PL) = CMWC1(PL) + 1: If CMWC1(PL) >= 20 + CPE(CPG1(PL)).Wt Then InpU1(PL) = False: InpD1(PL) = (CMWC1(PL) > 20 + CPE(CPG1(PL)).Wt)
Else
CMWC1(PL) = 0
End If
End If
End Select

If InpL1(PL) And InpR1(PL) Then InpL1(PL) = False: InpR1(PL) = False
If InpU1(PL) And InpD1(PL) Then InpU1(PL) = False: InpD1(PL) = False
'If InpL1(PL) Or InpR1(PL) Then InpU1(PL) = False: InpD1(PL) = False
If VM = 1 Then
VD = 0
VD = VD - InpL1(PL)
VD = VD - InpR1(PL) * 2
VD = VD - InpU1(PL) * 4
VD = VD - InpD1(PL) * 8
VD = VD - InpA1(PL) * 16
VD = VD - InpB1(PL) * 32
If VDC < 63 And VD = VDD Then VDC = VDC + 1 Else Mid$(VDS, VPP, 1) = Mid$(VTx, VDD + 1, 1): Mid$(VDS, VPP + 1, 1) = Mid$(VTx, VDC + 1, 1): VDD = VD: VDC = 0: VPP = VP: VP = VP + 2: If VP > VSizeMax Then VM = 0
End If
Else 'ビデオモード
If VDC = 0 Then
If VP < VSize - 1 Then
VD = (InStr(VTx, Mid$(VDS, VP, 1)) - 1) Mod 64: If VD < 0 Then VD = 0
VDC = (InStr(VTx, Mid$(VDS, VP + 1, 1)) - 1) Mod 64: If VDC < 0 Then VDC = 0
VP = VP + 2
Else
VD = 0: VDC = 0
End If
Else
VDC = VDC - 1
End If
InpL1(PL) = (VD Mod 2) >= 1
InpR1(PL) = (VD Mod 4) >= 2
InpU1(PL) = (VD Mod 8) >= 4
InpD1(PL) = (VD Mod 16) >= 8
InpA1(PL) = (VD Mod 32) >= 16
InpB1(PL) = (VD Mod 64) >= 32
End If
If InpL1(PL) Then InpLL1(PL) = InpLL1(PL) + 1 + (InpLL1(PL) > 0): InpLLL1(PL) = InpLLL1(PL) + 1 + (InpLLL1(PL) >= 15 + (TrM > 0) * 5) Else InpLL1(PL) = 0: InpLLL1(PL) = 0
If InpR1(PL) Then InpRR1(PL) = InpRR1(PL) + 1 + (InpRR1(PL) > 0): InpRRR1(PL) = InpRRR1(PL) + 1 + (InpRRR1(PL) >= 15 + (TrM > 0) * 5) Else InpRR1(PL) = 0: InpRRR1(PL) = 0
If InpU1(PL) Then InpUU1(PL) = InpUU1(PL) + 1 + (InpUU1(PL) >= 1) Else InpUU1(PL) = 0
If InpD1(PL) Then InpDD1(PL) = InpDD1(PL) + 1 + (InpDD1(PL) >= 1) Else InpDD1(PL) = 0
If InpA1(PL) Then InpAA1(PL) = InpAA1(PL) + 1 + (InpAA1(PL) >= 1) Else InpAA1(PL) = 0
If InpB1(PL) Then InpBB1(PL) = InpBB1(PL) + 1 + (InpBB1(PL) >= 1) Else InpBB1(PL) = 0
If InpS1(PL) Then InpSS1(PL) = InpSS1(PL) + 1 + (InpSS1(PL) >= 2) Else InpSS1(PL) = 0

'On Error Resume Next
'JS(0) = JS0
'If JC >= 1 Then DIDJ(0).Poll: DIDJ(0).GetDeviceStateJoystick JS(0) 'DEL
'On Error GoTo CE
'If JS(0).buttons(7) <> 0 Then Lv1(PL) = Lv1(PL) + 1 + (Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 249) 'DEL

If Not Tit And (VM < 2 Or VM > 3) And BA1(PL) <> 0 And WMC Then InpSS1(PL) = 1
If (Trn Or Liv1(PL) > 0) And BA1(PL) <> 0 And BA1(PL) <> 10 And InpSS1(PL) = 1 Then Pau = PL + 1: KeyInitAll: CPos = 0

'ブロック動作
BAA1(PL) = BA1(PL)
If BAA1(PL) = 0 Then 'スタート
SysC = 500: GmC = 600
If InpSS1(PL) = 1 Then InpSS1(PL) = 2: If ScLiv(2) > 0 Then Rush1(PL) = Not Rush1(PL): Sou(0).Pan = 0: Sou(0).On = True
BWC1(PL) = BWC1(PL) + 1: If BWC1(PL) >= 192 Then BWC1(PL) = 0: WCa1(PL) = (TrM = 2): DCa1(PL) = True: BA1(PL) = 1
End If
If BAA1(PL) = 1 Then '時間待ち
If BWC1(PL) = 0 Then BT1(PL) = Nx1(PL, 0): BX1(PL) = 6: BMX1(PL) = BX1(PL): BY1(PL) = -(BT1(PL) > 0): BYF1(PL) = 0: BM1(PL) = 0: BMM1(PL) = BM1(PL): CWC1(PL) = 0: CCWC1(PL) = 0: CMWC1(PL) = 0: If Tit Then CPG1(PL) = 10 + Int(Rnd * 12)
If PT(PL) = 5 And BWC1(PL) <= 3 Then CPAI BWC1(PL)
BWC1(PL) = BWC1(PL) + 1
If TrM = 2 And Not WCa1(PL) And InpUU1(PL) = 1 Then WCa1(PL) = True: DCa1(PL) = False
If Not DCa1(PL) And (TrM > 0 And InpUU1(PL) = 1 Or InpDD1(PL) = 1) Then DCa1(PL) = True: InpUU1(PL) = 2: InpDD1(PL) = 2
If TrM = 2 Then
If WCa1(PL) And BWC1(PL) > 0 And BWC1(PL) < 15 And (InpLL1(PL) = 1 Or InpRR1(PL) = 1 Or InpUU1(PL) = 1 Or InpDD1(PL) = 1 Or InpAA1(PL) = 1 Or InpBB1(PL) = 1) Then BWC1(PL) = 15: InpLLL1(PL) = InpLLL1(PL) + 3: InpRRR1(PL) = InpRRR1(PL) + 3
End If

If TA And Li1(PL) >= 100 - Fnl * 200 Then 'タイムアタック・FURTHESTクリア
PlFin = 1 - (Liv1(PL) >= 3 - Fnl * 7)
If Not Tit And VM <= 1 Then
If Not Fnl And Liv1(PL) > TmLiv(TrM) Then TmLiv(TrM) = Liv1(PL)
If Fnl And Liv1(PL) > LsLiv Then LsLiv = Liv1(PL)
If Fnl Then 'クラッカー
R = Rnd * 360
For U = 0 To 39
For I = 0 To 1
With EA(EAC)
.V = 2: .A = Int(Rnd * 7): .AF = Int(Rnd * 4)
.X = 312 + (I * 2 - 1) * 320: .Y = 480
.XX = (5 - ((R + U * 8.7) - Int((R + U * 8.7) / 10) * 10)) - (I * 2 - 1) * 5.8: .YY = Cos((R + U * 9 + I * 4.5) * Rg) * Rnd * 15 - 21
.XXX = -.XX / (180 + Rnd * 50): .YYY = 0.2 - Rnd * 0.06
End With
EAC = EAC + 1 + (EAC >= 159) * 160
Next I, U
End If
End If
BWC1(PL) = 0: BA1(PL) = 10: Pau = -4: PWC = 0: Sou(8).On = True: Sou(14).On = True: If VM >= 2 Then Pau = -6: PWC = 0: If VM = 2 Then VM = 4
Else
If Not (Tit Or Trn Or TA) And CB1(PL) = 0 And FT(PL) <= 0 Then 'スコアアタック・JOKER時間切れ
If Lv1(PL) < 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 - Fnl * 100 Then Liv1(PL) = 0
PlFin = 1 - (Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 - Fnl * 100 And Liv1(PL) >= 5 + Fnl * 4)
If VM <= 1 And Not Fnl And Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 And Liv1(PL) > ScLiv(TrM) Then ScLiv(TrM) = Liv1(PL)
If VM >= 2 Then Sc1(PL) = Sc1(PL) + SB1(PL) * ((1 + CB1(PL)) * 0.5): Sc1(PL) = Sc1(PL) + (Sc1(PL) > 1000000000) * (Sc1(PL) - 1000000000): CB1(PL) = 0: SB1(PL) = 0: If Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 Then Sc1(PL) = Sc1(PL) + Int(Sc1(PL) / 5 * (Liv1(PL))): Sc1(PL) = Sc1(PL) + (Sc1(PL) > 1000000000) * (Sc1(PL) - 1000000000)
Sou(14).On = True
If Not Tit And Fnl Then If VM <= 1 And Lv1(PL) < 200 Then Pau = 0: GFin = 1
BWC1(PL) = 0: BA1(PL) = 10: Pau = (Not Fnl Or (Lv1(PL) >= 200)) * 3: PWC = 0: If VM >= 2 Then Pau = -6: PWC = 0: If VM = 2 Then VM = 4
Else
If Not Tit And Fnl And Not TA And Lv1(PL) >= 300 And St1(PL) <= 0 Then 'JOKERストック切れ
PlFin = 2
BWC1(PL) = 0: BA1(PL) = 10: Pau = -3: PWC = 0: Sou(14).On = True: If VM >= 2 Then Pau = -6: PWC = 0: If VM = 2 Then VM = 4
Else
If BWC1(PL) >= 20 + (TrM > 0) * 5 + Fnl * (5 - TA * 4) Then BA1(PL) = 2: BAA1(PL) = 2: If Not TimInc Then TimInc = True
End If
End If
End If

End If
If BAA1(PL) = 2 Then '初期設定
Ch1(PL) = 0: Chh1(PL) = 0: BSC1(PL) = 0: AC1(PL) = 0
WCa1(PL) = False: DCa1(PL) = False
If Lv1(PL) >= 50 Then Inc1(PL) = 6000 Else Inc1(PL) = LS(Lv1(PL)): If Inc1(PL) <= 0 Then Inc1(PL) = 1
ASC1(PL) = 30 + (Lv1(PL) >= 50 And Lv1(PL) < 200) * Int((Lv1(PL) - 50) / 10) + (Lv1(PL) >= 200 And Lv1(PL) < 250) * Int((Lv1(PL) - 200) / 5) + (Lv1(PL) >= 250 And Lv1(PL) < 300) * (10 + Int((Lv1(PL) - 250) / 10)) + (Lv1(PL) >= 300 And Lv1(PL) < 500) * (15 + Int((Lv1(PL) - 300) / 50)) + (Lv1(PL) >= 500 And Lv1(PL) < 1000) * 19 + (Lv1(PL) >= 1000) * 20
For I = 0 To 24: Nx1(PL, I) = Nx1(PL, I + 1): Next I
If VM = 2 Or VM = 3 Then
If VP < VSize Then
Nx1(PL, 25) = (InStr(VTx, Mid$(VDS, VP, 1)) - 1) Mod 7: If Nx1(PL, 25) < 0 Then Nx1(PL, 25) = 0
VP = VP + 1
Else
Nx1(PL, 25) = 0
End If
Else
Nx1(PL, 25) = BRnd
If VM = 1 Then Mid$(VDS, VP, 1) = Mid$(VTx, Nx1(PL, 25) + 1, 1): VP = VP + 1: If VP > VSizeMax Then VM = 0
End If
BA1(PL) = 3: BAA1(PL) = 3
End If
If BAA1(PL) = 3 Then 'アクティブ
RushC1(PL) = -Rush1(PL) * 19
For J = 0 To -Rush1(PL) * 19
BMove
If BM1(PL) <> BMM1(PL) Then Sou(1).Pan = BX1(PL) * 60 - 330
If BX1(PL) <> BMX1(PL) Then Sou(5).Pan = BX1(PL) * 60 - 330
Ch1(PL) = 0 'ミス判定
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BYF1(PL) = BY1(PL) '下移動
BSC1(PL) = BSC1(PL) + Inc1(PL)
DB1(PL) = 0
If BSC1(PL) <= 240 And InpDD1(PL) = 1 Then BSC1(PL) = 240: DB1(PL) = -(Not (TA Or Fnl))
If BSC1(PL) <= 6000 And InpUU1(PL) = 1 And TrM > 0 Then BSC1(PL) = 6000: DB1(PL) = -(Not (TA Or Fnl)) * 3
While BSC1(PL) >= 240
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1) + 1) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BY1(PL) = BY1(PL) + 1: Sc1(PL) = Sc1(PL) + DB1(PL) * (Lv1(PL) + 1): BSC1(PL) = BSC1(PL) - 240: AC1(PL) = 0
Else
BSC1(PL) = 239
End If
Wend
If BY1(PL) > BYF1(PL) Then BYF1(PL) = BYF1(PL) + 1
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1) + 1) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then AC1(PL) = 0 Else AC1(PL) = AC1(PL) + 1: Sou(3).Pan = BX1(PL) * 60 - 330: Sou(3).On = Sou(3).On Or (AC1(PL) = 1): If (InpDD1(PL) = 1 Or (TrM > 0 And InpUU1(PL) = 1)) And AC1(PL) >= -(InpDD1(PL) = 1) * (6 - Int(Lv1(PL) / 5)) * (1 - Rush1(PL) * 19) And AC1(PL) <= 30 * (1 - Rush1(PL) * 19) Then Sc1(PL) = Sc1(PL) - (Not (TA Or Fnl)) * (15 - Int(AC1(PL) / (2 * (1 - Rush1(PL) * 19)))) * (Lv1(PL) + 1) * (1 - (TrM = 2 And InpUU1(PL) = 1) * 2): AC1(PL) = 30 * (1 - Rush1(PL) * 19): WCa1(PL) = (InpUU1(PL) = 1): DCa1(PL) = (InpUU1(PL) = 1 Or InpDD1(PL) = 1)
InpUU1(PL) = InpUU1(PL) - (AC1(PL) >= 30 And InpUU1(PL) = 1): InpDD1(PL) = InpDD1(PL) - (AC1(PL) >= 30 * (1 - Rush1(PL) * 19) And InpDD1(PL) = 1)
If BSC1(PL) >= 239 And AC1(PL) >= ASC1(PL) * (1 - Rush1(PL) * 19) Then RushC1(PL) = J: BA1(PL) = 4: Sou(2).Pan = BX1(PL) * 60 - 330: Sou(2).Fre = 22050 / (((BY1(PL) - 1) * 0.055) + 1): Sou(2).On = True
Else 'ミス
RushC1(PL) = J
If Trn Then TimInc = False
If Not Tit And Fnl And Not TA And Lv1(PL) >= 300 Then
PlFin = 2
BWC1(PL) = 0: BA1(PL) = 10: Pau = -3: PWC = 0: Sou(14).On = True: If VM >= 2 Then Pau = -6: PWC = 0: If VM = 2 Then VM = 4
Else
For I = 0 To 3
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = BT1(PL) + 1
With Src
.Left = BT1(PL) * 16: .Top = 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
BFSf(PL).BltFast (BX1(PL) + B(BT1(PL), BM1(PL), I, 0)) * 16 - 48, (BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) * 16 - 16, BCSf, Src, DDBLTFAST_WAIT
Next I
If Liv1(PL) <> 1 Then
Sou(10).Pan = 0: Sou(10).On = True: BA1(PL) = 1
Else
Sou(9).Pan = 0: Sou(9).On = True
If Not Tit And Fnl Then 'FINAL
If VM <= 1 And (TA Or Lv1(PL) < 200) Then Pau = 0: GFin = 1
End If
End If
Liv1(PL) = Liv1(PL) - 1: If Liv1(PL) < 0 Then Liv1(PL) = 0
BWC1(PL) = 0: BA1(PL) = 9
End If
End If
RBX1(PL, J) = BX1(PL): RBY1(PL, J) = BY1(PL): RBM1(PL, J) = BM1(PL): RAC1(PL, J) = AC1(PL): RBSC1(PL, J) = BSC1(PL): RInc1(PL, J) = Inc1(PL): RBYF1(PL, J) = BYF1(PL)
If RushC1(PL) < -Rush1(PL) * 19 Then J = -Rush1(PL) * 19
Next J
End If
If BAA1(PL) = 4 Then 'セット
RushC1(PL) = 0: J = RushC1(PL)
RBX1(PL, J) = BX1(PL): RBY1(PL, J) = BY1(PL): RBM1(PL, J) = BM1(PL): RAC1(PL, J) = AC1(PL): RBSC1(PL, J) = BSC1(PL): RInc1(PL, J) = Inc1(PL): RBYF1(PL, J) = BYF1(PL)
For I = 0 To 3
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = BT1(PL) + 1
With Src
.Left = BT1(PL) * 16: .Top = 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
BFSf(PL).BltFast (BX1(PL) + B(BT1(PL), BM1(PL), I, 0)) * 16 - 48, (BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) * 16 - 16, BCSf, Src, DDBLTFAST_WAIT
Next I
BA1(PL) = 5
End If
If BAA1(PL) = 5 Then '判定
Chh1(PL) = 0
For I = 0 To 3
Ch1(PL) = 0
For U = 3 To 12
If BY1(PL) + I > 0 And BY1(PL) + I < 23 And BF1(PL, U, BY1(PL) + I) > 0 Then Ch1(PL) = Ch1(PL) + 1
Next U
If Ch1(PL) >= 10 Then FE1(PL, Chh1(PL)) = BY1(PL) + I: Chh1(PL) = Chh1(PL) + 1
Next I
If Chh1(PL) > 0 Then DCa1(PL) = DCa1(PL) Or (TrM < 2): BA1(PL) = 6: BAA1(PL) = 6 Else Sc1(PL) = Sc1(PL) + SB1(PL) * ((1 + CB1(PL)) * 0.5): SB1(PL) = 0: CB1(PL) = 0: BWC1(PL) = 0: BA1(PL) = 1
End If
If BAA1(PL) = 6 Then 'ブロック消去
Sou(6 - (Chh1(PL) >= 4)).Pan = 0: Sou(6 - (Chh1(PL) >= 4)).Fre = 22050 * 2 ^ ((CB1(PL) + (CB1(PL) > 9) * (CB1(PL) - 9)) / 12): Sou(6 - (Chh1(PL) >= 4)).On = True
R = Rnd * 360
If CB1(PL) = 0 Then CBLv1(PL) = Lv1(PL)
For I = 0 To Chh1(PL) - 1
If BAni Then
For U = 0 To 9
With EA(EAC)
.V = 1 - ((SB1(PL) + Bn(Chh1(PL) - 1) * (Lv1(PL) + 1)) * (1 + CB1(PL) * 0.5) >= Bn(3) * (CBLv1(PL) + 1)): .A = BF1(PL, 3 + U, FE1(PL, I)) - 1: .AF = Int(Rnd * -((SB1(PL) + Bn(Chh1(PL) - 1) * (Lv1(PL) + 1)) * (1 + CB1(PL) * 0.5) >= Bn(3) * (CBLv1(PL) + 1)) * 4)
.X = 112 + U * 16: .Y = 48 + FE1(PL, I) * 16
.XX = Sin((R + I * 90 + U * 108) * Rg) * (4 + Chh1(PL) * 3): .YY = Cos((R + I * 90 + U * 108) * Rg) * (4 + Chh1(PL) * 3)
If TrM > 0 Then .XXX = .YY / 50 * (((I + U) Mod 2) * (2 * (TrM = 2)) + 1) * ((PL Mod 2) * 2 - 1): .YYY = -.XX / 50 * (((I + U) Mod 2) * (2 * (TrM = 2)) + 1) * ((PL Mod 2) * 2 - 1) Else .XXX = 0: .YYY = 0.2
End With
EAC = EAC + 1 + (EAC >= 159) * 160
Next U
End If
With Des
.Left = 0: .Top = FE1(PL, I) * 16 - 16: .Right = .Left + 160: .Bottom = .Top + 16
End With
BFSf(PL).BltColorFill Des, 0
Next I
BWC1(PL) = -(TrM = 0) * Int(Lv1(PL) / 5) * 3 - (TrM = 1) * (20 + Int(Lv1(PL) / 5)) - (TrM = 2) * 35
CB1(PL) = CB1(PL) + 1 + (CB1(PL) >= 100)
If Not (TA Or Fnl) Then SB1(PL) = SB1(PL) + Bn(Chh1(PL) - 1) * (Lv1(PL) + 1): If SB1(PL) > 10000000 Then SB1(PL) = 10000000
If Fnl And Chh1(PL) >= 4 Then St1(PL) = St1(PL) - ((Lv1(PL) < 300 Or St1(PL) > 0) And Lv1(PL) + St1(PL) < 1000)
Li1(PL) = Li1(PL) + Chh1(PL)
If Not Trn And Lv1(PL) < 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 - (Fnl And St1(PL) > 0) * 800 Then
If Li1(PL) >= NL1(PL) Then
Sou(4).Pan = 0: Sou(4).On = True: St1(PL) = St1(PL) + (Lv1(PL) >= 200): Lv1(PL) = Lv1(PL) + 1: Sou(8).On = Sou(8).On Or (Not (TA Or Fnl) And Lv1(PL) = 30 - (TrM >= 1) * 20 - (TrM = 2) * 150) Or Fnl And Lv1(PL) = 300: NF1(PL) = 65: NL1(PL) = (NL1(PL) + 8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50)): If Lv1(PL) >= 1000 Then St1(PL) = 0
If Not (Tit Or Trn) And VM <= 1 Then
If Not (TA Or Fnl) And Lv1(PL) > ScLv(TrM) Then ScLv(TrM) = Lv1(PL)
If Fnl And Not TA And Lv1(PL) > FnLv Then FnLv = Lv1(PL)
End If
If TrM = 2 Then FT(PL) = FT(PL) + 1500 - (Lv1(PL) = 200 - Fnl * 100) * 28500: FT(PL) = FT(PL) + (FT(PL) > 30000) * (FT(PL) - 30000): If Fnl And Not TA Then CT(PL) = CT(PL) + RT(PL): RT(PL) = 0: If CT(PL) > 360000 Then CT(PL) = 360000
End If
End If
BA1(PL) = 7: BAA1(PL) = 7
End If
If BAA1(PL) = 7 Then '時間待ち
If BWC1(PL) >= 35 - Chh1(PL) Then BWC1(PL) = 0: BA1(PL) = 8: BAA1(PL) = 8 Else BWC1(PL) = BWC1(PL) + 1
End If
If BAA1(PL) = 8 Then 'フィールドずらし
Do
I = BWC1(PL)
For U = FE1(PL, I) To 2 Step -1
For Y = 3 To 12
BF1(PL, Y, U) = BF1(PL, Y, U - 1)
Next Y, U
For U = 3 To 12
BF1(PL, U, 1) = 0
Next U
With Src
.Left = 0: .Top = 0: .Right = .Left + 160: .Bottom = .Top + (FE1(PL, I)) * 16 - 16
End With
BFSf(PL).BltFast 0, 16, BFSf(PL), Src, DDBLTFAST_WAIT
With Des
.Left = 0: .Top = 0: .Right = .Left + 160: .Bottom = .Top + 16
End With
BFSf(PL).BltColorFill Des, 0
BWC1(PL) = BWC1(PL) + 1
Loop While TrM = 2 And BWC1(PL) < Chh1(PL)
If BWC1(PL) = Chh1(PL) Then Sou(13).Pan = 0: Sou(13).On = True: BWC1(PL) = 0: BA1(PL) = 1
End If

If BAA1(PL) = 9 Then 'ミス
If BWC1(PL) < 60 Then
If BWC1(PL) >= 16 And BWC1(PL) < 38 Then
For I = 3 To 12: BF1(PL, I, BWC1(PL) - 15) = 0: Next I
With Des
.Left = 0: .Top = (BWC1(PL) - 16) * 16: .Right = .Left + 160: .Bottom = .Top + 16
End With
BFSf(PL).BltColorFill Des, 0
End If
BWC1(PL) = BWC1(PL) + 1
Else
Sc1(PL) = Sc1(PL) + SB1(PL) * ((1 + CB1(PL)) * 0.5): SB1(PL) = 0: CB1(PL) = 0: If Trn Then CT(PL) = 0: Sc1(PL) = 0: Li1(PL) = 0
If Fnl And Not TA And Lv1(PL) >= 200 Then Ranking: If TrRnk < 5 Or TdRnk < 5 Then TrNm = TrNmm: RR = InStr(TrNm, "\"): NmPos = RR - 1 - (RR = 0) * 7: Nm = -(RR = 0 Or RR > 1) * 45
If Tit Or Trn Or Liv1(PL) > 0 Then WCa1(PL) = True: DCa1(PL) = True: BWC1(PL) = 0: BA1(PL) = 1 Else BWC1(PL) = 0: BA1(PL) = 10: Pau = (Not Fnl) + (Fnl And Not TA And Lv1(PL) >= 200) * (2 - (TrRnk = 5 And TdRnk = 5) * 3): PWC = 0: If VM >= 2 Then Pau = -6: PWC = 0: If VM = 2 Then VM = 4
End If
End If

If Not (Trn Or (TA And Not Fnl)) And (Lv1(PL) >= 30 - (TrM >= 1) * 20 Or TrM = 2) And St1(PL) < 100 And Lv1(PL) + St1(PL) < 300 And Liv1(PL) > 0 And TimInc Then FT(PL) = FT(PL) - 1 + ((FT(PL) Mod 5) > 0): If FT(PL) < 0 Then FT(PL) = 0
If Trn Or BA1(PL) > 0 And Liv1(PL) > 0 Then
If (Trn Or TA And Liv1(PL) > 0 And Li1(PL) < 100 - Fnl * 200) And TimInc Then CT(PL) = CT(PL) + 1 - ((CT(PL) Mod 5) > 0): If CT(PL) > 360000 Then CT(PL) = 360000
If Fnl And Liv1(PL) > 0 And Fnl And Liv1(PL) > 0 And St1(PL) < 100 And Lv1(PL) + St1(PL) < 300 And TimInc Then RT(PL) = RT(PL) + 1 - ((RT(PL) Mod 5) > 0): If RT(PL) > 36000 Then RT(PL) = 36000
If SpdE <= 100 Then SysC = SysC + SpdE Else SysC = SysC + 100
If SysC > 1000000000 Then SysC = 1000000000
GmC = GmC + 17 + ((GmC Mod 50) = 0): If GmC > 1000000000 Then GmC = 1000000000
SpdR = Int((GmC / SysC * 1000) + 0.5)
If SpdR < 0 Then SpdR = 0
If SpdR > 5000 Then SpdR = 5000
End If
If Sc1(PL) > 1000000000 Then Sc1(PL) = 1000000000
If Li1(PL) > 10000 Then Li1(PL) = 10000
End If
End If

If VM = 2 Or VM = 3 Then VSC = VSC - 4 Else VSC = -1
Wend
If VM = 2 Or VM = 3 Then VSC = VSC + VS Else VSC = 0

End If

If PlFin > 0 And PlFC < 220 Then '終了時ブロック消去
Do
RR1 = (PlF Mod 10): RR2 = Int(PlF / 10)
If BF1(PL, 3 + RR1, 1 + RR2) > 0 Then
If BAni Then
With EA(EAC)
.V = 1 - (PlFin >= 2): .A = BF1(PL, 3 + RR1, 1 + RR2) - 1: .AF = Int(Rnd * -(PlFin >= 2) * 4)
.X = 112 + RR1 * 16: .Y = 64 + RR2 * 16
.XX = 0: .YY = 0
.XXX = Sin((PlFC * 17 Mod 360) * Rg) * 0.2: .YYY = -Cos((PlFC * 17 Mod 360) * Rg) * 0.2
End With
EAC = EAC + 1 + (EAC >= 159) * 160
End If
BF1(PL, 3 + RR1, 1 + RR2) = 0
With Des
.Left = RR1 * 16: .Top = RR2 * 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
BFSf(PL).BltColorFill Des, 0
PlFCh = True
Else
PlFCh = False
End If
PlF = PlF + 27: PlF = PlF Mod 220
PlFC = PlFC + 1
Loop While PlFC < 220 And Not PlFCh
End If

If PlFin > 0 And PlFC >= 220 And Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 - Fnl * 100 Then '終了時「雪」
If PlFC >= 223 Then
If BAni Then
With EA(EAC)
.V = 1 - (PlFin >= 2): .A = Int(Rnd * 7): .AF = Int(Rnd * -(PlFin >= 2) * 4)
.X = Rnd * 640 - 16: .Y = -16
.XX = Rnd * 2 - 1: .YY = 1
.XXX = (Rnd * 2 - 1) * 0.01: .YYY = 0.05 + Rnd * 0.05
End With
EAC = EAC + 1 + (EAC >= 159) * 160
End If
PlFC = 220
Else
PlFC = PlFC + 1
End If
End If

WMC = False

'背景表示
BGD TrM, Lv1(PL)
'ボード表示
With Src
.Left = 0: .Top = 0: .Right = .Left + 224: .Bottom = .Top + 368
End With
If Not FS Then BBSf.BltFast 80, 64, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
If Rush1(PL) Then
With Src
.Left = 32 + RushAniC: .Top = 0: .Right = 192: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 112, 64, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
With Src
.Left = 32: .Top = 0: .Right = 32 + RushAniC: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 272 - RushAniC, 64, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If

'ライフ表示
For I = 1 To Liv1(PL) + (Liv1(PL) > 14) * (Liv1(PL) - 14)
With Src
.Left = 256: .Top = ((10 + LAni * (1 - (I Mod 2) * 2)) Mod 10) * 32: .Right = .Left + 32: .Bottom = .Top + 32
End With
If Not FS Then BBSf.BltFast 80, 408 - I * 24, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
If VM <> 3 And Pau <= 0 Then
'フィールド表示
If Not Fnl Then
With Src
.Left = 0: .Top = 0: .Right = .Left + 160: .Bottom = .Top + 352
End With
If Not FS Then BBSf.BltFast 112, 64, BFSf(PL), Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
If (Not BAni) And BA1(PL) = 7 Then
For I = 0 To Chh1(PL) - 1
For U = 0 To 9
With Src
If ((80 + BWC1(PL) + Chh1(PL) - (Chh1(PL) = 4) * ((PL Mod 2) * 2 - 1) * (U + I * 2)) Mod 8) < 4 Then .Left = 112: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16 Else .Left = (BF1(PL, U + 3, FE1(PL, I)) - 1) * 16: .Top = -(BF1(PL, U + 3, FE1(PL, I)) = 8) * 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 112 + U * 16, 48 + FE1(PL, I) * 16, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next U
Next I
End If
End If
'アクティブブロック残像表示
If Zanzo And (VM <= 1 Or VM >= 4 Or VS <= 4) Then
If FS Then
For J = 0 To -Rush1(PL) * 19
If (Not (BSm And Not RInc1(PL, J) >= 240 And LAC1(PL, J) = 0) Or (16 + Int(LBSC1(PL, J) / 15)) = (16 + Int(RBSC1(PL, J) / 15))) And LBX1(PL, J) = RBX1(PL, J) And LBY1(PL, J) = RBY1(PL, J) And LBYF1(PL, J) = RBYF1(PL, J) And LBM1(PL, J) = RBM1(PL, J) Then CLB1(PL, J) = True Else CLB1(PL, J) = False
Next J
Else
For J = -(Not Zanzo Or FE Or BA1(PL) = 5) * LRushC1(PL) To LRushC1(PL)
If LBA1(PL) > 2 And LBA1(PL) < 6 And Not CLB1(PL, J) Then
For U = LBYF1(PL, J) To LBY1(PL, J)
For I = 0 To 3
If LBA1(PL) < 4 Or LBA1(PL) > 5 Or U < LBY1(PL, J) Or J < LRushC1(PL) Then
With Src
.Left = LBT1(PL) * 16: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
End With
Else
With Src
.Left = 112: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
End With
End If
If Not FS Then BBSf.BltFast 64 + (LBX1(PL, J) + B(LBT1(PL), LBM1(PL, J), I, 0)) * 16, 48 + (U + B(LBT1(PL), LBM1(PL, J), I, 1)) * 16 - (BSm And Not FE And Not (Zanzo And LInc1(PL, J) >= 240) And LAC1(PL, J) = 0) * Int(LBSC1(PL, J) / 15), BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I, U
End If
CLB1(PL, J) = True
Next J
End If
LRushC1(PL) = RushC1(PL): LBA1(PL) = BA1(PL): LBT1(PL) = BT1(PL)
For J = 0 To -Rush1(PL) * 19
LBX1(PL, J) = RBX1(PL, J): LBY1(PL, J) = RBY1(PL, J): LBYF1(PL, J) = RBYF1(PL, J): LBSC1(PL, J) = RBSC1(PL, J): LAC1(PL, J) = RAC1(PL, J): LBM1(PL, J) = RBM1(PL, J): LInc1(PL, J) = RInc1(PL, J)
Next J
End If
'アクティブブロック表示
If BA1(PL) > 2 And BA1(PL) < 6 Then
For J = -(Not Zanzo Or FE Or BA1(PL) = 5) * RushC1(PL) To RushC1(PL)
If Not Zanzo Or (VM >= 2 And VM <= 3 And VS > 4) Then RBYF1(PL, J) = RBY1(PL, J)
For U = RBYF1(PL, J) To RBY1(PL, J)
For I = 0 To 3
If BA1(PL) < 4 Or BA1(PL) > 5 Or U < RBY1(PL, J) Or J < RushC1(PL) Then
With Src
.Left = BT1(PL) * 16: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
End With
Else
With Src
.Left = 112: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
End With
End If
If Not FS Then BBSf.BltFast 64 + (RBX1(PL, J) + B(BT1(PL), RBM1(PL, J), I, 0)) * 16, 48 + (U + B(BT1(PL), RBM1(PL, J), I, 1)) * 16 - (BSm And Not FE And Not (Zanzo And RInc1(PL, J) >= 240) And RAC1(PL, J) = 0) * Int(RBSC1(PL, J) / 15), BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I, U
Next J
End If
BYF1(PL) = BY1(PL)
RushC1(PL) = 0: J = RushC1(PL)
RBX1(PL, J) = BX1(PL): RBY1(PL, J) = BY1(PL): RBM1(PL, J) = BM1(PL): RAC1(PL, J) = AC1(PL): RBSC1(PL, J) = BSC1(PL): RInc1(PL, J) = Inc1(PL): RBYF1(PL, J) = BYF1(PL)
'ゲームオーバー表示
If Not (Trn Or TA Or Fnl) And VM <= 1 And ((Pau = 0 Or Pau = -1) And BA1(PL) = 10 Or BA1(PL) = 9 And Liv1(PL) <= 0 And BWC1(PL) >= 30) Then StringD 120, 224, 7, "GAME OVER", False
If TA And Not Fnl And VM <= 1 And ((Pau = 0 Or Pau = -1) And BA1(PL) = 10 Or BA1(PL) = 9 And Liv1(PL) <= 0 And BWC1(PL) >= 30) Then StringD 136, 224, 7, "FAILURE", False
End If
'フィールド枠線表示
If VM = 3 Or (Fnl And (Pau <= 0 Or BA1(PL) <> 9)) Or Pau > 0 And BA1(PL) <> 9 Then
If BA1(PL) > 2 And BA1(PL) < 6 Then
For I = 0 To 3
FL(I) = BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1))
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = BT1(PL) + 1
Next I
End If
For I = 0 To 21
For U = 0 To 8
If BF1(PL, 3 + U, I + 1) = 0 Xor BF1(PL, 4 + U, I + 1) = 0 Then
With Src
 .Left = 160: .Top = 372: .Right = .Left + 2: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 127 + U * 16, 64 + I * 16, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next U, I
For I = 0 To 20
For U = 0 To 9
If BF1(PL, 3 + U, I + 1) = 0 Xor BF1(PL, 3 + U, I + 2) = 0 Then
With Src
 .Left = 160: .Top = 368: .Right = .Left + 16: .Bottom = .Top + 2
End With
If Not FS Then BBSf.BltFast 112 + U * 16, 79 + I * 16, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next U, I
If BA1(PL) > 2 And BA1(PL) < 6 Then
For I = 0 To 3
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = FL(I)
Next I
End If
End If
'ミス表示
If BA1(PL) = 9 And Pau <= 0 Then
For I = 0 To 21
RR1 = BWC1(PL) - I
If RR1 < 0 Then RR1 = 0
If RR1 > 16 Then RR1 = 16
RR2 = BWC1(PL) - I - 22
If RR2 < 0 Then RR2 = 0
If RR2 > 16 Then RR2 = 16
RR = Int(Rnd * 160)
With Src
.Left = RR: .Top = 368 + ((I + Int(CCC)) Mod 7) * 16 + RR2: .Right = 160: .Bottom = .Top + RR1 - RR2
End With
If Not FS Then BBSf.BltFast 112, 64 + I * 16 + RR2, BDSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 368 + ((I + Int(CCC)) Mod 7) * 16 + RR2: .Right = .Left + RR: .Bottom = .Top + RR1 - RR2
End With
If Not FS Then BBSf.BltFast 272 - RR, 64 + I * 16 + RR2, BDSf, Src, DDBLTFAST_WAIT
Next I
End If
'消去アニメ表示
EAni
'NEXT表示
If MNx < 4 Or BA1(PL) > 0 Then
For I = 0 To MNx - 1 + (MNx = 4) - (MNx = 0 And BA1(PL) <> 3)
With Src
.Left = Int(Nx1(PL, I) / 4) * 64: .Top = 32 + (Nx1(PL, I) Mod 4) * 32: .Right = .Left + 64: .Bottom = .Top + 32
End With
If Not FS Then BBSf.BltFast 160 + I * 80, 16, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
If MNx = 4 Then
For I = 0 To 8
With Src
.Left = Int(Nx1(PL, 3 + I) / 4) * 64: .Top = 32 + (Nx1(PL, 3 + I) Mod 4) * 32: .Right = .Left + 64: .Bottom = .Top + 32
End With
If Not FS Then BBSf.BltFast 320, 64 + I * 48, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
For I = 0 To 3
With Src
.Left = Int(Nx1(PL, 11 + I) / 4) * 64: .Top = 32 + (Nx1(PL, 11 + I) Mod 4) * 32: .Right = .Left + 64: .Bottom = .Top + 32
End With
If Not FS Then BBSf.BltFast 320 - I * 80, 448, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
For I = 0 To 9
With Src
.Left = Int(Nx1(PL, 15 + I) / 4) * 64: .Top = 32 + (Nx1(PL, 15 + I) Mod 4) * 32: .Right = .Left + 64: .Bottom = .Top + 32
End With
If Not FS Then BBSf.BltFast 0, 448 - I * 48, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
End If
Else
NxD
End If
StringD 88, 16, 9, "NEXT", False
'キー表示
If (VM = 2 And CsrUU <= 0) Or ((Trn And Not FE Or VM = 4) And Pau = 0) Then
VKA = VKA - InpA1(PL) * (VKA > 2) + (Not InpA1(PL)) * (VKA < 1)
If VKA <= 1 And InpA1(PL) Then VKA = 4
If VKA >= 2 And Not InpA1(PL) Then VKA = 0
VKB = VKB - InpB1(PL) * (VKB > 2) + (Not InpB1(PL)) * (VKB < 1)
If VKB <= 1 And InpB1(PL) Then VKB = 4
If VKB >= 2 And Not InpB1(PL) Then VKB = 0
With Src
.Left = 128 + VKA * 48: .Top = 0: .Right = .Left + 48: .Bottom = .Top + 48
End With
If Not FS Then BBSf.BltFast 512, 352, SpSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
With Src
.Left = 128 + VKB * 48: .Top = 0: .Right = .Left + 48: .Bottom = .Top + 48
End With
If Not FS Then BBSf.BltFast 568, 336, SpSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
With Src
.Left = 64: .Top = 0: .Right = .Left + 64: .Bottom = .Top + 64
End With
If Not FS Then BBSf.BltFast 416, 336, SpSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
If (InpL1(PL) Or InpR1(PL)) And (InpU1(PL) Or InpD1(PL)) Then RR = 18 Else RR = 24
VKX3 = VKX1 + ((416 + InpL1(PL) * RR - InpR1(PL) * RR) - VKX1) * 0.25: VKY3 = VKY1 + ((336 + InpU1(PL) * RR - InpD1(PL) * RR) - VKY1) * 0.25
VKX2 = VKX1 + ((416 + InpL1(PL) * RR - InpR1(PL) * RR) - VKX1) * 0.5: VKY2 = VKY1 + ((336 + InpU1(PL) * RR - InpD1(PL) * RR) - VKY1) * 0.5
VKX1 = VKX1 + ((416 + InpL1(PL) * RR - InpR1(PL) * RR) - VKX1) * 0.75: VKY1 = VKY1 + ((336 + InpU1(PL) * RR - InpD1(PL) * RR) - VKY1) * 0.75
With Src
.Left = 0: .Top = 0: .Right = .Left + 64: .Bottom = .Top + 64
End With
If Not FS Then BBSf.BltFast VKX3, VKY3, SpSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
If Not FS Then BBSf.BltFast VKX2, VKY2, SpSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
If Not FS Then BBSf.BltFast VKX1, VKY1, SpSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
With Src
.Left = 128: .Top = 48: .Right = .Left + 9: .Bottom = .Top + 9
End With
If Not FS Then BBSf.BltFast VKX3 + 13, VKY3 + 12, SpSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
If Not FS Then BBSf.BltFast VKX2 + 13, VKY2 + 12, SpSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
If Not FS Then BBSf.BltFast VKX1 + 13, VKY1 + 12, SpSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
'ライフ(>14)表示
If Liv1(PL) > 14 Then TextD 112, 400, 4 + Int(LAni / 5), Liv1(PL), True, 2
'モード表示
If Fnl Then 'FINAL
StringD 400, 16, 6, "FINAL", False
If Not TA Then StringD 400, 40, 4, "(JOKER)", False
If TA Then StringD 400, 40, 4, "(FURTHEST)", False
Else
If Trn Then 'TRAINING
StringD 400, 16, 6, "TRAINING", False
ElseIf TA Then 'TIME ATTACK
StringD 400, 16, 6, "TIME ATTACK", False
Else 'SCORE ATTACK
StringD 400, 16, 6, "SCORE ATTACK", False
End If
If TrM = 0 Then StringD 400, 40, 4, "(NORMAL)", False
If TrM = 1 Then StringD 400, 40, 4, "(HARD)", False
If TrM = 2 Then StringD 400, 40, 4, "(ADVANCE)", False
End If
'スコア表示
If Trn Or TA Or Fnl Then
StringD 448, 80 + Trn * 8, 9, "TIME", False
TextD 416, 112 + Trn * 16, CCC Mod 7, Int(CT(PL) / 6000), True, 2
StringD 416, 112 + Trn * 16, CCC Mod 7, Chr$(39), False, 1
If (Int(CT(PL) / 100) Mod 60) < 10 Then StringD 432, 112 + Trn * 16, CCC Mod 7, "0", False
TextD 464, 112 + Trn * 16, CCC Mod 7, Int(CT(PL) / 100) Mod 60, True, 2
StringD 464, 112 + Trn * 16, CCC Mod 7, Chr$(34), False, 1
If (CT(PL) Mod 100) < 10 Then StringD 480, 112 + Trn * 16, CCC Mod 7, "0", False
TextD 512, 112 + Trn * 16, CCC Mod 7, CT(PL) Mod 100, True, 2
If Fnl And Not TA And Liv1(PL) > 0 And BA1(PL) <> 10 And Pau >= 0 And RT(PL) > 0 Then
StringD 520, 112 + Trn * 16, 11, "+", False
TextD 560, 112 + Trn * 16, CCC Mod 7, Int(RT(PL) / 6000), True, 2
StringD 560, 112 + Trn * 16, CCC Mod 7, Chr$(39), False, 1
If (Int(RT(PL) / 100) Mod 60) < 10 Then StringD 576, 112 + Trn * 16, CCC Mod 7, "0", False
TextD 608, 112 + Trn * 16, CCC Mod 7, Int(RT(PL) / 100) Mod 60, True, 2
StringD 608, 112 + Trn * 16, CCC Mod 7, Chr$(34), False, 1
End If
End If
If Not TA And Not Fnl Then
StringD 432, 80 - Trn * 48, 9, "SCORE", False
TextD 512, 112 - Trn * 40, 10 - (Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150), Sc1(PL), True, 8
If SB1(PL) > 0 Then
StringD 520, 112 - Trn * 40, 11, "+", False
TextD 544, 112 - Trn * 40, 11, SB1(PL) * ((1 + CB1(PL)) * 0.5), False, 6
End If
End If
'レベル表示
NF1(PL) = NF1(PL) - 1 - (NF1(PL) <= 0)
If Not (TA And Fnl) Then
StringD 432, 160 - Trn * 24, 9, "LEVEL", False
TextD 512, 192 - Trn * 16, 3 - (NF1(PL) > 0 Or (Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150)) * 8, Lv1(PL), True, 8
Else
StringD 432, 160 - Trn * 24, 9, "LIVES", False
TextD 512, 192 - Trn * 16, 4 + Int(LAni / 5), Liv1(PL), True, 8
End If
'ライン表示
If Fnl And Not TA Then
StringD 432, 240, 9, "STOCK", False
TextD 512, 272 + Trn * 8, 3 - (Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150) * 8, St1(PL), True, 8
Else
StringD 432, 240, 9, "LINES", False
TextD 512, 272 + Trn * 8, 3 - (Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150) * 8, Li1(PL), True, 8
If (Not Trn And Lv1(PL) < 30 - (TrM >= 1) * 20) Or (TA And Li1(PL) < 100 - Fnl * 200) Then
StringD 520, 272 + Trn * 8, 4, "/", False
If TA Then
TextD 544, 272 + Trn * 8, 3 - (Lv1(PL) >= 29 - (TrM >= 1) * 20 - (TrM = 2) * 150) * 8, 100 - Fnl * 200, False, 6
Else
TextD 544, 272 + Trn * 8, 3 - (Lv1(PL) >= 29 - (TrM >= 1) * 20 - (TrM = 2) * 150) * 8, NL1(PL), False, 6
End If
End If
End If
'システムスピード表示
If Not (Tit Or Trn) And VM < 2 And SpdR < 950 Then
RR = -(SpdR < 900) * (Int(CC / 5) Mod 3)
StringD 416, 320, 9, "SYSTEM", False
TextD 480, 352, RR, Int(SpdR / 10), True, 8
StringD 480, 352, RR, ".", False
TextD 512, 352, RR, SpdR Mod 10, True, 8
End If
'残り時間表示
If Not (Trn Or (TA And Not Fnl)) And (Lv1(PL) >= 30 - (TrM >= 1) * 20 Or TrM = 2) And St1(PL) < 100 And Lv1(PL) + St1(PL) < 300 Then
StringD 88, 416, 8, "LIMIT", False
TextD 200, 416, -(FT(PL) < 3000) * (CCC Mod 7), Int(FT(PL) / 6000), True, 2
StringD 200, 416, -(FT(PL) < 3000) * (CCC Mod 7), Chr$(39), False, 1
If (Int(FT(PL) / 100) Mod 60) < 10 Then StringD 216, 416, -(FT(PL) < 3000) * (CCC Mod 7), "0", False
TextD 248, 416, -(FT(PL) < 3000) * (CCC Mod 7), Int(FT(PL) / 100) Mod 60, True, 2
StringD 248, 416, -(FT(PL) < 3000) * (CCC Mod 7), Chr$(34), False, 1
If (FT(PL) Mod 100) < 10 Then StringD 264, 416, -(FT(PL) < 3000) * (CCC Mod 7), "0", False
TextD 296, 416, -(FT(PL) < 3000) * (CCC Mod 7), FT(PL) Mod 100, True, 2
End If
'フィールドエディット表示
If FE Then
CursorD 112 + FEX * 16, 48 + FEY * 16, 11, 0
End If
'ポーズ表示
If FE And CC < 30 Then StringD 112, 64, 9, "FIELD EDIT", False
If Pau > 0 Then
If Trn Then
StringD 140, 160, 8 - (CPos = 0) * 3, "CONTINUE", False, 8
StringD 140, 192, 9, "LEVEL", False, 8
TextD 268, 192, 7 - (CPos = 1) * 4, Lv1(PL), True, 3
If BA1(PL) <> 9 Then StringD 140, 224, 8 - (CPos = 2) * 3, "FIELD EDIT", False, 8 Else StringD 140, 224, 7, "FIELD EDIT", False, 8
StringD 140, 256, 8 - (CPos = 3) * 3, "EXIT", False, 8
CursorD 118, 160 + CPos * 32, 11, 1 + (CPos = 1)
Else
If RecCh Then StringD 140, 224 + (VM = 4) * 32, 8 - (CPos = 0) * 3, "CONTINUE", False
If VM = 4 Then StringD 140, 224, 8 - (CPos = 1) * 3, "REPLAY", False
StringD 140, 256, 8 - (CPos = 1 - (VM = 4)) * 3, "EXIT", False
CursorD 118, 224 + (VM = 4) * 32 + CPos * 32, 11, 1
End If
End If
'スタート表示
If BA1(PL) = 0 Then
If Pau = 0 And Not FE Then
If VM <= 1 Then
If BWC1(PL) > 12 Then
RR = BWC1(PL) - 12
TextD 184, 224, 11, 3 - Int(RR / 60), False
For I = 0 To 61 - (RR Mod 60)
If RR < 60 Then
CursorD 184 - Sin(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60)), 224 + Cos(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60) * 0.75), -(RR >= 60) - (RR >= 120) * 7, 0
End If
If RR >= 60 And RR < 120 Then
CursorD 184 - Sin((-45 + (RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60)), 224 + Cos(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60) * 0.75), -(RR >= 60) - (RR >= 120) * 7, 0
CursorD 184 - Sin((45 + (RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60)), 224 + Cos(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60) * 0.75), -(RR >= 60) - (RR >= 120) * 7, 0
End If
If RR >= 120 And RR < 180 Then
CursorD 184 - Sin((-60 + (RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60)), 224 + Cos((-60 + (RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 50) * (1 + Int(RR / 60) * 0.125), -(RR >= 60) - (RR >= 120) * 7, 0
CursorD 184 - Sin(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 50) * (1 + Int(RR / 60) * 0.125), 224 + Cos(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60) * 0.75), -(RR >= 60) - (RR >= 120) * 7, 0
CursorD 184 - Sin((60 + (RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60)), 224 + Cos((60 + (RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60) * 0.75), -(RR >= 60) - (RR >= 120) * 7, 0
End If
Next I
End If
End If
If ScLiv(2) > 0 Then
StringD 124, 96, 9, "RUSH:", False, 8
If Rush1(PL) Then StringD 212, 96, 11, "ON", False Else StringD 212, 96, 11, "OFF", False
End If
End If
End If
'終了表示
If Pau = -3 Then
If Not (TA Or Fnl) Then
If Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 - Fnl * 100 Then
If Liv1(PL) >= 5 Then StringD 128, 176, 11, "PERFECT!", False Else StringD 136, 176, 9, "FINISH!", False
StringD 152, 224, 9, "BONUS", False
RR = Int(((Sc1(PL) + SB1(PL)) / 5 * (Liv1(PL)))): If RR > 1000000000 Then RR = 1000000000
TextD 192 - (Len(Str$(RR)) - 1) * 8, 256, 11, RR, False
Else
StringD 136, 224, 9, "TIME UP", False
End If
End If
If Fnl Then
If Lv1(PL) > 300 Then
StringD 112, 224, 11, "EXCELLENT!", False
Else
If Lv1(PL) >= 300 Then StringD 128, 224, 11, "PERFECT!", False Else StringD 136, 224, 9, "FINISH!", False
End If
End If
End If
If Pau = -4 Then
If Fnl Then
If Liv1(PL) >= 10 Then StringD 112, 224, 11, "EXCELLENT!", False, False Else StringD 136, 224, 11, "FINISH!", False
Else
If Liv1(PL) >= 3 Then StringD 128, 224, 11, "PERFECT!", False Else StringD 136, 224, 9, "FINISH!", False
End If
End If
'名前入力表示
If Pau = -2 Then
StringD 124, 176, 9, "RANK:", False
If TrRnk = 0 Then StringD 212, 176, 11, "1ST", False
If TrRnk = 1 Then StringD 212, 176, 3, "2ND", False
If TrRnk = 2 Then StringD 212, 176, 2, "3RD", False
If TrRnk = 3 Then StringD 212, 176, 1, "4TH", False
If TrRnk = 4 Then StringD 212, 176, 0, "5TH", False
If TrRnk > 4 Then StringD 212, 176, 7, "---", False
StringD 120, 224, 9, "YOUR NAME", False
StringD 144, 256, 1, Left$(TrNm, NmPos), False
If Nm = 45 Then OkD 144 + NmPos * 16, 256, 11 Else StringD 144 + NmPos * 16, 256, 11, Mid$(Ne, Nm + 1, 1), False
End If
'ファイル名入力表示
If Not GEnd And VM = 3 Then
StringD 128, 224, 9, "FILENAME", False
StringD 128, 256, 1, Left$(FNm, NmPos), False
If Nm = 36 Then OkD 128 + NmPos * 16, 256, 11 Else StringD 128 + NmPos * 16, 256, 11, Mid$(FNE, Nm + 1, 1), False
End If
'ビデオセーブ表示
If Pau = -5 Or Pau = -7 Then
If Pau = -5 Then If VM > 0 Then StringD 140, 224, 8 - (CPos = 0) * 3, "VIDEO", False Else StringD 140, 224, 7, "VIDEO", False
If Pau = -7 Then If VM > 0 Then StringD 140, 224, 8 - (CPos = 0) * 3, "REPLAY", False Else StringD 140, 224, 7, "REPLAY", False
StringD 140, 256, 8 - (CPos = 1) * 3, "EXIT", False
CursorD 118, 224 + CPos * 32, 11, 1
End If
'ビデオスピード表示
If Not GEnd And VM = 2 And CsrUU <= 0 Then
StringD 112, 64, 8, "SPEED", False
TextD 256, 64, 11, VS * 25, True, 4
StringD 256, 64, 11, "%", False
End If

'TextD 448, 336, 0, -CInt(InpL1(PL)), True, 4
'TextD 480, 336, 0, -CInt(InpR1(PL)), True, 4
'TextD 464, 320, 0, -CInt(InpU1(PL)), True, 4
'TextD 464, 352, 0, -CInt(InpD1(PL)), True, 4
'TextD 512, 352, 0, -CInt(InpA1(PL)), True, 4
'TextD 536, 352, 0, -CInt(InpB1(PL)), True, 4
'If VP > 0 Then
'TextD 536, 384, 0, VP - 1, True, 6
'TextD 536, 400, 0, SVP - 1, True, 6
'TextD 536, 400, 0, VD, True, 3
'TextD 536, 432, 0, VDC, True, 3
'End If

FChange

Loop
Set BFSf(0) = Nothing
Set BCSf = Nothing
Set BDSf = Nothing
Exit Sub

CE:
'Beep
GMode = 2
End Sub

Sub VS2P() '2P対戦
On Error GoTo CE
GEnd = False
FS = False
Pau = Not Tit And Not OfBt
If Not Tit Then Fade = True: ScrFC = 0
KeyInitAll

For I = 0 To 1
If Not OfBt Then CPG1(I) = RCPG1(I)
Next I
If Tit Then PT(0) = 5: PT(1) = 5: CPG1(0) = 6: CPG1(1) = 4
If Not OfBt Then For PL = 0 To 1: SLv1(PL) = 0: Next PL

'背景パレット初期化
If Not WM Then
For I = 0 To 127
With MPa(I)
.red = 0: .green = 0: .blue = 0
End With
Next I
If Not FS Then MPal.SetEntries 0, 256, MPa
End If

'ボード（オフスクリーン）の作成
BDCr BD

'ブロック（オフスクリーン）の作成
BCCr BC

BRndInit
For I = 0 To 776: BtNx(I) = BRnd: Next I
BtNx(776) = (BtNx(776) - (BtNx(776) = BtNx(0))) Mod 7
For I = 0 To 9: DBS(I) = I: Next I
For I = 0 To 8: RR = Int(Rnd * (10 - I)): RR1 = DBS(I): DBS(I) = DBS(RR): DBS(RR) = RR1: Next I
DAni = 0
GFin = 0: VSFinC = 0
VSA = 0: VSR = 3
For PL = 0 To 1
'フィールド（オフスクリーン）の作成
SD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
SD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
SD.lWidth = 160: SD.lHeight = 352
Set BFSf(PL) = DD.CreateSurface(SD)
CK.low = 0: CK.high = 0
BFSf(PL).SetColorKey DDCKEY_SRCBLT, CK
If Not WM Then BFSf(PL).SetPalette MPal
BFSf(PL).BltColorFill RC0, 0
'その他の設定
Rush1(PL) = False
If Not OfBt Then Liv1(PL) = 3
Sc1(PL) = 0: SB1(PL) = 0: CB1(PL) = 0: Li1(PL) = 0
If OfBt Then Lv1(PL) = SLv1(PL) Else Lv1(PL) = VSLv(TrM, SLv1(PL))
NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50))
NF1(PL) = 0: BA1(PL) = -Tit: BWC1(PL) = 0: WCa1(PL) = False: DCa1(PL) = False
LBA1(PL) = 0
Hit1(PL) = 0: Dmg1(PL) = 0
VSR1(PL) = 0
CPos1(PL) = -1 - (PT(PL) < 5) * 2
InpL1(PL) = False: InpR1(PL) = False: InpLL1(PL) = 0: InpRR1(PL) = 0: InpLLL1(PL) = 0: InpRRR1(PL) = 0
InpU1(PL) = False: InpD1(PL) = False: InpUU1(PL) = 0: InpDD1(PL) = 0
InpA1(PL) = False: InpB1(PL) = False: InpS1(PL) = False: InpAA1(PL) = 0: InpBB1(PL) = 0: InpSS1(PL) = 1
For I = 0 To 25: For U = 0 To 15: BF1(PL, U, I) = 0: Next U, I
For I = 0 To 22: BF1(PL, 2, I) = 9: BF1(PL, 13, I) = 9: Next I
For I = 3 To 12: BF1(PL, I, 0) = 9: BF1(PL, I, 23) = 9: Next I
For I = 0 To 25: Nx1(PL, I) = BtNx(I): Next I: NxC1(PL) = 25
DBSC1(PL) = 0
Next PL

BGInit

If OfBt Then
If Lv1(0) > Lv1(1) Then LvL = Lv1(0) Else LvL = Lv1(1)
Else
If VSLv(TrM, SLv1(0)) > VSLv(TrM, SLv1(1)) Then LvL = VSLv(TrM, SLv1(0)) Else LvL = VSLv(TrM, SLv1(1))
End If

St = Int((LvL + (LvL > 50) * (LvL - 50)) / 5)
If LvL >= 50 And LvL < 200 Then St = 10
If LvL >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 Then St = 11 - (Not (Fnl Or TA)) * 2
Stt = St
If BG >= 1 Then BGCr Stt ' And Stt < 12
FOC = 0: FIC = 0: FF = False
For I = 0 To 15: Sou(I).Fre = 22050: Sou(I).Pan = 0: Sou(I).On = False: Next I
EAC = 0: For I = 0 To 159: EA(I).V = False: Next I

If Not Tit Then
If TrM = 0 Then PlayMusic "BATTLE0", 3072, 76800, -OfBt
If TrM = 1 Then PlayMusic "BATTLE1", 16128, 114432, -OfBt
If TrM = 2 Then PlayMusic "BATTLE2", 4608, 78336, -OfBt
End If

'----メインループ----
Do..<GEnd

If GFin >= 1 Then
KeyScanAll
If Fade Then Fade = False: ScrFC = 0
If GFin = 2 And (CsrAA = 1 Or CsrBB = 1) Then ScrFC = 80
If ScrFC = 80 Then PlayMusic vbNullString$: GMode = 5: GEnd = True
End If
If GFin < 2 Then

If Pau <> 0 Then 'ポーズ
If Pau = -1 Then
KeyScanAll

If VSA = 0 Then '選択モード
For PL = 0 To 1
If PT(PL) = -1 Then
For I = 0 To 4
RR = False
For U = 0 To 1
If PT(U) = I Then RR = True
Next U
If PT(PL) = -1 And Not RR And (CsrAA1(I) = 1 Or CsrSS1(I)) Then PT(PL) = I: CPos1(PL) = 1: CsrAA1(PT(PL)) = CsrAA1(PT(PL)) - (CsrAA1(PT(PL)) = 1): CsrSS1(PT(PL)) = CsrSS1(PT(PL)) - (CsrSS1(PT(PL)) = 1): Sou(0).Pan = PL * 1000 - 500: Sou(0).On = True
Next I
End If
Next PL
For PL = 0 To 1
If PT(PL) >= 0 Then
If CPos1(PL) >= 0 And CsrUU1(PT(PL)) = 1 Then CPos1(PL) = CPos1(PL) - 1 - (CPos1(PL) <= 0) * 4: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
If CPos1(PL) >= 0 And CsrDD1(PT(PL)) = 1 Then CPos1(PL) = CPos1(PL) + 1 + (CPos1(PL) >= 3) * 4: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
If CPos1(PL) = 1 Then
If (CsrLL1(PT(PL)) = 1 Or CsrLL1(PT(PL)) = 15) And SLv1(PL) > 0 Then SLv1(PL) = SLv1(PL) - 1: Lv1(PL) = VSLv(TrM, SLv1(PL)): NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50)): Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
If (CsrRR1(PT(PL)) = 1 Or CsrRR1(PT(PL)) = 15) And SLv1(PL) < 2 - (TrM > 0) Then If SLv1(PL) <= 0 Or ScLv(TrM) >= VSLv(TrM, SLv1(PL) + 1) Then SLv1(PL) = SLv1(PL) + 1: Lv1(PL) = VSLv(TrM, SLv1(PL)): NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50)): Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
End If
If CPos1(PL) = 2 Then
If (CsrLL1(PT(PL)) = 1 Or CsrLL1(PT(PL)) = 15) And Liv1(PL) > 1 Then Liv1(PL) = Liv1(PL) - 1: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
If (CsrRR1(PT(PL)) = 1 Or CsrRR1(PT(PL)) = 15) And Liv1(PL) < 10 - 5 * ((ScLiv(TrM) >= 1) + (ScLiv(TrM) >= 5) + (TmLiv(TrM) >= 1) + (TmLiv(TrM) >= 3)) Then Liv1(PL) = Liv1(PL) + 1: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
End If
If PT(PL) >= 0 And CPos1(PL) >= 0 And CsrSS1(PT(PL)) = 1 Then
CPos1(PL) = -1: Sou(0).Pan = PL * 1000 - 500: Sou(0).On = True
Else
If CsrAA1(PT(PL)) = 1 Then
Select Case CPos1(PL)
Case 0
CPos1(PL) = -1: Sou(0).Pan = PL * 1000 - 500: Sou(0).On = True
Case 3
PT(PL) = -1: CPos1(PL) = -1: Sou(12).Pan = PL * 1000 - 500: Sou(12).On = True
End Select
Else
If CsrBB1(PT(PL)) = 1 Then
Select Case CPos1(PL)
Case -1
CPos1(PL) = 0: Sou(12).Pan = PL * 1000 - 500: Sou(12).On = True
Case Else
PT(PL) = -1: CsrBB = 2: CPos1(PL) = -1: Sou(12).Pan = PL * 1000 - 500: Sou(12).On = True
End Select
End If
End If
End If
End If
Next PL
RR1 = False: RR2 = False
For I = 0 To 1
If PT(I) >= 0 And CPos1(I) >= 0 Then RR1 = True
Next I
For I = 0 To 1
If PT(I) >= 0 Then RR2 = True
Next I
If Not RR1 And RR2 Then KeyInit: VSA = 1
If Not RR2 And CsrBB = 1 Then GFin = 2
End If

If VSA = 1 Then 'CP選択モード
RR = -1
For I = 1 To 0 Step -1
If PT(I) = -1 Then RR = I
Next I
If RR >= 0 Then PT(RR) = 5: CPos1(RR) = 1
For PL = 0 To 1
If PT(PL) = 5 Then
If CPos1(PL) >= 0 And CsrUU = 1 Then CPos1(PL) = CPos1(PL) - 1 - (CPos1(PL) <= 0) * 5: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
If CPos1(PL) >= 0 And CsrDD = 1 Then CPos1(PL) = CPos1(PL) + 1 + (CPos1(PL) >= 4) * 5: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
If CPos1(PL) = 1 Then
If (CsrLL = 1 Or CsrLL = 15) And CPG1(PL) > 1 Then CPG1(PL) = CPG1(PL) - 1: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
If (CsrRR = 1 Or CsrRR = 15) And CPG1(PL) < CPMax Then CPG1(PL) = CPG1(PL) + 1: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
End If
If CPos1(PL) = 2 Then
If (CsrLL = 1 Or CsrLL = 15) And SLv1(PL) > 0 Then SLv1(PL) = SLv1(PL) - 1: Lv1(PL) = VSLv(TrM, SLv1(PL)): NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50)): Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
If (CsrRR = 1 Or CsrRR = 15) And SLv1(PL) < 2 - (TrM > 0) Then If SLv1(PL) <= 0 Or ScLv(TrM) >= VSLv(TrM, SLv1(PL) + 1) Then SLv1(PL) = SLv1(PL) + 1: Lv1(PL) = VSLv(TrM, SLv1(PL)): NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50)): Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
End If
If CPos1(PL) = 3 Then
If (CsrLL = 1 Or CsrLL = 15) And Liv1(PL) > 1 Then Liv1(PL) = Liv1(PL) - 1: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
If (CsrRR = 1 Or CsrRR = 15) And Liv1(PL) < 10 - 5 * ((ScLiv(TrM) >= 1) + (ScLiv(TrM) >= 5) + (TmLiv(TrM) >= 1) + (TmLiv(TrM) >= 3)) Then Liv1(PL) = Liv1(PL) + 1: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
End If
If PT(PL) >= 0 And CPos1(PL) >= 0 And CsrSS = 1 Then
CPos1(PL) = -1: Sou(0).Pan = PL * 1000 - 500: Sou(0).On = True
Else
If CsrAA = 1 Then
Select Case CPos1(PL)
Case 0
CPos1(PL) = -1: Sou(0).Pan = PL * 1000 - 500: Sou(0).On = True
Case 4
PT(PL) = -1: CPos1(PL) = -1: Sou(12).Pan = PL * 1000 - 500: Sou(12).On = True
End Select
Else
If CsrBB = 1 Then
Select Case CPos1(PL)
Case -1
CPos1(PL) = 0: Sou(12).Pan = PL * 1000 - 500: Sou(12).On = True
Case Else
PT(PL) = -1: CsrBB = 2: CPos1(PL) = -1: Sou(12).Pan = PL * 1000 - 500: Sou(12).On = True
End Select
End If
End If
End If
End If
Next PL

RR1 = False: RR2 = False
For I = 0 To 1
If PT(I) = 5 Then RR1 = True
Next I
For I = 0 To 1
If PT(I) = -1 Then RR2 = True
Next I
If Not RR1 And RR2 Then
For PL = 0 To 1
If PT(PL) >= 0 Then VSA = 0: CPos1(PL) = 0
Next PL
End If
RR = False
For I = 0 To 1
If PT(I) = -1 Or CPos1(I) >= 0 Then RR = True
Next I
If Not RR Then
For I = 0 To 1: RCPG1(I) = CPG1(I): Next I
Pau = 0: Sou(8).Fre = 22050: Sou(8).On = True
End If
End If

Else
KeyScan PT(Pau - 1)
If CsrUU = 1 Or CsrDD = 1 Then CPos = -(CPos = 0): Sou(5).Pan = (Pau - 1) * 1000 - 500: Sou(5).On = True
If CsrSS = 1 Then Pau = 0
If CPos = 0 And CsrAA = 1 Then Pau = 0
If CPos = 1 And CsrAA = 1 Then GFin = 2: Fade = False: ScrFC = 0
End If
Else
If OfBt Then KeyScanAll
For PL = 0 To 1
'キー入力
DIDK.GetDeviceStateKeyboard KS
Select Case PT(PL)
Case 0 'キーボード
InpL1(PL) = KS.Key(DIK_LEFT) > 0 Or KS.Key(KArwL) > 0
InpR1(PL) = KS.Key(DIK_RIGHT) > 0 Or KS.Key(KArwR) > 0
InpU1(PL) = KS.Key(DIK_UP) > 0 Or KS.Key(KArwU) > 0
InpD1(PL) = KS.Key(DIK_DOWN) > 0 Or KS.Key(KArwD) > 0
InpA1(PL) = KS.Key(DIK_SPACE) > 0 Or KS.Key(KBL) > 0
InpB1(PL) = KS.Key(DIK_RETURN) > 0 Or KS.Key(DIK_NUMPADENTER) > 0 Or KS.Key(KBR) > 0
InpS1(PL) = KS.Key(DIK_TAB) > 0 Or KS.Key(KBS) > 0
Case 1 To 4 'パッド
On Error Resume Next
JS(PT(PL) - 1) = JS0
DIDJ(PT(PL) - 1).Poll: DIDJ(PT(PL) - 1).GetDeviceStateJoystick JS(PT(PL) - 1)
On Error GoTo CE
InpL1(PL) = (JS(PT(PL) - 1).X < 16384 And ((PArw(PT(PL) - 1) Mod 2) >= 1 Or JS(PT(PL) - 1).Y >= 16384 And JS(PT(PL) - 1).Y <= 49152))
InpR1(PL) = (JS(PT(PL) - 1).X > 49152 And ((PArw(PT(PL) - 1) Mod 2) >= 1 Or JS(PT(PL) - 1).Y >= 16384 And JS(PT(PL) - 1).Y <= 49152))
InpU1(PL) = (JS(PT(PL) - 1).Y < 16384 And ((PArw(PT(PL) - 1) Mod 4) >= 2 Or JS(PT(PL) - 1).X >= 16384 And JS(PT(PL) - 1).X <= 49152))
InpD1(PL) = (JS(PT(PL) - 1).Y > 49152 And ((PArw(PT(PL) - 1) Mod 4) >= 2 Or JS(PT(PL) - 1).X >= 16384 And JS(PT(PL) - 1).X <= 49152))
InpA1(PL) = JS(PT(PL) - 1).buttons(PBL(PT(PL) - 1)) > 64
InpB1(PL) = JS(PT(PL) - 1).buttons(PBR(PT(PL) - 1)) > 64
InpS1(PL) = JS(PT(PL) - 1).buttons(PBS(PT(PL) - 1)) > 64
Case 5 'CP
InpL1(PL) = False: InpR1(PL) = False: InpA1(PL) = False: InpB1(PL) = False: InpU1(PL) = False: InpD1(PL) = False
If (BA1(PL) = 1 And BWC1(PL) > 3) Or BA1(PL) = 2 Or BA1(PL) = 3 Then
If (Not CPE(CPG1(PL)).SRt) Or (BX1(PL) >= CSX1(PL) - 1 And BX1(PL) <= CSX1(PL) + 1) Then
If BM1(PL) = (CSM1(PL) + 2) + (CSM1(PL) >= 2) * 4 Then InpA1(PL) = True: InpB1(PL) = True
If BM1(PL) = (CSM1(PL) + 1) + (CSM1(PL) >= 3) * 4 Then InpA1(PL) = True
If BM1(PL) = (CSM1(PL) - 1) - (CSM1(PL) <= 0) * 4 Then InpB1(PL) = True
End If
If BM1(PL) = CSM1(PL) Then InpA1(PL) = False: InpB1(PL) = False
If CPE(CPG1(PL)).SRt And (BT1(PL) = 2 Or BT1(PL) = 5 Or BT1(PL) = 6) And (CSX1(PL) < 5 Or CSX1(PL) > 7) Then
If BX1(PL) = 6 Then InpA1(PL) = True: InpB1(PL) = True
If (BX1(PL) = 5 Or BX1(PL) = 7) Then InpA1(PL) = False: InpB1(PL) = False
End If
If CPE(CPG1(PL)).Spd = 0 And Not (BT1(PL) = 1 Or BT1(PL) = 3 Or BT1(PL) = 4) And (AC1(PL) >= 11 And AC1(PL) <= 13 And CCWC1(PL) = 0) Then InpA1(PL) = (BX1(PL) < CSX1(PL)) Xor (BT1(PL) <> 2): InpB1(PL) = (BX1(PL) > CSX1(PL)) Xor (BT1(PL) <> 2)
If BA1(PL) = 3 Then CWC1(PL) = CWC1(PL) + 1
If CWC1(PL) > CPE(CPG1(PL)).Spd - 1 Then
If BX1(PL) > CSX1(PL) Then InpL1(PL) = True
If BX1(PL) < CSX1(PL) Then InpR1(PL) = True
If CWC1(PL) > CPE(CPG1(PL)).Spd Then CWC1(PL) = 0
End If
If BM1(PL) = CSM1(PL) And BX1(PL) = CSX1(PL) Then
If BA1(PL) = 3 And AC1(PL) > 0 And CCWC1(PL) > Int(CPE(CPG1(PL)).Wt / 2) And (CSM1(PL) <> CS2M1(PL) Or CSX1(PL) <> CS2X1(PL)) Then CSM1(PL) = CS2M1(PL): CSX1(PL) = CS2X1(PL): CCWC1(PL) = Int(CPE(CPG1(PL)).Wt / 2)
CCWC1(PL) = CCWC1(PL) + 1 + (CCWC1(PL) > CPE(CPG1(PL)).Wt): If CCWC1(PL) > CPE(CPG1(PL)).Wt Then If ((TrM > 0 And CPE(CPG1(PL)).QD) Or TrM = 2) And CSM1(PL) = CS2M1(PL) And CSX1(PL) = CS2X1(PL) Then InpU1(PL) = True Else InpD1(PL) = (Lv1(PL) < 30 Or CSM1(PL) = CS2M1(PL) And CSX1(PL) = CS2X1(PL))
End If
If BX1(PL) = BMX1(PL) And BM1(PL) = BMM1(PL) Then
CMWC1(PL) = CMWC1(PL) + 1: If CMWC1(PL) >= 20 + CPE(CPG1(PL)).Wt Then InpU1(PL) = False: InpD1(PL) = (CMWC1(PL) > 20 + CPE(CPG1(PL)).Wt)
Else
CMWC1(PL) = 0
End If
End If
End Select

If InpL1(PL) And InpR1(PL) Then InpL1(PL) = False: InpR1(PL) = False
If InpU1(PL) And InpD1(PL) Then InpU1(PL) = False: InpD1(PL) = False
If InpL1(PL) Then InpLL1(PL) = InpLL1(PL) + 1 + (InpLL1(PL) > 0): InpLLL1(PL) = InpLLL1(PL) + 1 + (InpLLL1(PL) >= 15 + (TrM > 0) * 5) Else InpLL1(PL) = 0: InpLLL1(PL) = 0
If InpR1(PL) Then InpRR1(PL) = InpRR1(PL) + 1 + (InpRR1(PL) > 0): InpRRR1(PL) = InpRRR1(PL) + 1 + (InpRRR1(PL) >= 15 + (TrM > 0) * 5) Else InpRR1(PL) = 0: InpRRR1(PL) = 0
If InpU1(PL) Then InpUU1(PL) = InpUU1(PL) + 1 + (InpUU1(PL) >= 1) Else InpUU1(PL) = 0
If InpD1(PL) Then InpDD1(PL) = InpDD1(PL) + 1 + (InpDD1(PL) >= 1) Else InpDD1(PL) = 0
If InpA1(PL) Then InpAA1(PL) = InpAA1(PL) + 1 + (InpAA1(PL) >= 1) Else InpAA1(PL) = 0
If InpB1(PL) Then InpBB1(PL) = InpBB1(PL) + 1 + (InpBB1(PL) >= 1) Else InpBB1(PL) = 0
If InpS1(PL) Then InpSS1(PL) = InpSS1(PL) + 1 + (InpSS1(PL) >= 2) Else InpSS1(PL) = 0

'On Error Resume Next
'JS(0) = JS0
'If JC >= 1 Then DIDJ(0).Poll: DIDJ(0).GetDeviceStateJoystick JS(0)
'On Error GoTo CE
'If JS(0).buttons(7) <> 0 Then Lv1(PL) = Lv1(PL) + 1 + (Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150)

If Not OfBt And VSR > 2 And BA1(PL) <> 0 And InpSS1(PL) = 1 And Pau = 0 Then Pau = PL + 1: KeyInit: CPos = 0

Next PL

'同時決着判定
VSLiv = True
For PL = 0 To 1
If Liv1(PL) > 1 Or (Liv1(PL) = 1 And (BA1(PL) <> 9 Or BWC1(PL) > 0)) Then VSLiv = False
Next PL

For PL = 0 To 1
'ブロック動作
BAA1(PL) = BA1(PL)
If BAA1(PL) = 0 Then 'スタート
If InpSS1(PL) = 1 Then InpSS1(PL) = 2: If ScLiv(2) > 0 Then Rush1(PL) = Not Rush1(PL): Sou(0).Pan = 0: Sou(0).On = True
BWC1(PL) = BWC1(PL) + 1: If BWC1(PL) >= 192 Then BWC1(PL) = 0: WCa1(PL) = (TrM = 2): DCa1(PL) = True: BA1(PL) = 1
End If
If BAA1(PL) = 1 Then '時間待ち
If BWC1(PL) = 0 Then DmC1(PL) = Dmg1(PL) * (-(CB1(PL) = 0)): If DmC1(PL) > 0 Then Sou(11).Pan = PL * 1000 - 500: Sou(11).On = True: DBSC1(PL) = DBSC1(PL) + 1 + (DBSC1(PL) >= 9) * 10: If DmC1(PL) > 4 Then DmC1(PL) = 4
If TrM < 2 Then
If BWC1(PL) < DmC1(PL) Then
Dmg1(PL) = Dmg1(PL) - 1
For I = 1 To 21
For U = 3 To 12
BF1(PL, U, I) = BF1(PL, U, I + 1)
Next U, I
For I = 3 To 12
BF1(PL, I, 22) = 8 * (-(I - 3 <> DBS(DBSC1(PL))))
Next I
With Src
.Left = 0: .Top = 16: .Right = .Left + 160: .Bottom = .Top + 336
End With
BFSf(PL).BltFast 0, 0, BFSf(PL), Src, DDBLTFAST_WAIT
With Des
.Left = 0: .Top = 336: .Right = .Left + 160: .Bottom = .Top + 16
End With
BFSf(PL).BltColorFill Des, 0
For I = 0 To 9
If I <> DBS(DBSC1(PL)) Then
With Src
.Left = 112: .Top = 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
BFSf(PL).BltFast I * 16, 336, BCSf, Src, DDBLTFAST_WAIT
End If
Next I
End If
Else
If BWC1(PL) = 0 And DmC1(PL) > 0 Then
Dmg1(PL) = Dmg1(PL) - DmC1(PL)
For I = 1 To 23 - DmC1(PL)
For U = 3 To 12
BF1(PL, U, I) = BF1(PL, U, I + DmC1(PL))
Next U, I
For I = 23 - DmC1(PL) To 22: For U = 3 To 12
BF1(PL, U, I) = 8 * (-(U - 3 <> DBS(DBSC1(PL))))
Next U, I
With Src
.Left = 0: .Top = DmC1(PL) * 16: .Right = 160: .Bottom = 352
End With
BFSf(PL).BltFast 0, 0, BFSf(PL), Src, DDBLTFAST_WAIT
With Des
.Left = 0: .Top = 352 - DmC1(PL) * 16: .Right = 160: .Bottom = 352
End With
BFSf(PL).BltColorFill Des, 0
For I = 1 To DmC1(PL): For U = 0 To 9
If U <> DBS(DBSC1(PL)) Then
With Src
.Left = 112: .Top = 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
BFSf(PL).BltFast U * 16, 352 - I * 16, BCSf, Src, DDBLTFAST_WAIT
End If
Next U, I
End If
End If
If BWC1(PL) = 0 Then BT1(PL) = Nx1(PL, 0): BX1(PL) = 6: BMX1(PL) = BX1(PL): BY1(PL) = -(BT1(PL) > 0): BYF1(PL) = 0: BM1(PL) = 0: BMM1(PL) = BM1(PL): CWC1(PL) = 0: CCWC1(PL) = 0: CMWC1(PL) = 0
If PT(PL) = 5 And BWC1(PL) >= -(TrM < 2) * 4 And BWC1(PL) < 4 - (TrM < 2) * 4 Then CPAI BWC1(PL) + (TrM < 2) * 4
BWC1(PL) = BWC1(PL) + 1
If TrM = 2 And Not WCa1(PL) And InpUU1(PL) = 1 Then WCa1(PL) = True: DCa1(PL) = False
If Not DCa1(PL) And (TrM > 0 And InpUU1(PL) = 1 Or InpDD1(PL) = 1) Then DCa1(PL) = True: InpUU1(PL) = 2: InpDD1(PL) = 2
If TrM = 2 Then
If WCa1(PL) And BWC1(PL) > 0 And BWC1(PL) < 15 And (InpLL1(PL) = 1 Or InpRR1(PL) = 1 Or InpUU1(PL) = 1 Or InpDD1(PL) = 1 Or InpAA1(PL) = 1 Or InpBB1(PL) = 1) Then BWC1(PL) = 15: InpLLL1(PL) = InpLLL1(PL) + 3 + (InpLLL1(PL) > 7) * (InpLLL1(PL) - 7): InpRRR1(PL) = InpRRR1(PL) + 3 + (InpRRR1(PL) > 7) * (InpRRR1(PL) - 7)
End If
If BWC1(PL) >= 20 + (TrM > 0) * 5 Then BA1(PL) = 2: BAA1(PL) = 2
End If
If BAA1(PL) = 2 Then '初期設定
Ch1(PL) = 0: Chh1(PL) = 0: BSC1(PL) = 0: AC1(PL) = 0
WCa1(PL) = False: DCa1(PL) = False
If Lv1(PL) >= 50 Then Inc1(PL) = 6000 Else Inc1(PL) = LS(Lv1(PL)): If Inc1(PL) <= 0 Then Inc1(PL) = 1
ASC1(PL) = 30 + (Lv1(PL) >= 50 And Lv1(PL) < 200) * Int((Lv1(PL) - 50) / 10) + (Lv1(PL) >= 200 And Lv1(PL) < 250) * Int((Lv1(PL) - 200) / 5) + (Lv1(PL) >= 250 And Lv1(PL) < 300) * (10 + Int((Lv1(PL) - 250) / 10)) + (Lv1(PL) >= 300 And Lv1(PL) < 500) * (15 + Int((Lv1(PL) - 300) / 50)) + (Lv1(PL) >= 500 And Lv1(PL) < 1000) * 19 + (Lv1(PL) >= 1000) * 20
For I = 0 To 24: Nx1(PL, I) = Nx1(PL, I + 1): Next I
NxC1(PL) = NxC1(PL) + 1 + (NxC1(PL) >= 776) * 777: Nx1(PL, 25) = BtNx(NxC1(PL))
BA1(PL) = 3: BAA1(PL) = 3
End If
If BAA1(PL) = 3 Then 'アクティブ
RushC1(PL) = -Rush1(PL) * 19
For J = 0 To -Rush1(PL) * 19
BMove
If BM1(PL) <> BMM1(PL) Then Sou(1).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500
If BX1(PL) <> BMX1(PL) Then Sou(5).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500
Ch1(PL) = 0 'ミス判定
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BYF1(PL) = BY1(PL) '下移動
BSC1(PL) = BSC1(PL) + Inc1(PL)
If BSC1(PL) <= 240 And InpDD1(PL) = 1 Then BSC1(PL) = 240
If BSC1(PL) <= 6000 And InpUU1(PL) = 1 And TrM > 0 Then BSC1(PL) = 6000
While BSC1(PL) >= 240
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1) + 1) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BY1(PL) = BY1(PL) + 1: BSC1(PL) = BSC1(PL) - 240: AC1(PL) = 0
Else
BSC1(PL) = 239
End If
Wend
If BY1(PL) > BYF1(PL) Then BYF1(PL) = BYF1(PL) + 1
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1) + 1) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then AC1(PL) = 0 Else AC1(PL) = AC1(PL) + 1: Sou(3).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500: Sou(3).On = Sou(3).On Or (AC1(PL) = 1): If (InpDD1(PL) = 1 Or (TrM > 0 And InpUU1(PL) = 1)) And AC1(PL) >= -(InpDD1(PL) = 1) * (6 - Int(Lv1(PL) / 5)) * (1 - Rush1(PL) * 19) And AC1(PL) <= 30 * (1 - Rush1(PL) * 19) Then AC1(PL) = 30 * (1 - Rush1(PL) * 19): WCa1(PL) = (InpUU1(PL) = 1): DCa1(PL) = (InpUU1(PL) = 1 Or InpDD1(PL) = 1)
InpUU1(PL) = InpUU1(PL) - (AC1(PL) >= 30 * (1 - Rush1(PL) * 19) And InpUU1(PL) = 1): InpDD1(PL) = InpDD1(PL) - (AC1(PL) >= 30 * (1 - Rush1(PL) * 19) And InpDD1(PL) = 1)
If BSC1(PL) >= 239 And AC1(PL) >= ASC1(PL) * (1 - Rush1(PL) * 19) Then RushC1(PL) = J: BA1(PL) = 4: Sou(2).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500: Sou(2).Fre = 22050 / (((BY1(PL) - 1) * 0.055) + 1): Sou(2).On = True
Else
RushC1(PL) = J
For I = 0 To 3
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = BT1(PL) + 1
With Src
.Left = BT1(PL) * 16: .Top = 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
BFSf(PL).BltFast (BX1(PL) + B(BT1(PL), BM1(PL), I, 0)) * 16 - 48, (BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) * 16 - 16, BCSf, Src, DDBLTFAST_WAIT
Next I
BWC1(PL) = 0: BA1(PL) = 9
End If
RBX1(PL, J) = BX1(PL): RBY1(PL, J) = BY1(PL): RBM1(PL, J) = BM1(PL): RAC1(PL, J) = AC1(PL): RBSC1(PL, J) = BSC1(PL): RInc1(PL, J) = Inc1(PL): RBYF1(PL, J) = BYF1(PL)
If RushC1(PL) < -Rush1(PL) * 19 Then J = -Rush1(PL) * 19
Next J
End If
If BAA1(PL) = 4 Then 'セット
RushC1(PL) = 0: J = RushC1(PL)
RBX1(PL, J) = BX1(PL): RBY1(PL, J) = BY1(PL): RBM1(PL, J) = BM1(PL): RAC1(PL, J) = AC1(PL): RBSC1(PL, J) = BSC1(PL): RInc1(PL, J) = Inc1(PL): RBYF1(PL, J) = BYF1(PL)
For I = 0 To 3
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = BT1(PL) + 1
With Src
.Left = BT1(PL) * 16: .Top = 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
BFSf(PL).BltFast (BX1(PL) + B(BT1(PL), BM1(PL), I, 0)) * 16 - 48, (BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) * 16 - 16, BCSf, Src, DDBLTFAST_WAIT
Next I
BA1(PL) = 5
End If
If BAA1(PL) = 5 Then '判定
Chh1(PL) = 0
For I = 0 To 3
Ch1(PL) = 0
For U = 3 To 12
If BY1(PL) + I > 0 And BY1(PL) + I < 23 And BF1(PL, U, BY1(PL) + I) > 0 Then Ch1(PL) = Ch1(PL) + 1
Next U
If Ch1(PL) >= 10 Then FE1(PL, Chh1(PL)) = BY1(PL) + I: Chh1(PL) = Chh1(PL) + 1
Next I
If Chh1(PL) > 0 Then DCa1(PL) = DCa1(PL) Or (TrM < 2): BA1(PL) = 6: BAA1(PL) = 6 Else CB1(PL) = 0: BWC1(PL) = 0: Dmg1(1 - PL) = Dmg1(1 - PL) + Hit1(PL): Hit1(PL) = 0: BA1(PL) = 1
End If
If BAA1(PL) = 6 Then 'ブロック消去
Sou(6 - (Chh1(PL) >= 4)).Pan = PL * 1000 - 500: Sou(6 - (Chh1(PL) >= 4)).Fre = 22050 * 2 ^ ((CB1(PL) + (CB1(PL) > 9) * (CB1(PL) - 9)) / 12): Sou(6 - (Chh1(PL) >= 4)).On = True
Hitt = Chh1(PL) - 1 - (Chh1(PL) = 4) - (CB1(PL) > 0) * (1 - (Chh1(PL) = 4))
If Chh1(PL) < 4 And Dmg1(PL) > 0 Then Dmg1(PL) = Dmg1(PL) - Hitt: Hitt = 0: If Dmg1(PL) < 0 Then Hitt = Hitt - Dmg1(PL): Dmg1(PL) = 0
Hit1(PL) = Hit1(PL) + Hitt
R = Rnd * 360
For I = 0 To Chh1(PL) - 1
If BAni Then
For U = 0 To 9
With EA(EAC)
.V = 1 - (Hit1(PL) >= 4): .A = BF1(PL, 3 + U, FE1(PL, I)) - 1: .AF = Int(Rnd * -(Hit1(PL) >= 4) * 4)
.X = 112 + PL * 256 + U * 16: .Y = 48 + FE1(PL, I) * 16
.XX = Sin((R + I * 90 + U * 108) * Rg) * (4 + Chh1(PL) * 3): .YY = Cos((R + I * 90 + U * 108) * Rg) * (4 + Chh1(PL) * 3)
If TrM > 0 Then .XXX = .YY / 50 * (((I + U) Mod 2) * (2 * (TrM = 2)) + 1) * ((PL Mod 2) * 2 - 1): .YYY = -.XX / 50 * (((I + U) Mod 2) * (2 * (TrM = 2)) + 1) * ((PL Mod 2) * 2 - 1) Else .XXX = 0: .YYY = 0.2
End With
EAC = EAC + 1 + (EAC >= 159) * 160
Next U
End If
With Des
.Left = 0: .Top = FE1(PL, I) * 16 - 16: .Right = .Left + 160: .Bottom = .Top + 16
End With
BFSf(PL).BltColorFill Des, 0
Next I
BWC1(PL) = -(TrM = 0) * Int(Lv1(PL) / 5) * 3 - (TrM = 1) * (20 + Int(Lv1(PL) / 5)) - (TrM = 2) * 35
CB1(PL) = CB1(PL) + 1 + (CB1(PL) >= 100)
Li1(PL) = Li1(PL) + Chh1(PL): If Not OfBt And Lv1(PL) < 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 Then If Li1(PL) >= NL1(PL) Then Sou(4).Pan = PL * 1000 - 500: Sou(4).On = True: Lv1(PL) = Lv1(PL) + 1: NF1(PL) = 65: NL1(PL) = (NL1(PL) + 8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50))
BA1(PL) = 7: BAA1(PL) = 7
End If
If BAA1(PL) = 7 Then '時間待ち
If BWC1(PL) >= 35 - Chh1(PL) Then BWC1(PL) = 0: BA1(PL) = 8: BAA1(PL) = 8 Else BWC1(PL) = BWC1(PL) + 1
End If
If BAA1(PL) = 8 Then 'フィールドずらし
Do
I = BWC1(PL)
For U = FE1(PL, I) To 2 Step -1
For Y = 3 To 12
BF1(PL, Y, U) = BF1(PL, Y, U - 1)
Next Y, U
For U = 3 To 12
BF1(PL, U, 1) = 0
Next U
With Src
.Left = 0: .Top = 0: .Right = .Left + 160: .Bottom = .Top + (FE1(PL, I)) * 16 - 16
End With
BFSf(PL).BltFast 0, 16, BFSf(PL), Src, DDBLTFAST_WAIT
With Des
.Left = 0: .Top = 0: .Right = .Left + 160: .Bottom = .Top + 16
End With
BFSf(PL).BltColorFill Des, 0
BWC1(PL) = BWC1(PL) + 1
Loop While TrM = 2 And BWC1(PL) < Chh1(PL)
If BWC1(PL) = Chh1(PL) Then Sou(13).Pan = PL * 1000 - 500: Sou(13).On = True: BWC1(PL) = 0: BA1(PL) = 1
End If

If BAA1(PL) = 9 Then 'ミス
If BWC1(PL) = 0 Then
If VSLiv Then Liv1(PL) = Liv1(PL) + 1
If Liv1(PL) > 1 Then Sou(10).Pan = PL * 1000 - 500: Sou(10).On = True Else VSR = VSR + (VSR1(PL) = 0): Sou(9).Pan = PL * 1000 - 500: Sou(9).On = True
Liv1(PL) = Liv1(PL) - 1: If Liv1(PL) < 0 Then Liv1(PL) = 0
Li1(PL) = 0
If Not OfBt Then Lv1(PL) = VSLv(TrM, SLv1(PL))
NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50))
CB1(PL) = 0: Dmg1(PL) = 0
End If
If BWC1(PL) < 60 Then
If BWC1(PL) >= 16 And BWC1(PL) < 38 Then
For I = 3 To 12: BF1(PL, I, BWC1(PL) - 15) = 0: Next I
With Des
.Left = 0: .Top = (BWC1(PL) - 16) * 16: .Right = .Left + 160: .Bottom = .Top + 16
End With
BFSf(PL).BltColorFill Des, 0
End If
BWC1(PL) = BWC1(PL) + 1
Else
Dmg1(1 - PL) = Dmg1(1 - PL) + Hit1(PL): Hit1(PL) = 0
If Tit Or Liv1(PL) > 0 Then WCa1(PL) = True: DCa1(PL) = True: BWC1(PL) = 0: BA1(PL) = 1 Else BWC1(PL) = 0: BA1(PL) = 10: BAA1(PL) = 10
End If
End If

If BAA1(PL) = 10 Then 'ゲームオーバー
End If

If Li1(PL) > 10000 Then Li1(PL) = 10000
If Hit1(PL) > 10000 Then Hit1(PL) = 10000
If Dmg1(PL) > 10000 Then Dmg1(PL) = 10000

Next PL
For PL = 0 To 1
If VSR1(PL) = 0 And Liv1(PL) = 0 Then VSR1(PL) = VSR
Next PL
For PL = 0 To 1
If VSR1(PL) = 0 And VSR = 2 Then VSR1(PL) = 1: Sou(8).Fre = 22050: Sou(8).On = True
Next PL
If VSR <= 2 Then '対戦決着
If VSFinC = 1 Then Mut = True
If VSFinC = 199 Then GFin = 1
VSFinC = VSFinC + 1: If VSFinC > 200 Then VSFinC = 200
End If
End If
End If

If Not Tit Or TC >= 430 Then

'背景表示
If OfBt Then
If SLv1(0) > SLv1(1) Then LvL = SLv1(0) Else LvL = SLv1(1)
Else
If VSLv(TrM, SLv1(0)) > VSLv(TrM, SLv1(1)) Then LvL = VSLv(TrM, SLv1(0)) Else LvL = VSLv(TrM, SLv1(1))
End If
BGD TrM, LvL
'ボード表示
For PL = 0 To 1
With Src
.Left = 0: .Top = 0: .Right = .Left + 224: .Bottom = .Top + 368
End With
If Not FS Then BBSf.BltFast 80 + PL * 256, 64, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
If Rush1(PL) Then
With Src
.Left = 32 + RushAniC: .Top = 0: .Right = 192: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 112 + PL * 256, 64, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
With Src
.Left = 32: .Top = 0: .Right = 32 + RushAniC: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 272 + PL * 256 - RushAniC, 64, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next PL
'ダメージ表示
DAni = DAni + 1 + (DAni >= 5) * 6
For PL = 0 To 1
If Liv1(PL) > 0 Then
For I = 1 To Dmg1(PL) + (Dmg1(PL) > 22) * (Dmg1(PL) - 22)
With Src
.Left = 224: .Top = ((44 + DAni - I * 2) Mod 6) * 16: .Right = .Left + 32: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 272 + PL * 64, 416 - I * 16, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
For I = Dmg1(PL) + 1 To Dmg1(PL) + Hit1(1 - PL) + (Dmg1(PL) + Hit1(1 - PL) > 22) * (Dmg1(PL) + Hit1(1 - PL) - 22)
With Src
.Left = 224: .Top = 96 + ((44 + DAni - I * 2) Mod 6) * 16: .Right = .Left + 32: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 272 + PL * 64, 416 - I * 16, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
End If
Next PL
'ライフ表示
For PL = 0 To 1
For I = 1 To Liv1(PL) + (Liv1(PL) > 14) * (Liv1(PL) - 14)
With Src
.Left = 256: .Top = ((10 + LAni * (1 - (I Mod 2) * 2)) Mod 10) * 32: .Right = .Left + 32: .Bottom = .Top + 32
End With
If Not FS Then BBSf.BltFast 80 + PL * 448, 408 - I * 24, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
Next PL
'フィールド枠線表示
For PL = 0 To 1
If ((OfBt Or VSR <= 2) And InpS1(PL)) Or (Not OfBt And BA1(PL) <> 9 And Pau > 0) Or VSR1(PL) = 1 Then
If BA1(PL) > 2 And BA1(PL) < 6 Then
For I = 0 To 3
FL(I) = BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1))
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = BT1(PL) + 1
Next I
End If
For I = 0 To 21
For U = 0 To 8
If BF1(PL, 3 + U, I + 1) = 0 Xor BF1(PL, 4 + U, I + 1) = 0 Then
With Src
 .Left = 160: .Top = 372: .Right = .Left + 2: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 127 + PL * 256 + U * 16, 64 + I * 16, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next U, I
For I = 0 To 20
For U = 0 To 9
If BF1(PL, 3 + U, I + 1) = 0 Xor BF1(PL, 3 + U, I + 2) = 0 Then
With Src
 .Left = 160: .Top = 368: .Right = .Left + 16: .Bottom = .Top + 2
End With
If Not FS Then BBSf.BltFast 112 + PL * 256 + U * 16, 79 + I * 16, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next U, I
If BA1(PL) > 2 And BA1(PL) < 6 Then
For I = 0 To 3
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = FL(I)
Next I
End If
End If
Next PL
If Pau = 0 Then
'フィールド表示
For PL = 0 To 1
If Not ((OfBt Or VSR <= 2) And InpS1(PL) Or VSR1(PL) = 1) Then
With Src
.Left = 0: .Top = 0: .Right = .Left + 160: .Bottom = .Top + 352
End With
If Not FS Then BBSf.BltFast 112 + PL * 256, 64, BFSf(PL), Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
If (Not BAni) And BA1(PL) = 7 Then
For I = 0 To Chh1(PL) - 1
For U = 0 To 9
With Src
If ((80 + BWC1(PL) + Chh1(PL) - (Chh1(PL) = 4) * ((PL Mod 2) * 2 - 1) * (U + I * 2)) Mod 8) < 4 Then .Left = 112: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16 Else .Left = (BF1(PL, U + 3, FE1(PL, I)) - 1) * 16: .Top = -(BF1(PL, U + 3, FE1(PL, I)) = 8) * 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 112 + PL * 256 + U * 16, 48 + FE1(PL, I) * 16, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next U
Next I
End If
End If
Next PL
'アクティブブロック残像表示
For PL = 0 To 1
If Not ((OfBt Or VSR <= 2) And InpS1(PL) Or VSR1(PL) = 1) Then
If Zanzo Then
'If FS Then
'If (Not (BSm And Not Inc1(PL) >= 240 And LAC1(PL) = 0) Or (16 + Int(LBSC1(PL) / 15)) = (16 + Int(BSC1(PL) / 15))) And LBX1(PL) = BX1(PL) And LBY1(PL) = BY1(PL) And LBYF1(PL) = BYF1(PL) And LBM1(PL) = BM1(PL) Then CLB1(PL) = True Else CLB1(PL) = False
'Else
'If LBA1(PL) > 2 And LBA1(PL) < 6 And Not CLB1(PL) Then
'For U = LBYF1(PL) To LBY1(PL)
'For I = 0 To 3
'If LBA1(PL) < 4 Or LBA1(PL) > 5 Or U < LBY1(PL) Then
'With Src
'.Left = LBT1(PL) * 16: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
'End With
'Else
'With Src
'.Left = 112: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
'End With
'End If
'If Not FS Then BBSf.BltFast 64 + PL * 256 + (LBX1(PL) + B(LBT1(PL), LBM1(PL), I, 0)) * 16, 48 + (U + B(LBT1(PL), LBM1(PL), I, 1)) * 16 - (BSm And Not (Zanzo And Inc1(PL) >= 240) And LAC1(PL) = 0) * Int(LBSC1(PL) / 15), BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
'Next I, U
'CLB1(PL) = True
'End If
'End If
'LBX1(PL) = BX1(PL): LBY1(PL) = BY1(PL): LBYF1(PL) = BYF1(PL): LBSC1(PL) = BSC1(PL): LAC1(PL) = AC1(PL): LBT1(PL) = BT1(PL): LBM1(PL) = BM1(PL): LBA1(PL) = BA1(PL)
If FS Then
For J = 0 To -Rush1(PL) * 19
If (Not (BSm And Not RInc1(PL, J) >= 240 And LAC1(PL, J) = 0) Or (16 + Int(LBSC1(PL, J) / 15)) = (16 + Int(RBSC1(PL, J) / 15))) And LBX1(PL, J) = RBX1(PL, J) And LBY1(PL, J) = RBY1(PL, J) And LBYF1(PL, J) = RBYF1(PL, J) And LBM1(PL, J) = RBM1(PL, J) Then CLB1(PL, J) = True Else CLB1(PL, J) = False
Next J
Else
For J = -(Not Zanzo Or BA1(PL) = 5) * LRushC1(PL) To LRushC1(PL)
If LBA1(PL) > 2 And LBA1(PL) < 6 And Not CLB1(PL, J) Then
For U = LBYF1(PL, J) To LBY1(PL, J)
For I = 0 To 3
If LBA1(PL) < 4 Or LBA1(PL) > 5 Or U < LBY1(PL, J) Or J < LRushC1(PL) Then
With Src
.Left = LBT1(PL) * 16: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
End With
Else
With Src
.Left = 112: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
End With
End If
If Not FS Then BBSf.BltFast 64 + PL * 256 + (LBX1(PL, J) + B(LBT1(PL), LBM1(PL, J), I, 0)) * 16, 48 + (U + B(LBT1(PL), LBM1(PL, J), I, 1)) * 16 - (BSm And Not FE And Not (Zanzo And LInc1(PL, J) >= 240) And LAC1(PL, J) = 0) * Int(LBSC1(PL, J) / 15), BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I, U
End If
CLB1(PL, J) = True
Next J
End If
LRushC1(PL) = RushC1(PL): LBA1(PL) = BA1(PL): LBT1(PL) = BT1(PL)
For J = 0 To -Rush1(PL) * 19
LBX1(PL, J) = RBX1(PL, J): LBY1(PL, J) = RBY1(PL, J): LBYF1(PL, J) = RBYF1(PL, J): LBSC1(PL, J) = RBSC1(PL, J): LAC1(PL, J) = RAC1(PL, J): LBM1(PL, J) = RBM1(PL, J): LInc1(PL, J) = RInc1(PL, J)
Next J
End If
End If
Next PL
'アクティブブロック表示
For PL = 0 To 1
If Not ((OfBt Or VSR <= 2) And InpS1(PL) Or VSR1(PL) = 1) Then
If BA1(PL) > 2 And BA1(PL) < 6 Then
For J = -(Not Zanzo Or BA1(PL) = 5) * RushC1(PL) To RushC1(PL)
If Not Zanzo Then RBYF1(PL, J) = RBY1(PL, J)
For U = RBYF1(PL, J) To RBY1(PL, J)
For I = 0 To 3
If BA1(PL) < 4 Or BA1(PL) > 5 Or U < RBY1(PL, J) Or J < RushC1(PL) Then
With Src
.Left = BT1(PL) * 16: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
End With
Else
With Src
.Left = 112: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
End With
End If
If Not FS Then BBSf.BltFast 64 + PL * 256 + (RBX1(PL, J) + B(BT1(PL), RBM1(PL, J), I, 0)) * 16, 48 + (U + B(BT1(PL), RBM1(PL, J), I, 1)) * 16 - (BSm And Not (Zanzo And RInc1(PL, J) >= 240) And RAC1(PL, J) = 0) * Int(RBSC1(PL, J) / 15), BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I, U
Next J
End If
End If
BYF1(PL) = BY1(PL)
RushC1(PL) = 0: J = RushC1(PL)
RBX1(PL, J) = BX1(PL): RBY1(PL, J) = BY1(PL): RBM1(PL, J) = BM1(PL): RAC1(PL, J) = AC1(PL): RBSC1(PL, J) = BSC1(PL): RInc1(PL, J) = Inc1(PL): RBYF1(PL, J) = BYF1(PL)
Next PL
'順位2表示
For PL = 0 To 1
If BA1(PL) = 10 Or BA1(PL) = 9 And BWC1(PL) >= 30 And VSR1(PL) = 2 Then StringD 120 + PL * 256, 224, 7, "GAME OVER", False
Next PL
'ミス表示
For PL = 0 To 1
If BA1(PL) = 9 Then
For I = 0 To 21
RR1 = BWC1(PL) - I
If RR1 < 0 Then RR1 = 0
If RR1 > 16 Then RR1 = 16
RR2 = BWC1(PL) - I - 22
If RR2 < 0 Then RR2 = 0
If RR2 > 16 Then RR2 = 16
RR = Int(Rnd * 160)
With Src
.Left = RR: .Top = 368 + ((I + Int(CCC)) Mod 7) * 16 + RR2: .Right = 160: .Bottom = .Top + RR1 - RR2
End With
If Not FS Then BBSf.BltFast 112 + PL * 256, 64 + I * 16 + RR2, BDSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 368 + ((I + Int(CCC)) Mod 7) * 16 + RR2: .Right = .Left + RR: .Bottom = .Top + RR1 - RR2
End With
If Not FS Then BBSf.BltFast 272 + PL * 256 - RR, 64 + I * 16 + RR2, BDSf, Src, DDBLTFAST_WAIT
Next I
End If
Next PL
'勝者表示
For PL = 0 To 1
If VSR1(PL) = 1 Then StringD 128 + PL * 256, 224, 11, "WINNER!!", False
Next PL
End If
'消去アニメ表示
EAni
'NEXT表示
For PL = 0 To 1
If MNx < 4 Or BA1(PL) > 0 Then
For I = 0 To MNx - 1 - (MNx = 4) - (MNx = 0 And BA1(PL) <> 3)
With Src
.Left = Int(Nx1(PL, I) / 4) * 64: .Top = 32 + (Nx1(PL, I) Mod 4) * 32: .Right = .Left + 64: .Bottom = .Top + 32
End With
If Not FS Then BBSf.BltFast 160 + PL * 256 + I * 80 * (PL * 2 - 1), 16, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
If MNx = 4 Then
For I = 0 To 8
With Src
.Left = Int(Nx1(PL, 3 + I) / 4) * 64: .Top = 32 + (Nx1(PL, 3 + I) Mod 4) * 32: .Right = .Left + 64: .Bottom = .Top + 32
End With
If Not FS Then BBSf.BltFast PL * 576, 64 + I * 48, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
End If
Else
NxD2
End If
StringD 224 + PL * 120, 16, 9, "NEXT", False
Next PL
'デバイス表示
If Not Tit Then
For PL = 0 To 1
Select Case PT(PL)
Case 0
StringD 128 + PL * 256, 64, 8, "KEYBOARD", False
Case 1 To 4
StringD 140 + PL * 256, 64, 8, "PAD", False
StringD 196 + PL * 256, 64, 8, "( )", False
StringD 212 + PL * 256, 64, 8, Chr$(64 + PT(PL)), False
Case 5
StringD 112 + PL * 256, 64, 8, "CP GRADE", False
TextD 272 + PL * 256, 64, 0, CPG1(PL), True, 2
End Select
Next PL
End If
'レベル表示
For PL = 0 To 1
NF1(PL) = NF1(PL) - 1 - (NF1(PL) <= 0)
StringD 224 + PL * 256, 432, 9, "LEVEL", False
TextD 304 + PL * 256, 448, 3 - (NF1(PL) > 0 Or (Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150)) * 8, Lv1(PL), True, 4
Next PL
'ライン表示
If Not OfBt Then
For PL = 0 To 1
StringD 80 + PL * 256, 432, 9, "LINES", False
TextD 160 + PL * 256, 448, 3 - (Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150) * 8, Li1(PL), True, 5
If Lv1(PL) < 30 - (TrM >= 1) * 20 Then
StringD 160 + PL * 256, 448, 4, "/", False
TextD 176 + PL * 256, 448, 3 - (Lv1(PL) >= 29 - (TrM >= 1) * 20 - (TrM = 2) * 150) * 8, NL1(PL), False, 3
End If
Next PL
End If
'ライフ(>14)表示
For PL = 0 To 1
If Liv1(PL) > 14 Then TextD 112 + PL * 448, 400, 4 + Int(LAni / 5), Liv1(PL), True, 2
Next PL
If Pau = -1 Then '設定表示
For PL = 0 To 1
If PT(PL) >= 0 Then
StringD 140 + PL * 256, 160, 8 - (CPos1(PL) = 0) * 3, "START", False
If PT(PL) = 5 Then
StringD 140 + PL * 256, 192, 9, "GRADE", False
TextD 268 + PL * 256, 192, 7 - (CPos1(PL) = 1) * 4, CPG1(PL), True, 3
End If
StringD 140 + PL * 256, 192 - (PT(PL) = 5) * 32, 9, "LEVEL", False
TextD 268 + PL * 256, 192 - (PT(PL) = 5) * 32, 7 - (CPos1(PL) = 1 - (PT(PL) = 5)) * 4, Lv1(PL), True, 3
StringD 140 + PL * 256, 224 - (PT(PL) = 5) * 32, 9, "LIVES", False
TextD 268 + PL * 256, 224 - (PT(PL) = 5) * 32, 7 - (CPos1(PL) = 2 - (PT(PL) = 5)) * 4, Liv1(PL), True, 3
StringD 140 + PL * 256, 256 - (PT(PL) = 5) * 32, 8 - (CPos1(PL) = 3 - (PT(PL) = 5)) * 3, "CANCEL", False
If CPos1(PL) >= 0 Then CursorD 118 + PL * 256, 160 + CPos1(PL) * 32, 11, -(CPos1(PL) = 0 Or CPos1(PL) = 3 - (PT(PL) = 5))
Else
If VSA = 0 Then
If CC < 30 Then StringD 152 + PL * 256, 224, 8, "ENTRY", False, 10
Else
If CC < 30 Then StringD 160 + PL * 256, 224, 8, "WAIT", False, 10
End If
End If
Next PL
End If
If Pau > 0 Then 'ポーズ表示
For PL = 0 To 1
If PL = Pau - 1 Then
StringD 140 + PL * 256, 224, 8 - (CPos = 0) * 3, "CONTINUE", False
StringD 140 + PL * 256, 256, 8 - (CPos = 1) * 3, "EXIT", False
CursorD 118 + PL * 256, 224 + CPos * 32, 11, 1
Else
If CC < 30 Then StringD 160 + PL * 256, 224, 9, "WAIT", False
End If
Next PL
End If
'スタート表示
If Pau = 0 Then
For PL = 0 To 1
If BA1(PL) = 0 Then
If BWC1(PL) > 12 Then
RR = BWC1(PL) - 12
TextD 184 + PL * 256, 224, 11, 3 - Int(RR / 60), False
For I = 0 To 61 - (RR Mod 60)
If RR < 60 Then CursorD 184 + PL * 256 - Sin(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60)), 224 + Cos(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60) * 0.75), -(RR >= 60) - (RR >= 120) * 7, 0
If RR >= 60 And RR < 120 Then CursorD 184 + PL * 256 - Sin(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 50) * (1 + Int(RR / 60) * 0.125), 224 + Cos(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60) * 0.75), -(RR >= 60) - (RR >= 120) * 7, 0
If RR >= 120 Then CursorD 184 + PL * 256 - Sin(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60)), 224 + Cos(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 50) * (1 + Int(RR / 60) * 0.125), -(RR >= 60) - (RR >= 120) * 7, 0
Next I
End If
If ScLiv(2) > 0 And PT(PL) < 5 Then
StringD 124 + PL * 256, 96, 9, "RUSH:", False, 8
If Rush1(PL) Then StringD 212 + PL * 256, 96, 11, "ON", False Else StringD 212 + PL * 256, 96, 11, "OFF", False
End If
End If
Next PL
End If

Else 'オープニング消去アニメ表示
EAni

End If

If Tit Then 'オープニング
If TC = 400 Then For I = 0 To 191: OpB(I).X = 15 - OpB(I).X: OpB(I).Y = 11 - OpB(I).Y: Next I 'パーツ入れ替え
If TC < 430 Then 'オープニングアニメ1
If TC < 300 Then If Not FS Then BBSf.BltColorFill RC0, 0
With Src
.Left = 0: .Top = 192: .Right = .Left + 40: .Bottom = .Top + 40
End With
For I = 0 To 191
RR1 = Int(I / 4): RR2 = Int(TC / 4)
If RR2 > RR1 + 12 Then
If Not FS Then BBSf.BltFast OpB(I).X * 40, OpB(I).Y * 40, SpSf, Src, DDBLTFAST_WAIT
Else
If RR2 > RR1 Then
BltClip OpB(I).X * 40 + Sin(OpBPos(RR1) * Rg) * ((RR1 + 12) * 4 + 4 - TC) ^ 4 / 10000, OpB(I).Y * 40 + Cos(OpBPos(RR1) * Rg) * ((RR1 + 12) * 4 + 4 - TC) ^ 4 / 10000, SpSf, Src, DDBLTFAST_WAIT
End If
End If
Next I
End If
If TC >= 430 And TC < 580 Then 'オープニングアニメ2
With Src
.Left = 0: .Top = 192: .Right = .Left + 40: .Bottom = .Top + 40
End With
For I = 191 To 0 Step -1
RR1 = Int(I / 4): RR2 = (TC - 430)
If RR2 < RR1 Then
If Not FS Then BBSf.BltFast OpB(I).X * 40, OpB(I).Y * 40, SpSf, Src, DDBLTFAST_WAIT
Else
If RR2 < RR1 + 60 Then
BltClip OpB(I).X * 40 + Sin(OpBPos(RR1) * Rg) * (-RR1 - 1 + (TC - 430)) ^ 2 / 4, OpB(I).Y * 40 + Cos(OpBPos(RR1) * Rg) * (-RR1 - 1 + (TC - 430)) ^ 2 / 4, SpSf, Src, DDBLTFAST_WAIT
End If
End If
Next I
End If
End If

FChange

Loop
For I = 1 To 0 Step -1: Set BFSf(I) = Nothing: Next I
Set BCSf = Nothing
Set BDSf = Nothing
Exit Sub

CE:
'Beep
GMode = 2
End Sub

Sub VS4P() '4P対戦
On Error GoTo CE
GEnd = False
FS = False
Pau = Not Tit
Fade = True: ScrFC = 0
KeyInitAll

For I = 0 To 3: CPG1(I) = RCPG1(I): Next I
For PL = 0 To 3: SLv1(PL) = 0: Next PL
If Tit Then PT(0) = 5: PT(1) = 5: PT(2) = 5: PT(3) = 5: CPG1(0) = 4: CPG1(1) = 6: CPG1(2) = 5: CPG1(3) = 7: For PL = 0 To 3: SLv1(PL) = 1: Next PL

'背景パレット初期化
If Not WM Then
For I = 0 To 127
With MPa(I)
.red = 0: .green = 0: .blue = 0
End With
Next I
If Not FS Then MPal.SetEntries 0, 256, MPa
End If

'ボード（オフスクリーン）の作成
BDCr BD

'ブロック（オフスクリーン）の作成
BCCr BC

BRndInit
For I = 0 To 776: BtNx(I) = BRnd: Next I
BtNx(776) = (BtNx(776) - (BtNx(776) = BtNx(0))) Mod 7
For I = 0 To 9: DBS(I) = I: Next I
For I = 0 To 8: RR = Int(Rnd * (10 - I)): RR1 = DBS(I): DBS(I) = DBS(RR): DBS(RR) = RR1: Next I
DAni = 0
GFin = 0: VSFinC = 0
VSA = 0: VSR = 5
For PL = 0 To 3
SD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
SD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
SD.lWidth = 120: SD.lHeight = 264
Set BFSf(PL) = DD.CreateSurface(SD)
CK.low = 0: CK.high = 0
BFSf(PL).SetColorKey DDCKEY_SRCBLT, CK
If Not WM Then BFSf(PL).SetPalette MPal
BFSf(PL).BltColorFill RC0, 0

Rush1(PL) = False
Liv1(PL) = 3: Sc1(PL) = 0: SB1(PL) = 0: CB1(PL) = 0: Li1(PL) = 0: Lv1(PL) = VSLv(TrM, SLv1(PL)): NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50))
NF1(PL) = 0: BA1(PL) = -Tit: BWC1(PL) = 0: WCa1(PL) = False: DCa1(PL) = False
LBA1(PL) = 0
Hit1(PL) = 0: Dmg1(PL) = 0
VSR1(PL) = 0
CPos1(PL) = -1 - (PT(PL) < 5) * 2
InpL1(PL) = False: InpR1(PL) = False: InpLL1(PL) = 0: InpRR1(PL) = 0: InpLLL1(PL) = 0: InpRRR1(PL) = 0
InpU1(PL) = False: InpD1(PL) = False: InpUU1(PL) = 0: InpDD1(PL) = 0
InpA1(PL) = False: InpB1(PL) = False: InpS1(PL) = False: InpAA1(PL) = 0: InpBB1(PL) = 0: InpSS1(PL) = 1
For I = 0 To 25: For U = 0 To 15: BF1(PL, U, I) = 0: Next U, I
For I = 0 To 22: BF1(PL, 2, I) = 9: BF1(PL, 13, I) = 9: Next I
For I = 3 To 12: BF1(PL, I, 0) = 9: BF1(PL, I, 23) = 9: Next I
For I = 0 To 25: Nx1(PL, I) = BtNx(I): Next I: NxC1(PL) = 25
DBSC1(PL) = 0
Next PL

BGInit

LvL = 0
For PL = 0 To 3
If VSLv(TrM, SLv1(PL)) > LvL Then LvL = VSLv(TrM, SLv1(PL))
Next PL

St = Int((LvL + (LvL > 50) * (LvL - 50)) / 5)
If LvL >= 50 And LvL < 200 Then St = 10
If LvL >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 Then St = 11 - (Not (Fnl Or TA)) * 2
Stt = St
If BG >= 1 Then BGCr Stt ' And Stt < 12
FOC = 0: FIC = 0: FF = False
For I = 0 To 15: Sou(I).Fre = 22050: Sou(I).Pan = 0: Sou(I).On = False: Next I
EAC = 0: For I = 0 To 159: EA(I).V = False: Next I

If Not Tit Then
If TrM = 0 Then PlayMusic "BATTLE0", 3072, 76800
If TrM = 1 Then PlayMusic "BATTLE1", 16128, 114432
If TrM = 2 Then PlayMusic "BATTLE2", 4608, 78336
End If

'----メインループ----
Do..<GEnd

If GFin >= 1 Then
KeyScanAll
If Fade Then Fade = False: ScrFC = 0
If GFin = 2 And (CsrAA = 1 Or CsrBB = 1) Then ScrFC = 80
If ScrFC = 80 Then PlayMusic vbNullString$: GMode = 5: GEnd = True
End If
If GFin < 2 Then

If Pau <> 0 Then 'ポーズ
If Pau = -1 Then
KeyScanAll

If VSA = 0 Then '選択モード
For PL = 0 To 3
If PT(PL) = -1 Then
For I = 0 To 4
RR = False
For U = 0 To 3
If PT(U) = I Then RR = True
Next U
If PT(PL) = -1 And Not RR And (CsrAA1(I) = 1 Or CsrSS1(I)) Then PT(PL) = I: CPos1(PL) = 1: CsrAA1(PT(PL)) = CsrAA1(PT(PL)) - (CsrAA1(PT(PL)) = 1): CsrSS1(PT(PL)) = CsrSS1(PT(PL)) - (CsrSS1(PT(PL)) = 1): Sou(0).Pan = PL * 400 - 600: Sou(0).On = True
Next I
End If
Next PL
For PL = 0 To 3
If PT(PL) >= 0 Then
If CPos1(PL) >= 0 And CsrUU1(PT(PL)) = 1 Then CPos1(PL) = CPos1(PL) - 1 - (CPos1(PL) <= 0) * 4: Sou(5).Pan = PL * 400 - 600: Sou(5).On = True
If CPos1(PL) >= 0 And CsrDD1(PT(PL)) = 1 Then CPos1(PL) = CPos1(PL) + 1 + (CPos1(PL) >= 3) * 4: Sou(5).Pan = PL * 400 - 600: Sou(5).On = True
If CPos1(PL) = 1 Then
If (CsrLL1(PT(PL)) = 1 Or CsrLL1(PT(PL)) = 15) And SLv1(PL) > 0 Then SLv1(PL) = SLv1(PL) - 1: Lv1(PL) = VSLv(TrM, SLv1(PL)): NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50)): Sou(5).Pan = PL * 400 - 600: Sou(5).On = True
If (CsrRR1(PT(PL)) = 1 Or CsrRR1(PT(PL)) = 15) And SLv1(PL) < 2 - (TrM > 0) Then If SLv1(PL) <= 0 Or ScLv(TrM) >= VSLv(TrM, SLv1(PL) + 1) Then SLv1(PL) = SLv1(PL) + 1: Lv1(PL) = VSLv(TrM, SLv1(PL)): NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50)): Sou(5).Pan = PL * 400 - 600: Sou(5).On = True
End If
If CPos1(PL) = 2 Then
If (CsrLL1(PT(PL)) = 1 Or CsrLL1(PT(PL)) = 15) And Liv1(PL) > 1 Then Liv1(PL) = Liv1(PL) - 1: Sou(5).Pan = PL * 400 - 600: Sou(5).On = True
If (CsrRR1(PT(PL)) = 1 Or CsrRR1(PT(PL)) = 15) And Liv1(PL) < 10 - 5 * ((ScLiv(TrM) >= 1) + (ScLiv(TrM) >= 5) + (TmLiv(TrM) >= 1) + (TmLiv(TrM) >= 3)) Then Liv1(PL) = Liv1(PL) + 1: Sou(5).Pan = PL * 400 - 600: Sou(5).On = True
End If
If PT(PL) >= 0 And CPos1(PL) >= 0 And CsrSS1(PT(PL)) = 1 Then
CPos1(PL) = -1: Sou(0).Pan = PL * 400 - 600: Sou(0).On = True
Else
If CsrAA1(PT(PL)) = 1 Then
Select Case CPos1(PL)
Case 0
CPos1(PL) = -1: Sou(0).Pan = PL * 400 - 600: Sou(0).On = True
Case 3
PT(PL) = -1: CPos1(PL) = -1: Sou(12).Pan = PL * 400 - 600: Sou(12).On = True
End Select
Else
If CsrBB1(PT(PL)) = 1 Then
Select Case CPos1(PL)
Case -1
CPos1(PL) = 0: Sou(12).Pan = PL * 400 - 600: Sou(12).On = True
Case Else
PT(PL) = -1: CsrBB = 2: CPos1(PL) = -1: Sou(12).Pan = PL * 400 - 600: Sou(12).On = True
End Select
End If
End If
End If
End If
Next PL
RR1 = False: RR2 = False
For I = 0 To 3
If PT(I) >= 0 And CPos1(I) >= 0 Then RR1 = True
Next I
For I = 0 To 3
If PT(I) >= 0 Then RR2 = True
Next I
If Not RR1 And RR2 Then KeyInit: VSA = 1
If Not RR2 And CsrBB = 1 Then GFin = 2
End If

If VSA = 1 Then 'CP選択モード
RR = False
For I = 0 To 3
If PT(I) = 5 And CPos1(I) >= 0 Then RR = True
Next I
If Not RR Then
RR = -1
For I = 3 To 0 Step -1
If PT(I) = -1 Then RR = I
Next I
End If
If RR >= 0 Then
PT(RR) = 5: CPos1(RR) = 1
SCPG1(RR) = CPG1(RR)
For I = 0 To 2
For U = 0 To 3
If RR <> U And PT(U) = 5 And CPG1(RR) = CPG1(U) Then CPG1(RR) = CPG1(RR) + 1
Next U, I
If CPG1(RR) > CPMax Then
CPG1(RR) = SCPG1(RR)
For I = 0 To 2
For U = 0 To 3
If RR <> U And PT(U) = 5 And CPG1(RR) = CPG1(U) Then CPG1(RR) = CPG1(RR) - 1
Next U, I
End If
End If
PL = -1
For I = 3 To 0 Step -1
If PT(I) = 5 And CPos1(I) >= 0 Then PL = I
Next I
If PL >= 0 Then
If PT(PL) = 5 Then
If CPos1(PL) >= 0 And CsrUU = 1 Then CPos1(PL) = CPos1(PL) - 1 - (CPos1(PL) <= 0) * 5: Sou(5).Pan = PL * 400 - 600: Sou(5).On = True
If CPos1(PL) >= 0 And CsrDD = 1 Then CPos1(PL) = CPos1(PL) + 1 + (CPos1(PL) >= 4) * 5: Sou(5).Pan = PL * 400 - 600: Sou(5).On = True
If CPos1(PL) = 1 Then
If (CsrLL = 1 Or CsrLL = 15) Then
SCPG1(PL) = CPG1(PL)
CPG1(PL) = CPG1(PL) - 1
For I = 0 To 2
For U = 0 To 3
If PL <> U And PT(U) = 5 And CPG1(PL) = CPG1(U) Then CPG1(PL) = CPG1(PL) - 1
Next U, I
If CPG1(PL) < 1 Then CPG1(PL) = SCPG1(PL) Else Sou(5).Pan = PL * 400 - 600: Sou(5).On = True
End If
If (CsrRR = 1 Or CsrRR = 15) Then
SCPG1(PL) = CPG1(PL)
CPG1(PL) = CPG1(PL) + 1
For I = 0 To 2
For U = 0 To 3
If PL <> U And PT(U) = 5 And CPG1(PL) = CPG1(U) Then CPG1(PL) = CPG1(PL) + 1
Next U, I
If CPG1(PL) > CPMax Then CPG1(PL) = SCPG1(PL) Else Sou(5).Pan = PL * 400 - 600: Sou(5).On = True
End If
End If
If CPos1(PL) = 2 Then
If (CsrLL = 1 Or CsrLL = 15) And SLv1(PL) > 0 Then SLv1(PL) = SLv1(PL) - 1: Lv1(PL) = VSLv(TrM, SLv1(PL)): NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50)): Sou(5).Pan = PL * 400 - 600: Sou(5).On = True
If (CsrRR = 1 Or CsrRR = 15) And SLv1(PL) < 2 - (TrM > 0) Then If SLv1(PL) <= 0 Or ScLv(TrM) >= VSLv(TrM, SLv1(PL) + 1) Then SLv1(PL) = SLv1(PL) + 1: Lv1(PL) = VSLv(TrM, SLv1(PL)): NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50)): Sou(5).Pan = PL * 400 - 600: Sou(5).On = True
End If
If CPos1(PL) = 3 Then
If (CsrLL = 1 Or CsrLL = 15) And Liv1(PL) > 1 Then Liv1(PL) = Liv1(PL) - 1: Sou(5).Pan = PL * 400 - 600: Sou(5).On = True
If (CsrRR = 1 Or CsrRR = 15) And Liv1(PL) < 10 - 5 * ((ScLiv(TrM) >= 1) + (ScLiv(TrM) >= 5) + (TmLiv(TrM) >= 1) + (TmLiv(TrM) >= 3)) Then Liv1(PL) = Liv1(PL) + 1: Sou(5).Pan = PL * 400 - 600: Sou(5).On = True
End If
If PT(PL) >= 0 And CPos1(PL) >= 0 And CsrSS = 1 Then
CPos1(PL) = -1: Sou(0).Pan = PL * 400 - 600: Sou(0).On = True
Else
If CsrAA = 1 Then
Select Case CPos1(PL)
Case 0
CPos1(PL) = -1: Sou(0).Pan = PL * 400 - 600: Sou(0).On = True
Case 4
PT(PL) = -1: CPos1(PL) = -1: Sou(12).Pan = PL * 400 - 600: Sou(12).On = True
RR = -1
For I = 3 To 0 Step -1
If RR = -1 And PT(I) = 5 Then RR = I
Next I
If RR >= 0 Then CPos1(RR) = 0
End Select
Else
If CsrBB = 1 Then
Select Case CPos1(PL)
Case -1
CPos1(PL) = 0: Sou(12).Pan = PL * 400 - 600: Sou(12).On = True
Case Else
PT(PL) = -1: CsrBB = 2: CPos1(PL) = -1: Sou(12).Pan = PL * 400 - 600: Sou(12).On = True
RR = -1
For I = 3 To 0 Step -1
If RR = -1 And PT(I) = 5 Then RR = I
Next I
If RR >= 0 Then CPos1(RR) = 0
End Select
End If
End If
End If
End If
End If

RR1 = False: RR2 = False
For I = 0 To 3
If PT(I) = 5 Then RR1 = True
Next I
For I = 0 To 3
If PT(I) = -1 Then RR2 = True
Next I
If Not RR1 And RR2 Then
For PL = 0 To 3
If PT(PL) >= 0 Then VSA = 0: CPos1(PL) = 0
Next PL
End If
RR = False
For I = 0 To 3
If PT(I) = -1 Or CPos1(I) >= 0 Then RR = True
Next I
If Not RR Then
For I = 0 To 3: RCPG1(I) = CPG1(I): Next I
Pau = 0: Sou(8).Fre = 22050: Sou(8).On = True
End If
End If

Else
KeyScan PT(Pau - 1)
If CsrUU = 1 Or CsrDD = 1 Then CPos = -(CPos = 0): Sou(5).Pan = (Pau - 1) * 400 - 600: Sou(5).On = True
If CsrSS = 1 Then Pau = 0
If CPos = 0 And CsrAA = 1 Then Pau = 0
If CPos = 1 And CsrAA = 1 Then GFin = 2: Fade = False: ScrFC = 0
End If
Else
For PL = 0 To 3
'キー入力
DIDK.GetDeviceStateKeyboard KS
Select Case PT(PL)
Case 0 'キーボード
InpL1(PL) = KS.Key(DIK_LEFT) > 0 Or KS.Key(KArwL) > 0
InpR1(PL) = KS.Key(DIK_RIGHT) > 0 Or KS.Key(KArwR) > 0
InpU1(PL) = KS.Key(DIK_UP) > 0 Or KS.Key(KArwU) > 0
InpD1(PL) = KS.Key(DIK_DOWN) > 0 Or KS.Key(KArwD) > 0
InpA1(PL) = KS.Key(DIK_SPACE) > 0 Or KS.Key(KBL) > 0
InpB1(PL) = KS.Key(DIK_RETURN) > 0 Or KS.Key(DIK_NUMPADENTER) > 0 Or KS.Key(KBR) > 0
InpS1(PL) = KS.Key(DIK_TAB) > 0 Or KS.Key(KBS) > 0
Case 1 To 4 'パッド
On Error Resume Next
JS(PT(PL) - 1) = JS0
DIDJ(PT(PL) - 1).Poll: DIDJ(PT(PL) - 1).GetDeviceStateJoystick JS(PT(PL) - 1)
On Error GoTo CE
InpL1(PL) = (JS(PT(PL) - 1).X < 16384 And ((PArw(PT(PL) - 1) Mod 2) >= 1 Or JS(PT(PL) - 1).Y >= 16384 And JS(PT(PL) - 1).Y <= 49152))
InpR1(PL) = (JS(PT(PL) - 1).X > 49152 And ((PArw(PT(PL) - 1) Mod 2) >= 1 Or JS(PT(PL) - 1).Y >= 16384 And JS(PT(PL) - 1).Y <= 49152))
InpU1(PL) = (JS(PT(PL) - 1).Y < 16384 And ((PArw(PT(PL) - 1) Mod 4) >= 2 Or JS(PT(PL) - 1).X >= 16384 And JS(PT(PL) - 1).X <= 49152))
InpD1(PL) = (JS(PT(PL) - 1).Y > 49152 And ((PArw(PT(PL) - 1) Mod 4) >= 2 Or JS(PT(PL) - 1).X >= 16384 And JS(PT(PL) - 1).X <= 49152))
InpA1(PL) = JS(PT(PL) - 1).buttons(PBL(PT(PL) - 1)) > 64
InpB1(PL) = JS(PT(PL) - 1).buttons(PBR(PT(PL) - 1)) > 64
InpS1(PL) = JS(PT(PL) - 1).buttons(PBS(PT(PL) - 1)) > 64
Case 5 'CP
InpL1(PL) = False: InpR1(PL) = False: InpA1(PL) = False: InpB1(PL) = False: InpU1(PL) = False: InpD1(PL) = False
If (BA1(PL) = 1 And BWC1(PL) > 3) Or BA1(PL) = 2 Or BA1(PL) = 3 Then
If (Not CPE(CPG1(PL)).SRt) Or (BX1(PL) >= CSX1(PL) - 1 And BX1(PL) <= CSX1(PL) + 1) Then
If BM1(PL) = (CSM1(PL) + 2) + (CSM1(PL) >= 2) * 4 Then InpA1(PL) = True: InpB1(PL) = True
If BM1(PL) = (CSM1(PL) + 1) + (CSM1(PL) >= 3) * 4 Then InpA1(PL) = True
If BM1(PL) = (CSM1(PL) - 1) - (CSM1(PL) <= 0) * 4 Then InpB1(PL) = True
End If
If BM1(PL) = CSM1(PL) Then InpA1(PL) = False: InpB1(PL) = False
If CPE(CPG1(PL)).SRt And (BT1(PL) = 2 Or BT1(PL) = 5 Or BT1(PL) = 6) And (CSX1(PL) < 5 Or CSX1(PL) > 7) Then
If BX1(PL) = 6 Then InpA1(PL) = True: InpB1(PL) = True
If (BX1(PL) = 5 Or BX1(PL) = 7) Then InpA1(PL) = False: InpB1(PL) = False
End If
If CPE(CPG1(PL)).Spd = 0 And Not (BT1(PL) = 1 Or BT1(PL) = 3 Or BT1(PL) = 4) And (AC1(PL) >= 11 And AC1(PL) <= 13 And CCWC1(PL) = 0) Then InpA1(PL) = (BX1(PL) < CSX1(PL)) Xor (BT1(PL) <> 2): InpB1(PL) = (BX1(PL) > CSX1(PL)) Xor (BT1(PL) <> 2)
If BA1(PL) = 3 Then CWC1(PL) = CWC1(PL) + 1
If CWC1(PL) > CPE(CPG1(PL)).Spd - 1 Then
If BX1(PL) > CSX1(PL) Then InpL1(PL) = True
If BX1(PL) < CSX1(PL) Then InpR1(PL) = True
If CWC1(PL) > CPE(CPG1(PL)).Spd Then CWC1(PL) = 0
End If
If BM1(PL) = CSM1(PL) And BX1(PL) = CSX1(PL) Then
If BA1(PL) = 3 And AC1(PL) > 0 And CCWC1(PL) > Int(CPE(CPG1(PL)).Wt / 2) And (CSM1(PL) <> CS2M1(PL) Or CSX1(PL) <> CS2X1(PL)) Then CSM1(PL) = CS2M1(PL): CSX1(PL) = CS2X1(PL): CCWC1(PL) = Int(CPE(CPG1(PL)).Wt / 2)
CCWC1(PL) = CCWC1(PL) + 1 + (CCWC1(PL) > CPE(CPG1(PL)).Wt): If CCWC1(PL) > CPE(CPG1(PL)).Wt Then If ((TrM > 0 And CPE(CPG1(PL)).QD) Or TrM = 2) And CSM1(PL) = CS2M1(PL) And CSX1(PL) = CS2X1(PL) Then InpU1(PL) = True Else InpD1(PL) = (Lv1(PL) < 30 Or CSM1(PL) = CS2M1(PL) And CSX1(PL) = CS2X1(PL))
End If
If BX1(PL) = BMX1(PL) And BM1(PL) = BMM1(PL) Then
CMWC1(PL) = CMWC1(PL) + 1: If CMWC1(PL) >= 20 + CPE(CPG1(PL)).Wt Then InpU1(PL) = False: InpD1(PL) = (CMWC1(PL) > 20 + CPE(CPG1(PL)).Wt)
Else
CMWC1(PL) = 0
End If
End If
End Select

If InpL1(PL) And InpR1(PL) Then InpL1(PL) = False: InpR1(PL) = False
If InpU1(PL) And InpD1(PL) Then InpU1(PL) = False: InpD1(PL) = False
If InpL1(PL) Then InpLL1(PL) = InpLL1(PL) + 1 + (InpLL1(PL) > 0): InpLLL1(PL) = InpLLL1(PL) + 1 + (InpLLL1(PL) >= 15 + (TrM > 0) * 5) Else InpLL1(PL) = 0: InpLLL1(PL) = 0
If InpR1(PL) Then InpRR1(PL) = InpRR1(PL) + 1 + (InpRR1(PL) > 0): InpRRR1(PL) = InpRRR1(PL) + 1 + (InpRRR1(PL) >= 15 + (TrM > 0) * 5) Else InpRR1(PL) = 0: InpRRR1(PL) = 0
If InpU1(PL) Then InpUU1(PL) = InpUU1(PL) + 1 + (InpUU1(PL) >= 1) Else InpUU1(PL) = 0
If InpD1(PL) Then InpDD1(PL) = InpDD1(PL) + 1 + (InpDD1(PL) >= 1) Else InpDD1(PL) = 0
If InpA1(PL) Then InpAA1(PL) = InpAA1(PL) + 1 + (InpAA1(PL) >= 1) Else InpAA1(PL) = 0
If InpB1(PL) Then InpBB1(PL) = InpBB1(PL) + 1 + (InpBB1(PL) >= 1) Else InpBB1(PL) = 0
If InpS1(PL) Then InpSS1(PL) = InpSS1(PL) + 1 + (InpSS1(PL) >= 2) Else InpSS1(PL) = 0

'On Error Resume Next
'JS(0) = JS0
'If JC >= 1 Then DIDJ(0).Poll: DIDJ(0).GetDeviceStateJoystick JS(0)
'On Error GoTo CE
'If JS(0).buttons(7) <> 0 Then Lv1(PL) = Lv1(PL) + 1 + (Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150)

If VSR > 2 And BA1(PL) <> 0 And InpSS1(PL) = 1 And Pau = 0 Then Pau = PL + 1: KeyInit: CPos = 0

Next PL

'同時決着判定
VSLiv = True
For PL = 0 To 3
If Liv1(PL) > 1 Or (Liv1(PL) = 1 And (BA1(PL) <> 9 Or BWC1(PL) > 0)) Then VSLiv = False
Next PL

For PL = 0 To 3
'ブロック動作
BAA1(PL) = BA1(PL)
If BAA1(PL) = 0 Then 'スタート
If InpSS1(PL) = 1 Then InpSS1(PL) = 2: If ScLiv(2) > 0 Then Rush1(PL) = Not Rush1(PL): Sou(0).Pan = 0: Sou(0).On = True
BWC1(PL) = BWC1(PL) + 1: If BWC1(PL) >= 192 Then BWC1(PL) = 0: WCa1(PL) = (TrM = 2): DCa1(PL) = True: BA1(PL) = 1
End If

If BAA1(PL) = 1 Then '時間待ち
If BWC1(PL) = 0 Then DmC1(PL) = Dmg1(PL) * (-(CB1(PL) = 0)): If DmC1(PL) > 0 Then Sou(11).Pan = PL * 400 - 600: Sou(11).On = True: DBSC1(PL) = DBSC1(PL) + 1 + (DBSC1(PL) >= 9) * 10: If DmC1(PL) > 4 Then DmC1(PL) = 4
If TrM < 2 Then
If BWC1(PL) < DmC1(PL) Then
Dmg1(PL) = Dmg1(PL) - 1
For I = 1 To 21
For U = 3 To 12
BF1(PL, U, I) = BF1(PL, U, I + 1)
Next U, I
For I = 3 To 12
BF1(PL, I, 22) = 8 * (-(I - 3 <> DBS(DBSC1(PL))))
Next I
With Src
.Left = 0: .Top = 12: .Right = .Left + 120: .Bottom = .Top + 252
End With
BFSf(PL).BltFast 0, 0, BFSf(PL), Src, DDBLTFAST_WAIT
With Des
.Left = 0: .Top = 252: .Right = .Left + 120: .Bottom = .Top + 12
End With
BFSf(PL).BltColorFill Des, 0
For I = 0 To 9
If I <> DBS(DBSC1(PL)) Then
With Src
.Left = 84: .Top = 12: .Right = .Left + 12: .Bottom = .Top + 12
End With
BFSf(PL).BltFast I * 12, 252, BCSf, Src, DDBLTFAST_WAIT
End If
Next I
End If
Else
If BWC1(PL) = 0 And DmC1(PL) > 0 Then
Dmg1(PL) = Dmg1(PL) - DmC1(PL)
For I = 1 To 23 - DmC1(PL)
For U = 3 To 12
BF1(PL, U, I) = BF1(PL, U, I + DmC1(PL))
Next U, I
For I = 23 - DmC1(PL) To 22: For U = 3 To 12
BF1(PL, U, I) = 8 * (-(U - 3 <> DBS(DBSC1(PL))))
Next U, I
With Src
.Left = 0: .Top = DmC1(PL) * 12: .Right = 120: .Bottom = 264
End With
BFSf(PL).BltFast 0, 0, BFSf(PL), Src, DDBLTFAST_WAIT
With Des
.Left = 0: .Top = 264 - DmC1(PL) * 12: .Right = 120: .Bottom = 264
End With
BFSf(PL).BltColorFill Des, 0
For I = 1 To DmC1(PL): For U = 0 To 9
If U <> DBS(DBSC1(PL)) Then
With Src
.Left = 84: .Top = 12: .Right = .Left + 12: .Bottom = .Top + 12
End With
BFSf(PL).BltFast U * 12, 264 - I * 12, BCSf, Src, DDBLTFAST_WAIT
End If
Next U, I
End If
End If
If BWC1(PL) = 0 Then BT1(PL) = Nx1(PL, 0): BX1(PL) = 6: BMX1(PL) = BX1(PL): BY1(PL) = -(BT1(PL) > 0): BYF1(PL) = 0: BM1(PL) = 0: BMM1(PL) = BM1(PL): CWC1(PL) = 0: CCWC1(PL) = 0: CMWC1(PL) = 0
If PT(PL) = 5 And BWC1(PL) >= -(TrM < 2) * 4 And BWC1(PL) < 4 - (TrM < 2) * 4 Then CPAI BWC1(PL) + (TrM < 2) * 4
BWC1(PL) = BWC1(PL) + 1
If TrM = 2 And Not WCa1(PL) And InpUU1(PL) = 1 Then WCa1(PL) = True: DCa1(PL) = False
If Not DCa1(PL) And (TrM > 0 And InpUU1(PL) = 1 Or InpDD1(PL) = 1) Then DCa1(PL) = True: InpUU1(PL) = 2: InpDD1(PL) = 2
If TrM = 2 Then
If WCa1(PL) And BWC1(PL) > 0 And BWC1(PL) < 15 And (InpLL1(PL) = 1 Or InpRR1(PL) = 1 Or InpUU1(PL) = 1 Or InpDD1(PL) = 1 Or InpAA1(PL) = 1 Or InpBB1(PL) = 1) Then BWC1(PL) = 15: InpLLL1(PL) = InpLLL1(PL) + 3 + (InpLLL1(PL) > 7) * (InpLLL1(PL) - 7): InpRRR1(PL) = InpRRR1(PL) + 3 + (InpRRR1(PL) > 7) * (InpRRR1(PL) - 7)
End If
If BWC1(PL) >= 20 + (TrM > 0) * 5 Then BA1(PL) = 2: BAA1(PL) = 2
End If

If BAA1(PL) = 2 Then '初期設定
Ch1(PL) = 0: Chh1(PL) = 0: BSC1(PL) = 0: AC1(PL) = 0
WCa1(PL) = False: DCa1(PL) = False
If Lv1(PL) >= 50 Then Inc1(PL) = 6000 Else Inc1(PL) = LS(Lv1(PL)): If Inc1(PL) <= 0 Then Inc1(PL) = 1
ASC1(PL) = 30 + (Lv1(PL) >= 50 And Lv1(PL) < 200) * Int((Lv1(PL) - 50) / 10) + (Lv1(PL) >= 200 And Lv1(PL) < 250) * Int((Lv1(PL) - 200) / 5) + (Lv1(PL) >= 250 And Lv1(PL) < 300) * (10 + Int((Lv1(PL) - 250) / 10)) + (Lv1(PL) >= 300 And Lv1(PL) < 500) * (15 + Int((Lv1(PL) - 300) / 50)) + (Lv1(PL) >= 500 And Lv1(PL) < 1000) * 19 + (Lv1(PL) >= 1000) * 20
For I = 0 To 24: Nx1(PL, I) = Nx1(PL, I + 1): Next I
NxC1(PL) = NxC1(PL) + 1 + (NxC1(PL) >= 776) * 777: Nx1(PL, 25) = BtNx(NxC1(PL))
BA1(PL) = 3: BAA1(PL) = 3
End If

If BAA1(PL) = 3 Then 'アクティブ
RushC1(PL) = -Rush1(PL) * 19
For J = 0 To -Rush1(PL) * 19
BMove
If BM1(PL) <> BMM1(PL) Then Sou(1).Pan = BX1(PL) * 40 - 220 + PL * 400 - 600
If BX1(PL) <> BMX1(PL) Then Sou(5).Pan = BX1(PL) * 40 - 220 + PL * 400 - 600
Ch1(PL) = 0 'ミス判定
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BYF1(PL) = BY1(PL) '下移動
BSC1(PL) = BSC1(PL) + Inc1(PL)
If BSC1(PL) <= 240 And InpDD1(PL) = 1 Then BSC1(PL) = 240
If BSC1(PL) <= 6000 And InpUU1(PL) = 1 And TrM > 0 Then BSC1(PL) = 6000
While BSC1(PL) >= 240
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1) + 1) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BY1(PL) = BY1(PL) + 1: BSC1(PL) = BSC1(PL) - 240: AC1(PL) = 0
Else
BSC1(PL) = 239
End If
Wend
If BY1(PL) > BYF1(PL) Then BYF1(PL) = BYF1(PL) + 1
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1) + 1) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then AC1(PL) = 0 Else AC1(PL) = AC1(PL) + 1: Sou(3).Pan = BX1(PL) * 40 - 220 + PL * 400 - 600: Sou(3).On = Sou(3).On Or (AC1(PL) = 1): If (InpDD1(PL) = 1 Or (TrM > 0 And InpUU1(PL) = 1)) And AC1(PL) >= -(InpDD1(PL) = 1) * (6 - Int(Lv1(PL) / 5)) * (1 - Rush1(PL) * 19) And AC1(PL) <= 30 * (1 - Rush1(PL) * 19) Then AC1(PL) = 30 * (1 - Rush1(PL) * 19): WCa1(PL) = (InpUU1(PL) = 1): DCa1(PL) = (InpUU1(PL) = 1 Or InpDD1(PL) = 1)
InpUU1(PL) = InpUU1(PL) - (AC1(PL) >= 30 * (1 - Rush1(PL) * 19) And InpUU1(PL) = 1): InpDD1(PL) = InpDD1(PL) - (AC1(PL) >= 30 * (1 - Rush1(PL) * 19) And InpDD1(PL) = 1)
If BSC1(PL) >= 239 And AC1(PL) >= ASC1(PL) * (1 - Rush1(PL) * 19) Then RushC1(PL) = J: BA1(PL) = 4: Sou(2).Pan = BX1(PL) * 40 - 220 + PL * 400 - 600: Sou(2).Fre = 22050 / (((BY1(PL) - 1) * 0.055) + 1): Sou(2).On = True
Else
RushC1(PL) = J
For I = 0 To 3
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = BT1(PL) + 1
With Src
.Left = BT1(PL) * 12: .Top = 12: .Right = .Left + 12: .Bottom = .Top + 12
End With
BFSf(PL).BltFast (BX1(PL) + B(BT1(PL), BM1(PL), I, 0)) * 12 - 36, (BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) * 12 - 12, BCSf, Src, DDBLTFAST_WAIT
Next I
BWC1(PL) = 0: BA1(PL) = 9
End If
RBX1(PL, J) = BX1(PL): RBY1(PL, J) = BY1(PL): RBM1(PL, J) = BM1(PL): RAC1(PL, J) = AC1(PL): RBSC1(PL, J) = BSC1(PL): RInc1(PL, J) = Inc1(PL): RBYF1(PL, J) = BYF1(PL)
If RushC1(PL) < -Rush1(PL) * 19 Then J = -Rush1(PL) * 19
Next J
End If
If BAA1(PL) = 4 Then 'セット
RushC1(PL) = 0: J = RushC1(PL)
RBX1(PL, J) = BX1(PL): RBY1(PL, J) = BY1(PL): RBM1(PL, J) = BM1(PL): RAC1(PL, J) = AC1(PL): RBSC1(PL, J) = BSC1(PL): RInc1(PL, J) = Inc1(PL): RBYF1(PL, J) = BYF1(PL)
For I = 0 To 3
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = BT1(PL) + 1
With Src
.Left = BT1(PL) * 12: .Top = 12: .Right = .Left + 12: .Bottom = .Top + 12
End With
BFSf(PL).BltFast (BX1(PL) + B(BT1(PL), BM1(PL), I, 0)) * 12 - 36, (BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) * 12 - 12, BCSf, Src, DDBLTFAST_WAIT
Next I
BA1(PL) = 5
End If
If BAA1(PL) = 5 Then '判定
Chh1(PL) = 0
For I = 0 To 3
Ch1(PL) = 0
For U = 3 To 12
If BY1(PL) + I > 0 And BY1(PL) + I < 23 And BF1(PL, U, BY1(PL) + I) > 0 Then Ch1(PL) = Ch1(PL) + 1
Next U
If Ch1(PL) >= 10 Then FE1(PL, Chh1(PL)) = BY1(PL) + I: Chh1(PL) = Chh1(PL) + 1
Next I
If Chh1(PL) > 0 Then DCa1(PL) = DCa1(PL) Or (TrM < 2): BA1(PL) = 6: BAA1(PL) = 6 Else CB1(PL) = 0: BWC1(PL) = 0: For I = 0 To 3: Dmg1(I) = Dmg1(I) + Hit1(PL) * (-(I <> PL)): Next I: Hit1(PL) = 0: BA1(PL) = 1
End If
If BAA1(PL) = 6 Then 'ブロック消去
Sou(6 - (Chh1(PL) >= 4)).Pan = PL * 400 - 600: Sou(6 - (Chh1(PL) >= 4)).Fre = 22050 * 2 ^ ((CB1(PL) + (CB1(PL) > 9) * (CB1(PL) - 9)) / 12): Sou(6 - (Chh1(PL) >= 4)).On = True
Hitt = Chh1(PL) - 1 - (Chh1(PL) = 4) - (CB1(PL) > 0) * (1 - (Chh1(PL) = 4))
If Chh1(PL) < 4 And Dmg1(PL) > 0 Then Dmg1(PL) = Dmg1(PL) - Hitt: Hitt = 0: If Dmg1(PL) < 0 Then Hitt = Hitt - Dmg1(PL): Dmg1(PL) = 0
Hit1(PL) = Hit1(PL) + Hitt
R = Rnd * 360
For I = 0 To Chh1(PL) - 1
If BAni Then
For U = 0 To 9
With EA(EAC)
.V = 1 - (Hit1(PL) >= 4): .A = BF1(PL, 3 + U, FE1(PL, I)) - 1: .AF = Int(Rnd * -(Hit1(PL) >= 4) * 4)
.X = 44 + PL * 144 + U * 12: .Y = 116 + FE1(PL, I) * 12
.XX = Sin((R + I * 90 + U * 108) * Rg) * (4 + Chh1(PL) * 3): .YY = Cos((R + I * 90 + U * 108) * Rg) * (4 + Chh1(PL) * 3)
If TrM > 0 Then .XXX = .YY / 50 * (((I + U) Mod 2) * (2 * (TrM = 2)) + 1) * ((PL Mod 2) * 2 - 1): .YYY = -.XX / 50 * (((I + U) Mod 2) * (2 * (TrM = 2)) + 1) * ((PL Mod 2) * 2 - 1) Else .XXX = 0: .YYY = 0.2
End With
EAC = EAC + 1 + (EAC >= 159) * 160
Next U
End If
With Des
.Left = 0: .Top = FE1(PL, I) * 12 - 12: .Right = .Left + 120: .Bottom = .Top + 12
End With
BFSf(PL).BltColorFill Des, 0
Next I
BWC1(PL) = -(TrM = 0) * Int(Lv1(PL) / 5) * 3 - (TrM = 1) * (20 + Int(Lv1(PL) / 5)) - (TrM = 2) * 35
CB1(PL) = CB1(PL) + 1 + (CB1(PL) >= 100)
Li1(PL) = Li1(PL) + Chh1(PL): If Lv1(PL) < 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 Then If Li1(PL) >= NL1(PL) Then Sou(4).Pan = PL * 400 - 600: Sou(4).On = True: Lv1(PL) = Lv1(PL) + 1: NF1(PL) = 65: NL1(PL) = (NL1(PL) + 8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50))
BA1(PL) = 7: BAA1(PL) = 7
End If
If BAA1(PL) = 7 Then '時間待ち
If BWC1(PL) >= 35 - Chh1(PL) Then BWC1(PL) = 0: BA1(PL) = 8: BAA1(PL) = 8 Else BWC1(PL) = BWC1(PL) + 1
End If
If BAA1(PL) = 8 Then 'フィールドずらし
Do
I = BWC1(PL)
For U = FE1(PL, I) To 2 Step -1
For Y = 3 To 12
BF1(PL, Y, U) = BF1(PL, Y, U - 1)
Next Y, U
For U = 3 To 12
BF1(PL, U, 1) = 0
Next U
With Src
.Left = 0: .Top = 0: .Right = .Left + 120: .Bottom = .Top + (FE1(PL, I)) * 12 - 12
End With
BFSf(PL).BltFast 0, 12, BFSf(PL), Src, DDBLTFAST_WAIT
With Des
.Left = 0: .Top = 0: .Right = .Left + 120: .Bottom = .Top + 12
End With
BFSf(PL).BltColorFill Des, 0
BWC1(PL) = BWC1(PL) + 1
Loop While TrM = 2 And BWC1(PL) < Chh1(PL)
If BWC1(PL) = Chh1(PL) Then Sou(13).Pan = PL * 400 - 600: Sou(13).On = True: BWC1(PL) = 0: BA1(PL) = 1
End If

If BAA1(PL) = 9 Then 'ミス
If BWC1(PL) = 0 Then
If VSLiv Then Liv1(PL) = Liv1(PL) + 1
If Liv1(PL) > 1 Then Sou(10).Pan = PL * 400 - 600: Sou(10).On = True Else VSR = VSR + (VSR1(PL) = 0): Sou(9).Pan = PL * 400 - 600: Sou(9).On = True
Liv1(PL) = Liv1(PL) - 1: If Liv1(PL) < 0 Then Liv1(PL) = 0
Li1(PL) = 0: Lv1(PL) = VSLv(TrM, SLv1(PL)): NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50))
CB1(PL) = 0: Dmg1(PL) = 0
End If
If BWC1(PL) < 60 Then
If BWC1(PL) >= 16 And BWC1(PL) < 38 Then
For I = 3 To 12: BF1(PL, I, BWC1(PL) - 15) = 0: Next I
With Des
.Left = 0: .Top = (BWC1(PL) - 16) * 12: .Right = .Left + 120: .Bottom = .Top + 12
End With
BFSf(PL).BltColorFill Des, 0
End If
BWC1(PL) = BWC1(PL) + 1
Else
For I = 0 To 3: Dmg1(I) = Dmg1(I) + Hit1(PL) * (-(I <> PL)): Next I: Hit1(PL) = 0
If Tit Or Liv1(PL) > 0 Then WCa1(PL) = True: DCa1(PL) = True: BWC1(PL) = 0: BA1(PL) = 1 Else BWC1(PL) = 0: BA1(PL) = 10: BAA1(PL) = 10
End If
End If

If BAA1(PL) = 10 Then 'ゲームオーバー
End If

If Li1(PL) > 10000 Then Li1(PL) = 10000
If Hit1(PL) > 10000 Then Hit1(PL) = 10000
If Dmg1(PL) > 10000 Then Dmg1(PL) = 10000

Next PL
For PL = 0 To 3
If VSR1(PL) = 0 And Liv1(PL) = 0 Then VSR1(PL) = VSR
Next PL
For PL = 0 To 3
If VSR1(PL) = 0 And VSR = 2 Then VSR1(PL) = 1: Sou(8).Fre = 22050: Sou(8).On = True
Next PL
If VSR <= 2 Then '対戦決着
If VSFinC = 1 Then Mut = True
If VSFinC = 199 Then GFin = 1
VSFinC = VSFinC + 1: If VSFinC > 200 Then VSFinC = 200
End If
End If
End If

'背景表示
LvL = 0
For PL = 0 To 3
If VSLv(TrM, SLv1(PL)) > LvL Then LvL = VSLv(TrM, SLv1(PL))
Next PL
BGD TrM, LvL
'ボード表示
For PL = 3 To 0 Step -1
With Src
.Left = 0: .Top = 0: .Right = 168: .Bottom = 276
End With
If Not FS Then BBSf.BltFast 20 + PL * 144, 128, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
If Rush1(PL) Then
RR = Int(RushAniC * 3 / 4)
With Src
.Left = 24 + RR: .Top = 0: .Right = 144: .Bottom = .Top + 12
End With
If Not FS Then BBSf.BltFast 44 + PL * 144, 128, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
With Src
.Left = 24: .Top = 0: .Right = 24 + RR: .Bottom = .Top + 12
End With
If Not FS Then BBSf.BltFast 164 + PL * 144 - RR, 128, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next PL
'ライフ表示
For PL = 0 To 3
For I = 1 To Liv1(PL) + (Liv1(PL) > 14) * (Liv1(PL) - 14)
With Src
.Left = 192: .Top = ((10 + LAni * (1 - (I Mod 2) * 2)) Mod 10) * 24: .Right = .Left + 24: .Bottom = .Top + 24
End With
If Not FS Then BBSf.BltFast 164 + PL * 144, 116 + I * 18, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
Next PL
'ダメージ表示
DAni = DAni + 1 + (DAni >= 5) * 6
For PL = 0 To 3
If Liv1(PL) > 0 Then
For I = 1 To Dmg1(PL) + (Dmg1(PL) > 22) * (Dmg1(PL) - 22)
With Src
.Left = 168: .Top = ((44 + DAni - I * 2) Mod 6) * 12: .Right = .Left + 24: .Bottom = .Top + 12
End With
If Not FS Then BBSf.BltFast 164 + PL * 144, 392 - I * 12, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
RR = 0
For I = 0 To 3
If I <> PL Then RR = RR + Hit1(I)
Next I
For I = Dmg1(PL) + 1 To Dmg1(PL) + RR + (Dmg1(PL) + RR > 22) * (Dmg1(PL) + RR - 22)
With Src
.Left = 168: .Top = 72 + ((44 + DAni - I * 2) Mod 6) * 12: .Right = .Left + 24: .Bottom = .Top + 12
End With
If Not FS Then BBSf.BltFast 164 + PL * 144, 392 - I * 12, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
End If
Next PL
'フィールド枠線表示
For PL = 0 To 3
If (VSR <= 2 And InpS1(PL)) Or (BA1(PL) <> 9 And Pau > 0) Or VSR1(PL) = 1 Then
If BA1(PL) > 2 And BA1(PL) < 6 Then
For I = 0 To 3
FL(I) = BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1))
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = BT1(PL) + 1
Next I
End If
For I = 0 To 21
For U = 0 To 8
If BF1(PL, 3 + U, I + 1) = 0 Xor BF1(PL, 4 + U, I + 1) = 0 Then
With Src
 .Left = 120: .Top = 279: .Right = .Left + 2: .Bottom = .Top + 12
End With
If Not FS Then BBSf.BltFast 55 + PL * 144 + U * 12, 128 + I * 12, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next U, I
For I = 0 To 20
For U = 0 To 9
If BF1(PL, 3 + U, I + 1) = 0 Xor BF1(PL, 3 + U, I + 2) = 0 Then
With Src
 .Left = 120: .Top = 276: .Right = .Left + 12: .Bottom = .Top + 2
End With
If Not FS Then BBSf.BltFast 44 + PL * 144 + U * 12, 139 + I * 12, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next U, I
If BA1(PL) > 2 And BA1(PL) < 6 Then
For I = 0 To 3
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = FL(I)
Next I
End If
End If
Next PL
If Pau = 0 Then
'フィールド表示
For PL = 0 To 3
If Not (VSR <= 2 And InpS1(PL) Or VSR1(PL) = 1) Then
With Src
.Left = 0: .Top = 0: .Right = .Left + 120: .Bottom = .Top + 264
End With
If Not FS Then BBSf.BltFast 44 + PL * 144, 128, BFSf(PL), Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
If (Not BAni) And BA1(PL) = 7 Then
For I = 0 To Chh1(PL) - 1
For U = 0 To 9
With Src
If ((80 + BWC1(PL) + Chh1(PL) - (Chh1(PL) = 4) * ((PL Mod 2) * 2 - 1) * (U + I * 2)) Mod 8) < 4 Then .Left = 84: .Top = 0: .Right = .Left + 12: .Bottom = .Top + 12 Else .Left = (BF1(PL, U + 3, FE1(PL, I)) - 1) * 12: .Top = -(BF1(PL, U + 3, FE1(PL, I)) = 8) * 12: .Right = .Left + 12: .Bottom = .Top + 12
End With
If Not FS Then BBSf.BltFast 44 + PL * 144 + U * 12, 116 + FE1(PL, I) * 12, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next U
Next I
End If
End If
Next PL
'アクティブブロック残像表示
For PL = 0 To 3
If Not ((OfBt Or VSR <= 2) And InpS1(PL) Or VSR1(PL) = 1) Then
If Zanzo Then
'If FS Then
'If (Not (BSm And Not Inc1(PL) >= 240 And LAC1(PL) = 0) Or (16 + Int(LBSC1(PL) / 20)) = (16 + Int(BSC1(PL) / 20))) And LBX1(PL) = BX1(PL) And LBY1(PL) = BY1(PL) And LBYF1(PL) = BYF1(PL) And LBM1(PL) = BM1(PL) Then CLB1(PL) = True Else CLB1(PL) = False
'Else
'If LBA1(PL) > 2 And LBA1(PL) < 6 And Not CLB1(PL) Then
'For U = LBYF1(PL) To LBY1(PL)
'For I = 0 To 3
'If LBA1(PL) < 4 Or LBA1(PL) > 5 Or U < LBY1(PL) Then
'With Src
'.Left = LBT1(PL) * 12: .Top = 0: .Right = .Left + 12: .Bottom = .Top + 12
'End With
'Else
'With Src
'.Left = 84: .Top = 0: .Right = .Left + 12: .Bottom = .Top + 12
'End With
'End If
'If Not FS Then BBSf.BltFast 8 + PL * 144 + (LBX1(PL) + B(LBT1(PL), LBM1(PL), I, 0)) * 12, 116 + (U + B(LBT1(PL), LBM1(PL), I, 1)) * 12 - (BSm And Not (Zanzo And Inc1(PL) >= 240) And LAC1(PL) = 0) * Int(LBSC1(PL) / 20), BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
'Next I, U
'CLB1(PL) = True
'End If
'End If
'LBX1(PL) = BX1(PL): LBY1(PL) = BY1(PL): LBYF1(PL) = BYF1(PL): LBSC1(PL) = BSC1(PL): LAC1(PL) = AC1(PL): LBT1(PL) = BT1(PL): LBM1(PL) = BM1(PL): LBA1(PL) = BA1(PL)
If FS Then
For J = 0 To -Rush1(PL) * 19
If (Not (BSm And Not RInc1(PL, J) >= 240 And LAC1(PL, J) = 0) Or (16 + Int(LBSC1(PL, J) / 20)) = (16 + Int(RBSC1(PL, J) / 20))) And LBX1(PL, J) = RBX1(PL, J) And LBY1(PL, J) = RBY1(PL, J) And LBYF1(PL, J) = RBYF1(PL, J) And LBM1(PL, J) = RBM1(PL, J) Then CLB1(PL, J) = True Else CLB1(PL, J) = False
Next J
Else
For J = -(Not Zanzo Or BA1(PL) = 5) * LRushC1(PL) To LRushC1(PL)
If LBA1(PL) > 2 And LBA1(PL) < 6 And Not CLB1(PL, J) Then
For U = LBYF1(PL, J) To LBY1(PL, J)
For I = 0 To 3
If LBA1(PL) < 4 Or LBA1(PL) > 5 Or U < LBY1(PL, J) Or J < LRushC1(PL) Then
With Src
.Left = LBT1(PL) * 12: .Top = 0: .Right = .Left + 12: .Bottom = .Top + 12
End With
Else
With Src
.Left = 84: .Top = 0: .Right = .Left + 12: .Bottom = .Top + 12
End With
End If
If Not FS Then BBSf.BltFast 8 + PL * 144 + (LBX1(PL, J) + B(LBT1(PL), LBM1(PL, J), I, 0)) * 12, 116 + (U + B(LBT1(PL), LBM1(PL, J), I, 1)) * 12 - (BSm And Not FE And Not (Zanzo And LInc1(PL, J) >= 240) And LAC1(PL, J) = 0) * Int(LBSC1(PL, J) / 20), BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I, U
End If
CLB1(PL, J) = True
Next J
End If
LRushC1(PL) = RushC1(PL): LBA1(PL) = BA1(PL): LBT1(PL) = BT1(PL)
For J = 0 To -Rush1(PL) * 19
LBX1(PL, J) = RBX1(PL, J): LBY1(PL, J) = RBY1(PL, J): LBYF1(PL, J) = RBYF1(PL, J): LBSC1(PL, J) = RBSC1(PL, J): LAC1(PL, J) = RAC1(PL, J): LBM1(PL, J) = RBM1(PL, J): LInc1(PL, J) = RInc1(PL, J)
Next J
End If
End If
Next PL
'アクティブブロック表示
For PL = 0 To 3
If Not ((OfBt Or VSR <= 2) And InpS1(PL) Or VSR1(PL) = 1) Then
If BA1(PL) > 2 And BA1(PL) < 6 Then
For J = -(Not Zanzo Or BA1(PL) = 5) * RushC1(PL) To RushC1(PL)
If Not Zanzo Then RBYF1(PL, J) = RBY1(PL, J)
For U = RBYF1(PL, J) To RBY1(PL, J)
For I = 0 To 3
If BA1(PL) < 4 Or BA1(PL) > 5 Or U < RBY1(PL, J) Or J < RushC1(PL) Then
With Src
.Left = BT1(PL) * 12: .Top = 0: .Right = .Left + 12: .Bottom = .Top + 12
End With
Else
With Src
.Left = 84: .Top = 0: .Right = .Left + 12: .Bottom = .Top + 12
End With
End If
If Not FS Then BBSf.BltFast 8 + PL * 144 + (RBX1(PL, J) + B(BT1(PL), RBM1(PL, J), I, 0)) * 12, 116 + (U + B(BT1(PL), RBM1(PL, J), I, 1)) * 12 - (BSm And Not (Zanzo And RInc1(PL, J) >= 240) And RAC1(PL, J) = 0) * Int(RBSC1(PL, J) / 20), BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I, U
Next J
End If
End If
BYF1(PL) = BY1(PL)
RushC1(PL) = 0: J = RushC1(PL)
RBX1(PL, J) = BX1(PL): RBY1(PL, J) = BY1(PL): RBM1(PL, J) = BM1(PL): RAC1(PL, J) = AC1(PL): RBSC1(PL, J) = BSC1(PL): RInc1(PL, J) = Inc1(PL): RBYF1(PL, J) = BYF1(PL)
Next PL
'順位2-4表示
For PL = 0 To 3
If BA1(PL) = 10 Or BA1(PL) = 9 And BWC1(PL) >= 25 Then
If VSR1(PL) = 2 Then StringD 80 + PL * 144, 236, 3, "2ND", False
If VSR1(PL) = 3 Then StringD 80 + PL * 144, 236, 2, "3RD", False
If VSR1(PL) = 4 Then StringD 80 + PL * 144, 236, 1, "4TH", False
End If
Next PL
'ミス表示
For PL = 0 To 3
If BA1(PL) = 9 Then
For I = 0 To 21
RR1 = BWC1(PL) - I
If RR1 < 0 Then RR1 = 0
If RR1 > 12 Then RR1 = 12
RR2 = BWC1(PL) - I - 22
If RR2 < 0 Then RR2 = 0
If RR2 > 12 Then RR2 = 12
RR = Int(Rnd * 120)
With Src
.Left = RR: .Top = 276 + ((I + Int(CCC)) Mod 7) * 12 + RR2: .Right = 120: .Bottom = .Top + RR1 - RR2
End With
If Not FS Then BBSf.BltFast 44 + PL * 144, 128 + I * 12 + RR2, BDSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 276 + ((I + Int(CCC)) Mod 7) * 12 + RR2: .Right = .Left + RR: .Bottom = .Top + RR1 - RR2
End With
If Not FS Then BBSf.BltFast 164 + PL * 144 - RR, 128 + I * 12 + RR2, BDSf, Src, DDBLTFAST_WAIT
Next I
End If
Next PL
'勝者表示
For PL = 0 To 3
If VSR1(PL) = 1 Then StringD 48 + PL * 144, 236, 11, "WINNER!", False
Next PL
End If
'消去アニメ表示
EAni4
'NEXT表示
For PL = 0 To 3
If MNx < 4 Or BA1(PL) > 0 Then
For I = 0 To MNx - 1 + (MNx > 4) * (MNx - 4) - (MNx = 0 And BA1(PL) <> 3)
With Src
.Left = Int(Nx1(PL, I) / 4) * 48: .Top = 24 + (Nx1(PL, I) Mod 4) * 24: .Right = .Left + 48: .Bottom = .Top + 24
End With
If Not FS Then BBSf.BltFast 80 + PL * 144, 92 - I * 30, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
Else
NxD4
End If
If MNx < 4 Then StringD 72 + PL * 144, 96 - (MNx - (MNx = 0)) * 30, 9, "NEXT", False
Next PL
'デバイス表示
If Not Tit Then
For PL = 0 To 3
Select Case PT(PL)
Case 0
StringD 80 + PL * 144, 124, 8, "KEY", False
Case 1 To 4
StringD 56 + PL * 144, 124, 8, "PAD( )", False
StringD 120 + PL * 144, 124, 8, Chr$(64 + PT(PL)), False
Case 5
StringD 72 + PL * 144, 124, 8, "CP", False
TextD 136 + PL * 144, 124, 0, CPG1(PL), True, 2
End Select
Next PL
End If
'ライフ(>14)表示
For PL = 0 To 3
If Liv1(PL) > 14 Then TextD 188 + PL * 144, 128, 4 + Int(LAni / 5), Liv1(PL), True, 2
Next PL
'レベル表示
For PL = 0 To 3
NF1(PL) = NF1(PL) - 1 - (NF1(PL) <= 0)
StringD 40 + PL * 144, 408, 9, "LEVEL", False
TextD 120 + PL * 144, 424, 3 - (NF1(PL) > 0 Or (Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150)) * 8, Lv1(PL), True, 4
Next PL
'ライン表示
For PL = 0 To 3
StringD 40 + PL * 144, 444, 9, "LINES", False
TextD 120 + PL * 144, 460, 3 - (Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150) * 8, Li1(PL), True, 5
If Lv1(PL) < 30 - (TrM >= 1) * 20 Then
StringD 120 + PL * 144, 460, 4, "/", False
TextD 136 + PL * 144, 460, 3 - (Lv1(PL) >= 29 - (TrM >= 1) * 20 - (TrM = 2) * 150) * 8, NL1(PL), False, 3
End If
Next PL
If Pau = -1 Then '設定表示
For PL = 0 To 3
If PT(PL) >= 0 Then
StringD 66 + PL * 144, 192, 8 - (CPos1(PL) = 0) * 3, "START", False
If PT(PL) = 5 Then
StringD 66 + PL * 144, 224, 9, "GRD", False
TextD 154 + PL * 144, 224, 7 - (CPos1(PL) = 1) * 4, CPG1(PL), True, 3
End If
StringD 66 + PL * 144, 224 - (PT(PL) = 5) * 32, 9, "LV", False
TextD 154 + PL * 144, 224 - (PT(PL) = 5) * 32, 7 - (CPos1(PL) = 1 - (PT(PL) = 5)) * 4, Lv1(PL), True, 3
StringD 66 + PL * 144, 256 - (PT(PL) = 5) * 32, 9, "LIV", False
TextD 154 + PL * 144, 256 - (PT(PL) = 5) * 32, 7 - (CPos1(PL) = 2 - (PT(PL) = 5)) * 4, Liv1(PL), True, 3
StringD 66 + PL * 144, 288 - (PT(PL) = 5) * 32, 8 - (CPos1(PL) = 3 - (PT(PL) = 5)) * 3, "CANCEL", False
If CPos1(PL) >= 0 Then CursorD 46 + PL * 144, 192 + CPos1(PL) * 32, 11, -(CPos1(PL) = 0 Or CPos1(PL) = 3 - (PT(PL) = 5))
Else
If VSA = 0 Then
If CC < 30 Then StringD 64 + PL * 144, 236, 8, "ENTRY", False, 10
Else
If CC < 30 Then StringD 72 + PL * 144, 236, 9, "WAIT", False, 10
End If
End If
Next PL
End If
If Pau > 0 Then 'ポーズ表示
For PL = 0 To 3
If PL = Pau - 1 Then
StringD 80 + PL * 144, 236, 8 - (CPos = 0) * 3, "CONT", False
StringD 80 + PL * 144, 268, 8 - (CPos = 1) * 3, "EXIT", False
CursorD 58 + PL * 144, 236 + CPos * 32, 11, 1
Else
If CC < 30 Then StringD 72 + PL * 144, 236, 9, "WAIT", False
End If
Next PL
End If
'スタート表示
If Pau = 0 Then
For PL = 0 To 3
If BA1(PL) = 0 Then
If BWC1(PL) > 12 Then
RR = BWC1(PL) - 12
TextD 96 + PL * 144, 236, 11, 3 - Int(RR / 60), False
For I = 0 To 61 - (RR Mod 60)
If RR < 60 Then CursorD 96 + PL * 144 - Sin(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60) * 0.5), 236 + Cos(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60) * 0.75), -(RR >= 60) - (RR >= 120) * 7, 0
If RR >= 60 And RR < 120 Then CursorD 96 + PL * 144 - Sin(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 50) * (1 + Int(RR / 60) * 0.125), 236 + Cos(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60) * 0.75), -(RR >= 60) - (RR >= 120) * 7, 0
If RR >= 120 Then CursorD 96 + PL * 144 - Sin(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60) * 0.5), 236 + Cos(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 50) * (1 + Int(RR / 60) * 0.125), -(RR >= 60) - (RR >= 120) * 7, 0
Next I
End If
If ScLiv(2) > 0 And PT(PL) < 5 Then
StringD 44 + PL * 144, 148, 9, "RUSH:", False, 4
If Rush1(PL) Then StringD 116 + PL * 144, 148, 11, "ON", False Else StringD 116 + PL * 144, 148, 11, "OFF", False
End If
End If
Next PL
End If

FChange

Loop
For I = 3 To 0 Step -1: Set BFSf(I) = Nothing: Next I
Set BCSf = Nothing
Set BDSf = Nothing
Exit Sub

CE:
'Beep
GMode = 2

End Sub

Sub VSNet() 'ネット対戦
On Error GoTo CE

If Not Par Then
If Not NetCont Then
BBSf.BltColorFill RC0, 0
If Not WM Then PrSf.BltColorFill RC0, 0
SfDestroy
DD.RestoreDisplayMode
DD.SetCooperativeLevel Form1.hWnd, DDSCL_NORMAL
Set DIDM = Nothing
NetDestroy
NetCreate
JoinCreate

If SsC <= 0 Then
NetDestroy
NetCreate
NameCreate
GMode = 5: GEnd = True
End If

'画面の変更
If Not WM Then
DD.SetCooperativeLevel Form1.hWnd, DDSCL_EXCLUSIVE Or DDSCL_FULLSCREEN Or DDSCL_ALLOWMODEX
FullScrErr = False
On Error GoTo CDisp
DD.SetDisplayMode WW, HH, BPP, 60, DDSDM_DEFAULT
On Error GoTo CE
If FullScrErr Then
FullScrErr = False
On Error GoTo CDisp
DD.SetDisplayMode WW, HH, BPP, 0, DDSDM_DEFAULT
On Error GoTo CE
If FullScrErr Then WM = True
End If
End If
If WM Then
DD.SetCooperativeLevel Picture1.hWnd, DDSCL_NORMAL
End If
'マウスデバイスの作成
If Not WM Then
Set DIDM = DI.CreateDevice("GUID_SysMouse")
DIDM.SetCommonDataFormat DIFORMAT_MOUSE
DIDM.SetCooperativeLevel Form1.hWnd, DISCL_FOREGROUND Or DISCL_EXCLUSIVE
DIDM.Acquire
End If
SfCreate

End If
Else
SsC = 1
End If

If SsC > 0 Then

GEnd = False
FS = False
Pau = -1
Fade = True: ScrFC = 0
KeyInitAll

NCC = NC: NC = 0: For I = 0 To 1: PLID1(I) = 0: JN1(I) = vbNullString$: Next I
SessionCheck
If NCC < 2 And NC >= 2 Then StrMsgLogAdd "": StrMsgLogAdd "#: " + Left$(HNN, InStr(HNN, " ") - 1 - (InStr(HNN, " ") = 0) * 7) + " " + String$(3, 62 + NetPar * 2) + " " + Left$(JN, InStr(JN, " ") - 1 - (InStr(JN, " ") = 0) * 7) + " (" + TrMN(TrM) + ")"

For PL = 0 To 1
If NetFldLR > 0 Then PT(PL) = (1 + Par * 2) * (PL <> NetFldLR - 1) Else PT(PL) = (1 + Par * 2) * (PLID1(PL) <> PlID)
Next PL

For PL = 0 To 1: SLv1(PL) = 0: Next PL

'背景パレット初期化
If Not WM Then
For I = 0 To 127
With MPa(I)
.red = 0: .green = 0: .blue = 0
End With
Next I
If Not FS Then MPal.SetEntries 0, 256, MPa
End If

'ボード（オフスクリーン）の作成
BDCr BD

'ブロック（オフスクリーン）の作成
BCCr BC

BRndInit

If Not Par Then '参加者
For I = 0 To 776: BtNx(I) = BRnd: Next I
BtNx(776) = (BtNx(776) - (BtNx(776) = BtNx(0))) Mod 7
For I = 0 To 9: DBS(I) = I: Next I
For I = 0 To 8: RR = Int(Rnd * (10 - I)): RR1 = DBS(I): DBS(I) = DBS(RR): DBS(RR) = RR1: Next I
For I = 0 To 110
ParBtNx(I) = 0
For U = 0 To 6
ParBtNx(I) = ParBtNx(I) * 7
ParBtNx(I) = ParBtNx(I) + BtNx(I * 7 + U)
Next U, I
For I = 0 To 1
ParDBS(I) = 0
For U = 0 To 4
ParDBS(I) = ParDBS(I) * 10
ParDBS(I) = ParDBS(I) + DBS(I * 5 + U)
Next U, I
Else '親
For I = 0 To 110
RR = ParBtNx(I)
For U = 0 To 6
BtNx(I * 7 + (6 - U)) = RR Mod 7
RR = Int(RR / 7)
Next U, I
For I = 0 To 1
RR = ParDBS(I)
For U = 0 To 4
DBS(I * 5 + (4 - U)) = RR Mod 10
RR = Int(RR / 10)
Next U, I
Set DPM = DP.CreateMessage: DPM.WriteLong -3 - NetQ: DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing
End If

DAni = 0
GFin = 0: VSFinC = 0
VSA = 0: VSR = 3
StrEdit = False: StrRsv = 0
For PL = 0 To 1
'フィールド（オフスクリーン）の作成
SD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
SD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
SD.lWidth = 160: SD.lHeight = 352
Set BFSf(PL) = DD.CreateSurface(SD)
CK.low = 0: CK.high = 0
BFSf(PL).SetColorKey DDCKEY_SRCBLT, CK
If Not WM Then BFSf(PL).SetPalette MPal
BFSf(PL).BltColorFill RC0, 0
'テキスト（オフスクリーン）の作成
SD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
SD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
SD.lWidth = 160: SD.lHeight = 144
Set StrSf(PL) = DD.CreateSurface(SD)
CK.low = 0: CK.high = 0
StrSf(PL).SetColorKey DDCKEY_SRCBLT, CK
If Not WM Then StrSf(PL).SetPalette MPal
StrSf(PL).SetFont Picture1.Font
StrSf(PL).BltColorFill RC0, 0
StrMsgInit
StrMsgBT = StrMsgB
StrMsgJ = ""

Rush1(PL) = False
Liv1(PL) = 3
Sc1(PL) = 0: SB1(PL) = 0: CB1(PL) = 1: Li1(PL) = 0: Lv1(PL) = VSLv(TrM, SLv1(PL)): NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50))
NF1(PL) = 0: BA1(PL) = 0: BWC1(PL) = 0: WCa1(PL) = False: DCa1(PL) = False
LBA1(PL) = 0
Hit1(PL) = 0: Dmg1(PL) = 0
VSR1(PL) = 0
CPos1(PL) = 1
InpL1(PL) = False: InpR1(PL) = False: InpLL1(PL) = 0: InpRR1(PL) = 0: InpLLL1(PL) = 0: InpRRR1(PL) = 0
InpU1(PL) = False: InpD1(PL) = False: InpUU1(PL) = 0: InpDD1(PL) = 0
InpA1(PL) = False: InpB1(PL) = False: InpS1(PL) = False: InpAA1(PL) = 0: InpBB1(PL) = 0: InpSS1(PL) = 1
For I = 0 To 25: For U = 0 To 15: BF1(PL, U, I) = 0: Next U, I
For I = 0 To 22: BF1(PL, 2, I) = 9: BF1(PL, 13, I) = 9: Next I
For I = 3 To 12: BF1(PL, I, 0) = 9: BF1(PL, I, 23) = 9: Next I
For I = 0 To 25: Nx1(PL, I) = BtNx(I): Next I: NxC1(PL) = 25
DBSC1(PL) = 0
Next PL

BGInit

If VSLv(TrM, SLv1(0)) > VSLv(TrM, SLv1(1)) Then LvL = VSLv(TrM, SLv1(0)) Else LvL = VSLv(TrM, SLv1(1))
St = Int((LvL + (LvL > 50) * (LvL - 50)) / 5)
If LvL >= 50 And LvL < 200 Then St = 10
If LvL >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 Then St = 11 - (Not (Fnl Or TA)) * 2
Stt = St
VSNetPlay = False
If BG >= 1 Then BGCr Stt ' And Stt < 12
FOC = 0: FIC = 0: FF = False
For I = 0 To 15: Sou(I).Fre = 22050: Sou(I).Pan = 0: Sou(I).On = False: Next I
EAC = 0: For I = 0 To 159: EA(I).V = False: Next I

If Not NetCont Then
If TrM = 0 Then PlayMusic "BATTLE0", 3072, 76800
If TrM = 1 Then PlayMusic "BATTLE1", 16128, 114432
If TrM = 2 Then PlayMusic "BATTLE2", 4608, 78336
Else
Mut = False
NetCont = False
End If

End If
'----メインループ----
Do..<GEnd

If GFin >= 1 Then
KeyScanAll
If Fade Then Fade = False: ScrFC = 0
'If GFin = 2 And (CsrAA = 1 Or CsrBB = 1) Then ScrFC = 80
If ScrFC = 80 Then GMode = 5 - (Not Par And GFin = 1) * 2: GEnd = True: NetCont = (GMode = 7): If Not NetCont Then PlayMusic vbNullString$
End If
If GFin < 2 Then

If Pau <> 0 Then 'ポーズ
If Pau = -1 Then
KeyScanAll

If StrRsv < 2 Then '受信
Do
If DP.GetMessageCount(PlID) > 0 Then Set DPM = DP.Receive(JID, PlID, DPRECEIVE_ALL): NetMsg = DPM.ReadLong: Set DPM = Nothing Else NetMsg = -1
Loop While StrRsv = 1 And NetMsg < -14 And NetMsg > -13 And DP.GetMessageCount(PlID) > 0
Else 'テキストメッセージ受信
If DP.GetMessageCount(PlID) > 0 Then
Set DPM = DP.Receive(JID, PlID, DPRECEIVE_ALL): StrMsgJ = DPM.ReadString: Set DPM = Nothing: StrRsv = 0: StrMsgDraw 1: StrMsgLogAdd "<: " + StrMsgJ
For PL = 0 To 1
If PT(PL) = 1 Then RR = PL
Next PL
Sou(0).Pan = RR * 1000 - 500: Sou(0).On = True
End If
End If

If Not Par And (NetMsg = 2147483647 Or ParReq) Then 'NEXT,攻撃ブロック情報送信
Set DPM = DP.CreateMessage: DPM.WriteLong 2147483647: DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing
Set DPM = DP.CreateMessage: DPM.WriteLong NetQ + TrM * 6: DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing
For I = 0 To 110
Set DPM = DP.CreateMessage: DPM.WriteLong ParBtNx(I): DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing
Next I
For I = 0 To 1
Set DPM = DP.CreateMessage: DPM.WriteLong ParDBS(I): DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing
Next I
ParReq = False
NetMsg = -1
End If

If Not Par And (NetMsg >= -8 And NetMsg <= -3) Then '親参加・自カーソルステータス送信
NetQQ = -3 - NetMsg
For PL = 0 To 1
If PT(PL) = -1 Then RR = PL
Next PL
PT(RR) = 1: Sou(0).Pan = RR * 1000 - 500: Sou(0).On = True
Set DPM = DP.CreateMessage: DPM.WriteLong (CPos1(1 - RR) + 1) + (Liv1(1 - RR)) * 5 + SLv1(1 - RR) * 500: DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing
NetMsg = -1
End If

For PL = 0 To 1
If PT(PL) >= 0 Then
If PT(PL) = 0 Then 'キーボード・パッド
If StrEdit Then 'テキストメッセージ編集
StrMsgEdit PL * 1000 - 500
If SMTp Then Set DPM = DP.CreateMessage: DPM.WriteLong -10: DP.Send PlID, JID, DPSEND_DEFAULT, DPM: Set DPM = Nothing
If SMMv Then Set DPM = DP.CreateMessage: DPM.WriteLong -11: DP.Send PlID, JID, DPSEND_DEFAULT, DPM: Set DPM = Nothing
If SMdl Then Set DPM = DP.CreateMessage: DPM.WriteLong -12: DP.Send PlID, JID, DPSEND_DEFAULT, DPM: Set DPM = Nothing
If Not StrEdit Then
If StrMsgB <> "" Then
Set DPM = DP.CreateMessage: DPM.WriteLong -13: DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing
Set DPM = DP.CreateMessage: DPM.WriteString StrMsgB: DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing
StrMsgLogAdd ">: " + StrMsgB
Else
Set DPM = DP.CreateMessage: DPM.WriteLong -14: DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing
StrMsgB = StrMsgBT
End If
StrMsgDraw 0
End If
Else '一般設定
RR = False
If CPos1(PL) >= 0 And CsrUU = 1 Then CPos1(PL) = CPos1(PL) - 1 - (CPos1(PL) <= 0) * 4: RR = True: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
If CPos1(PL) >= 0 And CsrDD = 1 Then CPos1(PL) = CPos1(PL) + 1 + (CPos1(PL) >= 3) * 4: RR = True: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
If CPos1(PL) = 1 Then
If (CsrLL = 1 Or CsrLL = 15) And SLv1(PL) > 0 Then SLv1(PL) = SLv1(PL) - 1: Lv1(PL) = VSLv(TrM, SLv1(PL)): NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50)): RR = True: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
If (CsrRR = 1 Or CsrRR = 15) And SLv1(PL) < 2 - (TrM > 0) Then If SLv1(PL) <= 0 Or ScLv(TrM) >= VSLv(TrM, SLv1(PL) + 1) Then SLv1(PL) = SLv1(PL) + 1: Lv1(PL) = VSLv(TrM, SLv1(PL)): NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50)): RR = True: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
End If
If CPos1(PL) = 2 Then
If (CsrLL = 1 Or CsrLL = 15) And Liv1(PL) > 1 Then Liv1(PL) = Liv1(PL) - 1: RR = True: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
If (CsrRR = 1 Or CsrRR = 15) And Liv1(PL) < 10 - 5 * ((ScLiv(TrM) >= 1) + (ScLiv(TrM) >= 5) + (TmLiv(TrM) >= 1) + (TmLiv(TrM) >= 3)) Then Liv1(PL) = Liv1(PL) + 1: RR = True: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
End If
If PT(1 - PL) >= 0 And CPos1(PL) >= 0 And CsrSS = 1 Then
CPos1(PL) = -1: RR = True: Sou(0).Pan = PL * 1000 - 500: Sou(0).On = True
Else
If PT(1 - PL) >= 0 And CPos1(PL) = 0 And CsrAA = 1 Then
CPos1(PL) = -1: RR = True: Sou(0).Pan = PL * 1000 - 500: Sou(0).On = True
End If
End If
If PT(1 - PL) >= 0 And RR Then Set DPM = DP.CreateMessage: DPM.WriteLong (CPos1(PL) + 1) + (Liv1(PL)) * 5 + SLv1(PL) * 500: DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing
If PT(1 - PL) >= 0 And CPos1(PL) = 3 And CsrAA = 1 Then StrEdit = True: StrMsgInit: Set DPM = DP.CreateMessage: DPM.WriteLong -9: DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing: Sou(0).Pan = PL * 1000 - 500: Sou(0).On = True 'テキストメッセージ編集開始
End If
Else '対戦相手
If NetMsg >= 0 Then '通常
CPos1(PL) = (NetMsg Mod 5) - 1: Liv1(PL) = Int(NetMsg / 5) Mod 100: SLv1(PL) = Int(NetMsg / 500) Mod 4
Lv1(PL) = VSLv(TrM, SLv1(PL)): NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50))
If CPos1(PL) = -1 Then Sou(0).Pan = PL * 1000 - 500: Sou(0).On = True Else Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True
End If
If NetMsg = -9 Then StrRsv = 1: NetMsg = -1: Sou(0).Pan = PL * 1000 - 500: Sou(0).On = True 'テキストメッセージ受信準備
If NetMsg = -10 Then NetMsg = -1: Sou(1).Pan = PL * 1000 - 500: Sou(1).On = True 'テキストメッセージタイプ音
If NetMsg = -11 Then NetMsg = -1: Sou(5).Pan = PL * 1000 - 500: Sou(5).On = True 'テキストメッセージカーソル音
If NetMsg = -12 Then NetMsg = -1: Sou(12).Pan = PL * 1000 - 500: Sou(12).On = True 'テキストメッセージデリート音
If NetMsg = -13 Then StrRsv = 2: NetMsg = -1 'テキストメッセージ受信開始
If NetMsg = -14 Then StrRsv = 0: NetMsg = -1: Sou(12).Pan = RR * 1000 - 500: Sou(12).On = True 'テキストメッセージ空文字
End If
End If
Next PL
RR1 = False
For I = 0 To 1
If PT(I) < 0 Or CPos1(I) >= 0 Then RR1 = True
Next I
If Not RR1 Then NetMsg = 0: RsvNetMsg = -1: Pau = 0: For I = 0 To 1: SLiv1(I) = Liv1(I): VSLivC1(I) = 0: Next I: Sou(8).Fre = 22050: Sou(8).On = True: If NetQQ > NetQ Then NetQQ = NetQ

End If
Else 'ゲームメイン
For PL = 0 To 1
'キー入力
DIDK.GetDeviceStateKeyboard KS
Select Case PT(PL)
Case 0 'キーボード・パッド
InpL1(PL) = KS.Key(DIK_LEFT) > 0 Or KS.Key(KArwL) > 0
InpR1(PL) = KS.Key(DIK_RIGHT) > 0 Or KS.Key(KArwR) > 0
InpU1(PL) = KS.Key(DIK_UP) > 0 Or KS.Key(KArwU) > 0
InpD1(PL) = KS.Key(DIK_DOWN) > 0 Or KS.Key(KArwD) > 0
InpA1(PL) = KS.Key(DIK_SPACE) > 0 Or KS.Key(KBL) > 0
InpB1(PL) = KS.Key(DIK_RETURN) > 0 Or KS.Key(DIK_NUMPADENTER) > 0 Or KS.Key(KBR) > 0
InpS1(PL) = KS.Key(DIK_TAB) > 0 Or KS.Key(KBS) > 0
For I = 0 To JC - 1
On Error Resume Next
JS(I) = JS0
DIDJ(I).Poll: DIDJ(I).GetDeviceStateJoystick JS(I)
On Error GoTo CE
InpL1(PL) = InpL1(PL) Or (JS(I).X < 16384 And ((PArw(I) Mod 2) >= 1 Or JS(I).Y >= 16384 And JS(I).Y <= 49152))
InpR1(PL) = InpR1(PL) Or (JS(I).X > 49152 And ((PArw(I) Mod 2) >= 1 Or JS(I).Y >= 16384 And JS(I).Y <= 49152))
InpU1(PL) = InpU1(PL) Or (JS(I).Y < 16384 And ((PArw(I) Mod 4) >= 2 Or JS(I).X >= 16384 And JS(I).X <= 49152))
InpD1(PL) = InpD1(PL) Or (JS(I).Y > 49152 And ((PArw(I) Mod 4) >= 2 Or JS(I).X >= 16384 And JS(I).X <= 49152))
InpA1(PL) = InpA1(PL) Or JS(I).buttons(PBL(I)) > 64
InpB1(PL) = InpB1(PL) Or JS(I).buttons(PBR(I)) > 64
InpS1(PL) = InpS1(PL) Or JS(I).buttons(PBS(I)) > 64
Next I
End Select

If InpL1(PL) And InpR1(PL) Then InpL1(PL) = False: InpR1(PL) = False
If InpU1(PL) And InpD1(PL) Then InpU1(PL) = False: InpD1(PL) = False
If InpL1(PL) Then InpLL1(PL) = InpLL1(PL) + 1 + (InpLL1(PL) > 0): InpLLL1(PL) = InpLLL1(PL) + 1 + (InpLLL1(PL) >= 15 + (TrM > 0) * 5) Else InpLL1(PL) = 0: InpLLL1(PL) = 0
If InpR1(PL) Then InpRR1(PL) = InpRR1(PL) + 1 + (InpRR1(PL) > 0): InpRRR1(PL) = InpRRR1(PL) + 1 + (InpRRR1(PL) >= 15 + (TrM > 0) * 5) Else InpRR1(PL) = 0: InpRRR1(PL) = 0
If InpU1(PL) Then InpUU1(PL) = InpUU1(PL) + 1 + (InpUU1(PL) >= 1) Else InpUU1(PL) = 0
If InpD1(PL) Then InpDD1(PL) = InpDD1(PL) + 1 + (InpDD1(PL) >= 1) Else InpDD1(PL) = 0
If InpA1(PL) Then InpAA1(PL) = InpAA1(PL) + 1 + (InpAA1(PL) >= 1) Else InpAA1(PL) = 0
If InpB1(PL) Then InpBB1(PL) = InpBB1(PL) + 1 + (InpBB1(PL) >= 1) Else InpBB1(PL) = 0
If InpS1(PL) Then InpSS1(PL) = InpSS1(PL) + 1 + (InpSS1(PL) >= 2) Else InpSS1(PL) = 0

'If Not OfBt And VSR > 2 And InpSS1(PL) = 1 And Pau = 0 Then Pau = PL + 1: KeyInit: CPos = 0

Next PL

'同時決着判定
'VSLiv = True
'For PL = 0 To 1
'If Liv1(PL) > 1 Or (Liv1(PL) = 1 And (BA1(PL) <> 9 Or BWC1(PL) > 0)) Then VSLiv = False
'Next PL

'メッセージ受信
For PL = 0 To 1
If PT(PL) = 1 Then
Do
RR = DP.GetMessageCount(PlID)
If RsvNetMsg = -1 Or RsvNetMsg <= -104 Then If RR > 0 Then Set DPM = DP.Receive(JID, PlID, DPRECEIVE_ALL): RsvNetMsg = DPM.ReadLong: Set DPM = Nothing
If RsvNetMsg = 2147483647 Then ParReq = True: RsvNetMsg = -1
If RsvNetMsg <= -104 Then NetBX = (-RsvNetMsg - 104) Mod 16: NetBY = Int((-RsvNetMsg - 104) / 16) Mod 24: NetBM = Int((-RsvNetMsg - 104) / 384) Mod 4
Loop While RR > 0 And RsvNetMsg <= -104
If RsvNetMsg <= -104 Then RsvNetMsg = -1
If RsvNetMsg >= 0 And NetMsg = -1 Then NetMsg = RsvNetMsg: RsvNetMsg = -1
End If
Next PL

For PL = 0 To 1
'ブロック動作
BAA1(PL) = BA1(PL)
If BAA1(PL) = 0 Then 'スタート
If InpSS1(PL) = 1 Then InpSS1(PL) = 2: If ScLiv(2) > 0 Then Rush1(PL) = Not Rush1(PL): Sou(0).Pan = 0: Sou(0).On = True
BWC1(PL) = BWC1(PL) + 1: If BWC1(PL) >= 192 Then BWC1(PL) = 0: WCa1(PL) = (TrM = 2): DCa1(PL) = True: BA1(PL) = 1: VSNetPlay = True
End If
If BAA1(PL) = 1 Then '時間待ち
If BWC1(PL) = 0 Then
If PT(PL) = 0 Then DmC1(PL) = Dmg1(PL) * (-(CB1(PL) = 0))
If PT(PL) = 0 And DmC1(PL) > 0 Then Sou(11).Pan = PL * 1000 - 500: Sou(11).On = True: DBSC1(PL) = DBSC1(PL) + 1 + (DBSC1(PL) >= 9) * 10: If DmC1(PL) > 4 Then DmC1(PL) = 4
If PT(PL) = 0 And CB1(PL) = 0 Then Set DPM = DP.CreateMessage: DPM.WriteLong BX1(PL) + BY1(PL) * 16 + BM1(PL) * 384 + DmC1(PL) * 1536: DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing
If PT(PL) = 1 Then DmC1(PL) = Int(NetMsg / 1536) * (-(CB1(PL) = 0)): If DmC1(PL) > 0 Then Sou(11).Pan = PL * 1000 - 500: Sou(11).On = True: DBSC1(PL) = DBSC1(PL) + 1 + (DBSC1(PL) >= 9) * 10
End If
If TrM < 2 Then
If BWC1(PL) < DmC1(PL) Then
Dmg1(PL) = Dmg1(PL) - 1
For I = 1 To 21
For U = 3 To 12
BF1(PL, U, I) = BF1(PL, U, I + 1)
Next U, I
For I = 3 To 12
BF1(PL, I, 22) = 8 * (-(I - 3 <> DBS(DBSC1(PL))))
Next I
With Src
.Left = 0: .Top = 16: .Right = .Left + 160: .Bottom = .Top + 336
End With
BFSf(PL).BltFast 0, 0, BFSf(PL), Src, DDBLTFAST_WAIT
With Des
.Left = 0: .Top = 336: .Right = .Left + 160: .Bottom = .Top + 16
End With
BFSf(PL).BltColorFill Des, 0
For I = 0 To 9
If I <> DBS(DBSC1(PL)) Then
With Src
.Left = 112: .Top = 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
BFSf(PL).BltFast I * 16, 336, BCSf, Src, DDBLTFAST_WAIT
End If
Next I
End If
Else
If BWC1(PL) = 0 And DmC1(PL) > 0 Then
Dmg1(PL) = Dmg1(PL) - DmC1(PL)
For I = 1 To 23 - DmC1(PL)
For U = 3 To 12
BF1(PL, U, I) = BF1(PL, U, I + DmC1(PL))
Next U, I
For I = 23 - DmC1(PL) To 22: For U = 3 To 12
BF1(PL, U, I) = 8 * (-(U - 3 <> DBS(DBSC1(PL))))
Next U, I
With Src
.Left = 0: .Top = DmC1(PL) * 16: .Right = 160: .Bottom = 352
End With
BFSf(PL).BltFast 0, 0, BFSf(PL), Src, DDBLTFAST_WAIT
With Des
.Left = 0: .Top = 352 - DmC1(PL) * 16: .Right = 160: .Bottom = 352
End With
BFSf(PL).BltColorFill Des, 0
For I = 1 To DmC1(PL): For U = 0 To 9
If U <> DBS(DBSC1(PL)) Then
With Src
.Left = 112: .Top = 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
BFSf(PL).BltFast U * 16, 352 - I * 16, BCSf, Src, DDBLTFAST_WAIT
End If
Next U, I
End If
End If
If BWC1(PL) = 0 Then BT1(PL) = Nx1(PL, 0): BX1(PL) = 6: BY1(PL) = -(BT1(PL) > 0): BYF1(PL) = BY1(PL): BM1(PL) = 0: CWC1(PL) = 0: CCWC1(PL) = 0: If PT(PL) = 1 Then NetBX = BX1(PL): NetBY = BY1(PL): NetBM = BM1(PL) Else NetPBX = BX1(PL): NetPBY = BY1(PL): NetPBM = BM1(PL): NetQC = 2 ^ (5 - NetQQ)
'If PT(PL) = 5 And BWC1(PL) >= -(TrM < 2) * 4 And BWC1(PL) < 4 - (TrM < 2) * 4 Then CPAI BWC1(PL) + (TrM < 2) * 4
BWC1(PL) = BWC1(PL) + 1
If TrM = 2 And Not WCa1(PL) And InpUU1(PL) = 1 Then WCa1(PL) = True: DCa1(PL) = False
If Not DCa1(PL) And (TrM > 0 And InpUU1(PL) = 1 Or InpDD1(PL) = 1) Then DCa1(PL) = True: InpUU1(PL) = 2: InpDD1(PL) = 2
If TrM = 2 Then
If WCa1(PL) And BWC1(PL) > 0 And BWC1(PL) < 15 And (InpLL1(PL) = 1 Or InpRR1(PL) = 1 Or InpUU1(PL) = 1 Or InpDD1(PL) = 1 Or InpAA1(PL) = 1 Or InpBB1(PL) = 1) Then BWC1(PL) = 15: InpLLL1(PL) = InpLLL1(PL) + 3 + (InpLLL1(PL) > 7) * (InpLLL1(PL) - 7): InpRRR1(PL) = InpRRR1(PL) + 3 + (InpRRR1(PL) > 7) * (InpRRR1(PL) - 7)
If PT(PL) = 1 Then BWC1(PL) = 15
End If
If BWC1(PL) >= 20 + (PT(PL) = 1) * 10 + (TrM > 0) * 5 Then
BA1(PL) = 2: BAA1(PL) = 2
If PT(PL) = 1 Then NetMsg = -1
End If
End If

'If BAA1(PL) = 11 Then 'ネット待機中
'If NetMsg >= 0 Then '一般
'BX1(PL) = NetMsg Mod 16: BY1(PL) = Int(NetMsg / 16) Mod 24: BM1(PL) = Int(NetMsg / 384) Mod 4: BYF1(PL) = -(BT1(PL) > 0 Or BM1(PL) = 1 Or BM1(PL) = 3)
'ChN1(PL) = 0
'For I = 0 To 3
'If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) > 0 Then ChN1(PL) = 1
'Next I
'If ChN1(PL) = 0 Then
'Sou(3).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500: Sou(3).On = True
'Sou(2).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500: Sou(2).Fre = 22050 / (((BY1(PL) - 1) * 0.055) + 1): Sou(2).On = True
'End If
'BA1(PL) = 2: BAA1(PL) = 2
'End If
'End If

If BAA1(PL) = 2 Then '初期設定
Ch1(PL) = 0: Chh1(PL) = 0: BSC1(PL) = 0: AC1(PL) = 0
WCa1(PL) = False: DCa1(PL) = False
If Lv1(PL) >= 50 Then Inc1(PL) = 6000 Else Inc1(PL) = LS(Lv1(PL)): If Inc1(PL) <= 0 Then Inc1(PL) = 1
ASC1(PL) = 30 + (Lv1(PL) >= 50 And Lv1(PL) < 200) * Int((Lv1(PL) - 50) / 10) + (Lv1(PL) >= 200 And Lv1(PL) < 250) * Int((Lv1(PL) - 200) / 5) + (Lv1(PL) >= 250 And Lv1(PL) < 300) * (10 + Int((Lv1(PL) - 250) / 10)) + (Lv1(PL) >= 300 And Lv1(PL) < 500) * (15 + Int((Lv1(PL) - 300) / 50)) + (Lv1(PL) >= 500 And Lv1(PL) < 1000) * 19 + (Lv1(PL) >= 1000) * 20
For I = 0 To 24: Nx1(PL, I) = Nx1(PL, I + 1): Next I
NxC1(PL) = NxC1(PL) + 1 + (NxC1(PL) >= 776) * 777: Nx1(PL, 25) = BtNx(NxC1(PL))
BA1(PL) = 3: BAA1(PL) = 3
End If

If BAA1(PL) = 3 Then 'アクティブ
RushC1(PL) = -Rush1(PL) * 19
For J = 0 To -Rush1(PL) * 19
If PT(PL) = 0 Then '自分
BMove
If BM1(PL) <> BMM1(PL) Then Sou(1).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500
If BX1(PL) <> BMX1(PL) Then Sou(5).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500
Else 'ネット相手
ChN1(PL) = False
BYF1(PL) = BY1(PL)
If NetBX <> BX1(PL) Then BX1(PL) = NetBX: Sou(5).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500: Sou(5).On = True
If NetBM <> BM1(PL) Then BM1(PL) = NetBM: Sou(1).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500: Sou(1).On = True
If NetBY <> BY1(PL) Then
BY1(PL) = NetBY
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1) + 1) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) > 0 Then Sou(3).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500: Sou(3).On = True
End If
If NetMsg >= 0 Then '一般
BX1(PL) = NetMsg Mod 16: BY1(PL) = Int(NetMsg / 16) Mod 24: BM1(PL) = Int(NetMsg / 384) Mod 4
If NetBX <> BX1(PL) Then Sou(5).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500: Sou(5).On = True
If NetBM <> BM1(PL) Then Sou(1).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500: Sou(1).On = True
If NetBY <> BY1(PL) Then Sou(3).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500: Sou(3).On = True
ChN1(PL) = True
BA1(PL) = 4
End If
If BY1(PL) > BYF1(PL) Then BYF1(PL) = BYF1(PL) + 1
End If
Ch1(PL) = 0 'ミス判定
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) > 0 Then Ch1(PL) = 1
Next I
If PT(PL) = 1 And Not ChN1(PL) Or Ch1(PL) = 0 Then 'ミスなし
If PT(PL) = 0 Then '自分
BYF1(PL) = BY1(PL) '下移動
BSC1(PL) = BSC1(PL) + Inc1(PL)
If BSC1(PL) <= 240 And InpDD1(PL) = 1 Then BSC1(PL) = 240
If BSC1(PL) <= 6000 And InpUU1(PL) = 1 And TrM > 0 Then BSC1(PL) = 6000
While BSC1(PL) >= 240
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1) + 1) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then
BY1(PL) = BY1(PL) + 1: BSC1(PL) = BSC1(PL) - 240: AC1(PL) = 0
Else
BSC1(PL) = 239
End If
Wend
If BY1(PL) > BYF1(PL) Then BYF1(PL) = BYF1(PL) + 1
Ch1(PL) = 0
For I = 0 To 3
If BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1) + 1) > 0 Then Ch1(PL) = 1
Next I
If Ch1(PL) = 0 Then AC1(PL) = 0 Else AC1(PL) = AC1(PL) + 1: Sou(3).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500: Sou(3).On = Sou(3).On Or (AC1(PL) = 1): If (InpDD1(PL) = 1 Or (TrM > 0 And InpUU1(PL) = 1)) And AC1(PL) >= -(InpDD1(PL) = 1) * (6 - Int(Lv1(PL) / 5)) * (1 - Rush1(PL) * 19) And AC1(PL) <= 30 * (1 - Rush1(PL) * 19) Then AC1(PL) = 30 * (1 - Rush1(PL) * 19): WCa1(PL) = (InpUU1(PL) = 1): DCa1(PL) = (InpUU1(PL) = 1 Or InpDD1(PL) = 1)
InpUU1(PL) = InpUU1(PL) - (AC1(PL) >= 30 * (1 - Rush1(PL) * 19) And InpUU1(PL) = 1): InpDD1(PL) = InpDD1(PL) - (AC1(PL) >= 30 * (1 - Rush1(PL) * 19) And InpDD1(PL) = 1)
If BSC1(PL) >= 239 And AC1(PL) >= ASC1(PL) * (1 - Rush1(PL) * 19) Then RushC1(PL) = J: BA1(PL) = 4: Sou(2).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500: Sou(2).Fre = 22050 / (((BY1(PL) - 1) * 0.055) + 1): Sou(2).On = True
If PT(PL) = 0 And NetQQ > 0 Then
If NetQC > 0 And J = -Rush1(PL) * 19 Then NetQC = NetQC - 1
If NetQC <= 0 And (BX1(PL) <> NetPBX Or BY1(PL) <> NetPBY Or BM1(PL) <> NetPBM) Then NetPBX = BX1(PL): NetPBY = BY1(PL): NetPBM = BM1(PL): Set DPM = DP.CreateMessage: DPM.WriteLong -104 - (NetPBX + NetPBY * 16 + NetPBM * 384): DP.Send PlID, JID, DPSEND_DEFAULT, DPM: Set DPM = Nothing: NetQC = 2 ^ (5 - NetQQ)
End If
Else
If ChN1(PL) Then Sou(2).Pan = BX1(PL) * 60 - 330 + PL * 1000 - 500: Sou(2).Fre = 22050 / (((BY1(PL) - 1) * 0.055) + 1): Sou(2).On = True
End If
Else 'ミス
RushC1(PL) = J
For I = 0 To 3
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = BT1(PL) + 1
With Src
.Left = BT1(PL) * 16: .Top = 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
BFSf(PL).BltFast (BX1(PL) + B(BT1(PL), BM1(PL), I, 0)) * 16 - 48, (BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) * 16 - 16, BCSf, Src, DDBLTFAST_WAIT
Next I
BWC1(PL) = 0: BA1(PL) = 9 - (PT(PL) = 0 And Liv1(PL) <= 1) * 3
If PT(PL) = 0 And Liv1(PL) <= 1 Then Set DPM = DP.CreateMessage: DPM.WriteLong -2: DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing
NetDmg1(PL) = Dmg1(PL)
If PT(PL) = 0 Then Set DPM = DP.CreateMessage: DPM.WriteLong BX1(PL) + BY1(PL) * 16 + BM1(PL) * 384 + NetDmg1(PL) * 1536: DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing
End If
RBX1(PL, J) = BX1(PL): RBY1(PL, J) = BY1(PL): RBM1(PL, J) = BM1(PL): RAC1(PL, J) = AC1(PL): RBSC1(PL, J) = BSC1(PL): RInc1(PL, J) = Inc1(PL): RBYF1(PL, J) = BYF1(PL)
If RushC1(PL) < -Rush1(PL) * 19 Then J = -Rush1(PL) * 19
Next J
End If

If BAA1(PL) = 4 Then 'セット
RushC1(PL) = 0: J = RushC1(PL)
RBX1(PL, J) = BX1(PL): RBY1(PL, J) = BY1(PL): RBM1(PL, J) = BM1(PL): RAC1(PL, J) = AC1(PL): RBSC1(PL, J) = BSC1(PL): RInc1(PL, J) = Inc1(PL): RBYF1(PL, J) = BYF1(PL)
For I = 0 To 3
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = BT1(PL) + 1
With Src
.Left = BT1(PL) * 16: .Top = 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
BFSf(PL).BltFast (BX1(PL) + B(BT1(PL), BM1(PL), I, 0)) * 16 - 48, (BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) * 16 - 16, BCSf, Src, DDBLTFAST_WAIT
Next I
BA1(PL) = 5
End If
If BAA1(PL) = 5 Then '判定
Chh1(PL) = 0
For I = 0 To 3
Ch1(PL) = 0
For U = 3 To 12
If BY1(PL) + I > 0 And BY1(PL) + I < 23 And BF1(PL, U, BY1(PL) + I) > 0 Then Ch1(PL) = Ch1(PL) + 1
Next U
If Ch1(PL) >= 10 Then FE1(PL, Chh1(PL)) = BY1(PL) + I: Chh1(PL) = Chh1(PL) + 1
Next I
If Chh1(PL) > 0 Then DCa1(PL) = DCa1(PL) Or (TrM < 2): BA1(PL) = 6: BAA1(PL) = 6 Else CB1(PL) = 0: BWC1(PL) = 0: Dmg1(1 - PL) = Dmg1(1 - PL) + Hit1(PL): Hit1(PL) = 0: BA1(PL) = 1
End If
If BAA1(PL) = 6 Then 'ブロック消去
Sou(6 - (Chh1(PL) >= 4)).Pan = PL * 1000 - 500: Sou(6 - (Chh1(PL) >= 4)).Fre = 22050 * 2 ^ ((CB1(PL) + (CB1(PL) > 9) * (CB1(PL) - 9)) / 12): Sou(6 - (Chh1(PL) >= 4)).On = True
If PT(PL) = 0 Then
Hitt = Chh1(PL) - 1 - (Chh1(PL) = 4) - (CB1(PL) > 0) * (1 - (Chh1(PL) = 4))
If Chh1(PL) < 4 And Dmg1(PL) > 0 Then Dmg1(PL) = Dmg1(PL) - Hitt: Hitt = 0: If Dmg1(PL) < 0 Then Hitt = Hitt - Dmg1(PL): Dmg1(PL) = 0
Hit1(PL) = Hit1(PL) + Hitt
If PT(PL) = 0 Then Set DPM = DP.CreateMessage: DPM.WriteLong BX1(PL) + BY1(PL) * 16 + BM1(PL) * 384 + Hitt * 1536: DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing
Else
Hitt = Chh1(PL) - 1 - (Chh1(PL) = 4) - (CB1(PL) > 0) * (1 - (Chh1(PL) = 4))
RR = Int(NetMsg / 1536): Dmg1(PL) = Dmg1(PL) - Hitt + RR: Hit1(PL) = Hit1(PL) + RR
End If
R = Rnd * 360
For I = 0 To Chh1(PL) - 1
If BAni Then
For U = 0 To 9
With EA(EAC)
.V = 1 - (Hit1(PL) >= 4): .A = BF1(PL, 3 + U, FE1(PL, I)) - 1: .AF = Int(Rnd * -(Hit1(PL) >= 4) * 4)
.X = 112 + PL * 256 + U * 16: .Y = 48 + FE1(PL, I) * 16
.XX = Sin((R + I * 90 + U * 108) * Rg) * (4 + Chh1(PL) * 3): .YY = Cos((R + I * 90 + U * 108) * Rg) * (4 + Chh1(PL) * 3)
If TrM > 0 Then .XXX = .YY / 50 * (((I + U) Mod 2) * (2 * (TrM = 2)) + 1) * ((PL Mod 2) * 2 - 1): .YYY = -.XX / 50 * (((I + U) Mod 2) * (2 * (TrM = 2)) + 1) * ((PL Mod 2) * 2 - 1) Else .XXX = 0: .YYY = 0.2
End With
EAC = EAC + 1 + (EAC >= 159) * 160
Next U
End If
With Des
.Left = 0: .Top = FE1(PL, I) * 16 - 16: .Right = .Left + 160: .Bottom = .Top + 16
End With
BFSf(PL).BltColorFill Des, 0
Next I
BWC1(PL) = -(TrM = 0) * Int(Lv1(PL) / 5) * 3 - (TrM = 1) * (20 + Int(Lv1(PL) / 5)) - (TrM = 2) * 35
CB1(PL) = CB1(PL) + 1 + (CB1(PL) >= 100)
Li1(PL) = Li1(PL) + Chh1(PL): If Lv1(PL) < 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 Then If Li1(PL) >= NL1(PL) Then Sou(4).Pan = PL * 1000 - 500: Sou(4).On = True: Lv1(PL) = Lv1(PL) + 1: NF1(PL) = 65: NL1(PL) = (NL1(PL) + 8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50))
BA1(PL) = 7: BAA1(PL) = 7
End If
If BAA1(PL) = 7 Then '時間待ち
If BWC1(PL) >= 35 - Chh1(PL) Then BWC1(PL) = 0: BA1(PL) = 8: BAA1(PL) = 8 Else BWC1(PL) = BWC1(PL) + 1
End If
If BAA1(PL) = 8 Then 'フィールドずらし
Do
I = BWC1(PL)
For U = FE1(PL, I) To 2 Step -1
For Y = 3 To 12
BF1(PL, Y, U) = BF1(PL, Y, U - 1)
Next Y, U
For U = 3 To 12
BF1(PL, U, 1) = 0
Next U
With Src
.Left = 0: .Top = 0: .Right = .Left + 160: .Bottom = .Top + (FE1(PL, I)) * 16 - 16
End With
BFSf(PL).BltFast 0, 16, BFSf(PL), Src, DDBLTFAST_WAIT
With Des
.Left = 0: .Top = 0: .Right = .Left + 160: .Bottom = .Top + 16
End With
BFSf(PL).BltColorFill Des, 0
BWC1(PL) = BWC1(PL) + 1
Loop While TrM = 2 And BWC1(PL) < Chh1(PL)
If BWC1(PL) = Chh1(PL) Then Sou(13).Pan = PL * 1000 - 500: Sou(13).On = True: BWC1(PL) = 0: BA1(PL) = 1
End If

If BAA1(PL) = 12 Then '相打ち確認
If BWC1(PL) = 0 Then
If RsvNetMsg = -2 Then For I = 0 To 1: VSLivC1(I) = True: Next I
If RsvNetMsg >= -103 And RsvNetMsg <= -3 Then If VSNetPlay Then VSNetPlay = False: StrMsgLogAdd "*: LV" + Mid$(Str$(VSLv(TrM, SLv1(PL))), 2) + "(0/" + Mid$(Str$(SLiv1(PL)), 2) + ") - LV" + Mid$(Str$(VSLv(TrM, SLv1(1 - PL))), 2) + "(" + Mid$(Str$(-RsvNetMsg - 3), 2) + "/" + Mid$(Str$(SLiv1(1 - PL)), 2) + ")"
If RsvNetMsg >= -103 And RsvNetMsg <= -2 Then RsvNetMsg = -1: BWC1(PL) = 1
End If
If BWC1(PL) = 1 Then
BWC1(PL) = 0: BA1(PL) = 9: BAA1(PL) = 9
End If
End If

If BAA1(PL) = 9 Then 'ミス
If BWC1(PL) = 0 Then
If VSLivC1(PL) Or (Liv1(PL) <= 1 And Liv1(1 - PL) <= 0) Then Liv1(PL) = Liv1(PL) + 1
If VSLivC1(PL) Then VSLivC1(PL) = False
If Liv1(PL) > 1 Then Sou(10).Pan = PL * 1000 - 500: Sou(10).On = True Else VSR = VSR + (VSR1(PL) = 0): Sou(9).Pan = PL * 1000 - 500: Sou(9).On = True
Liv1(PL) = Liv1(PL) - 1: If Liv1(PL) < 0 Then Liv1(PL) = 0
Li1(PL) = 0: Lv1(PL) = VSLv(TrM, SLv1(PL)): NL1(PL) = (8 + Int(Lv1(PL) / 10) * (2 - TrM)) * (1 + (Lv1(PL) >= 50))
CB1(PL) = 1
If PT(PL) = 0 Then Dmg1(PL) = Dmg1(PL) - NetDmg1(PL)
If PT(PL) = 1 Then Dmg1(PL) = Dmg1(PL) - Int(NetMsg / 1536)
End If
If BWC1(PL) < 60 Then
If BWC1(PL) >= 16 And BWC1(PL) < 38 Then
For I = 3 To 12: BF1(PL, I, BWC1(PL) - 15) = 0: Next I
With Des
.Left = 0: .Top = (BWC1(PL) - 16) * 16: .Right = .Left + 160: .Bottom = .Top + 16
End With
BFSf(PL).BltColorFill Des, 0
End If
BWC1(PL) = BWC1(PL) + 1
Else
Dmg1(1 - PL) = Dmg1(1 - PL) + Hit1(PL): Hit1(PL) = 0
If Liv1(PL) > 0 Then WCa1(PL) = True: DCa1(PL) = True: BWC1(PL) = 0: BA1(PL) = 1 Else BWC1(PL) = 0: BA1(PL) = 10: BAA1(PL) = 10
End If
End If

If BAA1(PL) = 10 Then 'ゲームオーバー
End If

If Li1(PL) > 10000 Then Li1(PL) = 10000
If Hit1(PL) > 10000 Then Hit1(PL) = 10000
If Dmg1(PL) > 10000 Then Dmg1(PL) = 10000

If RsvNetMsg = -2 And PT(PL) = 0 And BA1(PL) <> 12 Then Set DPM = DP.CreateMessage: DPM.WriteLong -3 - Liv1(PL): DP.Send PlID, JID, DPSEND_GUARANTEED, DPM: Set DPM = Nothing: RsvNetMsg = -1: If VSNetPlay Then VSNetPlay = False: StrMsgLogAdd "*: LV" + Mid$(Str$(VSLv(TrM, SLv1(PL))), 2) + "(" + Mid$(Str$(Liv1(PL)), 2) + "/" + Mid$(Str$(SLiv1(PL)), 2) + ") - LV" + Mid$(Str$(VSLv(TrM, SLv1(1 - PL))), 2) + "(0/" + Mid$(Str$(SLiv1(1 - PL)), 2) + ")"

Next PL
For PL = 0 To 1
If VSR1(PL) = 0 And Liv1(PL) = 0 Then VSR1(PL) = VSR
Next PL
For PL = 0 To 1
If VSR1(PL) = 0 And VSR = 2 Then VSR1(PL) = 1: Sou(8).Fre = 22050: Sou(8).On = True
Next PL
If VSR <= 2 Then '対戦決着
If VSFinC = 1 Then Mut = True
If VSFinC = 199 Then GFin = 1
VSFinC = VSFinC + 1: If VSFinC > 200 Then VSFinC = 200
End If
End If
End If

'背景表示
If VSLv(TrM, SLv1(0)) > VSLv(TrM, SLv1(1)) Then LvL = VSLv(TrM, SLv1(0)) Else LvL = VSLv(TrM, SLv1(1))
BGD TrM, LvL
'ボード表示
For PL = 0 To 1
With Src
.Left = 0: .Top = 0: .Right = .Left + 224: .Bottom = .Top + 368
End With
If Not FS Then BBSf.BltFast 80 + PL * 256, 64, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
If Rush1(PL) Then
With Src
.Left = 32 + RushAniC: .Top = 0: .Right = 192: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 112 + PL * 256, 64, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
With Src
.Left = 32: .Top = 0: .Right = 32 + RushAniC: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 272 + PL * 256 - RushAniC, 64, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next PL
'ダメージ表示
DAni = DAni + 1 + (DAni >= 5) * 6
If GFin < 2 Then
For PL = 0 To 1
If Liv1(PL) > 0 Then
For I = 1 To Dmg1(PL) + (Dmg1(PL) > 22) * (Dmg1(PL) - 22)
With Src
.Left = 224: .Top = ((44 + DAni - I * 2) Mod 6) * 16: .Right = .Left + 32: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 272 + PL * 64, 416 - I * 16, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
For I = Dmg1(PL) + 1 To Dmg1(PL) + Hit1(1 - PL) + (Dmg1(PL) + Hit1(1 - PL) > 22) * (Dmg1(PL) + Hit1(1 - PL) - 22)
With Src
.Left = 224: .Top = 96 + ((44 + DAni - I * 2) Mod 6) * 16: .Right = .Left + 32: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 272 + PL * 64, 416 - I * 16, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
End If
Next PL
End If
'ライフ表示
If GFin < 2 Then
For PL = 0 To 1
For I = 1 To Liv1(PL) + (Liv1(PL) > 14) * (Liv1(PL) - 14)
With Src
.Left = 256: .Top = ((10 + LAni * (1 - (I Mod 2) * 2)) Mod 10) * 32: .Right = .Left + 32: .Bottom = .Top + 32
End With
If Not FS Then BBSf.BltFast 80 + PL * 448, 408 - I * 24, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
Next PL
End If
'フィールド枠線表示
If GFin < 2 Then
For PL = 0 To 1
If InpS1(PL) Or VSR1(PL) = 1 Then
If BA1(PL) > 2 And BA1(PL) < 6 Then
For I = 0 To 3
FL(I) = BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1))
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = BT1(PL) + 1
Next I
End If
For I = 0 To 21
For U = 0 To 8
If BF1(PL, 3 + U, I + 1) = 0 Xor BF1(PL, 4 + U, I + 1) = 0 Then
With Src
 .Left = 160: .Top = 372: .Right = .Left + 2: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 127 + PL * 256 + U * 16, 64 + I * 16, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next U, I
For I = 0 To 20
For U = 0 To 9
If BF1(PL, 3 + U, I + 1) = 0 Xor BF1(PL, 3 + U, I + 2) = 0 Then
With Src
 .Left = 160: .Top = 368: .Right = .Left + 16: .Bottom = .Top + 2
End With
If Not FS Then BBSf.BltFast 112 + PL * 256 + U * 16, 79 + I * 16, BDSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next U, I
If BA1(PL) > 2 And BA1(PL) < 6 Then
For I = 0 To 3
BF1(PL, BX1(PL) + B(BT1(PL), BM1(PL), I, 0), BY1(PL) + B(BT1(PL), BM1(PL), I, 1)) = FL(I)
Next I
End If
End If
Next PL
End If
If Pau = 0 Then
'フィールド表示
If GFin < 2 Then
For PL = 0 To 1
If Not (InpS1(PL) Or VSR1(PL) = 1) Then
With Src
.Left = 0: .Top = 0: .Right = .Left + 160: .Bottom = .Top + 352
End With
If Not FS Then BBSf.BltFast 112 + PL * 256, 64, BFSf(PL), Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
If (Not BAni) And BA1(PL) = 7 Then
For I = 0 To Chh1(PL) - 1
For U = 0 To 9
With Src
If ((80 + BWC1(PL) + Chh1(PL) - (Chh1(PL) = 4) * ((PL Mod 2) * 2 - 1) * (U + I * 2)) Mod 8) < 4 Then .Left = 112: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16 Else .Left = (BF1(PL, U + 3, FE1(PL, I)) - 1) * 16: .Top = -(BF1(PL, U + 3, FE1(PL, I)) = 8) * 16: .Right = .Left + 16: .Bottom = .Top + 16
End With
If Not FS Then BBSf.BltFast 112 + PL * 256 + U * 16, 48 + FE1(PL, I) * 16, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next U
Next I
End If
End If
Next PL
End If
'アクティブブロック残像表示
If GFin < 2 Then
For PL = 0 To 1
If Not (InpS1(PL) Or VSR1(PL) = 1) Then
If Zanzo Then
'If FS Then
'If (Not (BSm And Not Inc1(PL) >= 240 And LAC1(PL) = 0) Or (16 + Int(LBSC1(PL) / 15)) = (16 + Int(BSC1(PL) / 15))) And LBX1(PL) = BX1(PL) And LBY1(PL) = BY1(PL) And LBYF1(PL) = BYF1(PL) And LBM1(PL) = BM1(PL) Then CLB1(PL) = True Else CLB1(PL) = False
'Else
'If LBA1(PL) > 2 And LBA1(PL) < 6 And Not CLB1(PL) Then
'For U = LBYF1(PL) To LBY1(PL)
'For I = 0 To 3
'If LBA1(PL) < 4 Or LBA1(PL) > 5 Or U < LBY1(PL) Then
'With Src
'.Left = LBT1(PL) * 16: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
'End With
'Else
'With Src
'.Left = 112: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
'End With
'End If
'If Not FS Then BBSf.BltFast 64 + PL * 256 + (LBX1(PL) + B(LBT1(PL), LBM1(PL), I, 0)) * 16, 48 + (U + B(LBT1(PL), LBM1(PL), I, 1)) * 16 - (BSm And Not (Zanzo And Inc1(PL) >= 240) And LAC1(PL) = 0) * Int(LBSC1(PL) / 15), BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
'Next I, U
'CLB1(PL) = True
'End If
'End If
'LBX1(PL) = BX1(PL): LBY1(PL) = BY1(PL): LBYF1(PL) = BYF1(PL): LBSC1(PL) = BSC1(PL): LAC1(PL) = AC1(PL): LBT1(PL) = BT1(PL): LBM1(PL) = BM1(PL): LBA1(PL) = BA1(PL)
If FS Then
For J = 0 To -Rush1(PL) * 19
If (Not (BSm And Not RInc1(PL, J) >= 240 And LAC1(PL, J) = 0) Or (16 + Int(LBSC1(PL, J) / 15)) = (16 + Int(RBSC1(PL, J) / 15))) And LBX1(PL, J) = RBX1(PL, J) And LBY1(PL, J) = RBY1(PL, J) And LBYF1(PL, J) = RBYF1(PL, J) And LBM1(PL, J) = RBM1(PL, J) Then CLB1(PL, J) = True Else CLB1(PL, J) = False
Next J
Else
For J = -(Not Zanzo Or BA1(PL) = 5) * LRushC1(PL) To LRushC1(PL)
If LBA1(PL) > 2 And LBA1(PL) < 6 And Not CLB1(PL, J) Then
For U = LBYF1(PL, J) To LBY1(PL, J)
For I = 0 To 3
If LBA1(PL) < 4 Or LBA1(PL) > 5 Or U < LBY1(PL, J) Or J < LRushC1(PL) Then
With Src
.Left = LBT1(PL) * 16: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
End With
Else
With Src
.Left = 112: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
End With
End If
If Not FS Then BBSf.BltFast 64 + PL * 256 + (LBX1(PL, J) + B(LBT1(PL), LBM1(PL, J), I, 0)) * 16, 48 + (U + B(LBT1(PL), LBM1(PL, J), I, 1)) * 16 - (BSm And Not FE And Not (Zanzo And LInc1(PL, J) >= 240) And LAC1(PL, J) = 0) * Int(LBSC1(PL, J) / 15), BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I, U
End If
CLB1(PL, J) = True
Next J
End If
LRushC1(PL) = RushC1(PL): LBA1(PL) = BA1(PL): LBT1(PL) = BT1(PL)
For J = 0 To -Rush1(PL) * 19
LBX1(PL, J) = RBX1(PL, J): LBY1(PL, J) = RBY1(PL, J): LBYF1(PL, J) = RBYF1(PL, J): LBSC1(PL, J) = RBSC1(PL, J): LAC1(PL, J) = RAC1(PL, J): LBM1(PL, J) = RBM1(PL, J): LInc1(PL, J) = RInc1(PL, J)
Next J
End If
End If
Next PL
End If
'アクティブブロック表示
If GFin < 2 Then
For PL = 0 To 1
If Not (InpS1(PL) Or VSR1(PL) = 1) Then
If BA1(PL) > 2 And BA1(PL) < 6 Then
For J = -(Not Zanzo Or BA1(PL) = 5) * RushC1(PL) To RushC1(PL)
If Not Zanzo Then RBYF1(PL, J) = RBY1(PL, J)
For U = RBYF1(PL, J) To RBY1(PL, J)
For I = 0 To 3
If BA1(PL) < 4 Or BA1(PL) > 5 Or U < RBY1(PL, J) Or J < RushC1(PL) Then
With Src
.Left = BT1(PL) * 16: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
End With
Else
With Src
.Left = 112: .Top = 0: .Right = .Left + 16: .Bottom = .Top + 16
End With
End If
If Not FS Then BBSf.BltFast 64 + PL * 256 + (RBX1(PL, J) + B(BT1(PL), RBM1(PL, J), I, 0)) * 16, 48 + (U + B(BT1(PL), RBM1(PL, J), I, 1)) * 16 - (BSm And Not (Zanzo And RInc1(PL, J) >= 240) And RAC1(PL, J) = 0) * Int(RBSC1(PL, J) / 15), BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I, U
Next J
End If
End If
BYF1(PL) = BY1(PL)
RushC1(PL) = 0: J = RushC1(PL)
RBX1(PL, J) = BX1(PL): RBY1(PL, J) = BY1(PL): RBM1(PL, J) = BM1(PL): RAC1(PL, J) = AC1(PL): RBSC1(PL, J) = BSC1(PL): RInc1(PL, J) = Inc1(PL): RBYF1(PL, J) = BYF1(PL)
Next PL
End If
'順位2表示
If GFin < 2 Then
For PL = 0 To 1
If BA1(PL) = 10 Or BA1(PL) = 9 And BWC1(PL) >= 30 And VSR1(PL) = 2 Then StringD 120 + PL * 256, 224, 7, "GAME OVER", False
Next PL
End If
'ミス表示
If GFin < 2 Then
For PL = 0 To 1
If BA1(PL) = 9 Then
For I = 0 To 21
RR1 = BWC1(PL) - I
If RR1 < 0 Then RR1 = 0
If RR1 > 16 Then RR1 = 16
RR2 = BWC1(PL) - I - 22
If RR2 < 0 Then RR2 = 0
If RR2 > 16 Then RR2 = 16
RR = Int(Rnd * 160)
With Src
.Left = RR: .Top = 368 + ((I + Int(CCC)) Mod 7) * 16 + RR2: .Right = 160: .Bottom = .Top + RR1 - RR2
End With
If Not FS Then BBSf.BltFast 112 + PL * 256, 64 + I * 16 + RR2, BDSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 368 + ((I + Int(CCC)) Mod 7) * 16 + RR2: .Right = .Left + RR: .Bottom = .Top + RR1 - RR2
End With
If Not FS Then BBSf.BltFast 272 + PL * 256 - RR, 64 + I * 16 + RR2, BDSf, Src, DDBLTFAST_WAIT
Next I
End If
Next PL
End If
'勝者表示
For PL = 0 To 1
If VSR1(PL) = 1 Then StringD 128 + PL * 256, 224, 11, "WINNER!!", False
Next PL
End If
'消去アニメ表示
EAni
'NEXT表示
For PL = 0 To 1
If MNx < 4 Or BA1(PL) > 0 Then
For I = 0 To MNx - 1 - (MNx = 4) - (MNx = 0 And BA1(PL) <> 3)
With Src
.Left = Int(Nx1(PL, I) / 4) * 64: .Top = 32 + (Nx1(PL, I) Mod 4) * 32: .Right = .Left + 64: .Bottom = .Top + 32
End With
If Not FS Then BBSf.BltFast 160 + PL * 256 + I * 80 * (PL * 2 - 1), 16, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
If MNx = 4 Then
For I = 0 To 8
With Src
.Left = Int(Nx1(PL, 3 + I) / 4) * 64: .Top = 32 + (Nx1(PL, 3 + I) Mod 4) * 32: .Right = .Left + 64: .Bottom = .Top + 32
End With
If Not FS Then BBSf.BltFast PL * 576, 64 + I * 48, BCSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
End If
Else
NxD2
End If
StringD 224 + PL * 120, 16, 9, "NEXT", False
Next PL
'デバイス表示
If GFin < 2 Then
For PL = 0 To 1
If PT(PL) = 0 Then
StringD 144 + PL * 256, 64, 1, HNN, False
Else
StringD 144 + PL * 256, 64, 1, JN, False
End If
Next PL
End If
'レベル表示
If GFin < 2 Then
For PL = 0 To 1
NF1(PL) = NF1(PL) - 1 - (NF1(PL) <= 0)
StringD 224 + PL * 256, 432, 9, "LEVEL", False
TextD 304 + PL * 256, 448, 3 - (NF1(PL) > 0 Or (Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150)) * 8, Lv1(PL), True, 4
Next PL
End If
'ライン表示
If GFin < 2 Then
For PL = 0 To 1
StringD 80 + PL * 256, 432, 9, "LINES", False
TextD 160 + PL * 256, 448, 3 - (Lv1(PL) >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150) * 8, Li1(PL), True, 5
If Lv1(PL) < 30 - (TrM >= 1) * 20 Then
StringD 160 + PL * 256, 448, 4, "/", False
TextD 176 + PL * 256, 448, 3 - (Lv1(PL) >= 29 - (TrM >= 1) * 20 - (TrM = 2) * 150) * 8, NL1(PL), False, 3
End If
Next PL
End If
'ライフ(>14)表示
If GFin < 2 Then
For PL = 0 To 1
If Liv1(PL) > 14 Then TextD 112 + PL * 448, 400, 4 + Int(LAni / 5), Liv1(PL), True, 2
Next PL
End If
If Pau < 0 Then 'テキストメッセージ表示
For PL = 0 To 1
If PT(PL) >= 0 Then If Not FS Then BBSf.BltFast 112 + PL * 256, 264, StrSf(PT(PL)), RC0, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
If GFin < 2 And PT(PL) = 0 Then If StrEdit And SMSel < 0 Then CursorD 104 + PL * 256 + SMX, 264 + SMY, 11, 3
Next PL
End If
If GFin < 2 And Pau = -1 Then '設定表示
For PL = 0 To 1
If PT(PL) >= 0 Then
StringD 140 + PL * 256, 136, 8 - (CPos1(PL) = 0) * 3, "START", False
StringD 140 + PL * 256, 168, 9, "LEVEL", False
TextD 268 + PL * 256, 168, 7 - (CPos1(PL) = 1) * 4, Lv1(PL), True, 3
StringD 140 + PL * 256, 200, 9, "LIVES", False
TextD 268 + PL * 256, 200, 7 - (CPos1(PL) = 2) * 4, Liv1(PL), True, 3
StringD 140 + PL * 256, 232, 8 - (CPos1(PL) = 3 And (PT(PL) <> 0 Or Not StrEdit) And (PT(PL) <> 1 Or StrRsv = 0)) * 3, "MESSAGE", False
If CPos1(PL) >= 0 Then CursorD 118 + PL * 256, 136 + CPos1(PL) * 32, 11, -(CPos1(PL) = 0 Or CPos1(PL) = 3)
Else
If VSA = 0 Then
If CC < 30 Then StringD 152 + PL * 256, 224, 8, "ENTRY", False, 10
Else
If CC < 30 Then StringD 160 + PL * 256, 224, 8, "WAIT", False, 10
End If
End If
Next PL
End If
If Pau > 0 Then 'ポーズ表示
For PL = 0 To 1
If PL = Pau - 1 Then
StringD 140 + PL * 256, 224, 8 - (CPos = 0) * 3, "CONTINUE", False
StringD 140 + PL * 256, 256, 8 - (CPos = 1) * 3, "EXIT", False
CursorD 118 + PL * 256, 224 + CPos * 32, 11, 1
Else
If CC < 30 Then StringD 160 + PL * 256, 224, 9, "WAIT", False
End If
Next PL
End If
'スタート表示
If GFin < 2 And Pau = 0 Then
For PL = 0 To 1
If BA1(PL) = 0 Then
If BWC1(PL) > 12 Then
RR = BWC1(PL) - 12
TextD 184 + PL * 256, 224, 11, 3 - Int(RR / 60), False
For I = 0 To 61 - (RR Mod 60)
If RR < 60 Then CursorD 184 + PL * 256 - Sin(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60)), 224 + Cos(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60) * 0.75), -(RR >= 60) - (RR >= 120) * 7, 0
If RR >= 60 And RR < 120 Then CursorD 184 + PL * 256 - Sin(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 50) * (1 + Int(RR / 60) * 0.125), 224 + Cos(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60) * 0.75), -(RR >= 60) - (RR >= 120) * 7, 0
If RR >= 120 Then CursorD 184 + PL * 256 - Sin(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 65) * (1 + Int(RR / 60)), 224 + Cos(((RR Mod 60) * 72 + (I / (62 - (RR Mod 60)) * 360)) * Rg) * Sqr((RR Mod 60) * 50) * (1 + Int(RR / 60) * 0.125), -(RR >= 60) - (RR >= 120) * 7, 0
Next I
End If
If ScLiv(2) > 0 And PT(PL) = 0 Then
StringD 124 + PL * 256, 96, 9, "RUSH:", False, 8
If Rush1(PL) Then StringD 212 + PL * 256, 96, 11, "ON", False Else StringD 212 + PL * 256, 96, 11, "OFF", False
End If
End If
Next PL
End If

'TextD 0, 0, 0, RsvNetMsg, False
'TextD 0, 16, 0, NetMsg, False
'TextD 0, 40, 0, BA1(0), False
'TextD 40, 40, 0, BA1(1), False
'TextD 0, 0, 0, NetBX, False
'TextD 0, 16, 0, NetBY, False
'TextD 0, 32, 0, NetBM, False

FChange
If NC < 2 Then GFin = 2

Loop

If VSNetPlay Then
VSNetPlay = False
RR = -1
For PL = 0 To 1
If PT(PL) = 0 Then RR = PL
Next PL
If RR >= 0 Then StrMsgLogAdd "?: LV" + Mid$(Str$(VSLv(TrM, SLv1(RR))), 2) + "(" + Mid$(Str$(Liv1(RR)), 2) + "/" + Mid$(Str$(SLiv1(RR)), 2) + ") - LV" + Mid$(Str$(VSLv(TrM, SLv1(1 - RR))), 2) + "(" + Mid$(Str$(Liv1(1 - RR)), 2) + "/" + Mid$(Str$(SLiv1(1 - RR)), 2) + ")"
End If

If GFin = 2 And SsC > 0 Then NetDestroy: NetCreate: NameCreate
ParStC = -1
For I = 1 To 0 Step -1: Set StrSf(I) = Nothing: Set BFSf(I) = Nothing: Next I
Set BCSf = Nothing
Set BDSf = Nothing
Exit Sub

CDisp:
FullScrErr = True
Resume Next
Exit Sub

CE:
GFin = 2
Resume Next
End Sub

Sub DebugMenu() 'デバッグメニュー
On Error GoTo CE
GEnd = False
Tit = False
Fade = True: ScrFC = 80
PlayMusic vbNullString$, -1
Do..<GEnd
'背景
If Not FS Then BBSf.BltColorFill RC0, 0
'エラー表示
If CC < 24 Then StringD 32, 32, 0, "ERROR!!", False
StringD 32, 80, 1, "[Esc]: RESTORE", False
StringD 32, 112, 1, "[Alt]+[F4]: END", False
FChange
Loop
Exit Sub

CE:
'Beep
GMode = 0
End Sub

Sub Opening() 'オープニング
On Error GoTo CE
GEnd = False
Tic = DX.TickCount: E1 = Tic - Int(Tic / 65536) * 65536: TE1 = E1: TEE = 0
KeyInitAll
If CmdFile = vbNullString$ Then

If RecCh Then '一般スタート

I = 0
OpB(I).X = 0: OpB(I).Y = 0: OpB(I + 1).X = 1: OpB(I + 1).Y = 0: OpB(I + 2).X = 1: OpB(I + 2).Y = 1: OpB(I + 3).X = 2: OpB(I + 3).Y = 1: I = I + 4 'A
OpB(I).X = 2: OpB(I).Y = 0: OpB(I + 1).X = 3: OpB(I + 1).Y = 0: OpB(I + 2).X = 4: OpB(I + 2).Y = 0: OpB(I + 3).X = 5: OpB(I + 3).Y = 0: I = I + 4 'B
OpB(I).X = 6: OpB(I).Y = 0: OpB(I + 1).X = 7: OpB(I + 1).Y = 0: OpB(I + 2).X = 6: OpB(I + 2).Y = 1: OpB(I + 3).X = 7: OpB(I + 3).Y = 1: I = I + 4 'C
OpB(I).X = 8: OpB(I).Y = 0: OpB(I + 1).X = 8: OpB(I + 1).Y = 1: OpB(I + 2).X = 7: OpB(I + 2).Y = 2: OpB(I + 3).X = 8: OpB(I + 3).Y = 2: I = I + 4 'D
OpB(I).X = 9: OpB(I).Y = 0: OpB(I + 1).X = 10: OpB(I + 1).Y = 0: OpB(I + 2).X = 9: OpB(I + 2).Y = 1: OpB(I + 3).X = 10: OpB(I + 3).Y = 1: I = I + 4 'E
OpB(I).X = 11: OpB(I).Y = 0: OpB(I + 1).X = 11: OpB(I + 1).Y = 1: OpB(I + 2).X = 12: OpB(I + 2).Y = 1: OpB(I + 3).X = 12: OpB(I + 3).Y = 2: I = I + 4 'F
OpB(I).X = 12: OpB(I).Y = 0: OpB(I + 1).X = 13: OpB(I + 1).Y = 0: OpB(I + 2).X = 14: OpB(I + 2).Y = 0: OpB(I + 3).X = 15: OpB(I + 3).Y = 0: I = I + 4 'G
OpB(I).X = 0: OpB(I).Y = 1: OpB(I + 1).X = 0: OpB(I + 1).Y = 2: OpB(I + 2).X = 0: OpB(I + 2).Y = 3: OpB(I + 3).X = 1: OpB(I + 3).Y = 3: I = I + 4 'H
OpB(I).X = 3: OpB(I).Y = 1: OpB(I + 1).X = 4: OpB(I + 1).Y = 1: OpB(I + 2).X = 4: OpB(I + 2).Y = 2: OpB(I + 3).X = 4: OpB(I + 3).Y = 3: I = I + 4 'I
OpB(I).X = 5: OpB(I).Y = 1: OpB(I + 1).X = 5: OpB(I + 1).Y = 2: OpB(I + 2).X = 6: OpB(I + 2).Y = 2: OpB(I + 3).X = 5: OpB(I + 3).Y = 3: I = I + 4 'J
OpB(I).X = 13: OpB(I).Y = 1: OpB(I + 1).X = 14: OpB(I + 1).Y = 1: OpB(I + 2).X = 15: OpB(I + 2).Y = 1: OpB(I + 3).X = 15: OpB(I + 3).Y = 2: I = I + 4 'K
OpB(I).X = 1: OpB(I).Y = 2: OpB(I + 1).X = 2: OpB(I + 1).Y = 2: OpB(I + 2).X = 3: OpB(I + 2).Y = 2: OpB(I + 3).X = 2: OpB(I + 3).Y = 3: I = I + 4 'L
OpB(I).X = 9: OpB(I).Y = 2: OpB(I + 1).X = 8: OpB(I + 1).Y = 3: OpB(I + 2).X = 9: OpB(I + 2).Y = 3: OpB(I + 3).X = 10: OpB(I + 3).Y = 3: I = I + 4 'M
OpB(I).X = 10: OpB(I).Y = 2: OpB(I + 1).X = 11: OpB(I + 1).Y = 2: OpB(I + 2).X = 11: OpB(I + 2).Y = 3: OpB(I + 3).X = 12: OpB(I + 3).Y = 3: I = I + 4 'N
OpB(I).X = 13: OpB(I).Y = 2: OpB(I + 1).X = 14: OpB(I + 1).Y = 2: OpB(I + 2).X = 13: OpB(I + 2).Y = 3: OpB(I + 3).X = 14: OpB(I + 3).Y = 3: I = I + 4 'O
OpB(I).X = 3: OpB(I).Y = 3: OpB(I + 1).X = 3: OpB(I + 1).Y = 4: OpB(I + 2).X = 4: OpB(I + 2).Y = 4: OpB(I + 3).X = 5: OpB(I + 3).Y = 4: I = I + 4 'P
OpB(I).X = 6: OpB(I).Y = 3: OpB(I + 1).X = 7: OpB(I + 1).Y = 3: OpB(I + 2).X = 7: OpB(I + 2).Y = 4: OpB(I + 3).X = 8: OpB(I + 3).Y = 4: I = I + 4 'Q
OpB(I).X = 15: OpB(I).Y = 3: OpB(I + 1).X = 14: OpB(I + 1).Y = 4: OpB(I + 2).X = 15: OpB(I + 2).Y = 4: OpB(I + 3).X = 14: OpB(I + 3).Y = 5: I = I + 4 'R
OpB(I).X = 0: OpB(I).Y = 4: OpB(I + 1).X = 0: OpB(I + 1).Y = 5: OpB(I + 2).X = 1: OpB(I + 2).Y = 5: OpB(I + 3).X = 1: OpB(I + 3).Y = 6: I = I + 4 'S
OpB(I).X = 1: OpB(I).Y = 4: OpB(I + 1).X = 2: OpB(I + 1).Y = 4: OpB(I + 2).X = 2: OpB(I + 2).Y = 5: OpB(I + 3).X = 3: OpB(I + 3).Y = 5: I = I + 4 'T
OpB(I).X = 6: OpB(I).Y = 4: OpB(I + 1).X = 4: OpB(I + 1).Y = 5: OpB(I + 2).X = 5: OpB(I + 2).Y = 5: OpB(I + 3).X = 6: OpB(I + 3).Y = 5: I = I + 4 'U
OpB(I).X = 9: OpB(I).Y = 4: OpB(I + 1).X = 10: OpB(I + 1).Y = 4: OpB(I + 2).X = 11: OpB(I + 2).Y = 4: OpB(I + 3).X = 12: OpB(I + 3).Y = 4: I = I + 4 'V
OpB(I).X = 13: OpB(I).Y = 4: OpB(I + 1).X = 13: OpB(I + 1).Y = 5: OpB(I + 2).X = 12: OpB(I + 2).Y = 6: OpB(I + 3).X = 13: OpB(I + 3).Y = 6: I = I + 4 'W
OpB(I).X = 7: OpB(I).Y = 5: OpB(I + 1).X = 8: OpB(I + 1).Y = 5: OpB(I + 2).X = 9: OpB(I + 2).Y = 5: OpB(I + 3).X = 9: OpB(I + 3).Y = 6: I = I + 4 'X
OpB(I).X = 10: OpB(I).Y = 5: OpB(I + 1).X = 11: OpB(I + 1).Y = 5: OpB(I + 2).X = 12: OpB(I + 2).Y = 5: OpB(I + 3).X = 11: OpB(I + 3).Y = 6: I = I + 4 'Y
OpB(I).X = 15: OpB(I).Y = 5: OpB(I + 1).X = 15: OpB(I + 1).Y = 6: OpB(I + 2).X = 15: OpB(I + 2).Y = 7: OpB(I + 3).X = 15: OpB(I + 3).Y = 8: I = I + 4 'Z
OpB(I).X = 0: OpB(I).Y = 6: OpB(I + 1).X = 0: OpB(I + 1).Y = 7: OpB(I + 2).X = 1: OpB(I + 2).Y = 7: OpB(I + 3).X = 2: OpB(I + 3).Y = 7: I = I + 4 'a
OpB(I).X = 2: OpB(I).Y = 6: OpB(I + 1).X = 3: OpB(I + 1).Y = 6: OpB(I + 2).X = 4: OpB(I + 2).Y = 6: OpB(I + 3).X = 5: OpB(I + 3).Y = 6: I = I + 4 'b
OpB(I).X = 6: OpB(I).Y = 6: OpB(I + 1).X = 7: OpB(I + 1).Y = 6: OpB(I + 2).X = 5: OpB(I + 2).Y = 7: OpB(I + 3).X = 6: OpB(I + 3).Y = 7: I = I + 4 'c
OpB(I).X = 8: OpB(I).Y = 6: OpB(I + 1).X = 7: OpB(I + 1).Y = 7: OpB(I + 2).X = 8: OpB(I + 2).Y = 7: OpB(I + 3).X = 9: OpB(I + 3).Y = 7: I = I + 4 'd
OpB(I).X = 10: OpB(I).Y = 6: OpB(I + 1).X = 10: OpB(I + 1).Y = 7: OpB(I + 2).X = 11: OpB(I + 2).Y = 7: OpB(I + 3).X = 11: OpB(I + 3).Y = 8: I = I + 4 'e
OpB(I).X = 14: OpB(I).Y = 6: OpB(I + 1).X = 12: OpB(I + 1).Y = 7: OpB(I + 2).X = 13: OpB(I + 2).Y = 7: OpB(I + 3).X = 14: OpB(I + 3).Y = 7: I = I + 4 'f
OpB(I).X = 3: OpB(I).Y = 7: OpB(I + 1).X = 4: OpB(I + 1).Y = 7: OpB(I + 2).X = 3: OpB(I + 2).Y = 8: OpB(I + 3).X = 4: OpB(I + 3).Y = 8: I = I + 4 'g
OpB(I).X = 0: OpB(I).Y = 8: OpB(I + 1).X = 0: OpB(I + 1).Y = 9: OpB(I + 2).X = 1: OpB(I + 2).Y = 9: OpB(I + 3).X = 0: OpB(I + 3).Y = 10: I = I + 4 'h
OpB(I).X = 1: OpB(I).Y = 8: OpB(I + 1).X = 2: OpB(I + 1).Y = 8: OpB(I + 2).X = 2: OpB(I + 2).Y = 9: OpB(I + 3).X = 3: OpB(I + 3).Y = 9: I = I + 4 'i
OpB(I).X = 5: OpB(I).Y = 8: OpB(I + 1).X = 6: OpB(I + 1).Y = 8: OpB(I + 2).X = 7: OpB(I + 2).Y = 8: OpB(I + 3).X = 5: OpB(I + 3).Y = 9: I = I + 4 'j
OpB(I).X = 8: OpB(I).Y = 8: OpB(I + 1).X = 9: OpB(I + 1).Y = 8: OpB(I + 2).X = 10: OpB(I + 2).Y = 8: OpB(I + 3).X = 10: OpB(I + 3).Y = 9: I = I + 4 'k
OpB(I).X = 12: OpB(I).Y = 8: OpB(I + 1).X = 13: OpB(I + 1).Y = 8: OpB(I + 2).X = 14: OpB(I + 2).Y = 8: OpB(I + 3).X = 13: OpB(I + 3).Y = 9: I = I + 4 'l
OpB(I).X = 4: OpB(I).Y = 9: OpB(I + 1).X = 3: OpB(I + 1).Y = 10: OpB(I + 2).X = 4: OpB(I + 2).Y = 10: OpB(I + 3).X = 5: OpB(I + 3).Y = 10: I = I + 4 'm
OpB(I).X = 6: OpB(I).Y = 9: OpB(I + 1).X = 7: OpB(I + 1).Y = 9: OpB(I + 2).X = 6: OpB(I + 2).Y = 10: OpB(I + 3).X = 7: OpB(I + 3).Y = 10: I = I + 4 'n
OpB(I).X = 8: OpB(I).Y = 9: OpB(I + 1).X = 9: OpB(I + 1).Y = 9: OpB(I + 2).X = 9: OpB(I + 2).Y = 10: OpB(I + 3).X = 10: OpB(I + 3).Y = 10: I = I + 4 'o
OpB(I).X = 11: OpB(I).Y = 9: OpB(I + 1).X = 12: OpB(I + 1).Y = 9: OpB(I + 2).X = 11: OpB(I + 2).Y = 10: OpB(I + 3).X = 12: OpB(I + 3).Y = 10: I = I + 4 'p
OpB(I).X = 14: OpB(I).Y = 9: OpB(I + 1).X = 13: OpB(I + 1).Y = 10: OpB(I + 2).X = 14: OpB(I + 2).Y = 10: OpB(I + 3).X = 13: OpB(I + 3).Y = 11: I = I + 4 'q
OpB(I).X = 15: OpB(I).Y = 9: OpB(I + 1).X = 15: OpB(I + 1).Y = 10: OpB(I + 2).X = 14: OpB(I + 2).Y = 11: OpB(I + 3).X = 15: OpB(I + 3).Y = 11: I = I + 4 'r
OpB(I).X = 1: OpB(I).Y = 10: OpB(I + 1).X = 2: OpB(I + 1).Y = 10: OpB(I + 2).X = 0: OpB(I + 2).Y = 11: OpB(I + 3).X = 1: OpB(I + 3).Y = 11: I = I + 4 's
OpB(I).X = 8: OpB(I).Y = 10: OpB(I + 1).X = 6: OpB(I + 1).Y = 11: OpB(I + 2).X = 7: OpB(I + 2).Y = 11: OpB(I + 3).X = 8: OpB(I + 3).Y = 11: I = I + 4 't
OpB(I).X = 2: OpB(I).Y = 11: OpB(I + 1).X = 3: OpB(I + 1).Y = 11: OpB(I + 2).X = 4: OpB(I + 2).Y = 11: OpB(I + 3).X = 5: OpB(I + 3).Y = 11: I = I + 4 'u
OpB(I).X = 9: OpB(I).Y = 11: OpB(I + 1).X = 10: OpB(I + 1).Y = 11: OpB(I + 2).X = 11: OpB(I + 2).Y = 11: OpB(I + 3).X = 12: OpB(I + 3).Y = 11: I = I + 4 'x
For I = 0 To 47
RR = Int(I + Rnd * (48 - I))
For U = 0 To 3
OpBRR = OpB(RR * 4 + U): OpB(RR * 4 + U) = OpB(I * 4 + U): OpB(I * 4 + U) = OpBRR
Next U, I
R = Rnd * 360
For I = 0 To 47: OpBPos(I) = R + I * 78: Next I

Tit = True: TC = 0: Mut = True
VM = 0
TrM = 0: Trn = False: TA = False: Fnl = False: OfBt = False
Fade = True: ScrFC = 80
GMode = 3: GEnd = True
PlayMusic "OPENING", -1
Else 'レコードなしスタート
PlayMusic vbNullString$: GMode = 5: GEnd = True
End If
Else 'ビデオスタート
Tit = False: Mut = False
VLoad CmdFile
CmdFile = vbNullString$
Fade = False: ScrFC = 0: GMode = 1: Trn = False: VM = 2
GEnd = True
End If

Exit Sub
CE:
'Beep
GMode = 0
End Sub

Sub Title() 'タイトル
On Error GoTo CE
GEnd = False
Tit = False: TTL = 1800: Mut = False
TitBGC = 0: TitBGF = 0: TitBGFC = 0: TitY = -160
Fade = True: ScrFC = 80: TFC = 0

'背景パレット初期化
If Not WM Then
For I = 0 To 127
With MPa(I)
.red = 0: .green = 0: .blue = 0
End With
Next I
If Not FS Then MPal.SetEntries 0, 256, MPa
End If

'ブロック（オフスクリーン）の作成
BCCr 1

BGCrTit '背景の作成

'テキスト（オフスクリーン）の作成
SD.lFlags = DDSD_CAPS Or DDSD_WIDTH Or DDSD_HEIGHT
SD.ddsCaps.lCaps = DDSCAPS_OFFSCREENPLAIN Or -WM * DDSCAPS_SYSTEMMEMORY
SD.lWidth = 160: SD.lHeight = 144
Set StrSf(0) = DD.CreateSurface(SD)
CK.low = 0: CK.high = 0
StrSf(0).SetColorKey DDCKEY_SRCBLT, CK
If Not WM Then StrSf(0).SetPalette MPal
StrSf(0).SetFont Picture1.Font
StrSf(0).BltColorFill RC0, 0
StrMsgInit
StrMsgBT = StrMsgB

For I = 0 To 15: Sou(I).Fre = 22050: Sou(I).Pan = 0: Sou(I).On = False: Next I

OBMax = 2
If ScLv(1) >= 25 Then OBMax = 3
If ScLv(1) >= 30 Then OBMax = 4
If ScLv(1) >= 50 Then OBMax = 5
If ScLv(2) >= 35 Then OBMax = 6
If ScLv(2) >= 200 Then OBMax = 7

TxMx = 108
For I = 0 To TxMx: Tx(I) = "": Next I
'GAME START
If RecCh Then Tx(0) = "TRAINING"
If RecCh Then Tx(1) = "TRIAL"
If RecCh Then Tx(2) = "2P BATTLE"
If RecCh Then Tx(3) = "4P BATTLE"
If RecCh Then Tx(4) = "NET BATTLE"

'TRIAL
If RecCh Then Tx(5) = "SCORE ATTACK"
If RecCh And ScLv(0) >= 30 Or ScLv(1) >= 50 Or ScLv(2) >= 200 Then Tx(6) = "TIME ATTACK"
If RecCh And DTCl >= 900 Then Tx(7) = "FINAL"
'TRAINING,SCORE ATTACK,TIME ATTACK,VS2P,VS4P
If RecCh Then Tx(8) = TrMN(0)
If RecCh Then Tx(9) = TrMN(1)
If RecCh And ScLiv(0) > 0 And ScLiv(1) > 0 Then Tx(10) = TrMN(2)
'OPTION
Tx(11) = "MIDI:"
Tx(12) = "SOUND VOLUME:"
Tx(13) = "SOUND STEREO:"
Tx(14) = "EFFECT"
Tx(15) = "SYSTEM"
Tx(16) = "KEY CONFIG"
If RecCh Then Tx(17) = "NETWORK"
Tx(17 - RecCh) = "ObjMidi"
Tx(18 - RecCh) = "ObjSVol"
Tx(19 - RecCh) = "ObjSPan"
'EFFECT
Tx(21) = "BACKGROUND:"
Tx(22) = "BLOCK SPLASH:"
Tx(23) = "MOVE SMOOTH:"
Tx(24) = "LOCUS:"
Tx(25) = "BLOCK GRAPHIC:"
Tx(26) = "FIELD GRAPHIC:"
Tx(27) = "NEXT BLOCKS:"
Tx(28) = "CLOCK:"
Tx(29) = "ObjBG"
Tx(30) = "ObjAni"
Tx(31) = "ObjBSm"
Tx(32) = "ObjZanzo"
Tx(33) = "ObjBC"
Tx(34) = "ObjBD"
Tx(35) = "ObjMNx"
Tx(36) = "ObjClkC"
'PERFORMANCE
Tx(37) = "DISPLAY MODE:"
Tx(38) = "FRAME SKIP:"
Tx(39) = "SCREEN MODE:"
Tx(40) = "BG MEMORY:"
Tx(41) = "ObjTV"
Tx(42) = "ObjAS"
Tx(43) = "ObjWM"
Tx(44) = "ObjBGMem"
'PAD
If JC >= 1 Then
Tx(45) = "ROTATE LEFT / ENTER:"
Tx(46) = "ROTATE RIGHT / CANCEL:"
If ScLiv(2) > 0 Then
Tx(47) = "RUSH / START / STOP:"
Else
Tx(47) = "START / STOP:"
End If
Tx(48) = "CURSOR:"
Tx(49) = "EXIT"
Tx(50) = "ObjPBL"
Tx(51) = "ObjPBR"
Tx(52) = "ObjPBS"
Tx(53) = "ObjPArwK"
End If
'KEY CONFIG
If JC >= 1 Then Tx(54) = "KEYBOARD"
If JC >= 1 Then Tx(55) = "PAD(A)"
If JC >= 2 Then Tx(56) = "PAD(B)"
If JC >= 3 Then Tx(57) = "PAD(C)"
If JC >= 4 Then Tx(58) = "PAD(D)"
'OFFCIAL MATCH
If RecCh And OBMax >= 1 Then Tx(59) = "PLAYER1 DEVICE:"
If RecCh And OBMax >= 1 Then Tx(60) = "PLAYER2 DEVICE:"
If RecCh And OBMax >= 1 Then Tx(61) = "BEGINNER (NORMAL  LV=3   LIV=2)"
If RecCh And OBMax >= 2 Then Tx(62) = "STANDARD (NORMAL  LV=10  LIV=3)"
If RecCh And OBMax >= 3 Then Tx(63) = "SENIOR   (HARD    LV=25  LIV=3)"
If RecCh And OBMax >= 4 Then Tx(64) = "EXPERT   (HARD    LV=30  LIV=3)"
If RecCh And OBMax >= 5 Then Tx(65) = "MASTER   (HARD    LV=50  LIV=5)"
If RecCh And OBMax >= 6 Then Tx(66) = "NINJA    (ADVANCE LV=35  LIV=5)"
If RecCh And OBMax >= 7 Then Tx(67) = "PRINCE   (ADVANCE LV=200 LIV=5)"
If RecCh And OBMax >= 1 Then Tx(61 + OBMax) = "ObjDev1"
If RecCh And OBMax >= 1 Then Tx(62 + OBMax) = "ObjDev2"
'RANKING
Tx(68) = ""
If RecCh Then Tx(71) = "ObjTrM"
'VIDEO
Tx(72) = "PAGE:"
Tx(73) = "______"
Tx(74) = "______"
Tx(75) = "______"
Tx(76) = "______"
Tx(77) = "______"
Tx(78) = "______"
Tx(79) = "______"
Tx(80) = "______"
Tx(81) = "ObjVPC"
'FINAL
If RecCh And FnLv >= 300 Then Tx(82) = "JOKER"
If RecCh And FnLv >= 300 Then Tx(83) = "FURTHEST"
'NETWORK CREATE
If RecCh Then Tx(84) = "CONNECT:"
If RecCh Then Tx(85) = "HANDLE NAME:"
If RecCh Then Tx(86) = "TRANSMISSION:"
If RecCh Then Tx(87) = "FIELD POSITION:"
If RecCh Then Tx(88) = "JAPANESE:"

If RecCh Then Tx(89) = "ObjNet"
If RecCh Then Tx(90) = "ObjNetnm"
If RecCh Then Tx(91) = "ObjNetQ"
If RecCh Then Tx(92) = "ObjNetFldLR"
If RecCh Then Tx(93) = "ObjJpn"
'KEY CONFIG
If RecCh Then Tx(94) = "ROTATE LEFT / ENTER:" Else Tx(94) = "ENTER:"
If RecCh Then Tx(95) = "ROTATE RIGHT / CANCEL:" Else Tx(95) = "CANCEL:"
If RecCh Then
If ScLiv(2) > 0 Then
Tx(96) = "RUSH / START / STOP:"
Else
Tx(96) = "START / STOP:"
End If
Else
Tx(96) = "STOP:"
End If
Tx(97) = "LEFT:"
Tx(98) = "RIGHT:"
Tx(99) = "UP:"
Tx(100) = "DOWN:"
Tx(101) = "EXIT"
Tx(102) = "ObjKBOK"
Tx(103) = "ObjKBCa"
Tx(104) = "ObjKBS"
Tx(105) = "ObjKArwL"
Tx(106) = "ObjKArwR"
Tx(107) = "ObjKArwU"
Tx(108) = "ObjKArwD"

MnMx = 22
MnDB(0).StN = 0: MnDB(0).MNs = 0: MnDB(0).Obs = 0: MnDB(0).X = 0: MnDB(0).Y = 0: MnDB(0).Cap = ""
MnDB(1).StN = 0: MnDB(1).MNs = 4: MnDB(1).Obs = 0: MnDB(1).X = 112: MnDB(1).Y = 128: MnDB(1).Cap = "GAME START"
MnDB(2).StN = 5: MnDB(2).MNs = 2 - (DTCl >= 900): MnDB(2).Obs = 0: MnDB(2).X = 128: MnDB(2).Y = 128: MnDB(2).Cap = "GAME START> TRIAL(" + Mid$(Str$(Int(DTCl / 10)), 2) + "." + Mid$(Str$(DTCl Mod 10), 2) + "%)"
MnDB(3).StN = 8: MnDB(3).MNs = 2 - (ScLiv(0) > 0 And ScLiv(1) > 0): MnDB(3).Obs = 0: MnDB(3).X = 128: MnDB(3).Y = 128: MnDB(3).Cap = "GAME START> TRAINING"
MnDB(4).StN = 8: MnDB(4).MNs = 2 - (ScLiv(0) > 0 And ScLiv(1) > 0): MnDB(4).Obs = 0: MnDB(4).X = 144: MnDB(4).Y = 128: MnDB(4).Cap = "GAME START> TRIAL> SCORE ATTACK"
MnDB(5).StN = 8: MnDB(5).MNs = 2 - (ScLiv(0) > 0 And ScLiv(1) > 0 And ScLv(2) >= 200): MnDB(5).Obs = 0: MnDB(5).X = 144: MnDB(5).Y = 128: MnDB(5).Cap = "GAME START> TRIAL> TIME ATTACK"
MnDB(6).StN = 8: MnDB(6).MNs = 2 - (ScLiv(0) > 0 And ScLiv(1) > 0): MnDB(6).Obs = 0: MnDB(6).X = 128: MnDB(6).Y = 128: MnDB(6).Cap = "GAME START> FREE BATTLE 2P"
MnDB(7).StN = 8: MnDB(7).MNs = 2 - (ScLiv(0) > 0 And ScLiv(1) > 0): MnDB(7).Obs = 0: MnDB(7).X = 128: MnDB(7).Y = 128: MnDB(7).Cap = "GAME START> FREE BATTLE 4P"
MnDB(8).StN = 11: MnDB(8).MNs = 6 - RecCh: MnDB(8).Obs = 3: MnDB(8).X = 112: MnDB(8).Y = 128: MnDB(8).Cap = "OPTIONS"
MnDB(9).StN = 21: MnDB(9).MNs = 8: MnDB(9).Obs = 8: MnDB(9).X = 128: MnDB(9).Y = 128: MnDB(9).Cap = "OPTIONS> VISUAL EFFECT"
MnDB(10).StN = 37: MnDB(10).MNs = 4: MnDB(10).Obs = 4: MnDB(10).X = 128: MnDB(10).Y = 128: MnDB(10).Cap = "OPTIONS> SYSTEM PERFORMANCE"
MnDB(11).StN = 45: MnDB(11).MNs = 5: MnDB(11).Obs = 4: MnDB(11).X = 144: MnDB(11).Y = 128: MnDB(11).Cap = "OPTIONS> KEY CONFIG> PAD"
MnDB(12).StN = 59: MnDB(12).MNs = 2 + OBMax: MnDB(12).Obs = 2: MnDB(12).X = 112: MnDB(12).Y = 128: MnDB(12).Cap = "OFFICIAL BATTLE"
MnDB(13).StN = 5: MnDB(13).MNs = 2 - (DTCl >= 900): MnDB(13).Obs = 0: MnDB(13).X = 128: MnDB(13).Y = 128: MnDB(13).Cap = "RANKING"
MnDB(14).StN = 70: MnDB(14).MNs = 1: MnDB(14).Obs = 1: MnDB(14).X = 448: MnDB(14).Y = 64: MnDB(14).Cap = "RANKING> SCORE ATTACK"
MnDB(15).StN = 70: MnDB(15).MNs = 1: MnDB(15).Obs = 1: MnDB(15).X = 448: MnDB(15).Y = 64: MnDB(15).Cap = "RANKING> TIME ATTACK"
MnDB(16).StN = 70: MnDB(16).MNs = 1: MnDB(16).Obs = 1: MnDB(16).X = 448: MnDB(16).Y = 64: MnDB(16).Cap = "RANKING> FINAL"
MnDB(17).StN = 54: MnDB(17).MNs = 1 + JC: MnDB(17).Obs = 0: MnDB(17).X = 128: MnDB(17).Y = 128: MnDB(17).Cap = "OPTIONS> KEY CONFIG"
MnDB(18).StN = 72: MnDB(18).MNs = 9: MnDB(18).Obs = 1: MnDB(18).X = 112: MnDB(18).Y = 160: MnDB(18).Cap = "VIDEO"
MnDB(19).StN = 82: MnDB(19).MNs = 2: MnDB(19).Obs = 0: MnDB(19).X = 144: MnDB(19).Y = 128: MnDB(19).Cap = "GAME START> TRIAL> FINAL"
MnDB(20).StN = 84: MnDB(20).MNs = 5: MnDB(20).Obs = 5: MnDB(20).X = 128: MnDB(20).Y = 128: MnDB(20).Cap = "OPTIONS> NETWORK CREATE"
MnDB(21).StN = 8: MnDB(21).MNs = 2 - (ScLiv(0) > 0 And ScLiv(1) > 0): MnDB(21).Obs = 0: MnDB(21).X = 128: MnDB(21).Y = 128: MnDB(21).Cap = "GAME START> NETWORK BATTLE"
MnDB(22).StN = 94: MnDB(22).MNs = 8: MnDB(22).Obs = 7: MnDB(22).X = 144: MnDB(22).Y = 128: MnDB(22).Cap = "OPTIONS> KEY CONFIG> KEYBOARD"

HN(0) = HNN
HNMax = 0
For I = 0 To 79
RR = False
For U = 0 To HNMax
If I >= 0 And I <= 29 Then
RRR = Left$(SRnk(I).Nm, InStr(SRnk(I).Nm, "\") - 1 - (InStr(SRnk(I).Nm, "\") = 0) * 7)
End If
If I >= 30 And I <= 59 Then
RRR = Left$(TRnk(I - 30).Nm, InStr(TRnk(I - 30).Nm, "\") - 1 - (InStr(TRnk(I - 30).Nm, "\") = 0) * 7)
End If
If I >= 60 And I <= 69 Then
RRR = Left$(FRnk(I - 60).Nm, InStr(FRnk(I - 60).Nm, "\") - 1 - (InStr(FRnk(I - 60).Nm, "\") = 0) * 7)
End If
If I >= 70 And I <= 79 Then
RRR = Left$(LRnk(I - 70).Nm, InStr(LRnk(I - 70).Nm, "\") - 1 - (InStr(LRnk(I - 70).Nm, "\") = 0) * 7)
End If
If Left$(RRR, 1) = "_" Or RRR = Left$(HN(U), InStr(HN(U), " ") - 1 - (InStr(HN(U), " ") = 0) * 7) Then RR = True
Next U
If Not RR Then
U = HNMax
While U >= 0 And RRR < HN(U + (U < 0) * U)
HN(U + 1) = HN(U)
U = U - 1
Wend
HN(U + 1) = RRR
HNMax = HNMax + 1
End If
Next I
HNNum = 0
For I = 0 To HNMax
If HN(I) = HNN Then HNNum = I
Next I
StrEdit = False

ReDim VFNL(0): ReDim VFD(0): VFC = 0: VPC = 0
EAC = 0: For I = 0 To 159: EA(I).V = False: Next I
TCPOS = -(Not RecCh) * 3: TtA = -(Not RecCh) * 144
CPos = 0
Mn = 0
MnCapLen = 0
TCX = 256 + Sin((TtA + TCPOS * 72) * Rg) * 960: TCY = 176 - Cos(TtA * Rg) * 960
For I = 0 To 4
With TtPos(I)
.X = 256 + Sin((TtA + I * 72 - 180) * Rg) * 480 * 2.5 ^ ((I * 2) Mod 5): .Y = 176 - Cos((TtA + I * 72 - 180) * Rg) * 480 * 2.5 ^ ((I * 2) Mod 5)
End With
Next I
CsrMove
CX = CXX: CY = CYY
TxMove 0, TxMx
For I = 0 To TxMx: TxPos(I).X = TxPos(I).XX: TxPos(I).Y = TxPos(I).YY: Next I
KeyInitAll
Tic = DX.TickCount: E1 = Tic - Int(Tic / 65536) * 65536

'----メインループ----
Do..<GEnd
KeyScanAll
Mnn = Mn: CCPos = CPos
If Mnn = 0 Then 'タイトル
If RecCh Then
TTL = TTL - 1
If CsrL Or CsrR Or CsrU Or CsrD Or CsrA Or CsrB Or CsrS Or EscC >= 1 Then TTL = 1800
If TTL <= 0 Then Fade = False: ScrFC = 0: GMode = 6: Mn = -1
End If
RR = (TtA + TCPOS * 72) Mod 360
If CsrLL = 1 Then
If RR >= 306 Or RR < 90 Then TCPOS = TCPOS - 1 - (TCPOS <= 0) * 5: Sou(5).On = True
If RR >= 90 And RR < 234 Then TCPOS = TCPOS + 1 + (TCPOS >= 4) * 5: Sou(5).On = True
End If
If CsrRR = 1 Then
If RR >= 126 And RR < 270 Then TCPOS = TCPOS - 1 - (TCPOS <= 0) * 5: Sou(5).On = True
If RR >= 270 Or RR < 54 Then TCPOS = TCPOS + 1 + (TCPOS >= 4) * 5: Sou(5).On = True
End If
If CsrUU = 1 Then
If RR >= 36 And RR < 180 Then TCPOS = TCPOS - 1 - (TCPOS <= 0) * 5: Sou(5).On = True
If RR >= 180 And RR < 324 Then TCPOS = TCPOS + 1 + (TCPOS >= 4) * 5: Sou(5).On = True
End If
If CsrDD = 1 Then
If RR >= 216 Or RR < 0 Then TCPOS = TCPOS - 1 - (TCPOS <= 0) * 5: Sou(5).On = True
If RR >= 0 And RR < 144 Then TCPOS = TCPOS + 1 + (TCPOS >= 4) * 5: Sou(5).On = True
End If
If CsrAA = 1 Then
TitBGF = TCPOS + 1
Select Case TCPOS
Case 0 'START
If RecCh Then MnDB(1).MNs = 4 - Net: Mn = 1: CPos = 1: Sou(0).On = True
Case 1 'OPTION
Mn = 8: CPos = 3: Sou(0).On = True
Case 2 'RANKING
If RecCh Then If ScLv(0) >= 30 Or ScLv(1) >= 50 Or ScLv(2) >= 200 Then Mn = 13: CPos = 0: Sou(0).On = True Else Mn = 14: CPos = 0: TA = False: TrM = 0: Sou(0).On = True
Case 3 'VIDEO
VFC = 0
VFN = XDir(VidDir + "*.*", vbHidden)
While VFN <> vbNullString$
If VCheck(VidDir + VFN) Then
ReDim Preserve VFNL(VFC)
ReDim Preserve VFD(VFC * 5 + 4)
VFS = 0: I = VFC - 1
While VFS = 0 And I >= 0
RR1 = RvInStr(VFN, ".") - 1: If RR1 >= 0 Then RRR1 = UCase$(Left$(VFN, RR1) + " " + Mid$(VFN, RR1 + 1)) Else RRR1 = UCase$(VFN + " .")
RR2 = RvInStr(VFNL(I), ".") - 1: If RR2 >= 0 Then RRR2 = UCase$(Left$(VFNL(I), RR2) + " " + Mid$(VFNL(I), RR2 + 1)) Else RRR2 = UCase$(VFNL(I) + " .")
If RRR1 >= RRR2 Then VFS = I + 1
I = I - 1
Wend
For I = VFC - 1 To VFS Step -1
VFNL(I + 1) = VFNL(I): For U = 0 To 4: VFD((I + 1) * 5 + U) = VFD(I * 5 + U): Next U
Next I
VFNL(VFS) = VFN: VFD(VFS * 5) = VDTrM: VFD(VFS * 5 + 1) = VDTA: VFD(VFS * 5 + 2) = VDFnl: VFD(VFS * 5 + 3) = VDSc: VFD(VFS * 5 + 4) = VDLi
VFC = VFC + 1
End If
VFN = Dir$
Wend
Mn = 18: CPos = 1: VPC = 0: Sou(0).On = True
Case 4 'MATCH
If RecCh Then
If JC > 0 Or CPMax >= 7 Then
Mn = 12
For I = JC To 0 Step -1
If CsrAA1(I) = 1 Then
OBPT(0) = I
OBPT(1) = I + 1: If OBPT(1) > JC Then OBPT(1) = -(JC >= 2) - (OBPT(0) = 0) * 12
End If
Next I
CPos = 1
Sou(0).On = True
End If
End If
End Select
End If
TCXX = 256 + Sin((TtA + TCPOS * 72) * Rg) * 160: TCYY = 176 - Cos((TtA + TCPOS * 72) * Rg) * 160
TCX = TCX + (TCXX - TCX) * 0.1: TCY = TCY + (TCYY - TCY) * 0.1
For I = 0 To 4
With TtPos(I)
.XX = 256 + Sin((TtA + I * 72) * Rg) * 160: .YY = 176 - Cos((TtA + I * 72) * Rg) * 160
.X = .X + (.XX - .X) * 0.1: .Y = .Y + (.YY - .Y) * 0.1
End With
Next I
Else 'タイトル(Mnn<>0)
TCXX = 256 + Sin((TtA + TCPOS * 72) * Rg) * 512: TCYY = 176 - Cos((TtA + TCPOS * 72) * Rg) * 512
TCX = TCX + (TCXX - TCX) * 0.1: TCY = TCY + (TCYY - TCY) * 0.1
For I = 0 To 4
With TtPos(I)
.XX = 256 + Sin((TtA + I * 72) * Rg) * 512: .YY = 176 - Cos((TtA + I * 72) * Rg) * 512
.X = .X + (.XX - .X) * 0.1: .Y = .Y + (.YY - .Y) * 0.1
End With
Next I
If Mnn > 0 And Mnn <= MnMx Then
RR1 = False: RR2 = False
For I = 1 To JC
RR1 = RR1 Or CsrU1(I)
RR2 = RR2 Or CsrD1(I)
Next I
If MnDB(Mnn).MNs > 1 And ((CsrUU = 1 Or (CPos > 0 And CsrUU = 15) And Mnn <> 8 And Mnn <> 10) And (Mnn <> 22 Or KS.Key(DIK_UP) > 0 Or RR1)) And Not StrEdit Then CPos = CPos - 1 - (CPos <= 0) * MnDB(Mnn).MNs: Sou(5).On = True
If MnDB(Mnn).MNs > 1 And ((CsrDD = 1 Or (CPos < MnDB(Mnn).MNs - 1 And CsrDD = 15) And Mnn <> 8 And Mnn <> 10) And (Mnn <> 22 Or KS.Key(DIK_DOWN) > 0 Or RR2)) And Not StrEdit Then CPos = CPos + 1 + (CPos >= MnDB(Mnn).MNs - 1) * MnDB(Mnn).MNs: Sou(5).On = True
End If
End If
If Mnn = 1 Then 'GAME START
If CsrBB = 1 Then Mn = 0: Sou(12).On = True
If CsrAA = 1 Then
If CCPos = 0 Then Mn = 3: CPos = 0: Sou(0).On = True 'TRAINING
If CCPos = 1 Then If ScLv(0) >= 30 Or ScLv(1) >= 50 Or ScLv(2) >= 200 Then Mn = 2: CPos = 0: Sou(0).On = True Else Mn = 4: CPos = 0: Sou(0).On = True 'TRIAL
If CCPos = 2 Then Mn = 6: CPos = 0: Sou(0).On = True '2P BATTLE
If CCPos = 3 Then Mn = 7: CPos = 0: Sou(0).On = True '4P BATTLE
If CCPos = 4 Then 'NET BATTLE
NetCont = False
If NC < 2 Then '参加者
ParReq = False
Par = False: Mn = 21: CPos = 0: Sou(0).On = True
Else '親
If ParStC >= 115 Then
Par = True: TrM = ParTrM: Fade = False: ScrFC = 0: GMode = 7: OfBt = False: Fnl = False: TA = False: Mn = -1: Sou(4).On = True
End If
End If
End If
End If
End If

If Mnn = 2 Or Mnn = 13 Then 'TRIAL,RANKING
If CsrBB = 1 Then
If Mnn = 2 Then Mn = 1: CPos = 1: Sou(12).On = True
If Mnn = 13 Then Mn = 0: Sou(12).On = True
End If
If CsrAA = 1 Then
If Mnn = 2 Then
If CCPos = 0 Then Mn = 4: CPos = 0: Sou(0).On = True: If MnCapLen > 17 Then MnCapLen = 17 'SCORE ATTACK
If CCPos = 1 Then
If ScLv(0) >= 30 And ScLv(1) >= 50 Then
Mn = 5: CPos = 0: Sou(0).On = True: If MnCapLen > 17 Then MnCapLen = 17 'TIME ATTACK
Else
Fade = False: ScrFC = 0: GMode = 1: TrM = -(ScLv(0) < 30): Fnl = False: TA = True: Trn = False: VM = 1: Mn = -1: Sou(4).On = True
End If
End If
If CCPos = 2 Then
If FnLv >= 300 Then
Mn = 19: CPos = 0: Sou(0).On = True: If MnCapLen > 17 Then MnCapLen = 17 'JOKER,FURTHEST
Else
Fade = False: ScrFC = 0: TrM = 2: GMode = 1: Fnl = True: TA = False: Trn = False: VM = 1: Mn = -1: Sou(4).On = True: Sou(7).On = True 'FINAL
End If
End If
End If
If Mnn = 13 Then
Mn = 14 + CPos: CPos = 0: TrM = (Mn = 15) * (ScLv(0) < 30): TA = False: Sou(0).On = True 'SCORE,TIME,FINAL
End If
End If
End If

If (Mnn >= 3 And Mnn <= 7) Or Mnn = 21 Then 'TRAINING,SCORE ATTACK,TIME ATTACK,FREE BATTLE 2P,FREE BATTLE 4P,NETWORK BATTLE
If CsrBB = 1 Or (Mnn = 21 And NC >= 2) Then
If Mnn = 3 Then Mn = 1: CPos = 0: Sou(12).On = True
If Mnn = 4 Then If ScLv(0) >= 30 Or ScLv(1) >= 50 Or ScLv(2) >= 200 Then Mn = 2: CPos = 0: Sou(12).On = True: MnCapLen = 17 Else Mn = 1: CPos = 1: Sou(12).On = True
If Mnn = 5 Then Mn = 2: CPos = 1: Sou(12).On = True: MnCapLen = 17
If Mnn = 6 Then Mn = 1: CPos = 2: Sou(12).On = True
If Mnn = 7 Then Mn = 1: CPos = 3: Sou(12).On = True
If Mnn = 21 Then Mn = 1: CPos = 4: Sou(12).On = True
End If
If CsrAA = 1 Then
TrM = CPos
If Mnn = 3 Then Fade = False: ScrFC = 0: GMode = 1: Fnl = False: TA = False: Trn = True: VM = 0: Mn = -1: Sou(4).On = True
If Mnn = 4 Then Fade = False: ScrFC = 0: GMode = 1: Fnl = False: TA = False: Trn = False: VM = 1: Mn = -1: Sou(4).On = True
If Mnn = 5 Then Fade = False: ScrFC = 0: GMode = 1: Fnl = False: TA = True: Trn = False: VM = 1: Mn = -1: Sou(4).On = True
If Mnn = 6 Then
For I = JC To 0 Step -1
If CsrAA1(I) = 1 Then PT(0) = I
Next I
PT(1) = -1
Fade = False: ScrFC = 0: GMode = 3: OfBt = False: Fnl = False: TA = False: Mn = -1: Sou(4).On = True
End If
If Mnn = 7 Then
For I = JC To 0 Step -1
If CsrAA1(I) = 1 Then PT(0) = I
Next I
For I = 1 To 3: PT(I) = -1: Next I
Fade = False: ScrFC = 0: GMode = 4: OfBt = False: Fnl = False: TA = False: Mn = -1: Sou(4).On = True
End If
If Mnn = 21 Then Fade = False: ScrFC = 0: GMode = 7: OfBt = False: Fnl = False: TA = False: Mn = -1: Sou(4).On = True
End If
End If

If Mnn = 8 Then 'OPTIONS
If CsrBB = 1 Then Mn = 0: Sou(12).On = True
If CsrAA = 1 Then
If CCPos = 0 Then PlayMusic "TEST", 9216, 58368
If CCPos = 1 Then Sou(2).On = True
If CCPos = 2 Then Sou(3).Pan = -330: Sou(3).On = True: Sou(13).Pan = 330: Sou(13).On = True
If CCPos = 3 Then Mn = 9: CPos = 0: Sou(0).On = True 'EFFECT
If CCPos = 4 Then Mn = 10: CPos = 0: Sou(0).On = True 'PERFORMANCE
If CCPos = 5 Then 'KEY CONFIG
If JC <= 0 Then
Mn = 22: CPos = 7: Sou(0).On = True 'KEYBOARD
Else
For I = JC To 0 Step -1
If CsrAA1(I) = 1 Then Mn = 17: CPos = I: Sou(0).On = True 'KEY GONFIG
Next I
End If
End If
If RecCh And CCPos = 6 Then 'NETWORK CREATE
Mn = 20: CPos = 0: Sou(0).On = True
End If
End If
If CsrLL = 1 Or CsrLL = 15 Then
If CCPos = 0 And MidiPortC > 0 Then Midi = Midi - 1 - (Midi <= 0) * (MidiPortC + 1): Sou(5).On = True
If CCPos = 1 And SVol > 0 Then SVol = SVol - 1: Sou(5).On = True
If CCPos = 2 And SPan > 0 Then SPan = SPan - 1: Sou(5).On = True
End If
If CsrRR = 1 Or CsrRR = 15 Then
If CCPos = 0 And MidiPortC > 0 Then Midi = Midi + 1 + (Midi >= MidiPortC) * (MidiPortC + 1): Sou(5).On = True
If CCPos = 1 And SVol < 10 Then SVol = SVol + 1: Sou(5).On = True
If CCPos = 2 And SPan < 10 Then SPan = SPan + 1: Sou(5).On = True
End If
End If

If Mnn = 9 Then 'VISUAL EFFECT
If CsrBB = 1 Then Mn = 8: CPos = 3: Sou(12).On = True
If CsrLL = 1 Or CsrLL = 15 Then
If CCPos = 0 Then BG = BG - 1 - (BG <= 0) * 3: Sou(5).On = True
If CCPos = 1 Then BAni = Not BAni: Sou(5).On = True
If CCPos = 2 Then BSm = Not BSm: Sou(5).On = True
If CCPos = 3 Then Zanzo = Not Zanzo: Sou(5).On = True
If CCPos = 4 Then BC = BC - 1 - (BC <= 0) * 4: Sou(5).On = True
If CCPos = 5 Then BD = BD - 1 - (BD <= 0) * 4: Sou(5).On = True
If CCPos = 6 Then MNx = MNx - 1 - (MNx <= 0) * 5: Sou(5).On = True
If CCPos = 7 Then ClkC = Not ClkC: Sou(5).On = True
End If
If CsrRR = 1 Or CsrRR = 15 Then
If CCPos = 0 Then BG = BG + 1 + (BG >= 2) * 3: Sou(5).On = True
If CCPos = 1 Then BAni = Not BAni: Sou(5).On = True
If CCPos = 2 Then BSm = Not BSm: Sou(5).On = True
If CCPos = 3 Then Zanzo = Not Zanzo: Sou(5).On = True
If CCPos = 4 Then BC = BC + 1 + (BC >= 3) * 4: Sou(5).On = True
If CCPos = 5 Then BD = BD + 1 + (BD >= 3) * 4: Sou(5).On = True
If CCPos = 6 Then MNx = MNx + 1 + (MNx >= 4) * 5: Sou(5).On = True
If CCPos = 7 Then ClkC = Not ClkC: Sou(5).On = True
End If
End If

If Mnn = 10 Then 'SYSTEM PERFORMANCE
If CsrBB = 1 Then Mn = 8: CPos = 4: Sou(12).On = True
If CsrLL = 1 Then
If CCPos = 0 Then TV = TV - 1 - (TV <= 0) * 4: Sou(5).On = True
If CCPos = 1 Then AFS = Not AFS: Sou(5).On = True
If CCPos = 2 Then WM = Not WM: Sou(5).On = True
If CCPos = 3 Then BGMem = Not BGMem: Sou(5).On = True
End If
If CsrRR = 1 Then
If CCPos = 0 Then TV = TV + 1 + (TV >= 3) * 4: Sou(5).On = True
If CCPos = 1 Then AFS = Not AFS: Sou(5).On = True
If CCPos = 2 Then WM = Not WM: Sou(5).On = True
If CCPos = 3 Then BGMem = Not BGMem: Sou(5).On = True
End If
If Not BGMemm And BGMem Then BGCreate: BGCrTit: BGMemm = BGMem
If BGMemm And Not BGMem Then BGDestroy: BGMemm = BGMem
End If

If Mnn = 20 Then 'NETWORK CREATE
If StrEdit Then
StrMsgEdit 0
Else
If CsrAA = 1 And CPos = 4 Then StrEdit = True: StrMsgInit: Sou(0).On = True
If CsrBB = 1 Then Mn = 8: CPos = 6: Sou(12).On = True
If CsrLL = 1 Or (CsrLL = 15 And Not (Net And CCPos = 1) And CCPos <> 0) Then
If CCPos = 0 Then Net = Not Net: Sou(5).On = True
If CCPos = 1 And HNMax > 0 Then
HNNum = HNNum - 1 - (HNNum <= 0) * (HNMax + 1): HNN = HN(HNNum)
If Net Then NetDestroy: NetCreate: NameCreate
Sou(5).On = True
End If
If CCPos = 2 And NetQ > 0 Then NetQ = NetQ - 1: Sou(5).On = True
If CCPos = 3 Then NetFldLR = NetFldLR - 1 - (NetFldLR <= 0) * 3: Sou(5).On = True
If CCPos = 4 Then Jpn = Not Jpn: Sou(5).On = True
End If
If CsrRR = 1 Or (CsrRR = 15 And Not (Net And CCPos = 1) And CCPos <> 0) Then
If CCPos = 0 Then Net = Not Net: Sou(5).On = True
If CCPos = 1 And HNMax > 0 Then
HNNum = HNNum + 1 + (HNNum >= HNMax) * (HNMax + 1): HNN = HN(HNNum)
If Net Then NetDestroy: NetCreate: NameCreate
Sou(5).On = True
End If
If CCPos = 2 And NetQ < 5 Then NetQ = NetQ + 1: Sou(5).On = True
If CCPos = 3 Then NetFldLR = NetFldLR + 1 + (NetFldLR >= 2) * 3: Sou(5).On = True
If CCPos = 4 Then Jpn = Not Jpn: Sou(5).On = True
End If

If Not Nett And Net Then NetCreate: NameCreate: Nett = Net
If Nett And Not Net Then NetDestroy: Nett = Net

End If
End If

If Mnn = 17 Then 'KEY CONFIG
For I = JC To 0 Step -1
If I <> CCPos And (CsrLL1(I) = 1 Or CsrRR1(I) = 1) Then CPos = I: Sou(5).On = True
Next I
If CsrBB = 1 Then Mn = 8: CPos = 5: Sou(12).On = True
If CsrAA = 1 Then
If CCPos >= 1 Then
Mn = 11: MnJ = CCPos - 1: MnDB(11).Cap = "OPTIONS> KEY CONFIG> PAD(" + Chr$(65 + MnJ) + ")": CPos = 4: Sou(0).On = True 'PAD
Else
Mn = 22: MnDB(11).Cap = "OPTIONS> KEY CONFIG> KEYBOARD": CPos = 7: Sou(0).On = True 'KEYBOARD
End If
End If
End If

If Mnn = 11 Then 'PAD()
MnPB = -1
For I = 31 To 0 Step -1
If JS(MnJ).buttons(I) > 64 Then If MnPB = -1 Then MnPB = I Else MnPB = 32
Next I
If MnPB = 32 Then MnPB = -1
If Not (CsrL Or CsrR Or CsrU Or CsrD) And MnPB >= 0 And MnPB <> MnPBB Then
If CCPos = 0 Then PadConfigCheck: PBL(MnJ) = MnPB: CPos = CPos + 1: Sou(5).On = True
If CCPos = 1 Then PadConfigCheck: PBR(MnJ) = MnPB: CPos = CPos + 1: Sou(5).On = True
If CCPos = 2 Then PadConfigCheck: PBS(MnJ) = MnPB: CPos = CPos + 1: Sou(5).On = True
End If
MnPBB = MnPB
If CsrLL1(MnJ + 1) = 1 Or CsrLL1(MnJ + 1) = 15 Then
If CPos = 3 Then PArw(MnJ) = PArw(MnJ) - 1 - (PArw(MnJ) <= 0) * 4: Sou(5).On = True
End If
If CsrRR1(MnJ + 1) = 1 Or CsrRR1(MnJ + 1) = 15 Then
If CPos = 3 Then PArw(MnJ) = PArw(MnJ) + 1 + (PArw(MnJ) >= 3) * 4: Sou(5).On = True
End If
If CCPos = 4 And (CsrAA = 1 Or CsrBB = 1) Then Mn = 17: CPos = MnJ + 1: Sou(12).On = True
End If

If Mnn = 22 Then 'KEYBOARD
MnPB = -1
For I = 255 To 1 Step -1
If KS.Key(I) > 0 And (I >= 2 And I <= 12 Or I >= 16 And I <= 27 Or I >= 30 And I <= 39 Or I >= 43 And I <= 53 Or I = 55 Or I >= 71 And I <= 83 Or I = 125 Or I = 141 Or I >= 144 And I <= 147 Or I = 179 Or I = 181) Then If MnPB = -1 Then MnPB = I Else MnPB = 256
Next I
If MnPB = 256 Then MnPB = -1
If MnPB >= 0 And MnPB <> MnPBB Then
If CCPos = 0 Then KeyConfigCheck: KBL = MnPB: CPos = CPos + 1: Sou(5).On = True
If CCPos = 1 Then KeyConfigCheck: KBR = MnPB: CPos = CPos + 1: Sou(5).On = True
If CCPos = 2 Then KeyConfigCheck: KBS = MnPB: CPos = CPos + 1: Sou(5).On = True
If CCPos = 3 Then KeyConfigCheck: KArwL = MnPB: CPos = CPos + 1: Sou(5).On = True
If CCPos = 4 Then KeyConfigCheck: KArwR = MnPB: CPos = CPos + 1: Sou(5).On = True
If CCPos = 5 Then KeyConfigCheck: KArwU = MnPB: CPos = CPos + 1: Sou(5).On = True
If CCPos = 6 Then KeyConfigCheck: KArwD = MnPB: CPos = CPos + 1: Sou(5).On = True
End If
MnPBB = MnPB
If CCPos = 7 And (CsrAA = 1 Or CsrBB = 1) Then
Mn = 8 - (JC >= 1) * 9: CPos = 5 + (JC >= 1) * 5: Sou(12).On = True
End If
End If

If Mnn = 12 Then 'MATCH
If CsrBB = 1 Then Mn = 0: Sou(12).On = True
If CCPos <= 1 Then
RR = -1
For I = JC To 0 Step -1
If CsrAA1(I) = 1 Then RR = I
Next I
If RR >= 0 And OBPT(CCPos) <> RR Then OBPT(CCPos) = RR: Sou(5).On = True
End If
If CCPos >= 2 And CCPos <= 8 And CsrAA = 1 Then
If OBPT(0) + (OBPT(0) > JC + 1) * (OBPT(0) - JC - 1) <> OBPT(1) + (OBPT(1) > JC + 1) * (OBPT(1) - JC - 1) Then
For I = 0 To 1
If OBPT(I) <= 4 Then PT(I) = OBPT(I) Else PT(I) = 5: CPG1(I) = OBPT(I) - 5
Next I
If CCPos = 2 Then Fade = False: ScrFC = 0: GMode = 3: TrM = 0: OfBt = True: Fnl = False: TA = False: Mn = -1: For I = 0 To 1: SLv1(I) = 3: Liv1(I) = 2: Next I: Sou(4).On = True: Sou(8).On = True
If CCPos = 3 Then Fade = False: ScrFC = 0: GMode = 3: TrM = 0: OfBt = True: Fnl = False: TA = False: Mn = -1: For I = 0 To 1: SLv1(I) = 10: Liv1(I) = 3: Next I: Sou(4).On = True: Sou(8).On = True
If CCPos = 4 Then Fade = False: ScrFC = 0: GMode = 3: TrM = 1: OfBt = True: Fnl = False: TA = False: Mn = -1: For I = 0 To 1: SLv1(I) = 25: Liv1(I) = 3: Next I: Sou(4).On = True: Sou(8).On = True
If CCPos = 5 Then Fade = False: ScrFC = 0: GMode = 3: TrM = 1: OfBt = True: Fnl = False: TA = False: Mn = -1: For I = 0 To 1: SLv1(I) = 30: Liv1(I) = 3: Next I: Sou(4).On = True: Sou(8).On = True
If CCPos = 6 Then Fade = False: ScrFC = 0: GMode = 3: TrM = 1: OfBt = True: Fnl = False: TA = False: Mn = -1: For I = 0 To 1: SLv1(I) = 50: Liv1(I) = 5: Next I: Sou(4).On = True: Sou(8).On = True
If CCPos = 7 Then Fade = False: ScrFC = 0: GMode = 3: TrM = 2: OfBt = True: Fnl = False: TA = False: Mn = -1: For I = 0 To 1: SLv1(I) = 35: Liv1(I) = 5: Next I: Sou(4).On = True: Sou(8).On = True
If CCPos = 8 Then Fade = False: ScrFC = 0: GMode = 3: TrM = 2: OfBt = True: Fnl = False: TA = False: Mn = -1: For I = 0 To 1: SLv1(I) = 200: Liv1(I) = 5: Next I: Sou(4).On = True: Sou(8).On = True
Else
CPos = 1: Sou(5).On = True
End If
End If
For I = 0 To 1
If I = CCPos Then
If (CsrLL = 1 Or CsrLL = 15) And OBPT(I) > 0 Then
OBPT(I) = OBPT(I) - 1: If OBPT(I) > JC And OBPT(I) <= 11 Then OBPT(I) = JC
Sou(5).On = True
End If
If (CsrRR = 1 Or CsrRR = 15) And OBPT(I) < 5 + CPMax + (CPMax < 7) * (CPMax + 5 - JC) Then
OBPT(I) = OBPT(I) + 1: If OBPT(I) > JC And OBPT(I) <= 11 Then OBPT(I) = 12
Sou(5).On = True
End If
End If
Next I
End If

If Mnn >= 14 And Mnn <= 16 Then 'RANKING-SCORE,TIME,FINAL
If CsrBB = 1 Then If ScLv(0) >= 30 Or ScLv(1) >= 50 Or ScLv(2) >= 200 Then Mn = 13: CPos = Mnn - 14: Sou(12).On = True Else Mn = 0: Sou(12).On = True
If Mnn = 14 Or (Mnn = 15 And (ScLv(0) >= 30 And ScLv(1) >= 50)) Then
If CsrLL = 1 Or CsrLL = 15 Then TrM = TrM - 1 - (TrM <= 0) * (2 - (ScLiv(0) > 0 And ScLiv(1) > 0 And (Mnn = 14 Or ScLv(2) >= 200))): Sou(5).On = True
If CsrAA = 1 Or CsrRR = 1 Or CsrRR = 15 Then TrM = TrM + 1 + (TrM >= (1 - (ScLiv(0) > 0 And ScLiv(1) > 0 And (Mnn = 14 Or ScLv(2) >= 200)))) * (2 - (ScLiv(0) > 0 And ScLiv(1) > 0 And (Mnn = 14 Or ScLv(2) >= 200))): Sou(5).On = True
Else
If Mnn = 16 And FnLv >= 300 And (CsrAA = 1 Or CsrLL = 1 Or CsrLL = 15 Or CsrRR = 1 Or CsrRR = 15) Then TA = Not TA: Sou(5).On = True
End If
End If

If Mnn = 18 Then 'VIDEO
If CsrBB = 1 Then Mn = 0: Sou(12).On = True
If CsrAA = 1 And CCPos >= 1 And VPC * 8 + (CCPos - 1) < VFC Then
If XDir(VidDir + VFNL(VPC * 8 + CCPos - 1)) <> vbNullString$ Then
If VCheck(VidDir + VFNL(VPC * 8 + CCPos - 1)) Then
VLoad VidDir + VFNL(VPC * 8 + CCPos - 1)
Fade = False: ScrFC = 0: GMode = 1: Trn = False: VM = 2: Mn = -1: Sou(4).On = True
End If
End If
End If
If VFC > 8 Then
If CsrRR = 1 Or CsrRR = 15 Then VPC = VPC + 1: Sou(5).On = True: If VPC > Int((VFC - 1) / 8) Then VPC = 0
If CsrLL = 1 Or CsrLL = 15 Then VPC = VPC - 1: Sou(5).On = True: If VPC < 0 Then VPC = Int((VFC - 1) / 8)
End If
End If

If Mnn = 19 Then 'FINAL
If CsrBB = 1 Then Mn = 2: CPos = 2: Sou(12).On = True: MnCapLen = 17
If CsrAA = 1 Then
If CCPos = 0 Then Fade = False: ScrFC = 0: TrM = 2: GMode = 1: Fnl = True: TA = False: Trn = False: VM = 1: Mn = -1: Sou(4).On = True: Sou(7).On = True 'JOKER
If CCPos = 1 Then Fade = False: ScrFC = 0: TrM = 2: GMode = 1: Fnl = True: TA = True: Trn = False: VM = 1: Mn = -1: Sou(4).On = True: Sou(7).On = True 'FURTHEST
End If
End If

If Mnn = -1 Then 'ゲーム開始
If (GMode <> 3 Or Not OfBt) And (GMode <> 6 And BG < 1 Or CsrAA = 1 Or CsrBB = 1 Or EscC = 1) And ScrFC < 80 Then ScrFC = 80
If ScrFC = 80 Then GEnd = True
End If

If Mn > 0 And Not StrEdit And EscC = 1 Then Mn = 0: Sou(12).On = True

If Mn <> Mnn Then PlayMusic vbNullString$

'オブジェクト文字
If RecCh Then
If ParStC < 0 Then
Tx(4) = "NET BATTLE"
Else
If ParStC < 115 Then
Tx(4) = "NET BATTLE (WAIT...)"
Else
If ParTrM >= 0 And ParTrM <= 2 Then Tx(4) = "NET BATTLE (" + TrMN(ParTrM) + ")"
End If
End If
End If

If Mn = 8 Then Tx(17 - RecCh) = PortName(Midi) Else Tx(17 - RecCh) = ""
If SVol > 0 Then Tx(18 - RecCh) = String$(SVol, ">") + Str$(SVol) Else Tx(18 - RecCh) = "OFF"
If SPan > 0 Then Tx(19 - RecCh) = String$(SPan, ">") + Str$(SPan) Else Tx(19 - RecCh) = "OFF"
If BG = 0 Then Tx(29) = "OFF"
If BG = 1 Then Tx(29) = "PICTURE"
If BG = 2 Then Tx(29) = "ANIMATION"
If BAni Then Tx(30) = "ON" Else Tx(30) = "OFF"
If BSm Then Tx(31) = "ON" Else Tx(31) = "OFF"
If Zanzo Then Tx(32) = "ON" Else Tx(32) = "OFF"
If BC = 0 Then Tx(33) = "KIDS"
If BC = 1 Then Tx(33) = "COLOR"
If BC = 2 Then Tx(33) = "FLUITS"
If BC = 3 Then Tx(33) = "MONO"
If BD = 0 Then Tx(34) = "GREEN"
If BD = 1 Then Tx(34) = "RED"
If BD = 2 Then Tx(34) = "BLUE"
If BD = 3 Then Tx(34) = "GRAY"
If MNx = 0 Then Tx(35) = "HIDDEN" Else If MNx = 4 Then Tx(35) = "FULL" Else Tx(35) = Mid$(Str$(MNx), 2, 1)
If ClkC Then Tx(36) = "ON" Else Tx(36) = "OFF"
If TV = 0 Then Tx(41) = "CRT"
If TV = 1 Then Tx(41) = "TV"
If TV = 2 Then Tx(41) = "60FPS"
If TV = 3 Then Tx(41) = "30FPS"
If AFS Then Tx(42) = "AUTO" Else Tx(42) = "OFF"
If WM Then Tx(43) = "WINDOW" Else Tx(43) = "FULLSCREEN"
If BGMem Then Tx(44) = "RAM" Else Tx(44) = "DISK"

If RecCh Then If Net Then Tx(89) = "ON" Else Tx(89) = "OFF"
If RecCh Then Tx(90) = HN(HNNum)
If RecCh Then If NetQ > 0 Then Tx(91) = String$(NetQ, ">") + Str$(NetQ) Else Tx(91) = "OFF"
If RecCh Then If NetFldLR = 0 Then Tx(92) = "AUTO"
If RecCh Then If NetFldLR >= 1 And NetFldLR <= 2 Then Tx(92) = "PLAYER" + Mid$(Str$(NetFldLR), 2)
If RecCh Then If Jpn Then Tx(93) = "ON" Else Tx(93) = "OFF"

If JC >= 1 Then
Tx(50) = "[" + Mid$(Str$(PBL(MnJ) + 1), 2) + "]"
Tx(51) = "[" + Mid$(Str$(PBR(MnJ) + 1), 2) + "]"
Tx(52) = "[" + Mid$(Str$(PBS(MnJ) + 1), 2) + "]"
If PArw(MnJ) = 0 Then Tx(53) = "4 ARROWS"
If PArw(MnJ) = 1 Then Tx(53) = "4 LEFT+RIGHT"
If PArw(MnJ) = 2 Then Tx(53) = "4 UP+DOWN"
If PArw(MnJ) = 3 Then Tx(53) = "8 ARROWS"
End If

Tx(102) = "[" + Mid$(Str$(KBL), 2) + "]"
Tx(103) = "[" + Mid$(Str$(KBR), 2) + "]"
Tx(104) = "[" + Mid$(Str$(KBS), 2) + "]"
Tx(105) = "[" + Mid$(Str$(KArwL), 2) + "]"
Tx(106) = "[" + Mid$(Str$(KArwR), 2) + "]"
Tx(107) = "[" + Mid$(Str$(KArwU), 2) + "]"
Tx(108) = "[" + Mid$(Str$(KArwD), 2) + "]"

For I = 0 To 1
If RecCh And OBPT(I) = 0 Then Tx(61 + OBMax + I) = "KEYBOARD"
For U = 0 To 3
If RecCh And OBPT(I) = U + 1 Then Tx(61 + OBMax + I) = "PAD(" + Chr$(65 + U) + ")"
Next U
If RecCh And OBPT(I) >= 5 Then Tx(61 + OBMax + I) = "CP GRADE " + Mid$(Str$(OBPT(I) - 5), 2)
Next I
If Mn <> 16 Then
If RecCh And TrM >= 0 And TrM <= 2 Then Tx(71) = TrMN(TrM)
Else
If RecCh Then Tx(71) = TrMN(3 - TA)
End If

For I = 0 To 7
If Mn = 18 Then
If VPC * 8 + I < VFC Then Tx(73 + I) = VFNL(VPC * 8 + I) Else Tx(73 + I) = "______"
Else
Tx(73 + I) = ""
End If
Next I
If VFC > 0 Then Tx(81) = Mid$(Str$(VPC + 1), 2) + " / " + Mid$(Str$(Int((VFC - 1) / 8) + 1), 2) Else Tx(81) = "0"

If Mnn <> Mn Then CsrMove: TxMove 0, TxMx 'MnDB(Mnn).StN, MnDB(Mnn).StN + MnDB(Mnn).MNs + 1 '文字移動

If Mn > 0 And Mn <= MnMx Then
For I = 0 To MnDB(Mn).MNs - 1
TxPos(I + MnDB(Mn).StN).XX = MnDB(Mn).X + I * 8 - (CPos = I) * 8: TxPos(I + MnDB(Mn).StN).YY = MnDB(Mn).Y + I * 32 'メニュー文字配置
Next I
For I = 0 To MnDB(Mn).Obs - 1
TxPos(I + MnDB(Mn).StN + MnDB(Mn).MNs).XX = MnDB(Mn).X + 12 + Len(Tx(I + MnDB(Mn).StN)) * 16 + I * 8 - (CPos = I) * 4: TxPos(I + MnDB(Mn).StN + MnDB(Mn).MNs).YY = MnDB(Mn).Y + I * 32 'オブジェクト文字配置
Next I
CXX = MnDB(Mn).X - 16 + CPos * 8: CYY = MnDB(Mn).Y + CPos * 32 'カーソル配置
End If

TitY = TitY + (64 - (Mn <> 0) * 432 - TitY) * 0.2

TtA = TtA + 0.2: If TtA >= 360 Then TtA = TtA - 360

CX = CX + (CXX - CX) * 0.5: CY = CY + (CYY - CY) * 0.5

For I = 0 To TxMx
With TxPos(I)
If BG < 2 Then .X = .XX: .Y = .YY Else .X = .X + (.XX - .X) * 0.2: .Y = .Y + (.YY - .Y) * 0.2
End With
Next I

If BAni Then
With EA(EAC)
.V = 2: .A = Int(TCPOS): .AF = 0
.X = TCX + 56 + Sin((EAC * 63) * Rg) * 56: .Y = TCY + 56 - Cos((EAC * 63) * Rg) * 56
.XX = Sin((EAC * 63) * Rg) * 4: .YY = Cos((EAC * 63) * Rg) * 5
.XXX = -.XX * 0.05: .YYY = -.YY * 0.05
End With
End If
EAC = EAC + 1 + (EAC >= 159) * 160

TitBGC = TitBGC + 1 + (TitBGC >= 159) * 160

If BG = 0 Then If Not FS Then BBSf.BltColorFill RC0, 0

If BG = 1 Then '背景（グレー画像のみ）表示
With Src
.Left = 0: .Top = 320: .Right = .Left + 80: .Bottom = .Top + 80
End With
If TitBGFC < 40 Then
For I = 0 To 5: For U = 0 To 7
If Not FS Then BBSf.BltFast U * 80, I * 80, BGSf, Src, DDBLTFAST_WAIT
Next U, I
End If
End If
'背景（グレー）表示
If GMode = 6 Or Mn = 0 Then TitBGFC = TitBGFC - 3 Else TitBGFC = TitBGFC + 2
If TitBGFC < 0 Then TitBGFC = 0 Else If TitBGFC > 40 Then TitBGFC = 40
RR = TitBGC Mod 40
With Src
.Left = 0: .Top = 320: .Right = .Left + 80: .Bottom = .Top + 80
End With
If TitBGC < 40 Then
For I = 0 To 79
If TitBGFC < 40 Then
If BG >= 2 Then BltClip -40 + (I Mod 10) * 80 + RR * ((Int(I / 10) Mod 2) * 2 - 1), -40 + Int(I / 10) * 80, BGSf, Src, DDBLTFAST_WAIT
End If
Next I
End If
If TitBGC >= 40 And TitBGC < 80 Then
For I = 0 To 79
If TitBGFC < 40 Then
If BG >= 2 Then BltClip -80 + (I Mod 10) * 80, -40 + Int(I / 10) * 80 + RR * ((I Mod 2) * 2 - 1), BGSf, Src, DDBLTFAST_WAIT
End If
Next I
End If
If TitBGC >= 80 And TitBGC < 120 Then
For I = 0 To 79
If TitBGFC < 40 Then
If BG >= 2 Then BltClip -80 + (I Mod 10) * 80 + RR * ((Int(I / 10) Mod 2) * 2 - 1), -80 + Int(I / 10) * 80, BGSf, Src, DDBLTFAST_WAIT
End If
Next I
End If
If TitBGC >= 120 Then
For I = 0 To 79
If TitBGFC < 40 Then
If BG >= 2 Then BltClip -40 + (I Mod 10) * 80, -80 + Int(I / 10) * 80 + RR * ((I Mod 2) * 2 - 1), BGSf, Src, DDBLTFAST_WAIT
End If
Next I
End If
'「PROGRAMMED BY MIHYS」表示
If TitBGFC < 40 Then StringD 374, 460 + (BG = 0) * TitBGFC ^ 2 / 3, 7, "PROGRAMMED BY MIHYS", False, 16
If BG = 1 Then '背景（各色画像のみ）表示
If TitBGFC > 0 Then
With Src
.Left = TitBGF * 80 + 40 - TitBGFC: .Top = 360 - TitBGFC: .Right = TitBGF * 80 + 40 + TitBGFC: .Bottom = 360 + TitBGFC
End With
For I = 0 To 6: For U = 0 To 8
BltClip -TitBGFC + U * 80, -TitBGFC + I * 80, BGSf, Src, DDBLTFAST_WAIT
Next U, I
End If
End If
'背景（各色）表示
With Src
.Left = TitBGF * 80 + 40 - TitBGFC: .Top = 360 - TitBGFC: .Right = TitBGF * 80 + 40 + TitBGFC: .Bottom = 360 + TitBGFC
End With
If TitBGC < 40 Then
For I = 0 To 79
If TitBGFC > 0 Then
If BG >= 2 Then BltClip -40 - TitBGFC + (I Mod 10) * 80 + RR * ((Int(I / 10) Mod 2) * 2 - 1), -40 - TitBGFC + Int(I / 10) * 80, BGSf, Src, DDBLTFAST_WAIT
End If
Next I
End If
If TitBGC >= 40 And TitBGC < 80 Then
For I = 0 To 79
If TitBGFC > 0 Then
If BG >= 2 Then BltClip -80 - TitBGFC + (I Mod 10) * 80, -40 - TitBGFC + Int(I / 10) * 80 + RR * ((I Mod 2) * 2 - 1), BGSf, Src, DDBLTFAST_WAIT
End If
Next I
End If
If TitBGC >= 80 And TitBGC < 120 Then
For I = 0 To 79
If TitBGFC > 0 Then
If BG >= 2 Then BltClip -80 - TitBGFC + (I Mod 10) * 80 + RR * ((Int(I / 10) Mod 2) * 2 - 1), -80 - TitBGFC + Int(I / 10) * 80, BGSf, Src, DDBLTFAST_WAIT
End If
Next I
End If
If TitBGC >= 120 Then
For I = 0 To 79
If TitBGFC > 0 Then
If BG >= 2 Then BltClip -40 - TitBGFC + (I Mod 10) * 80, -80 - TitBGFC + Int(I / 10) * 80 + RR * ((I Mod 2) * 2 - 1), BGSf, Src, DDBLTFAST_WAIT
End If
Next I
End If
'背景フェード
If TFC < 90 Then TFC = TFC + 1: If BG < 1 Then TFC = 90
FadeIn TFC - 10
'タイトル表示
With Src
.Left = 0: .Top = -((TitBGC Mod 32) >= 22 And (TitBGC Mod 32) < 28 Or (DTCl >= 1000 And (TitBGC Mod 32) >= 10 And (TitBGC Mod 32) < 16)) * 160: .Right = .Left + 480: .Bottom = .Top + 160
End With
If TitY > -160 And TitY < 480 Then BltClip 80, TitY, BGSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT

'タイトルメニューフィールド表示
For I = 0 To 4
With Src
.Left = 256: .Top = 64: .Right = .Left + 128: .Bottom = .Top + 128
End With
If Not FS Then BltClip TtPos(I).X, TtPos(I).Y, SpSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
'ブロックアニメ表示
EAni
With Src
.Left = (Int(CCC / 7) Mod 2) * 128: .Top = 64: .Right = .Left + 128: .Bottom = .Top + 128
End With
If Not FS Then BltClip TCX, TCY, SpSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT

If StrEdit Then 'テキストメッセージ表示
If Not FS Then BBSf.BltFast 168, 288, StrSf(0), RC0, DDBLTFAST_WAIT
If SMSel < 0 Then CursorD 160 + SMX, 288 + SMY, 11, 3
End If

'For I = 0 To 29 'ローマ字表示
'If Not FS Then If StrMsgOrg(I) <> "" Then BBSf.DrawText 0, I * 16, StrMsgOrg(I), False
'Next I

If Mn >= 14 And Mn <= 16 Then 'ランキング表示
StringD 64, 288, 9, "TODAY'S RANKING", False
For I = 0 To 1
StringD 80, 144 + I * 180, 8, "1ST", False
StringD 80, 168 + I * 180, 8, "2ND", False
StringD 80, 192 + I * 180, 8, "3RD", False
StringD 80, 216 + I * 180, 8, "4TH", False
StringD 80, 240 + I * 180, 8, "5TH", False
Next I
End If
'ランキング(SCORE ATTACK)表示
If Mn = 14 Then
StringD 64, 112, 9, "RANK", False, 4
StringD 176, 112, 9, "NAME", False, 4
StringD 368, 112, 9, "SCORE", False, 5
StringD 480, 112, 9, "LINES", False, 5
For I = 0 To 1: For U = 0 To 4
RR = InStr(SRnk(I * 15 + TrM * 5 + U).Nm, "\"): If RR > 0 Then RR = RR - 1 Else RR = 6
StringD 176, 144 + I * 180 + U * 24, 1, Left$(SRnk(I * 15 + TrM * 5 + U).Nm, RR), False, 6
TextD 448, 144 + I * 180 + U * 24, 10, SRnk(I * 15 + TrM * 5 + U).SC, True, 10
TextD 560, 144 + I * 180 + U * 24, 3, SRnk(I * 15 + TrM * 5 + U).Li, True, 5
Next U, I
End If
'ランキング(TIME ATTACK)表示
If Mn = 15 Then
StringD 64, 112, 9, "RANK", False, 4
StringD 176, 112, 9, "NAME", False, 4
StringD 368, 112, 9, "TIME", False, 5
StringD 480, 112, 9, "LINES", False, 5
For I = 0 To 1: For U = 0 To 4
RR = InStr(TRnk(I * 15 + TrM * 5 + U).Nm, "\"): If RR > 0 Then RR = RR - 1 Else RR = 6
StringD 176, 144 + I * 180 + U * 24, 1, Left$(TRnk(I * 15 + TrM * 5 + U).Nm, RR), False, 6
TextD 352, 144 + I * 180 + U * 24, CCC Mod 7, Int(TRnk(I * 15 + TrM * 5 + U).Tm / 6000), True, 2
StringD 352, 144 + I * 180 + U * 24, CCC Mod 7, Chr$(39), False, 1
If (Int(TRnk(I * 15 + TrM * 5 + U).Tm / 100) Mod 60) < 10 Then StringD 368, 144 + I * 180 + U * 24, CCC Mod 7, "0", False
TextD 400, 144 + I * 180 + U * 24, CCC Mod 7, Int(TRnk(I * 15 + TrM * 5 + U).Tm / 100) Mod 60, True, 2
StringD 400, 144 + I * 180 + U * 24, CCC Mod 7, Chr$(34), False, 1
If (TRnk(I * 15 + TrM * 5 + U).Tm Mod 100) < 10 Then StringD 416, 144 + I * 180 + U * 24, CCC Mod 7, "0", False
TextD 448, 144 + I * 180 + U * 24, CCC Mod 7, TRnk(I * 15 + TrM * 5 + U).Tm Mod 100, True, 2
TextD 560, 144 + I * 180 + U * 24, 3, TRnk(I * 15 + TrM * 5 + U).Li, True, 5
Next U, I
End If
'ランキング(FINAL)表示
If Mn = 16 Then
If TA Then
StringD 64, 112, 9, "RANK", False, 4
StringD 176, 112, 9, "NAME", False, 4
StringD 320, 112, 9, "LIVES", False, 5
StringD 496, 112, 9, "TIME", False, 5
For I = 0 To 1: For U = 0 To 4
RR = InStr(LRnk(I * 5 + U).Nm, "\"): If RR > 0 Then RR = RR - 1 Else RR = 6
StringD 176, 144 + I * 180 + U * 24, 1, Left$(LRnk(I * 5 + U).Nm, RR), False, 6
TextD 400, 144 + I * 180 + U * 24, 4 + Int(LAni / 5), LRnk(I * 5 + U).Liv, True, 10
TextD 464, 144 + I * 180 + U * 24, 3, Int(LRnk(I * 5 + U).Tm / 6000), True, 2
StringD 464, 144 + I * 180 + U * 24, 3, Chr$(39), False, 1
If (Int(LRnk(I * 5 + U).Tm / 100) Mod 60) < 10 Then StringD 480, 144 + I * 180 + U * 24, 3, "0", False
TextD 512, 144 + I * 180 + U * 24, 3, Int(LRnk(I * 5 + U).Tm / 100) Mod 60, True, 2
StringD 512, 144 + I * 180 + U * 24, 3, Chr$(34), False, 1
If (LRnk(I * 5 + U).Tm Mod 100) < 10 Then StringD 528, 144 + I * 180 + U * 24, 3, "0", False
TextD 560, 144 + I * 180 + U * 24, 3, LRnk(I * 5 + U).Tm Mod 100, True, 2
Next U, I
Else
StringD 64, 112, 9, "RANK", False, 4
StringD 176, 112, 9, "NAME", False, 4
StringD 320, 112, 9, "LEVEL", False, 5
StringD 496, 112, 9, "TIME", False, 5
For I = 0 To 1: For U = 0 To 4
RR = InStr(FRnk(I * 5 + U).Nm, "\"): If RR > 0 Then RR = RR - 1 Else RR = 6
StringD 176, 144 + I * 180 + U * 24, 1, Left$(FRnk(I * 5 + U).Nm, RR), False, 6
TextD 400, 144 + I * 180 + U * 24, 11, FRnk(I * 5 + U).Lv, True, 10
TextD 464, 144 + I * 180 + U * 24, 3, Int(FRnk(I * 5 + U).Tm / 6000), True, 2
StringD 464, 144 + I * 180 + U * 24, 3, Chr$(39), False, 1
If (Int(FRnk(I * 5 + U).Tm / 100) Mod 60) < 10 Then StringD 480, 144 + I * 180 + U * 24, 3, "0", False
TextD 512, 144 + I * 180 + U * 24, 3, Int(FRnk(I * 5 + U).Tm / 100) Mod 60, True, 2
StringD 512, 144 + I * 180 + U * 24, 3, Chr$(34), False, 1
If (FRnk(I * 5 + U).Tm Mod 100) < 10 Then StringD 528, 144 + I * 180 + U * 24, 3, "0", False
TextD 560, 144 + I * 180 + U * 24, 3, FRnk(I * 5 + U).Tm Mod 100, True, 2
Next U, I
End If
End If

'ビデオ情報表示
If Mn = 18 And CPos >= 1 And VPC * 8 + (CPos - 1) < VFC Then
If Not VFD((VPC * 8 + (CPos - 1)) * 5 + 1) And Not VFD((VPC * 8 + (CPos - 1)) * 5 + 2) Then
StringD 176, 64, 6, "SCORE ATTACK", False
StringD 224, 96, 9, "SCORE", False
TextD 304, 120, 10, VFD((VPC * 8 + (CPos - 1)) * 5 + 3), True, 10
StringD 336, 96, 9, "LINES", False
TextD 416, 120, 3, VFD((VPC * 8 + (CPos - 1)) * 5 + 4), True, 5
End If
If VFD((VPC * 8 + (CPos - 1)) * 5 + 1) And Not VFD((VPC * 8 + (CPos - 1)) * 5 + 2) Then
StringD 176, 64, 6, "TIME ATTACK", False
StringD 240, 96, 9, "TIME", False
TextD 208, 120, CCC Mod 7, Int(VFD((VPC * 8 + (CPos - 1)) * 5 + 3) / 6000), True, 2
StringD 208, 120, CCC Mod 7, Chr$(39), False, 1
If (Int(VFD((VPC * 8 + (CPos - 1)) * 5 + 3) / 100) Mod 60) < 10 Then StringD 224, 120, CCC Mod 7, "0", False
TextD 256, 120, CCC Mod 7, Int(VFD((VPC * 8 + (CPos - 1)) * 5 + 3) / 100) Mod 60, True, 2
StringD 256, 120, CCC Mod 7, Chr$(34), False, 1
If (VFD((VPC * 8 + (CPos - 1)) * 5 + 3) Mod 100) < 10 Then StringD 272, 120, CCC Mod 7, "0", False
TextD 304, 120, CCC Mod 7, VFD((VPC * 8 + (CPos - 1)) * 5 + 3) Mod 100, True, 2
StringD 336, 96, 9, "LINES", False
TextD 416, 120, 3, VFD((VPC * 8 + (CPos - 1)) * 5 + 4), True, 5
End If
If Not VFD((VPC * 8 + (CPos - 1)) * 5 + 1) And VFD((VPC * 8 + (CPos - 1)) * 5 + 2) Then
StringD 176, 64, 6, "FINAL", False
StringD 176, 96, 9, "LEVEL", False
TextD 256, 120, 11, VFD((VPC * 8 + (CPos - 1)) * 5 + 4), True, 5
StringD 352, 96, 9, "TIME", False
TextD 320, 120, 3, Int(VFD((VPC * 8 + (CPos - 1)) * 5 + 3) / 6000), True, 2
StringD 320, 120, 3, Chr$(39), False, 1
If (Int(VFD((VPC * 8 + (CPos - 1)) * 5 + 3) / 100) Mod 60) < 10 Then StringD 336, 120, 3, "0", False
TextD 368, 120, 3, Int(VFD((VPC * 8 + (CPos - 1)) * 5 + 3) / 100) Mod 60, True, 2
StringD 368, 120, 3, Chr$(34), False, 1
If (VFD((VPC * 8 + (CPos - 1)) * 5 + 3) Mod 100) < 10 Then StringD 384, 120, 3, "0", False
TextD 416, 120, 3, VFD((VPC * 8 + (CPos - 1)) * 5 + 3) Mod 100, True, 2
End If
If VFD((VPC * 8 + (CPos - 1)) * 5 + 1) And VFD((VPC * 8 + (CPos - 1)) * 5 + 2) Then
StringD 176, 64, 6, "FINAL", False
StringD 176, 96, 9, "LIVES", False
TextD 256, 120, 4 + Int(LAni / 5), VFD((VPC * 8 + (CPos - 1)) * 5 + 4), True, 5
StringD 352, 96, 9, "TIME", False
TextD 320, 120, 3, Int(VFD((VPC * 8 + (CPos - 1)) * 5 + 3) / 6000), True, 2
StringD 320, 120, 3, Chr$(39), False, 1
If (Int(VFD((VPC * 8 + (CPos - 1)) * 5 + 3) / 100) Mod 60) < 10 Then StringD 336, 120, 3, "0", False
TextD 368, 120, 3, Int(VFD((VPC * 8 + (CPos - 1)) * 5 + 3) / 100) Mod 60, True, 2
StringD 368, 120, 3, Chr$(34), False, 1
If (VFD((VPC * 8 + (CPos - 1)) * 5 + 3) Mod 100) < 10 Then StringD 384, 120, 3, "0", False
TextD 416, 120, 3, VFD((VPC * 8 + (CPos - 1)) * 5 + 3) Mod 100, True, 2
End If
If Not VFD((VPC * 8 + (CPos - 1)) * 5 + 2) Then
RR = VFD((VPC * 8 + (CPos - 1)) * 5): If RR >= 0 And RR <= 2 Then StringD 384 + VFD((VPC * 8 + (CPos - 1)) * 5 + 1) * 16, 64, 4, "(" + TrMN(RR) + ")", False
Else
If Not VFD((VPC * 8 + (CPos - 1)) * 5 + 1) Then StringD 272, 64, 4, "(" + TrMN(3) + ")", False Else StringD 272, 64, 4, "(" + TrMN(4) + ")", False
End If
End If

'タイトルメニュー表示
For I = 0 To 4
Select Case I
Case 0
If RecCh Then
StringD TtPos(I).X + 24, TtPos(I).Y + 56, 8 - (TCPOS = I) * 3, "START", False, 6
Else
StringD TtPos(I).X + 24, TtPos(I).Y + 56, 7, "-----", False, 6
End If
Case 1
StringD TtPos(I).X + 16, TtPos(I).Y + 56, 8 - (TCPOS = I) * 3, "OPTIONS", False, 6
Case 2
If RecCh Then
StringD TtPos(I).X + 16, TtPos(I).Y + 56, 8 - (TCPOS = I) * 3, "RANKING", False, 6
Else
StringD TtPos(I).X + 24, TtPos(I).Y + 56, 7, "-----", False, 6
End If
Case 3
StringD TtPos(I).X + 24, TtPos(I).Y + 56, 8 - (TCPOS = I) * 3, "VIDEO", False, 6
Case 4
If RecCh Then
StringD TtPos(I).X + 24, TtPos(I).Y + 56, 7 - (JC > 0 Or CPMax >= 7) * (1 - (TCPOS = I) * 3), "MATCH", False, 6
Else
StringD TtPos(I).X + 24, TtPos(I).Y + 56, 7, "-----", False, 6
End If
End Select
Next I
'メニュー・オブジェクト表示
For I = 0 To TxMx
RR = 8
For U = 1 To MnMx
If I >= MnDB(U).StN And I < MnDB(U).StN + MnDB(U).Obs Then RR = 9
If I >= MnDB(U).StN + MnDB(U).MNs And I < MnDB(U).StN + MnDB(U).MNs + MnDB(U).Obs Then RR = 7
Next U
If Mn > 0 And Mn <= MnMx Then
If CPos >= MnDB(Mn).Obs And I = MnDB(Mn).StN + CPos Then RR = 11
If I = MnDB(Mn).StN + MnDB(Mn).MNs + CPos Then RR = 10
End If
If TxPos(I).X > -400 And TxPos(I).X < 640 And TxPos(I).Y > -16 And TxPos(I).Y < 480 Then StringD TxPos(I).X, TxPos(I).Y, RR, Tx(I), False, 25
Next I
'メニュー場所表示
If Mn > 0 Then
If MnCapLen < Len(MnDB(Mn).Cap) Then MnCapLen = MnCapLen + 1
If MnCapLen > Len(MnDB(Mn).Cap) Then MnCapLen = Len(MnDB(Mn).Cap)
StringD 64, 64, 2, Left$(MnDB(Mn).Cap, MnCapLen), False, 32
Else
MnCapLen = 0
End If

'カーソル表示
If Mn > 0 And Mn <= MnMx Then CursorD CX, CY, 11, -(CPos >= MnDB(Mn).Obs)

'攻略率表示
'If Mn = 0 And CsrAA > 0 And CsrBB > 0 Then
'StringD 0, 0, 0, "SCORE-LIV", False
'For I = 0 To 2: TextD 216, I * 16, 11, ScLiv(I), True, 8: Next I
'StringD 0, 56, 0, "SCORE-LV", False
'For I = 0 To 2: TextD 216, 56 + I * 16, 11, ScLv(I), True, 8: Next I
'StringD 0, 112, 0, "TIME-LIV", False
'For I = 0 To 2: TextD 216, 112 + I * 16, 11, TmLiv(I), True, 8: Next I
'StringD 0, 168, 0, "JOKER-LV", False
'TextD 216, 168, 11, FnLv, True, 8
'StringD 0, 192, 0, "FURTH-LIV", False
'TextD 216, 192, 11, LsLiv, True, 8
'StringD 0, 224, 0, "RATIO", False
'TextD 216, 224, 11, DTCl, True, 8
'End If

FChange

Loop
Set StrSf(0) = Nothing
Set BGSf = Nothing
Set BCSf = Nothing
Exit Sub

CE:
'Beep
GMode = 0
End Sub

Sub KeyConfigCheck() 'キーコンフィグ多重チェック
On Error GoTo CE
Dim KB(6) As Byte
Dim I As Long
KB(0) = KBL: KB(1) = KBR: KB(2) = KBS: KB(3) = KArwL: KB(4) = KArwR: KB(5) = KArwU: KB(6) = KArwD
For I = 0 To 6
If CCPos <> I And KB(I) = MnPB Then KB(I) = KB(CCPos)
Next I
KBL = KB(0): KBR = KB(1): KBS = KB(2): KArwL = KB(3): KArwR = KB(4): KArwU = KB(5): KArwD = KB(6)
Exit Sub

CE:
'Beep
End Sub

Sub PadConfigCheck() 'パッドコンフィグ多重チェック
On Error GoTo CE
Dim PB(2) As Byte
Dim I As Long
PB(0) = PBL(MnJ): PB(1) = PBR(MnJ): PB(2) = PBS(MnJ)
For I = 0 To 2
If CCPos <> I And PB(I) = MnPB Then PB(I) = PB(CCPos)
Next I
PBL(MnJ) = PB(0): PBR(MnJ) = PB(1): PBS(MnJ) = PB(2)
Exit Sub

CE:
'Beep
End Sub

Sub NetLogCr() 'ネットログ作成
On Error GoTo CE
Dim At As VbFileAttribute
Dim I As Long, RR As Long
If StrMsgLogC >= 0 Then
FFi = FreeFile
If XDir(AppDir + "NETLOG.TXT", vbHidden) <> vbNullString$ Then
At = GetAttr(AppDir + "NETLOG.TXT"): If At And vbReadOnly Then SetAttr AppDir + "NETLOG.TXT", At And Not vbReadOnly
Open AppDir + "netlog.txt" For Append As #FFi
RR = 0
Else
Open AppDir + "netlog.txt" For Output As #FFi
RR = -(StrMsgLog(0) = "")
End If
For I = RR To StrMsgLogC
Print #FFi, StrMsgLog(I)
Next I
Close #FFi
End If
Exit Sub

CE:
'Beep
End Sub

Sub HTMLCr() 'マニュアル作成
On Error GoTo CE
Dim At As VbFileAttribute
Dim I As Long, RR As Long, RRR As String
DTClRate
If XDir(AppDir + "DTET.HTM", vbHidden) <> vbNullString$ Then At = GetAttr(AppDir + "DTET.HTM"): If At And vbReadOnly Then SetAttr AppDir + "DTET.HTM", At And Not vbReadOnly
FFi = FreeFile
Open AppDir + "DTET.htm" For Output As #FFi
Print #FFi, "<html>"
Print #FFi, "<head><title>DTET ユーザーズガイド</title></head>"
Print #FFi, "<body bgcolor=" + Chr$(34) + "#ffffff" + Chr$(34) + " text=" + Chr$(34) + "#600040" + Chr$(34) + " link=" + Chr$(34) + "#600040" + Chr$(34) + " alink=" + Chr$(34) + "#600040" + Chr$(34) + " vlink=" + Chr$(34) + "#600040" + Chr$(34) + ">"
Print #FFi, "<hr>"
Print #FFi, "<h3>DTET</h3>"
Print #FFi, "このゲームはいわゆる落ちものパズルです。<br>"
Print #FFi, "ちょっとした暇つぶしをしたい方や、本気で極めたいという方まで、幅広く楽しむことができます。<br>"
Print #FFi, "<br>"
Print #FFi, "<hr>"
Print #FFi, "<h3>動作環境</h3>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr><td>CPU</td><td>Pentium 200 MHz 〜</td></tr>"
Print #FFi, "<tr><td>メモリ</td><td>32 MB 〜</td></tr>"
Print #FFi, "<tr><td>ビデオメモリ</td><td>2.0 MB 〜</td></tr>"
Print #FFi, "<tr><td>空きHDD容量</td><td>20 MB 〜</td></tr>"
Print #FFi, "<tr><td>その他</td><td>DirectX 7.0 〜</td></tr>"
Print #FFi, "</table>"
Print #FFi, "<br>"
Print #FFi, "<hr>"
Print #FFi, "<h3>ルール</h3>"
Print #FFi, "全7種類のブロックが次々と降ってきます。これらをうまく操作して、すき間なく積んでいきましょう。<br>"
Print #FFi, "横にラインをそろえると、そのラインのブロックを消すことができ、得点となります。一度に複数のラインを消すと高得点です。<br>"
Print #FFi, "上まで積みあがり、ブロックが出現できない状態にしてしまうとライフを1つ失い、ブロックが崩壊します。<br>"
Print #FFi, "残りのライフが0になるとゲームオーバーとなります。<br>"
Print #FFi, "<br>"
Print #FFi, "<hr>"
Print #FFi, "<h3>操作方法</h3>"
Print #FFi, "以下のキーに対応しています。また、「OPTIONS → KEY CONFIG( → KEYBOARD)」でボタンの割り当てができます。<br>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr><td colspan=2 bgcolor=" + Chr$(34) + "#e0e080" + Chr$(34) + ">共通の操作</td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#f0f0c0" + Chr$(34) + ">リセット／終了</td><td bgcolor=" + Chr$(34) + "#f8f8e0" + Chr$(34) + "><big><tt>[Esc]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#f0f0c0" + Chr$(34) + ">フルスクリーン切り替え</td><td bgcolor=" + Chr$(34) + "#f8f8e0" + Chr$(34) + "><big><tt>[F4]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#f0f0c0" + Chr$(34) + ">最小化</td><td bgcolor=" + Chr$(34) + "#f8f8e0" + Chr$(34) + "><big><tt>[F5]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#f0f0c0" + Chr$(34) + ">強制終了</td><td bgcolor=" + Chr$(34) + "#f8f8e0" + Chr$(34) + "><big><tt>[Alt＋F4]</tt></big></td></tr>"
Print #FFi, "<tr><td></td></tr>"
Print #FFi, "<tr><td colspan=2 bgcolor=" + Chr$(34) + "#e0ffc0" + Chr$(34) + ">メニューの操作</td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#f0ffe0" + Chr$(34) + ">カーソル左移動</td><td bgcolor=" + Chr$(34) + "#f8fff0" + Chr$(34) + "><big><tt>[←]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#f0ffe0" + Chr$(34) + ">カーソル右移動</td><td bgcolor=" + Chr$(34) + "#f8fff0" + Chr$(34) + "><big><tt>[→]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#f0ffe0" + Chr$(34) + ">カーソル上移動</td><td bgcolor=" + Chr$(34) + "#f8fff0" + Chr$(34) + "><big><tt>[↑]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#f0ffe0" + Chr$(34) + ">カーソル下移動</td><td bgcolor=" + Chr$(34) + "#f8fff0" + Chr$(34) + "><big><tt>[↓]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#f0ffe0" + Chr$(34) + ">決定ボタン</td><td bgcolor=" + Chr$(34) + "#f8fff0" + Chr$(34) + "><big><tt>[Space] [Enter]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#f0ffe0" + Chr$(34) + ">キャンセルボタン</td><td bgcolor=" + Chr$(34) + "#f8fff0" + Chr$(34) + "><big><tt>[Back]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#f0ffe0" + Chr$(34) + ">ポーズ解除／スタート</td><td bgcolor=" + Chr$(34) + "#f8fff0" + Chr$(34) + "><big><tt>[Tab]</tt></big></td></tr>"
Print #FFi, "<tr><td></td></tr>"
Print #FFi, "<tr><td colspan=2 bgcolor=" + Chr$(34) + "#c0e0ff" + Chr$(34) + ">ゲーム中の操作</td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0f0ff" + Chr$(34) + ">ブロック左移動</td><td bgcolor=" + Chr$(34) + "#f0f8ff" + Chr$(34) + "><big><tt>[←]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0f0ff" + Chr$(34) + ">ブロック右移動</td><td bgcolor=" + Chr$(34) + "#f0f8ff" + Chr$(34) + "><big><tt>[→]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0f0ff" + Chr$(34) + ">ブロック反時計回転</td><td bgcolor=" + Chr$(34) + "#f0f8ff" + Chr$(34) + "><big><tt>[Space]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0f0ff" + Chr$(34) + ">ブロック時計回転</td><td bgcolor=" + Chr$(34) + "#f0f8ff" + Chr$(34) + "><big><tt>[Enter]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0f0ff" + Chr$(34) + ">ブロック半回転</td><td bgcolor=" + Chr$(34) + "#f0f8ff" + Chr$(34) + "><big><tt>[左回転＋右回転]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0f0ff" + Chr$(34) + ">ブロック高速落下</td><td bgcolor=" + Chr$(34) + "#f0f8ff" + Chr$(34) + "><big><tt>[↓]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0f0ff" + Chr$(34) + ">ブロック光速落下</td><td bgcolor=" + Chr$(34) + "#f0f8ff" + Chr$(34) + "><big><tt>[↑]</tt></big><font color=" + Chr$(34) + "#ff0000" + Chr$(34) + ">（NORMALでは使用不可）</font></td></tr>"
If ScLiv(2) > 0 Then
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0f0ff" + Chr$(34) + ">RUSH切り替え／ポーズ</td><td bgcolor=" + Chr$(34) + "#f0f8ff" + Chr$(34) + "><big><tt>[Tab]</tt></big></td></tr>"
Else
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0f0ff" + Chr$(34) + ">ポーズ（一時中断）</td><td bgcolor=" + Chr$(34) + "#f0f8ff" + Chr$(34) + "><big><tt>[Tab]</tt></big></td></tr>"
End If
Print #FFi, "</table>"
Print #FFi, "ゲームパッドにも対応しています。パッドを使用している場合、「OPTIONS → KEY CONFIG → PAD」でボタンの割り当てができます。<br>"
Print #FFi, "<br>"
Print #FFi, "<hr>"
Print #FFi, "<h3>メニューの説明</h3>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + ">START</td><td colspan=3 bgcolor=" + Chr$(34) + "#ffe0e0" + Chr$(34) + ">ゲームのメニューです。</td></tr>"
Print #FFi, "<tr><td></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#trn" + Chr$(34) + ">TRAINING</a></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffe0e0" + Chr$(34) + ">練習モードです。いろいろな研究ができます。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + ">NORMAL</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + ">HARD</td></tr>"
If ScLiv(0) > 0 And ScLiv(1) > 0 Then Print #FFi, "<tr><td colspan=2></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + ">ADVANCE</td></tr>"
Print #FFi, "<tr><td></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#tr" + Chr$(34) + ">TRIAL</a></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffe0e0" + Chr$(34) + ">メインとなる1人用トライアルです。攻略度が記録されます。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#s" + Chr$(34) + ">SCORE ATTACK</a></td><td bgcolor=" + Chr$(34) + "#ffe0e0" + Chr$(34) + ">スコアアタックです。一般向けです。</td></tr>"
Print #FFi, "<tr><td colspan=3></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#sn" + Chr$(34) + ">NORMAL</a></td></tr>"
Print #FFi, "<tr><td colspan=3></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#sh" + Chr$(34) + ">HARD</a></td></tr>"
If ScLiv(0) > 0 And ScLiv(1) > 0 Then Print #FFi, "<tr><td colspan=3></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#sa" + Chr$(34) + ">ADVANCE</a></td></tr>"
If ScLv(0) >= 30 Or ScLv(1) >= 50 Then Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#t" + Chr$(34) + ">TIME ATTACK</a></td><td bgcolor=" + Chr$(34) + "#ffe0e0" + Chr$(34) + ">タイムアタックです。中〜上級者向けです。</td></tr>"
If ScLv(0) >= 30 Then Print #FFi, "<tr><td colspan=3></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#tn" + Chr$(34) + ">NORMAL</a></td></tr>"
If ScLv(1) >= 50 Then Print #FFi, "<tr><td colspan=3></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#th" + Chr$(34) + ">HARD</a></td></tr>"
If ScLv(2) >= 200 Then Print #FFi, "<tr><td colspan=3></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#ta" + Chr$(34) + ">ADVANCE</a></td></tr>"
If DTCl >= 900 Then Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#f" + Chr$(34) + ">FINAL</a></td><td bgcolor=" + Chr$(34) + "#ffe0e0" + Chr$(34) + ">最終領域です。最上級者専用です。</td></tr>"
If DTCl >= 900 Then Print #FFi, "<tr><td colspan=3></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#fj" + Chr$(34) + ">JOKER</a></td></tr>"
If FnLv >= 300 Then Print #FFi, "<tr><td colspan=3></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#ff" + Chr$(34) + ">FURTHEST</a></td></tr>"
Print #FFi, "<tr><td></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#fb" + Chr$(34) + ">2P BATTLE</a></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffe0e0" + Chr$(34) + ">2人対戦です。CPとの対戦も可能です。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + ">NORMAL</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + ">HARD</td></tr>"
If ScLiv(0) > 0 And ScLiv(1) > 0 Then Print #FFi, "<tr><td colspan=2></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + ">ADVANCE</td></tr>"
Print #FFi, "<tr><td></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#fb" + Chr$(34) + ">4P BATTLE</a></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffe0e0" + Chr$(34) + ">4人同時対戦です。CPの参加も可能です。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + ">NORMAL</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + ">HARD</td></tr>"
If ScLiv(0) > 0 And ScLiv(1) > 0 Then Print #FFi, "<tr><td colspan=2></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + ">ADVANCE</td></tr>"
Print #FFi, "<tr><td></td><td bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + "><a href=" + Chr$(34) + "#nb" + Chr$(34) + ">NET BATTLE</a></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffe0e0" + Chr$(34) + ">通信対戦です。2人対戦専用です。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + ">NORMAL</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + ">HARD</td></tr>"
If ScLiv(0) > 0 And ScLiv(1) > 0 Then Print #FFi, "<tr><td colspan=2></td><td colspan=2 bgcolor=" + Chr$(34) + "#ffc0c0" + Chr$(34) + ">ADVANCE</td></tr>"
Print #FFi, "<tr><td></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">OPTIONS</td><td colspan=3 bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">さまざまな設定を行います。</td></tr>"
Print #FFi, "<tr><td></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">MIDI</td><td colspan=2 bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">MIDIポートを選択します。</td></tr>"
Print #FFi, "<tr><td></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">SOUND VOLUME</td><td colspan=2 bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">効果音のボリュームを調節します。</td></tr>"
Print #FFi, "<tr><td></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">SOUND STEREO</td><td colspan=2 bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">効果音のステレオレベルを調節します。</td></tr>"
Print #FFi, "<tr><td></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">EFFECT</td><td colspan=2 bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">画面表示に関する設定をします。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">BACKGROUND</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">背景を表示するかを選択します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">BLOCK SPLASH</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">ブロックを消したとき等に飛び散らせるかを選択します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">MOVE SMOOTH</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">操作ブロックを滑らかな動きにするかを選択します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">LOCUS</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">操作ブロックの残像を表示するかを選択します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">BLOCK GRAPHIC</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">ブロックの絵柄を選択します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">FIELD GRAPHIC</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">フィールドの絵柄を選択します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">NEXT BLOCKS</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">表示するNEXTの数を選択します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">CLOCK</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">現在時刻を表示するかを選択します。</td></tr>"
Print #FFi, "<tr><td></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">SYSTEM</td><td colspan=2 bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">システムに関する設定をします。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">DISPLAY MODE</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">使用しているディスプレイを選択します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">FRAME SKIP</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">処理速度を調節するかを選択します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">SCREEN MODE</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">ウィンドウモード／フルスクリーンを選択します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">BG MEMORY</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">プレイ中に背景画像を読み込む場所を選択します。</td></tr>"
Print #FFi, "<tr><td></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">KEY CONFIG</td><td colspan=2 bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">キー入力に関する設定をします。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">ROTATE LEFT / ENTER</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">ブロック反時計回転と決定のボタンを指定します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">ROTATE RIGHT / CANCEL</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">ブロック時計回転とキャンセルのボタンを指定します。</td></tr>"
If ScLiv(2) > 0 Then
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">RUSH / START / STOP</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">RUSH切り替えとポーズボタンを指定します。</td></tr>"
Else
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">START / STOP</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">ポーズボタンを指定します。</td></tr>"
End If
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">LEFT</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">キーボードでの左キーを指定します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">RIGHT</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">キーボードでの右キーを指定します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">UP</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">キーボードでの上キーを指定します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">DOWN</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">キーボードでの下キーを指定します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">CURSOR</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">パッドで斜め入力をしたときのブロックの動きを選択します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">EXIT</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">キー入力の設定を終了します。</td></tr>"
Print #FFi, "<tr><td></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">NETWORK</td><td colspan=2 bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">ネットワークに関する設定をします。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">CONNECT</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">通信対戦機能を使用するかを選択します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">HANDLE NAME</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">ハンドル名を選択します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">TRANSMISSION</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">環境に合わせて通信速度を調節します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">FIELD POSITION</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">左右どちらのフィールドを使用するかを選択します。</td></tr>"
Print #FFi, "<tr><td colspan=2></td><td bgcolor=" + Chr$(34) + "#ffffc0" + Chr$(34) + ">JAPANESE</td><td bgcolor=" + Chr$(34) + "#ffffe0" + Chr$(34) + ">対戦前メッセージの入力に日本語を使用するかを選択します。</td></tr>"
Print #FFi, "<tr><td></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#c0ffff" + Chr$(34) + "><a href=" + Chr$(34) + "#rnk" + Chr$(34) + ">RANKING</a></td><td colspan=3 bgcolor=" + Chr$(34) + "#e0ffff" + Chr$(34) + ">TRIALのランキングを表示します。</td></tr>"
Print #FFi, "<tr><td></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#c0ffc0" + Chr$(34) + "><a href=" + Chr$(34) + "#vd" + Chr$(34) + ">VIDEO</a></td><td colspan=3 bgcolor=" + Chr$(34) + "#e0ffe0" + Chr$(34) + ">TRIALのビデオファイルを再生します。</td></tr>"
Print #FFi, "<tr><td></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#ffc0ff" + Chr$(34) + "><a href=" + Chr$(34) + "#ob" + Chr$(34) + ">MATCH</a></td><td colspan=3 bgcolor=" + Chr$(34) + "#ffe0ff" + Chr$(34) + ">DTETの公式戦を行います。</td></tr>"
Print #FFi, "</table>"
Print #FFi, "<br>"
Print #FFi, "<a name=" + Chr$(34) + "trn" + Chr$(34) + "></a>"
Print #FFi, "<hr>"
Print #FFi, "<h3>TRAINING</h3>"
Print #FFi, "1人用の練習モードです。<br>"
Print #FFi, "SCORE ATTACKで到達したレベルまで選択可能で、苦手なレベルを練習することができます。<br>"
Print #FFi, "また、地形を作っていろいろな実験をすることもできます。<br>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr><td colspan=2 bgcolor=" + Chr$(34) + "#c0c0e0" + Chr$(34) + ">FIELD EDITの操作</td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0e0f0" + Chr$(34) + ">移動</td><td bgcolor=" + Chr$(34) + "#f0f0f8" + Chr$(34) + "><big><tt>[カーソルキー]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0e0f0" + Chr$(34) + ">ブロック追加</td><td bgcolor=" + Chr$(34) + "#f0f0f8" + Chr$(34) + "><big><tt>[決定ボタン]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0e0f0" + Chr$(34) + ">ブロック消去</td><td bgcolor=" + Chr$(34) + "#f0f0f8" + Chr$(34) + "><big><tt>[キャンセルボタン]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0e0f0" + Chr$(34) + ">終了</td><td bgcolor=" + Chr$(34) + "#f0f0f8" + Chr$(34) + "><big><tt>[ポーズ解除／スタート]</tt></big></td></tr>"
Print #FFi, "</table>"
Print #FFi, "<br>"
Print #FFi, "<a name=" + Chr$(34) + "tr" + Chr$(34) + "></a>"
Print #FFi, "<hr>"
Print #FFi, "<h3>TRIAL</h3>"
Print #FFi, "1人用のゲームです。<br>"
Print #FFi, "ランキングのベスト5に入ると名前を登録することができます。<br>"
Print #FFi, "また、プレイした内容をビデオデータとして保存することができます。<br>"
If FnLv < 300 Then Print #FFi, "攻略度によって、新たなモードが追加されることもあります。<br>"
Print #FFi, "<a name=" + Chr$(34) + "s" + Chr$(34) + "></a>"
Print #FFi, "<br>"
Print #FFi, "<table cellspacing=0>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#ffe0a0" + Chr$(34) + ">"
Print #FFi, "<br>"
Print #FFi, "<h3>SCORE ATTACK</h3>"
Print #FFi, "スコアを稼ぐゲームです。消したライン数によってレベルが上がり、自然落下のスピードが変化します。<br>"
Print #FFi, "最高レベルに到達して、一定の時間でクリアとなります。<br>"
Print #FFi, "ライフは5つです。<br>"
Print #FFi, "<br>"
Print #FFi, "</td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#fff0d0" + Chr$(34) + ">"
Print #FFi, "<br>"
Print #FFi, "<h3>スコアに関する基礎知識</h3>"
Print #FFi, "ラインを消したときに加算される基本の得点は、<br>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr><td></td><td>1段消し</td><td>2段同時消し</td><td>3段同時消し</td><td>4段同時消し</td></tr>"
Print #FFi, "<tr><td>LEVEL 0</td><td>40点</td><td>100点</td><td>300点</td><td>1200点</td></tr>"
Print #FFi, "<tr><td>LEVEL 1</td><td>80点</td><td>200点</td><td>600点</td><td>2400点</td></tr>"
Print #FFi, "<tr><td>LEVEL 2</td><td>120点</td><td>300点</td><td>900点</td><td>3600点</td></tr>"
Print #FFi, "<tr><td>LEVEL 3</td><td>160点</td><td>400点</td><td>1200点</td><td>4800点</td></tr>"
Print #FFi, "<tr><td>LEVEL 4</td><td>200点</td><td>500点</td><td>1500点</td><td>6000点</td></tr>"
Print #FFi, "<tr><td>LEVEL 5</td><td>240点</td><td>600点</td><td>1800点</td><td>7200点</td></tr>"
Print #FFi, "<tr><td>LEVEL 10</td><td>440点</td><td>1100点</td><td>3300点</td><td>13200点</td></tr>"
Print #FFi, "<tr><td>LEVEL 15</td><td>640点</td><td>1600点</td><td>4800点</td><td>19200点</td></tr>"
Print #FFi, "<tr><td>LEVEL 20</td><td>840点</td><td>2100点</td><td>6300点</td><td>25200点</td></tr>"
Print #FFi, "<tr><td>LEVEL 25 〜</td><td>1040点 〜</td><td>2600点 〜</td><td>7800点 〜</td><td>31200点 〜</td></tr>"
Print #FFi, "</table>"
Print #FFi, "となっています。<br>"
Print #FFi, "ブロックを連続で消していくと、加算される総得点の倍率が、<br>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr><td>2コンボ</td><td>1.5倍</td></tr>"
Print #FFi, "<tr><td>3コンボ</td><td>2.0倍</td></tr>"
Print #FFi, "<tr><td>4コンボ</td><td>2.5倍</td></tr>"
Print #FFi, "<tr><td>5コンボ</td><td>3.0倍</td></tr>"
Print #FFi, "<tr><td>6コンボ 〜</td><td>3.5倍 〜</td></tr>"
Print #FFi, "</table>"
Print #FFi, "と増えていきます。同時消しと組み合わせると効果的です。<br>"
Print #FFi, "ブロックを自分で落下させると、少しだけ得点が加算されます。レベルが上がると、得点の上昇も大きくなります。<br>"
Print #FFi, "<br>"
Print #FFi, "<a name=" + Chr$(34) + "sn" + Chr$(34) + "></a>"
Print #FFi, "<hr size=1>"
Print #FFi, "<h3>NORMAL</h3>"
Print #FFi, "レベルは0から始まり、最高レベルは30です。<br>"
Print #FFi, "レベルが上がるために必要なラインは、以下のようになっています。<br>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr><td>LEVEL 5</td><td>40ライン</td></tr>"
Print #FFi, "<tr><td>LEVEL 10</td><td>80ライン</td></tr>"
Print #FFi, "<tr><td>LEVEL 15</td><td>130ライン</td></tr>"
Print #FFi, "<tr><td>LEVEL 20</td><td>180ライン</td></tr>"
Print #FFi, "<tr><td>LEVEL 25</td><td>240ライン</td></tr>"
Print #FFi, "<tr><td>LEVEL 30</td><td>300ライン</td></tr>"
Print #FFi, "</table>"
Print #FFi, "レベル20で一度落下速度がゆっくりとなりますが、それ以降は速度の上がり方が急激になり、だんだんと空中での動作が困難になります。<br>"
Print #FFi, "しかし地形に接した後でも、およそ0.5秒間は操作が可能です。ブロックが光った瞬間、完全に固定となります。<br>"
Print #FFi, "最高レベルに到達すると、タイムリミットが表示されます。あとは時間切れまで耐え抜くことができれば終了です。<br>"
Print #FFi, "<br>"
Print #FFi, "<a name=" + Chr$(34) + "sh" + Chr$(34) + "></a>"
Print #FFi, "<hr size=1>"
Print #FFi, "<h3>HARD</h3>"
Print #FFi, "レベルは0から始まり、最高レベルは50です。<br>"
Print #FFi, "レベルが上がるために必要なラインは、以下のようになっています。<br>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr><td>LEVEL 10</td><td>80ライン</td></tr>"
Print #FFi, "<tr><td>LEVEL 20</td><td>170ライン</td></tr>"
Print #FFi, "<tr><td>LEVEL 30</td><td>270ライン</td></tr>"
Print #FFi, "<tr><td>LEVEL 40</td><td>380ライン</td></tr>"
Print #FFi, "<tr><td>LEVEL 50</td><td>500ライン</td></tr>"
Print #FFi, "</table>"
Print #FFi, "NORMALに比べ、ライン消しの時間や左右移動のオートリピートまでの時間などが若干短くなります。<br>"
Print #FFi, "また、上キーによる光速落下（通称ハードドロップ）を使用することができます。<br>"
Print #FFi, "レベル30以降も落下速度は上がり続けます。スライド操作が必須になるほか、先行入力や壁蹴りなどの上級テクニックが重要になります。<br>"
Print #FFi, "<br>"
If ScLiv(0) > 0 And ScLiv(1) > 0 Then
Print #FFi, "<a name=" + Chr$(34) + "sa" + Chr$(34) + "></a>"
Print #FFi, "<hr size=1>"
Print #FFi, "<h3>ADVANCE</h3>"
Print #FFi, "レベルは0から始まり、最高レベルは200です。<br>"
Print #FFi, "レベルが上がるために必要なラインは、以下のようになっています。<br>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr><td>LEVEL 10</td><td>80ライン</td></tr>"
Print #FFi, "<tr><td>LEVEL 20</td><td>160ライン</td></tr>"
Print #FFi, "<tr><td>LEVEL 30</td><td>240ライン</td></tr>"
Print #FFi, "<tr><td>LEVEL 40</td><td>320ライン</td></tr>"
Print #FFi, "<tr><td>LEVEL 50</td><td>400ライン</td></tr>"
Print #FFi, "<tr><td>LEVEL 100</td><td>450ライン以上</td></tr>"
Print #FFi, "<tr><td>LEVEL 150</td><td>500ライン以上</td></tr>"
Print #FFi, "<tr><td>LEVEL 200</td><td>550ライン以上</td></tr>"
Print #FFi, "</table>"
Print #FFi, "上キーによる光速落下の効果がHARDとは異なり、さらにすばやく操作することができます。<br>"
Print #FFi, "ゲーム開始時からタイムリミットが表示されます。最高レベルに到達していない場合でも、タイムが0になった時点で終了となってしまいます。<br>"
Print #FFi, "タイムはレベルが上がるたびに延長されます。<br>"
Print #FFi, "レベル50以降は、ライン消しを1回するたびにレベルアップです。<br>"
Print #FFi, "落下速度が最大で、さらに固定までの時間も短くなり、判断ミスや操作ミスが致命的になります。<br>"
Print #FFi, "<br>"
End If
Print #FFi, "</td></tr>"
Print #FFi, "</table>"
If ScLv(0) >= 30 Or ScLv(1) >= 50 Then
Print #FFi, "<a name=" + Chr$(34) + "t" + Chr$(34) + "></a>"
Print #FFi, "<br>"
Print #FFi, "<table cellspacing=0>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#a0ffe0" + Chr$(34) + ">"
Print #FFi, "<br>"
Print #FFi, "<h3>TIME ATTACK</h3>"
Print #FFi, "タイムを競うゲームです。テクニックよりもスピードが重要となります。<br>"
Print #FFi, "レベルは最高の状態からスタートし、何ライン消しても変化しません。<br>"
Print #FFi, "100ライン達成した瞬間にクリアとなります。<br>"
Print #FFi, "ライフは3つです。時間制限はありません。<br>"
If ScLv(0) < 30 Then Print #FFi, "<a name=" + Chr$(34) + "th" + Chr$(34) + "></a>" Else Print #FFi, "<a name=" + Chr$(34) + "tn" + Chr$(34) + "></a>"
Print #FFi, "<br>"
Print #FFi, "</td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#d0fff0" + Chr$(34) + ">"
If ScLv(0) >= 30 Then
Print #FFi, "<br>"
Print #FFi, "<h3>NORMAL</h3>"
Print #FFi, "レベルは30です。<br>"
Print #FFi, "落下速度は変化しないので、ブロックを固定するスピードが重要になります。<br>"
Print #FFi, "ライン消しの時間は一定なので、1段消しよりも同時消しが有利となります。<br>"
Print #FFi, "<br>"
End If
If ScLv(1) >= 50 Then
If ScLv(0) >= 30 Then Print #FFi, "<a name=" + Chr$(34) + "th" + Chr$(34) + "></a>"
If ScLv(0) >= 30 Then Print #FFi, "<hr size=1>" Else Print #FFi, "<br>"
Print #FFi, "<h3>HARD</h3>"
Print #FFi, "レベルは50です。<br>"
Print #FFi, "無駄のない操作が要求されます。<br>"
Print #FFi, "ライン消しの時間はかなり短いですが、同時消しが若干有利となります。<br>"
Print #FFi, "終了直前にブロックが多く残っているとタイムロスとなってしまいます。<br>"
Print #FFi, "<br>"
End If
If ScLv(2) >= 200 Then
Print #FFi, "<a name=" + Chr$(34) + "ta" + Chr$(34) + "></a>"
Print #FFi, "<hr size=1>"
Print #FFi, "<h3>ADVANCE</h3>"
Print #FFi, "レベルは200です。<br>"
Print #FFi, "ライン消しの時間は存在しないので、1段消しが多くてもタイムに全く影響がありません。<br>"
Print #FFi, "純粋にプレイヤーのスピードが反映されます。<br>"
Print #FFi, "上キーを使用するには、かなりの技術が必要です。<br>"
Print #FFi, "<br>"
End If
Print #FFi, "</td></tr>"
Print #FFi, "</table>"
End If
If DTCl >= 900 Then
Print #FFi, "<a name=" + Chr$(34) + "f" + Chr$(34) + "></a>"
Print #FFi, "<br>"
Print #FFi, "<table cellspacing=0>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#c0a0ff" + Chr$(34) + ">"
Print #FFi, "<br>"
Print #FFi, "<h3>FINAL</h3>"
Print #FFi, "DTETの最終領域です。難易度は最高クラスです。<br>"
Print #FFi, "<a name=" + Chr$(34) + "fj" + Chr$(34) + "></a>"
Print #FFi, "<br>"
Print #FFi, "</td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0c0ff" + Chr$(34) + ">"
Print #FFi, "<br>"
Print #FFi, "<h3>JOKER</h3>"
Print #FFi, "レベルは50から始まります。<br>"
Print #FFi, "究極のテクニックと持久力を要します。自分の実力を信じてクリアを目指しましょう。<br>"
Print #FFi, "<br>"
If FnLv >= 300 Then
Print #FFi, "<a name=" + Chr$(34) + "ff" + Chr$(34) + "></a>"
Print #FFi, "<hr size=1>"
Print #FFi, "<h3>FURTHEST</h3>"
Print #FFi, "DTETの最果てに突入します。<br>"
Print #FFi, "とにかくスピードに耐えるしか方法はありません。健闘を祈ります。<br>"
Print #FFi, "<br>"
End If
Print #FFi, "</td></tr>"
Print #FFi, "</table>"
End If
Print #FFi, "<br>"
Print #FFi, "<font color=" + Chr$(34) + "#ff0000" + Chr$(34) + ">終了時、システムスピードが90％未満の場合、歴代ランキングに登録することはできません。</font><br>"
Print #FFi, "プレイ中、95％未満になると、画面右下に警告が表示されます。<br>"
Print #FFi, "処理速度を上げるには、OPTIONで、ゲーム中の処理が軽くなるように設定する必要があります。<br>"
Print #FFi, "<br>"
Print #FFi, "<a name=" + Chr$(34) + "fb" + Chr$(34) + "></a>"
Print #FFi, "<hr>"
Print #FFi, "<h3>FREE BATTLE 2P / FREE BATTLE 4P</h3>"
Print #FFi, "友達やCPと対戦するモードです。ハンディキャップをつけることもできます。<br>"
Print #FFi, "<br>"
Print #FFi, "対戦を開始する前にレベルとライフ数を選択します。このときにボタンを押すことにより、別のプレイヤーも参加することができます。<br>"
Print #FFi, "残った部分はCPが参加します。レベルとライフ数のほか、CPの強さを決めることもできます。<br>"
Print #FFi, "グレードの数字が大きいほど強いCPとなります。<br>"
Print #FFi, "<br>"
Print #FFi, "対戦中に、同時消し、または連続消しをすることにより、相手のフィールドをせり上げることができます。<br>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr><td></td><td>1段消し</td><td>2段同時消し</td><td>3段同時消し</td><td>4段同時消し</td></tr>"
Print #FFi, "<tr><td>通常</td><td>なし</td><td>1段攻撃</td><td>2段攻撃</td><td>4段攻撃</td></tr>"
Print #FFi, "<tr><td>コンボ時</td><td>1段攻撃</td><td>2段攻撃</td><td>3段攻撃</td><td>6段攻撃</td></tr>"
Print #FFi, "</table>"
Print #FFi, "攻撃を受けた相手は、フィールドの横に青色マークが表示されます。これが赤色になったときに、下からせり上がります。<br>"
Print #FFi, "3段同時消し以下の場合、自分の赤色マークを相殺することができます。<br>"
Print #FFi, "<br>"
Print #FFi, "最終的にライフが残ったプレイヤーの勝利となります。<br>"
Print #FFi, "<br>"
Print #FFi, "<a name=" + Chr$(34) + "nb" + Chr$(34) + "></a>"
Print #FFi, "<hr>"
Print #FFi, "<h3>NETWORK BATTLE</h3>"
Print #FFi, "TCP/IPを使ってネットワーク対戦をするモードです。<br>"
Print #FFi, "対戦するためには、互いのマシンがインターネットに接続されている必要があります。<br>"
Print #FFi, "<br>"
Print #FFi, "まず、「OPTIONS → NETWORK」でネットワークの設定をします。<br>"
Print #FFi, "「HANDLE NAME」で自分のハンドル名を選択し、「CONNECT」を[ON]にします。<br>"
Print #FFi, "<br>"
Print #FFi, "次に、どちらか一方が「START → NET BATTLE」を決定します。このとき、相手のIPアドレスを入力します。<br>"
Print #FFi, "成功しなかった場合やキャンセルした場合、タイトル画面に戻ります。<br>"
Print #FFi, "<br>"
Print #FFi, "もう一方の画面右上には対戦相手のハンドル名が表示されます。そして、「START → NET BATTLE」で開始となります。<br>"
If XDir(AppDir + "NETLOG.TXT", vbHidden) <> vbNullString$ Then RRR = "<a target=" + Chr$(34) + "_blank" + Chr$(34) + " href=" + Chr$(34) + "./netlog.txt" + Chr$(34) + ">ログファイル</a>" Else RRR = "ログファイル"
Print #FFi, "対戦結果は" + RRR + "に出力されます。<br>"
Print #FFi, "<br>"
Print #FFi, "対戦前、キーボード入力で相手にメッセージを送ることができます。<br>"
Print #FFi, "「OPTIONS → NETWORK」で、「JAPANESE」が[OFF]の場合、「半角文字」のみ使用可能です。<br>"
Print #FFi, "「JAPANESE」が[ON]の場合、「半角文字」「全角ひらがな」「全角カタカナ」を使用することができます。<br>"
If XDir(AppDir + "DIC.TXT", vbHidden) <> vbNullString$ Then RRR = "<a target=" + Chr$(34) + "_blank" + Chr$(34) + " href=" + Chr$(34) + "./dic.txt" + Chr$(34) + ">辞書ファイル</a>" Else RRR = "辞書ファイル"
Print #FFi, "また、" + RRR + "が存在する場合は、ファイルに書かれている内容の単語や記号等も使用できます。<br>"
Print #FFi, "<br>"
Print #FFi, "辞書ファイルは、自由にカスタマイズすることが可能です。<br>"
Print #FFi, "最初に[ ]内で使用する単語、その次に( )内で読みがなを指定します。<br>"
Print #FFi, "登録できる単語と読みがなの数は、最大で500個です。<br>"
Print #FFi, "<br>"
Print #FFi, "<a name=" + Chr$(34) + "vd" + Chr$(34) + "></a>"
Print #FFi, "<hr>"
Print #FFi, "<h3>VIDEO</h3>"
Print #FFi, "TRIALで保存したビデオデータを観賞するモードです。<br>"
Print #FFi, "選択したファイルのビデオデータが再生されます。<br>"
Print #FFi, "再生中は、以下の操作が可能です。<br>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr><td colspan=2 bgcolor=" + Chr$(34) + "#c0e0c0" + Chr$(34) + ">VIDEOの操作</td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0f0e0" + Chr$(34) + ">再生速度変更</td><td bgcolor=" + Chr$(34) + "#f0f8f0" + Chr$(34) + "><big><tt>[カーソルキー左右]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0f0e0" + Chr$(34) + ">一時停止／コマ送り</td><td bgcolor=" + Chr$(34) + "#f0f8f0" + Chr$(34) + "><big><tt>[カーソルキー下]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0f0e0" + Chr$(34) + ">ボタン非表示</td><td bgcolor=" + Chr$(34) + "#f0f8f0" + Chr$(34) + "><big><tt>[カーソルキー上]</tt></big></td></tr>"
Print #FFi, "<tr><td bgcolor=" + Chr$(34) + "#e0f0e0" + Chr$(34) + ">停止</td><td bgcolor=" + Chr$(34) + "#f0f8f0" + Chr$(34) + "><big><tt>[キャンセルボタン] [ポーズ解除／スタート]</tt></big></td></tr>"
Print #FFi, "</table>"
Print #FFi, "ビデオを停止すると、TRIALのポーズ状態と同じになり、以後プレイヤーが操作することができます。<br>"
Print #FFi, "ただし、プレイ内容は記録されません。<br>"
Print #FFi, "<br>"
Print #FFi, "<a name=" + Chr$(34) + "ob" + Chr$(34) + "></a>"
Print #FFi, "<hr>"
Print #FFi, "<h3>OFFICIAL BATTLE</h3>"
Print #FFi, "1対1の公式試合をするモードです。イベントバトル等に利用できます。<br>"
Print #FFi, "あらかじめ決められたレベルとライフ数で対戦します。<br>"
Print #FFi, "最終的にライフが残ったプレイヤーの勝利となります。<br>"
Print #FFi, "<br>"
Print #FFi, "<hr>"
Print #FFi, "<h3>TRIALプレイ状況</h3>"
Print #FFi, "現在の攻略率は、 <big><tt>" + Mid$(Str$(Int(DTCl / 10)), 2) + "." + Mid$(Str$(DTCl Mod 10), 2) + "</tt></big> ％となっています。<br>"
Print #FFi, "ライフの総数は、 <big><tt>" + Mid$(Str$(Int(DTLiv)), 2) + "</tt></big> 個です。<br>"
Print #FFi, "CPのグレードは、 <big><tt>" + Mid$(Str$(Int(CPMax)), 2) + "</tt></big> です。<br>"
Print #FFi, "<br>"
Print #FFi, "SCORE ATTACKの攻略度は、以下のようになっています。<br>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr>"
Print #FFi, "<td></td>"
Print #FFi, "<td>NORMAL</td>"
Print #FFi, "<td>HARD</td>"
If ScLiv(0) > 0 And ScLiv(1) > 0 Then Print #FFi, "<td>ADVANCE</td>"
Print #FFi, "</tr>"
Print #FFi, "<tr>"
Print #FFi, "<td>LEVEL</td>"
Print #FFi, "<td><big><tt>" + Mid$(Str$(ScLv(0)), 2) + "</tt></big><small> / 30</small></td>"
Print #FFi, "<td><big><tt>" + Mid$(Str$(ScLv(1)), 2) + "</tt></big><small> / 50</small></td>"
If ScLiv(0) > 0 And ScLiv(1) > 0 Then Print #FFi, "<td><big><tt>" + Mid$(Str$(ScLv(2)), 2) + "</tt></big><small> / 200</small></td>"
Print #FFi, "</tr>"
Print #FFi, "<tr>"
Print #FFi, "<td>LIVES</td>"
Print #FFi, "<td><big><tt>" + String$(ScLiv(0), "★") + Left$("<font color=" + Chr$(34) + "#d8c0d0" + Chr$(34) + ">", -(ScLiv(0) < 5) * 22) + String$(5 - ScLiv(0), "☆") + Left$("</font>", -(ScLiv(0) < 5) * 7) + "</tt></big></td>"
Print #FFi, "<td><big><tt>" + String$(ScLiv(1), "★") + Left$("<font color=" + Chr$(34) + "#d8c0d0" + Chr$(34) + ">", -(ScLiv(1) < 5) * 22) + String$(5 - ScLiv(1), "☆") + Left$("</font>", -(ScLiv(1) < 5) * 7) + "</tt></big></td>"
If ScLiv(0) > 0 And ScLiv(1) > 0 Then Print #FFi, "<td><big><tt>" + String$(ScLiv(2), "★") + Left$("<font color=" + Chr$(34) + "#d8c0d0" + Chr$(34) + ">", -(ScLiv(2) < 5) * 22) + String$(5 - ScLiv(2), "☆") + Left$("</font>", -(ScLiv(2) < 5) * 7) + "</tt></big></td>"
Print #FFi, "</tr>"
Print #FFi, "</table>"
Print #FFi, "<br>"
If ScLv(0) >= 30 Or ScLv(1) >= 50 Then
Print #FFi, "TIME ATTACKの攻略度は、以下のようになっています。<br>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr>"
Print #FFi, "<td></td>"
If ScLv(0) >= 30 Then Print #FFi, "<td>NORMAL</td>"
If ScLv(1) >= 50 Then Print #FFi, "<td>HARD</td>"
If ScLv(2) >= 200 Then Print #FFi, "<td>ADVANCE</td>"
Print #FFi, "</tr>"
Print #FFi, "<tr>"
Print #FFi, "<td>LIVES</td>"
If ScLv(0) >= 30 Then Print #FFi, "<td><big><tt>" + String$(TmLiv(0), "★") + Left$("<font color=" + Chr$(34) + "#d8c0d0" + Chr$(34) + ">", -(TmLiv(0) < 3) * 22) + String$(3 - TmLiv(0), "☆") + Left$("</font>", -(TmLiv(0) < 3) * 7) + "</tt></big></td>"
If ScLv(1) >= 50 Then Print #FFi, "<td><big><tt>" + String$(TmLiv(1), "★") + Left$("<font color=" + Chr$(34) + "#d8c0d0" + Chr$(34) + ">", -(TmLiv(1) < 3) * 22) + String$(3 - TmLiv(1), "☆") + Left$("</font>", -(TmLiv(1) < 3) * 7) + "</tt></big></td>"
If ScLv(2) >= 200 Then Print #FFi, "<td><big><tt>" + String$(TmLiv(2), "★") + Left$("<font color=" + Chr$(34) + "#d8c0d0" + Chr$(34) + ">", -(TmLiv(2) < 3) * 22) + String$(3 - TmLiv(2), "☆") + Left$("</font>", -(TmLiv(2) < 3) * 7) + "</tt></big></td>"
Print #FFi, "</tr>"
Print #FFi, "</table>"
Print #FFi, "<br>"
End If
If DTCl >= 900 Then
Print #FFi, "FINALの攻略度は、以下のようになっています。<br>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr><td colspan=2>JOKER</td></tr>"
Print #FFi, "<tr><td>LEVEL</td><td><big><tt>" + Mid$(Str$(FnLv), 2) + "</tt></big></td></tr>"
Print #FFi, "<tr><td>LIVES</td><td><big><tt>" + String$(-(FnLv >= 300), "★") + Left$("<font color=" + Chr$(34) + "#d8c0d0" + Chr$(34) + ">", -(FnLv < 300) * 22) + String$(-(FnLv < 300), "☆") + Left$("</font>", -(FnLv < 300) * 7) + "</tt></big></td></tr>"
If FnLv >= 300 Then
Print #FFi, "<tr><td></td></tr>"
Print #FFi, "<tr><td colspan=2>FURTHEST</td></tr>"
Print #FFi, "<tr><td>LIVES</td><td><big><tt>" + String$(LsLiv, "★") + Left$("<font color=" + Chr$(34) + "#d8c0d0" + Chr$(34) + ">", -(LsLiv < 10) * 22) + String$(10 - LsLiv, "☆") + Left$("</font>", -(LsLiv < 10) * 7) + "</tt></big></td></tr>"
End If
Print #FFi, "</table>"
Print #FFi, "<br>"
End If
Print #FFi, "<a name=" + Chr$(34) + "rnk" + Chr$(34) + "></a>"
Print #FFi, "<hr>"
Print #FFi, "<h3>TRIALランキング</h3>"
Print #FFi, "SCORE ATTACKのランキングです。<br>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr>"
Print #FFi, "<td colspan=4>NORMAL</td>"
Print #FFi, "<td></td>"
Print #FFi, "<td colspan=4>HARD</td>"
If ScLiv(0) > 0 And ScLiv(1) > 0 Then Print #FFi, "<td></td>"
If ScLiv(0) > 0 And ScLiv(1) > 0 Then Print #FFi, "<td colspan=4>ADVANCE</td>"
Print #FFi, "</tr>"
Print #FFi, "<tr>"
Print #FFi, "<td>RANK</td><td>NAME</td><td>SCORE</td><td>LINES</td>"
Print #FFi, "<td></td>"
Print #FFi, "<td>RANK</td><td>NAME</td><td>SCORE</td><td>LINES</td>"
If ScLiv(0) > 0 And ScLiv(1) > 0 Then Print #FFi, "<td></td>"
If ScLiv(0) > 0 And ScLiv(1) > 0 Then Print #FFi, "<td>RANK</td><td>NAME</td><td>SCORE</td><td>LINES</td>"
Print #FFi, "</tr>"
For I = 0 To 4
Print #FFi, "<tr>"
RR = InStr(SRnk(I).Nm, "\"): If RR > 0 Then RR = RR - 1 Else RR = 6
Print #FFi, "<td align=right>" + Mid$(Str$(I + 1), 2) + "</td><td><big><tt>" + Left$(SRnk(I).Nm, RR) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(SRnk(I).SC), 2) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(SRnk(I).Li), 2) + "</tt></big></td>"
Print #FFi, "<td></td>"
RR = InStr(SRnk(5 + I).Nm, "\"): If RR > 0 Then RR = RR - 1 Else RR = 6
Print #FFi, "<td align=right>" + Mid$(Str$(I + 1), 2) + "</td><td><big><tt>" + Left$(SRnk(5 + I).Nm, RR) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(SRnk(5 + I).SC), 2) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(SRnk(5 + I).Li), 2) + "</tt></big></td>"
If ScLiv(0) > 0 And ScLiv(1) > 0 Then Print #FFi, "<td></td>"
If ScLiv(0) > 0 And ScLiv(1) > 0 Then RR = InStr(SRnk(10 + I).Nm, "\"): If RR > 0 Then RR = RR - 1 Else RR = 6
If ScLiv(0) > 0 And ScLiv(1) > 0 Then Print #FFi, "<td align=right>" + Mid$(Str$(I + 1), 2) + "</td><td><big><tt>" + Left$(SRnk(10 + I).Nm, RR) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(SRnk(10 + I).SC), 2) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(SRnk(10 + I).Li), 2) + "</tt></big></td>"
Print #FFi, "</tr>"
Next I
Print #FFi, "</table>"
Print #FFi, "<br>"
If ScLv(0) >= 30 Or ScLv(1) >= 50 Then
Print #FFi, "TIME ATTACKのランキングです。<br>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr>"
If ScLv(0) >= 30 Then Print #FFi, "<td colspan=4>NORMAL</td>"
If ScLv(0) >= 30 And ScLv(1) >= 50 Then Print #FFi, "<td></td>"
If ScLv(1) >= 50 Then Print #FFi, "<td colspan=4>HARD</td>"
If ScLv(2) >= 200 Then Print #FFi, "<td></td>"
If ScLv(2) >= 200 Then Print #FFi, "<td colspan=4>ADVANCE</td>"
Print #FFi, "</tr>"
Print #FFi, "<tr>"
If ScLv(0) >= 30 Then Print #FFi, "<td>RANK</td><td>NAME</td><td>TIME</td><td>LINES</td>"
If ScLv(0) >= 30 And ScLv(1) >= 50 Then Print #FFi, "<td></td>"
If ScLv(1) >= 50 Then Print #FFi, "<td>RANK</td><td>NAME</td><td>TIME</td><td>LINES</td>"
If ScLv(2) >= 200 Then Print #FFi, "<td></td>"
If ScLv(2) >= 200 Then Print #FFi, "<td>RANK</td><td>NAME</td><td>TIME</td><td>LINES</td>"
Print #FFi, "</tr>"
For I = 0 To 4
Print #FFi, "<tr>"
If ScLv(0) >= 30 Then RR = InStr(TRnk(I).Nm, "\"): If RR > 0 Then RR = RR - 1 Else RR = 6
If ScLv(0) >= 30 Then Print #FFi, "<td align=right>" + Mid$(Str$(I + 1), 2) + "</td><td><big><tt>" + Left$(TRnk(I).Nm, RR) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(Int(TRnk(I).Tm / 6000)), 2) + Chr$(39) + Left$("0", -((Int(TRnk(I).Tm / 100) Mod 60) < 10)) + Mid$(Str$(Int(TRnk(I).Tm / 100) Mod 60), 2) + Chr$(34) + Left$("0", -((TRnk(I).Tm Mod 100) < 10)) + Mid$(Str$(TRnk(I).Tm Mod 100), 2) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(TRnk(I).Li), 2) + "</tt></big></td>"
If ScLv(0) >= 30 And ScLv(1) >= 50 Then Print #FFi, "<td></td>"
If ScLv(1) >= 50 Then RR = InStr(TRnk(5 + I).Nm, "\"): If RR > 0 Then RR = RR - 1 Else RR = 6
If ScLv(1) >= 50 Then Print #FFi, "<td align=right>" + Mid$(Str$(I + 1), 2) + "</td><td><big><tt>" + Left$(TRnk(5 + I).Nm, RR) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(Int(TRnk(5 + I).Tm / 6000)), 2) + Chr$(39) + Left$("0", -((Int(TRnk(5 + I).Tm / 100) Mod 60) < 10)) + Mid$(Str$(Int(TRnk(5 + I).Tm / 100) Mod 60), 2) + Chr$(34) + Left$("0", -((TRnk(5 + I).Tm Mod 100) < 10)) + Mid$(Str$(TRnk(5 + I).Tm Mod 100), 2) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(TRnk(5 + I).Li), 2) + "</tt></big></td>"
If ScLv(2) >= 200 Then Print #FFi, "<td></td>"
If ScLv(2) >= 200 Then RR = InStr(TRnk(10 + I).Nm, "\"): If RR > 0 Then RR = RR - 1 Else RR = 6
If ScLv(2) >= 200 Then Print #FFi, "<td align=right>" + Mid$(Str$(I + 1), 2) + "</td><td><big><tt>" + Left$(TRnk(10 + I).Nm, RR) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(Int(TRnk(10 + I).Tm / 6000)), 2) + Chr$(39) + Left$("0", -((Int(TRnk(10 + I).Tm / 100) Mod 60) < 10)) + Mid$(Str$(Int(TRnk(10 + I).Tm / 100) Mod 60), 2) + Chr$(34) + Left$("0", -((TRnk(10 + I).Tm Mod 100) < 10)) + Mid$(Str$(TRnk(10 + I).Tm Mod 100), 2) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(TRnk(10 + I).Li), 2) + "</tt></big></td>"
Print #FFi, "</tr>"
Next I
Print #FFi, "</table>"
Print #FFi, "<br>"
End If
If DTCl >= 900 Then
Print #FFi, "FINALのランキングです。<br>"
Print #FFi, "<table border=1 cellspacing=1>"
Print #FFi, "<tr>"
Print #FFi, "<td colspan=4>JOKER</td>"
If FnLv >= 300 Then Print #FFi, "<td></td>"
If FnLv >= 300 Then Print #FFi, "<td colspan=4>FURTHEST</td>"
Print #FFi, "</tr>"
Print #FFi, "<tr>"
Print #FFi, "<td>RANK</td><td>NAME</td><td>LEVEL</td><td>TIME</td>"
If FnLv >= 300 Then Print #FFi, "<td></td>"
If FnLv >= 300 Then Print #FFi, "<td>RANK</td><td>NAME</td><td>LIVES</td><td>TIME</td>"
Print #FFi, "</tr>"
For I = 0 To 4
Print #FFi, "<tr>"
RR = InStr(FRnk(I).Nm, "\"): If RR > 0 Then RR = RR - 1 Else RR = 6
Print #FFi, "<td align=right>" + Mid$(Str$(I + 1), 2) + "</td><td><big><tt>" + Left$(FRnk(I).Nm, RR) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(FRnk(I).Lv), 2) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(Int(FRnk(I).Tm / 6000)), 2) + Chr$(39) + Left$("0", -((Int(FRnk(I).Tm / 100) Mod 60) < 10)) + Mid$(Str$(Int(FRnk(I).Tm / 100) Mod 60), 2) + Chr$(34) + Left$("0", -((FRnk(I).Tm Mod 100) < 10)) + Mid$(Str$(FRnk(I).Tm Mod 100), 2) + "</tt></big></td>"
If FnLv >= 300 Then Print #FFi, "<td></td>"
If FnLv >= 300 Then RR = InStr(LRnk(I).Nm, "\"): If RR > 0 Then RR = RR - 1 Else RR = 6
If FnLv >= 300 Then Print #FFi, "<td align=right>" + Mid$(Str$(I + 1), 2) + "</td><td><big><tt>" + Left$(LRnk(I).Nm, RR) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(LRnk(I).Liv), 2) + "</tt></big></td><td align=right><big><tt>" + Mid$(Str$(Int(LRnk(I).Tm / 6000)), 2) + Chr$(39) + Left$("0", -((Int(LRnk(I).Tm / 100) Mod 60) < 10)) + Mid$(Str$(Int(LRnk(I).Tm / 100) Mod 60), 2) + Chr$(34) + Left$("0", -((LRnk(I).Tm Mod 100) < 10)) + Mid$(Str$(LRnk(I).Tm Mod 100), 2) + "</tt></big></td>"
Print #FFi, "</tr>"
Next I
Print #FFi, "</table>"
Print #FFi, "<br>"
End If
Print #FFi, "<hr>"
Print #FFi, "</body>"
Print #FFi, "</html>"
Close #FFi
Exit Sub

CE:
'Beep
End Sub
