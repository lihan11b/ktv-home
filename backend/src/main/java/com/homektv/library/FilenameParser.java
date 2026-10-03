package com.homektv.library;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * 文件名规则兜底解析（P1.2，详设§9.3）。
 * 标签缺失时，从文件名按常见模式解析歌手/歌名：
 *   「歌手 - 歌名」「歌名 - 歌手」「歌手－歌名」（全角）等。
 * 分隔符两侧的空白会被裁剪；无法拆分时归入未识别。
 */
public final class FilenameParser {

    private static final Set<String> STRUCTURED_LANGUAGES = Set.of(
            "国语", "普通话", "中文",
            "粤语",
            "闽南语", "台语",
            "英语", "英文", "english",
            "日语", "日文", "japanese",
            "韩语", "韩文", "korean",
            "纯音乐", "instrumental",
            "其他"
    );

    private FilenameParser() {}

    /**
     * 结构化 KTV 文件名解析结果。
     *
     * 支持「歌手-歌名-语种-类型」；若歌名自身含连字符，则保留中间全部段为歌名。
     * 多歌手可在歌手字段内使用下划线，例如「周柏豪_蔡卓妍-请你爱我-粤语-合唱」。
     * artistHint 通常来自源文件父目录，用于正确处理 A-Lin 等歌手名本身含连字符的情况。
     */
    public record ExtendedMeta(String title, String artist, String language, String[] tags, boolean recognized) {
        public ExtendedMeta {
            tags = tags == null ? new String[0] : tags.clone();
        }
    }

    /**
     * @param filename 文件名（可含扩展名）
     * @return 解析结果；无法识别时 recognized=false，title 回退为去扩展名的文件名
     */
    public static ParsedMeta parse(String filename) {
        return parse(filename, "artist_title");
    }

    public static ParsedMeta parse(String filename, String rule) {
        String base = normalizeBase(filename);
        if (base.isBlank()) return ParsedMeta.unrecognized(base);

        // 用 " - " 或 "-" 分隔（优先带空格的）
        String[] parts = splitByDash(base);
        if (parts == null) {
            // 无分隔符：整个作为歌名，歌手未知
            return ParsedMeta.of(base.trim(), "");
        }

        String left = cleanPart(parts[0]);
        String right = cleanPart(parts[1]);
        if (left.isEmpty() || right.isEmpty()) {
            return ParsedMeta.of(base.trim(), "");
        }
        if (left.matches("(?i)track|track\\s*\\d*") && right.matches("\\d{2,}")) {
            return ParsedMeta.unrecognized(base.trim());
        }

        // For a name containing several separators, the first/last non-marker segment
        // is usually the catalogue suffix; retain it in the title rather than swapping identities.
        return "title_artist".equals(rule) ? ParsedMeta.of(left, right) : ParsedMeta.of(right, left);
    }

    /**
     * 解析「歌手-歌名-语种-类型」命名；不匹配时完整回退到原有解析规则。
     */
    public static ExtendedMeta parseExtended(String filename, String artistHint) {
        String normalized = normalizeStructuredBase(filename);
        if (normalized.isBlank()) {
            ParsedMeta fallback = ParsedMeta.unrecognized(normalized);
            return fallback(fallback);
        }

        // 结构化命名固定以 '-' 为字段边界；仅去除分隔符两侧空白。
        String compact = normalized.replaceAll("\\s*-\\s*", "-");

        String normalizedHint = normalizeArtistHint(artistHint);
        if (!normalizedHint.isBlank() && compact.startsWith(normalizedHint + "-")) {
            String remainder = compact.substring(normalizedHint.length() + 1);
            String[] remainderParts = remainder.split("-", -1);
            ExtendedMeta hinted = parseStructuredParts(normalizedHint, remainderParts);
            if (hinted != null) return hinted;
        }

        String[] parts = compact.split("-", -1);
        if (parts.length >= 4) {
            String artist = cleanPart(parts[0]);
            String[] remainderParts = Arrays.copyOfRange(parts, 1, parts.length);
            ExtendedMeta structured = parseStructuredParts(artist, remainderParts);
            if (structured != null) return structured;
        }

        return fallback(parse(filename));
    }

    private static ExtendedMeta parseStructuredParts(String artist, String[] remainderParts) {
        if (artist == null || artist.isBlank() || remainderParts.length < 3) return null;

        String language = cleanPart(remainderParts[remainderParts.length - 2]);
        String tag = cleanPart(remainderParts[remainderParts.length - 1]);
        if (!isStructuredLanguage(language) || tag.isBlank()) return null;

        String title = String.join("-",
                Arrays.copyOfRange(remainderParts, 0, remainderParts.length - 2)).trim();
        if (title.isBlank()) return null;

        return new ExtendedMeta(
                title,
                cleanPart(artist),
                language,
                new String[]{tag},
                true
        );
    }

    private static ExtendedMeta fallback(ParsedMeta parsed) {
        return new ExtendedMeta(
                parsed.title(),
                parsed.artist(),
                null,
                new String[0],
                parsed.recognized()
        );
    }

    private static boolean isStructuredLanguage(String value) {
        if (value == null || value.isBlank()) return false;
        return STRUCTURED_LANGUAGES.contains(value.trim().toLowerCase(Locale.ROOT));
    }

    private static String normalizeArtistHint(String artistHint) {
        if (artistHint == null) return "";
        return normalizeStructuredSeparators(artistHint).replaceAll("\\s*-\\s*", "-").trim();
    }

    /**
     * Structured KTV names use '-' between fields while '_' may belong to a multi-artist field.
     * Do not collapse '_' to '-' here; otherwise "周柏豪_蔡卓妍-歌名-粤语-合唱" would
     * incorrectly become artist=周柏豪, title=蔡卓妍-歌名.
     */
    private static String normalizeStructuredBase(String filename) {
        String base = stripExtension(filename).trim();
        if (base.isBlank()) return base;

        base = base.replaceFirst("^\\s*\\d{1,5}\\s*[-._)】]\\s*", "");
        base = base.replaceAll("\\s*\\[(?:KTV|MTV|MV|LIVE|伴奏|原唱|消音|卡拉OK)\\]\\s*$", "");
        base = base.replaceAll("\\s*\\((?:KTV|MTV|MV|LIVE|伴奏|原唱|消音|卡拉OK|Official Video)\\)\\s*$", "");
        base = base.replaceAll("(?i)\\s*[-|]\\s*(KTV|MTV|MV|LIVE|伴奏|原唱|消音|卡拉OK)\\s*$", "");

        return normalizeStructuredSeparators(base);
    }

    private static String normalizeBase(String filename) {
        String base = stripExtension(filename).trim();
        if (base.isBlank()) return base;

        // Strip catalogue numbers and transport/version markers before identifying fields.
        base = base.replaceFirst("^\\s*\\d{1,5}\\s*[-._)】]\\s*", "");
        base = base.replaceAll("\\s*\\[(?:KTV|MTV|MV|LIVE|伴奏|原唱|消音|卡拉OK)\\]\\s*$", "");
        base = base.replaceAll("\\s*\\((?:KTV|MTV|MV|LIVE|伴奏|原唱|消音|卡拉OK|Official Video)\\)\\s*$", "");
        base = base.replaceAll("(?i)\\s*[-|]\\s*(KTV|MTV|MV|LIVE|伴奏|原唱|消音|卡拉OK)\\s*$", "");

        return normalizeSeparators(base);
    }

    private static String normalizeStructuredSeparators(String value) {
        return value
                .replace('－', '-')   // 全角连字符
                .replace('—', '-')    // 破折号
                .replace('–', '-')
                .replace('｜', '|');
    }

    private static String normalizeSeparators(String value) {
        return normalizeStructuredSeparators(value)
                .replace('_', '-');
    }

    private static String[] splitByDash(String s) {
        for (String delimiter : new String[]{" - ", "|", "/", "\\", "-"}) {
            int idx = s.indexOf(delimiter);
            if (idx > 0 && idx < s.length() - delimiter.length()) {
                return new String[]{ s.substring(0, idx), s.substring(idx + delimiter.length()) };
            }
        }
        return null;
    }

    private static String cleanPart(String value) {
        return value.replaceAll("^\\s*[【\\[][^】\\]]+[】\\]]\\s*", "").trim();
    }

    static String stripExtension(String filename) {
        if (filename == null) return "";
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String name = slash >= 0 ? filename.substring(slash + 1) : filename;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
