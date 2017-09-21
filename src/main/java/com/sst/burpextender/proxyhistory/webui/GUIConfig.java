package com.sst.burpextender.proxyhistory.webui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.yaml.snakeyaml.Yaml;

import com.google.common.io.Files;

import lombok.Data;

/**
 * YAMLを使ったGUI設定の保存クラス
 * 
 * なぜYAMLを使うのか？ Swingコンポーネントならserializeableなので、Java標準のJNLPのPersistenceServiceなどを使えばよかったのでは？
 * 
 * => パスワードや close 時のJFrameの位置・サイズなど、単純にserializeしては地雷が潜んでいる可能性があり、
 * 地雷除去できるだけのスキルが2017-03時点では坂本が持っていなかった。
 * また、JNLPのPersistenceServiceでは、Windowsの場合の保存場所がAppDataの下などかなり深くなってしまい、
 * ユーザによる設定値クリアなどのトラブル時の運用ガイドが難しくなる可能性を感じた。
 * （同様の理由で、レジストリに保存する Preferences API も却下した）
 * その上で、シンプルなBeansで簡単にserialize/desirializeできて安定して動作するYAMLを用いることで、
 * コード全体を簡素化した。
 * 
 * もう一つの考慮事項として、今後の機能追加でGUI設定として保存する項目が増える場合がある。
 * その際、単純に「エラーになったらこのファイル削除して再起動して」と案内できるだけの
 * 単純さを実現できる方式を優先したかった。
 * (serializeにおけるフィールド追加による差分の扱いがもう一つの地雷だった）
 *  
 * 参考：
 * @see http://ateraimemo.com/Swing/Preferences.html
 * @see http://ateraimemo.com/Swing/PersistenceService.html
 */
@Data
public class GUIConfig {
    public static final String CONFIG_DOT_DIR = System.getProperty("user.home") + "/.burpextender-proxyhistory-webui";
    public static final File DEFAULT_CONFIG_FILE = new File(CONFIG_DOT_DIR + "/config.yml");
    public static final String DEFAULT_DB_NAME = System.getProperty("user.name");
    public static final int WEBUI_PORT_DEFAULT = 10080;

    int webuiPort = WEBUI_PORT_DEFAULT;
    String currentDbName = DEFAULT_DB_NAME;
    List<String> dbNames = Arrays.asList(DEFAULT_DB_NAME);
    List<String> targetHostNames = Arrays.asList("*");
    List<String> excludeFilenameExtensions = Arrays.asList("js", "gif", "jpg", "png", "css", "bmp", "svg", "ico");

    public static GUIConfig load(File f) throws IOException {
        Yaml y = new Yaml();
        GUIConfig c = y.loadAs(Files.toString(f, StandardCharsets.UTF_8), GUIConfig.class);
        c.dbNames = DataStore.getDbNames();
        return c;
    }

    public void save(File f) throws IOException {
        Yaml y = new Yaml();
        Files.write(y.dump(this), f, StandardCharsets.UTF_8);
    }

    public static String convertWildcardToRegexp(String s) {
        s = s.replace(".", "\\.");
        s = s.replace("*", ".*");
        s = "^" + s + "$";
        return s;
    }

    public static boolean isValidRegexpPattern(String p) {
        if (null == p) {
            return false;
        }
        try {
            Pattern.compile(p);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }
}
