package com.ninja.ghastmisc.utils;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {
    private static JavaPlugin plugin;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:([A-Fa-f0-9]{6}):([A-Fa-f0-9]{6})>(.*?)</gradient>");
    private static final Pattern RAINBOW_PATTERN = Pattern.compile("<rainbow>(.*?)</rainbow>");

    public static void initialize(JavaPlugin plugin) {
        ColorUtils.plugin = plugin;
    }

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Process gradients
        text = processGradients(text);

        // Process rainbow
        text = processRainbow(text);

        // Process hex colors
        text = processHexColors(text);

        // Process standard color codes
        text = ChatColor.translateAlternateColorCodes('&', text);

        return text;
    }

    public static List<String> colorizeList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }

        List<String> colorized = new ArrayList<>();
        for (String line : list) {
            colorized.add(colorize(line));
        }
        return colorized;
    }

    private static String processHexColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        while (matcher.find()) {
            String hexColor = matcher.group(1);
            String replacement = net.md_5.bungee.api.ChatColor.of("#" + hexColor).toString();
            text = text.replace(matcher.group(0), replacement);
        }
        return text;
    }

    private static String processGradients(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        while (matcher.find()) {
            String startColor = matcher.group(1);
            String endColor = matcher.group(2);
            String content = matcher.group(3);

            String gradientText = createGradient(content, startColor, endColor);
            text = text.replace(matcher.group(0), gradientText);
        }
        return text;
    }

    private static String processRainbow(String text) {
        Matcher matcher = RAINBOW_PATTERN.matcher(text);
        while (matcher.find()) {
            String content = matcher.group(1);
            String rainbowText = createRainbow(content);
            text = text.replace(matcher.group(0), rainbowText);
        }
        return text;
    }

    private static String createGradient(String text, String startHex, String endHex) {
        if (text.isEmpty()) return text;

        // Convert hex to RGB
        int startR = Integer.parseInt(startHex.substring(0, 2), 16);
        int startG = Integer.parseInt(startHex.substring(2, 4), 16);
        int startB = Integer.parseInt(startHex.substring(4, 6), 16);

        int endR = Integer.parseInt(endHex.substring(0, 2), 16);
        int endG = Integer.parseInt(endHex.substring(2, 4), 16);
        int endB = Integer.parseInt(endHex.substring(4, 6), 16);

        StringBuilder result = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            char character = text.charAt(i);
            if (character == ' ') {
                result.append(' ');
                continue;
            }

            // Calculate interpolated color
            float ratio = (float) i / (length - 1);
            int r = (int) (startR + ratio * (endR - startR));
            int g = (int) (startG + ratio * (endG - startG));
            int b = (int) (startB + ratio * (endB - startB));

            String hex = String.format("%02x%02x%02x", r, g, b);
            result.append(net.md_5.bungee.api.ChatColor.of("#" + hex)).append(character);
        }

        return result.toString();
    }

    private static String createRainbow(String text) {
        if (text.isEmpty()) return text;

        StringBuilder result = new StringBuilder();
        String[] colors = {"FF0000", "FF8000", "FFFF00", "80FF00", "00FF00", "00FF80", "00FFFF", "0080FF", "0000FF", "8000FF", "FF00FF", "FF0080"};

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == ' ') {
                result.append(' ');
                continue;
            }

            String color = colors[i % colors.length];
            result.append(net.md_5.bungee.api.ChatColor.of("#" + color)).append(character);
        }

        return result.toString();
    }

    public static String stripColors(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Strip hex colors
        text = text.replaceAll("&#[A-Fa-f0-9]{6}", "");

        // Strip gradients
        text = text.replaceAll("<gradient:[A-Fa-f0-9]{6}:[A-Fa-f0-9]{6}>(.*?)</gradient>", "$1");

        // Strip rainbow
        text = text.replaceAll("<rainbow>(.*?)</rainbow>", "$1");

        // Strip standard colors
        text = ChatColor.stripColor(text);

        return text;
    }
}