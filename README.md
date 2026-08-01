# FixIO

マインクラフトの一部処理をc言語で実装された高速ライブラリに置き換えます

## 修正一覧

- ネットワーク暗号化(Java Cipher to OpenSSL)
- ネットワーク圧縮(Java Deflate to libdeflate)
- Nbt圧縮
  - ファイルの読み書きをcで実装
  - 圧縮回りをlibdeflateに
- ワールドデータ圧縮(Java deflate to libdeflate)

## 動作環境

Fabric 26.2 Server (not running on client)

Fabric-Api不要

## その他

バグって落ちても責任取れません

テストコード及び、一部コメントが書かれている部分はGemini君に頑張ってもらいました

大規模サーバーじゃないとあまり効果でないかも？
