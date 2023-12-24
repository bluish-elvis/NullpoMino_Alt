# ルール設定項目


## 選択できるNEXT順生成アルゴリズム

BagRandomizer
: ７種全てのピースで１巡していく。一番最初にSZOが出ないバージョンや、１巡中のピースが＋１、－１、×２、×９になるバージョンも有る。

History*RollsRandomizer
: 一番最初にSZOが出ないほか、一度出たピースは4ピースの間は出にくくなる。
: 4Rolls>6Rolls>Strictの順にダブりにくくなる。

Memory-lessRandomizer
: 補正なし。

NintendoRandomizer
: 同じピースが連続しない

GameBoyRandomizer
: 同じ＋特定ピースが連続してこない

FixedSequence
: sequence.txtの順番を読み込む。

DistanceWeight
: 全ピースが平等に出るように都度当選確率が変動する。
