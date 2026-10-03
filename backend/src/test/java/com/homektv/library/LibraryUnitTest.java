package com.homektv.library;

import com.homektv.media.MediaProbe;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 曲库入库纯逻辑单测：拼音、类型判定、指纹、文件名解析、歌词类型（P1.2-P1.5）。
 */
class LibraryUnitTest {

    // ---- P1.5 拼音 ----
    @Test
    void pinyinFullAndInitials() {
        assertThat(PinyinUtil.fullPinyin("周杰伦")).isEqualTo("zhoujielun");
        assertThat(PinyinUtil.initials("周杰伦")).isEqualTo("zjl");
        assertThat(PinyinUtil.fullPinyin("晴天")).isEqualTo("qingtian");
        assertThat(PinyinUtil.initials("晴天")).isEqualTo("qt");
        // 英文数字保留
        assertThat(PinyinUtil.initials("G.E.M.邓紫棋")).isEqualTo("gemdzq");
    }

    // ---- P1.3 类型判定 ----
    @Test
    void classifyByProbe() {
        // 视频 + 双音轨 → KTV
        assertThat(MediaClassifier.classify(new MediaProbe(200000, 2, 1, true, "1920x1080")))
                .isEqualTo(MediaClassifier.KTV_VIDEO);
        // 视频 + 单音轨 → MV
        assertThat(MediaClassifier.classify(new MediaProbe(200000, 1, 0, true, "1280x720")))
                .isEqualTo(MediaClassifier.MV);
        // 纯音频 → AUDIO
        assertThat(MediaClassifier.classify(new MediaProbe(200000, 1, 0, false, null)))
                .isEqualTo(MediaClassifier.AUDIO);
    }

    @Test
    void hasVocalTrackNeedsTwoAudio() {
        assertThat(MediaClassifier.hasVocalTrack(new MediaProbe(1, 2, 0, true, null))).isTrue();
        assertThat(MediaClassifier.hasVocalTrack(new MediaProbe(1, 1, 0, true, null))).isFalse();
    }

    // ---- P1.3 指纹去重 ----
    @Test
    void fingerprintStableWithinDurationBucket() {
        // ±2s 内（同一 2000ms 桶）指纹相同
        String a = MediaClassifier.fingerprint("周杰伦", "晴天", 269000);
        String b = MediaClassifier.fingerprint("周杰伦", "晴天", 269500);
        assertThat(a).isEqualTo(b);
        // 大小写/空白不敏感
        assertThat(MediaClassifier.fingerprint(" Beyond ", "海阔天空", 312000))
                .isEqualTo(MediaClassifier.fingerprint("beyond", "海阔天空", 312000));
        // 不同歌不同指纹
        assertThat(a).isNotEqualTo(MediaClassifier.fingerprint("周杰伦", "七里香", 269000));
    }

    // ---- P1.2 文件名兜底 ----
    @Test
    void filenameParsing() {
        ParsedMeta m1 = FilenameParser.parse("周杰伦 - 晴天.mkv");
        assertThat(m1.recognized()).isTrue();
        assertThat(m1.artist()).isEqualTo("周杰伦");
        assertThat(m1.title()).isEqualTo("晴天");

        ParsedMeta m2 = FilenameParser.parse("Beyond-海阔天空.mp4");
        assertThat(m2.artist()).isEqualTo("Beyond");
        assertThat(m2.title()).isEqualTo("海阔天空");

        // 全角连字符
        ParsedMeta m3 = FilenameParser.parse("刘若英－后来.flac");
        assertThat(m3.artist()).isEqualTo("刘若英");
        assertThat(m3.title()).isEqualTo("后来");

        // 无分隔符 → 整体为歌名
        ParsedMeta m4 = FilenameParser.parse("TRACK_00321.mp3");
        assertThat(m4.title()).isNotBlank();
    }

    @Test
    void structuredKtvFilenameParsing() {
        FilenameParser.ExtendedMeta m1 = FilenameParser.parseExtended(
                "蔡卓妍-心如蝶舞-国语-流行.mkv", "蔡卓妍");
        assertThat(m1.recognized()).isTrue();
        assertThat(m1.artist()).isEqualTo("蔡卓妍");
        assertThat(m1.title()).isEqualTo("心如蝶舞");
        assertThat(m1.language()).isEqualTo("国语");
        assertThat(m1.tags()).containsExactly("流行");

        FilenameParser.ExtendedMeta m2 = FilenameParser.parseExtended(
                "张学友-吻别-Live-国语-流行.mkv", "张学友");
        assertThat(m2.artist()).isEqualTo("张学友");
        assertThat(m2.title()).isEqualTo("吻别-Live");
        assertThat(m2.language()).isEqualTo("国语");
        assertThat(m2.tags()).containsExactly("流行");

        FilenameParser.ExtendedMeta m3 = FilenameParser.parseExtended(
                "A-Lin-给我一个理由忘记-国语-流行.mkv", "A-Lin");
        assertThat(m3.artist()).isEqualTo("A-Lin");
        assertThat(m3.title()).isEqualTo("给我一个理由忘记");
        assertThat(m3.language()).isEqualTo("国语");
        assertThat(m3.tags()).containsExactly("流行");

        // 多歌手字段中的 '_' 属于歌手名，不应被当成结构化字段分隔符。
        FilenameParser.ExtendedMeta duet1 = FilenameParser.parseExtended(
                "周柏豪_蔡卓妍-请你爱我-粤语-合唱.mkv", "蔡卓妍");
        assertThat(duet1.recognized()).isTrue();
        assertThat(duet1.artist()).isEqualTo("周柏豪_蔡卓妍");
        assertThat(duet1.title()).isEqualTo("请你爱我");
        assertThat(duet1.language()).isEqualTo("粤语");
        assertThat(duet1.tags()).containsExactly("合唱");

        FilenameParser.ExtendedMeta duet2 = FilenameParser.parseExtended(
                "蔡卓妍_马天佑-亲爱的你好吗-国语-合唱.mkv", "蔡卓妍");
        assertThat(duet2.artist()).isEqualTo("蔡卓妍_马天佑");
        assertThat(duet2.title()).isEqualTo("亲爱的你好吗");
        assertThat(duet2.language()).isEqualTo("国语");
        assertThat(duet2.tags()).containsExactly("合唱");

        FilenameParser.ExtendedMeta duet3 = FilenameParser.parseExtended(
                "张继聪_蔡卓妍-不死-粤语-合唱.mkv", null);
        assertThat(duet3.artist()).isEqualTo("张继聪_蔡卓妍");
        assertThat(duet3.title()).isEqualTo("不死");
        assertThat(duet3.language()).isEqualTo("粤语");
        assertThat(duet3.tags()).containsExactly("合唱");

        FilenameParser.ExtendedMeta fallback = FilenameParser.parseExtended(
                "Beyond - 海阔天空.mkv", null);
        assertThat(fallback.artist()).isEqualTo("Beyond");
        assertThat(fallback.title()).isEqualTo("海阔天空");
        assertThat(fallback.language()).isNull();
        assertThat(fallback.tags()).isEmpty();

        // 非结构化旧命名仍保持原逻辑：artist_title 可回退为 artist-title。
        FilenameParser.ExtendedMeta legacyUnderscore = FilenameParser.parseExtended(
                "周杰伦_晴天.mkv", null);
        assertThat(legacyUnderscore.artist()).isEqualTo("周杰伦");
        assertThat(legacyUnderscore.title()).isEqualTo("晴天");
    }

    // ---- P1.4 歌词类型 ----
    @Test
    void lyricTypeDetection() {
        // 逐字（增强 LRC 带内联 <时间>）
        String word = "[00:12.00]<00:12.00>晴<00:12.30>天<00:12.60>";
        assertThat(LyricType.detect(word)).isEqualTo(LyricType.WORD);
        // 逐行 LRC
        String line = "[00:12.00]故事的小黄花\n[00:15.00]从出生那年就飘着";
        assertThat(LyricType.detect(line)).isEqualTo(LyricType.LINE);
        // 无时间轴
        assertThat(LyricType.detect("就是一段纯文本")).isEqualTo(LyricType.NONE);
        assertThat(LyricType.detect(null)).isEqualTo(LyricType.NONE);
    }
}
