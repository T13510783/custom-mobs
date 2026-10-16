package com.custommobs.update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* ───────── Release version: X.Y.Z-alpha, every part 0..99 ─────────
   Tags look like v0.0.1-alpha. next() counts up the last part; at 99 it rolls over into the part before it:
   0.0.99 -> 0.1.0, 0.99.99 -> 1.0.0; 99.99.99 is the last one (next() throws). Plain Java: the release script's
   Gradle task, the in-game updater and the unit test all use it. */
public record AlphaVersion(int major, int minor, int patch) implements Comparable<AlphaVersion> {
    public static final int MAX = 99;
    private static final Pattern P = Pattern.compile("^v?(\\d{1,2})\\.(\\d{1,2})\\.(\\d{1,2})-alpha$");

    public AlphaVersion {
        if (major < 0 || minor < 0 || patch < 0 || major > MAX || minor > MAX || patch > MAX)
            throw new IllegalArgumentException("version part outside 0..99: " + major + "." + minor + "." + patch);
    }

    /** "0.0.1-alpha" or "v0.0.1-alpha" -> version; anything else -> null. */
    public static AlphaVersion parse(String s) {
        if (s == null) return null;
        Matcher m = P.matcher(s.trim());
        if (!m.matches()) return null;
        return new AlphaVersion(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
    }

    /** The next release: last part + 1, rolling over at 99. */
    public AlphaVersion next() {
        if (patch < MAX) return new AlphaVersion(major, minor, patch + 1);
        if (minor < MAX) return new AlphaVersion(major, minor + 1, 0);
        if (major < MAX) return new AlphaVersion(major + 1, 0, 0);
        throw new IllegalStateException("v99.99.99-alpha is the last version");
    }

    @Override
    public int compareTo(AlphaVersion o) {
        if (major != o.major) return Integer.compare(major, o.major);
        if (minor != o.minor) return Integer.compare(minor, o.minor);
        return Integer.compare(patch, o.patch);
    }

    /** "0.0.1-alpha" (mod version, jar name). */
    @Override
    public String toString() { return major + "." + minor + "." + patch + "-alpha"; }

    /** "v0.0.1-alpha" (git tag, release title). */
    public String tag() { return "v" + this; }
}
