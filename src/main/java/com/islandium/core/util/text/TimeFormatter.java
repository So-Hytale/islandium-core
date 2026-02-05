package com.islandium.core.util.text;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeFormatter {
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhdwMy])");

    private TimeFormatter() {}

    public static Duration parseDuration(String input) {
        if (input == null || input.isEmpty()) return null;

        Matcher matcher = DURATION_PATTERN.matcher(input.toLowerCase());
        Duration total = Duration.ZERO;
        boolean found = false;

        while (matcher.find()) {
            found = true;
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            Duration duration = switch (unit) {
                case "s" -> Duration.ofSeconds(amount);
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                case "d" -> Duration.ofDays(amount);
                case "w" -> Duration.ofDays(amount * 7);
                case "M" -> Duration.ofDays(amount * 30);
                case "y" -> Duration.ofDays(amount * 365);
                default -> Duration.ZERO;
            };

            total = total.plus(duration);
        }

        return found ? total : null;
    }

    public static String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return "0s";
        }

        StringBuilder sb = new StringBuilder();
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (days > 0) {
            if (days >= 365) {
                long years = days / 365;
                days = days % 365;
                sb.append(years).append("a ");
            }
            if (days >= 30) {
                long months = days / 30;
                days = days % 30;
                sb.append(months).append("M ");
            }
            if (days >= 7) {
                long weeks = days / 7;
                days = days % 7;
                sb.append(weeks).append("sem ");
            }
            if (days > 0) {
                sb.append(days).append("j ");
            }
        }

        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    public static String formatTimestamp(long timestamp) {
        if (timestamp <= 0) return "Jamais";
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 0) {
            return formatDuration(Duration.ofMillis(-diff)) + " dans le futur";
        }

        return "il y a " + formatDuration(Duration.ofMillis(diff));
    }
}
