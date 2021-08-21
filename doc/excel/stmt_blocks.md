## 制御文
### begin ステートメント
指定された要素の繰り返し処理を開始します。

|パラメーター|概要|例|ヘッダー/フッター|シフト|
|:-----------|:---|:-|:---------------:|:-:|
|fields|フィールド定義を繰り返します。|`{#begin fields}`|〇|×|
|codes|コード記述を開始します。|`{#begin codes}`|×|×|
|description|処理記述を開始します。|`{#begin description}`|×|〇|
|assignment|代入編集記述を開始します。|`{#begin assignment}`|〇|〇|
|condition|条件分岐記述を開始します。|`{#begin condition}`|〇|〇|

#### コード記述(codes)
コード記述ブロック中(codes)に以下の記述を含める必要があります。

- 処理記述(description)
- 代入編集記述(assignment)
- 条件分岐記述(condition)

なお、上記3つのブロックは、コード記述ブロック外に書いても何の効果もありません。

#### 追加パラメーター
begin ステートメントでは追加パラメーターを指定できるものがあります。

|パラメーター|概要|例|
|:-----------|:---|:-|
|header|ヘッダー行数を指定します。|`{#begin condition header:2}`|
|footer|フッター行数を指定します。|`{#begin condition footer:1}`|
|shift|段落右シフトを指定します。|`{#begin description shift:1,5}`|

追加パラメーターは同時に複数指定も可能です。(例:`{#begin fields header:2 footer:1}`)

### end ステートメント
指定された要素の繰り返し処理を終了します。
