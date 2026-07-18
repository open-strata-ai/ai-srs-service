package com.openstrata.srs.domain;

import com.openstrata.srs.web.ErrorCode;

/**
 * Semantic version value object ({@code major.minor.patch}, SPECS §2.4). Comparable and
 * immutable. Also evaluates a small subset of npm-style version ranges used by Skill
 * dependencies (§7.2): exact ({@code 1.2.3}), caret ({@code ^1.2.3}), tilde
 * ({@code ~1.2.3}), and wildcard ({@code *} / {@code latest}).
 */
public record SemVer(int major, int minor, int patch) implements Comparable<SemVer> {

    public static SemVer parse(String raw) {
        if (raw == null) {
            throw new DomainException(ErrorCode.BAD_REQUEST, "version is null");
        }
        String v = raw.trim();
        if (v.startsWith("v")) v = v.substring(1);
        String[] parts = v.split("\\.");
        if (parts.length != 3) {
            throw new DomainException(ErrorCode.BAD_REQUEST, "invalid SemVer: " + raw);
        }
        try {
            return new SemVer(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            throw new DomainException(ErrorCode.BAD_REQUEST, "invalid SemVer: " + raw);
        }
    }

    @Override
    public int compareTo(SemVer o) {
        if (major != o.major) return Integer.compare(major, o.major);
        if (minor != o.minor) return Integer.compare(minor, o.minor);
        return Integer.compare(patch, o.patch);
    }

    /** True when this version satisfies the given range expression. */
    public boolean satisfies(String range) {
        if (range == null) return true;
        String r = range.trim();
        if (r.isEmpty() || r.equals("*") || r.equalsIgnoreCase("latest")) {
            return true;
        }
        if (r.startsWith("^")) {
            SemVer base = parse(r.substring(1));
            SemVer upper = new SemVer(base.major + 1, 0, 0);
            return this.compareTo(base) >= 0 && this.compareTo(upper) < 0;
        }
        if (r.startsWith("~")) {
            SemVer base = parse(r.substring(1));
            SemVer upper = new SemVer(base.major, base.minor + 1, 0);
            return this.compareTo(base) >= 0 && this.compareTo(upper) < 0;
        }
        if (r.startsWith(">=")) {
            return this.compareTo(parse(r.substring(2))) >= 0;
        }
        return this.equals(parse(r));
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
