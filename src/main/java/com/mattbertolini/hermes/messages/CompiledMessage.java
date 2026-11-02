package com.mattbertolini.hermes.messages;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.MessagePattern;
import com.ibm.icu.util.ULocale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * A parsed message pattern. GWT's {@code {n,list[,format]}} and
 * {@code {n,localdatetime,skeleton}} arguments, which ICU's MessageFormat
 * doesn't know, are rewritten: lists become extra arguments formatted with
 * ICU's ListFormatter, and localdatetime becomes an ICU date skeleton.
 */
final class CompiledMessage {
    private final MessageFormat format;
    private final List<ListArgument> lists = new ArrayList<>();
    private final ULocale locale;

    CompiledMessage(String pattern, ULocale locale, int parameterCount) {
        this.locale = locale;
        this.format = new MessageFormat("", locale);
        // GWT, like java.text.MessageFormat, treats every single quote as quoting
        this.format.applyPattern(rewrite(pattern, parameterCount), MessagePattern.ApostropheMode.DOUBLE_REQUIRED);
    }

    Object format(Object[] rawArgs, Object[] args, boolean safeHtml) {
        Object[] formatArgs = args;
        if (!lists.isEmpty()) {
            formatArgs = Arrays.copyOf(args, args.length + lists.size());
            for (int i = 0; i < lists.size(); i++) {
                formatArgs[args.length + i] = lists.get(i).format(rawArgs, safeHtml);
            }
        }
        if (safeHtml && formatArgs != null) {
            formatArgs = Arrays.stream(formatArgs).map(CompiledMessage::escape).toArray();
        }
        // MessageFormat isn't thread-safe, so each call formats with its own clone
        MessageFormat formatter = (MessageFormat) format.clone();
        String result = formatter.format(formatArgs, new StringBuffer(), null).toString();
        // Would rather use fromSafeConstant() but doesn't work on server.
        return safeHtml ? SafeHtmlUtils.fromTrustedString(result) : result;
    }

    private String rewrite(String pattern, int firstExtraArgument) {
        StringBuilder out = new StringBuilder();
        boolean inQuote = false;
        int i = 0;
        while (i < pattern.length()) {
            char ch = pattern.charAt(i);
            if (ch == '\'') {
                boolean doubled = i + 1 < pattern.length() && pattern.charAt(i + 1) == '\'';
                out.append(doubled ? "''" : "'");
                inQuote ^= !doubled;
                i += doubled ? 2 : 1;
            } else if (ch == '{' && !inQuote) {
                int end = argumentEnd(pattern, i);
                if (end < 0) {
                    out.append(pattern, i, pattern.length());   // Let MessageFormat report it
                    break;
                }
                out.append(rewriteArgument(pattern.substring(i + 1, end), firstExtraArgument));
                i = end + 1;
            } else {
                out.append(ch);
                i++;
            }
        }
        return out.toString();
    }

    private String rewriteArgument(String argument, int firstExtraArgument) {
        String[] parts = argument.split(",", 3);
        String type = parts.length > 1 ? parts[1].trim() : "";
        String style = parts.length > 2 ? parts[2].trim() : null;
        if ("list".equals(type)) {
            lists.add(new ListArgument(Integer.parseInt(parts[0].trim()), style, locale));
            return "{" + (firstExtraArgument + lists.size() - 1) + "}";
        }
        if ("localdatetime".equals(type)) {
            return "{" + parts[0].trim() + ",date,::" + (style == null ? "" : style) + "}";
        }
        return "{" + argument + "}";
    }

    // Index of the brace closing the argument at start, honoring quotes and nesting
    private static int argumentEnd(String pattern, int start) {
        int depth = 0;
        boolean inQuote = false;
        for (int i = start; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (ch == '\'') {
                inQuote = !inQuote;
            } else if (!inQuote && ch == '{') {
                depth++;
            } else if (!inQuote && ch == '}' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    // TODO: Find a home for this
    public static Object escape(Object arg) {
        if (arg instanceof SafeHtml) {
            return ((SafeHtml) arg).asString();
        }
        if (arg instanceof Number || arg instanceof Date) {
            return arg;   // Sub-format patterns need the real value
        }
        return SafeHtmlUtils.htmlEscape(String.valueOf(arg));
    }
}
