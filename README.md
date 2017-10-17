# burpextender-proxyhistory-webui

Burp Extender : Proxy History viewer in Web UI

特徴：

* Proxy History のHTTP通信を独自のログに書き込み、Burp拡張内部で起動する独自WebUIで表示できるようにします。
* 独自WebUI上からリクエスト/レスポンスの文字コードを変更できます。
  * Burp側で文字化けしてしまっても、独自WebUI上で正しい文字コードで表示できるようになります。
  * Burpの画面上でコピペすると、日本語などで切れてしまいますが、独自WebUI上ではtextareaタグの中で表示するので、安全にコピペできます。

About Burp Extender : https://portswigger.net/burp/help/extender.html

## Burp requirement

* Java8
* Burp Suite Free / Pro >= 1.7.x
  * https://portswigger.net/burp

## 基本的な使い方

1. jarファイルをDL
2. Burp Suiteを起動し、"Extender" タブから jar ファイルとして追加
3. "ProxyHistoryWebUI" タブが追加されたことを確認し、"Start Web UI & Logging" ボタンをクリック
   - 起動に成功したメッセージボックスが表示されたら "OK" ボタンをクリック
   - → 内部WebUIを標準ブラウザで開きます。
4. 終了する時は "Stop Web UI & Logging" ボタンをクリック
   - Burpをそのまま終了させても問題ありません。

### DBの保存場所

-  `$HOME/.burpextender-proxyhistory-webui/` の下に `h2db_(DB名)` というフォルダが作成されます。その中にh2dbのデータファイルが保存されます。
   - Windowsなら `C:\Users\(ログインユーザ名)\.burpextender-proxyhistory-webui` の下になります。
   - 「DB名」のデフォルトはログインユーザ名が使われます。
- バックアップするには `h2db_(DB名)` フォルダをそのままコピーしてください。
- 別のマシンの `$HOME/.burpextender-proxyhistory-webui/` にコピーすれば、そのまま利用できます。

### 設定のカスタマイズ

カスタマイズした設定を反映するには、(もし既にstartしていれば) 一度 stop した後、startし直してください。

基本的な設定:

- `target host`
  - 独自にログする対象ホスト名を1行に1つずつ設定。
  - ワイルドカード(`*`)が利用可能。
- `exclude filename extensions`
  - 除外するURLのファイル名拡張子を1行に1つずつ設定。
  - デフォルトでは代表的な静的ファイルの拡張子が登録されています。

詳細設定:

- Web UI用ポート番号を変更できます。
- 標準とは異なるDBを作成して、ログの保存先を変更できます。

※これらの設定は `$HOME/.burpextender-proxyhistory-webui/config.yml` に保存されます。DBには保存されない点に注意してください。

## 開発環境

* JDK >= 1.8.0_92
* Eclipse >= 4.5.2 (Mars.2 Release), "Eclipse IDE for Java EE Developers" パッケージを使用
* Maven >= 3.3.9 (maven-wrapperにて自動的にDLしてくれる)
* ソースコードやテキストファイル全般の文字コードはUTF-8を使用

## ビルドと実行

```
cd burpextender-proxyhistory-webui/

ビルド:
mvnw package

jarファイルから実行:
java -jar target/burpextender-proxyhistory-webui-xxx.jar
or
Burp Suite を起動して "Extender" タブからロードして実行

Mavenプロジェクトから直接実行:
mvnw exec:java
```

## Eclipseプロジェクト用の設定

https://github.com/SecureSkyTechnology/howto-eclipse-setup の `setup-type1` を使用。README.mdで以下を参照のこと:

* Ecipseのインストール
* Clean Up/Formatter 設定
* 必須プラグイン Lombok / オプションプラグイン Swing Designer のインストール 
* GitでcloneしたMavenプロジェクトのインポート 

 Swing Designer 備考:

* 初めてSwing Designerでフレームを作成し、レイアウトで `FormLayout` を選択したところ、Eclipse プロジェクト直下に `jgoodies-forms-1.8.0.jar` とそのsource jarが自動でDLされ、Eclipse プロジェクトの Java Build Path にライブラリとして自動で追加されてしまった。
* また、何がきっかけか不明だが `miglayout15-swing.jar`, `miglayout-src.zip` というのも Eclipse プロジェクト直下に気がついたらDLされていて、Java Build Path にjarが同様に追加されていた。
* Swing Designer が掴んでいたためか、Eclipse 起動中はこれらのファイルは完全には削除できなかった。
* →そのため、一旦Eclipseを終了させてファイルを削除したり、Eclipseプロジェクト プロパティのJava Build Path からこれらのjarを手作業で削除したりした。
* さらに、そのままでは `FormLayout` 関連のimportでエラーとなるため、pom.xml に同等の `com.jgoodies:jgoodies-forms:1.8.0` を追加してコンパイルエラーを解決した。

`FormLayout` で使用している `jgoodies-forms` について(2017-09-20時点):

* http://www.jgoodies.com/
* Java で高機能なGUIデスクトップアプリを高速開発するRAD製品。
* `jgoodies-forms` については "Open Source Java Libraries" に含まれている。
  * http://www.jgoodies.com/freeware/libraries/
* ただし、jgoodiesの事情で、現在は最新版の Java ライブラリはオープンソースやフリーソフトとしては公開しなくなってしまった。
  * 製品版を購入したユーザにのみ、最新の Java ライブラリも提供しているらしい。
  * 現在 search.maven.org などから検索できるのは、古いバージョンであり、これらについてはBSDライセンスが適用されている。
* Swing Designer で自動でDLされた `jgoodies-forms` については古いBSDライセンスが適用されたバージョンであったため、本ツールで利用するにあたり特に問題なしと判断した。(2017-09-20)
