/*
 * Hermes - GWT Server-side I18N Library
 * Copyright (C) 2011-2026  Matt Bertolini
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

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
 * A parsed message pattern. GWT syntax that ICU's MessageFormat doesn't know
 * is rewritten first: {@code {#}} becomes the last @PluralCount argument,
 * {@code {n,list[,format]}} and {@code {n,localdatetime,predef:NAME}} become
 * extra pre-formatted arguments, and {@code {n,localdatetime,skeleton}}
 * becomes an ICU date skeleton. Like GWT, an argument beyond the method's
 * parameters or an unterminated quote is an error.
 */
final class CompiledMessage {
    private static final String PREDEF = "predef:";

    private final MessageFormat format;
    private final List<ExtraArgument> extras = new ArrayList<>();
    private final ULocale locale;
    private final int parameterCount;
    private final int innerPluralIndex;

    /**
     * @param innerPluralIndex the last @PluralCount parameter, which {@code {#}}
     *                         refers to, or -1 if there is none
     */
    CompiledMessage(String pattern, ULocale locale, int parameterCount, int innerPluralIndex) {
        this.locale = locale;
        this.parameterCount = parameterCount;
        this.innerPluralIndex = innerPluralIndex;
        this.format = new MessageFormat("", locale);
        // GWT, like java.text.MessageFormat, treats every single quote as quoting
        this.format.applyPattern(rewrite(pattern), MessagePattern.ApostropheMode.DOUBLE_REQUIRED);
    }

    Object format(Object[] rawArgs, Object[] args, boolean safeHtml) {
        Object[] formatArgs = args;
        if (!extras.isEmpty()) {
            formatArgs = Arrays.copyOf(args, args.length + extras.size());
            for (int i = 0; i < extras.size(); i++) {
                formatArgs[args.length + i] = extras.get(i).format(rawArgs, safeHtml);
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

    private String rewrite(String pattern) {
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
                out.append(rewriteArgument(pattern.substring(i + 1, end), pattern));
                i = end + 1;
            } else {
                out.append(ch);
                i++;
            }
        }
        if (inQuote) {
            throw new IllegalArgumentException("Unterminated single quote: " + pattern);
        }
        return out.toString();
    }

    private String rewriteArgument(String argument, String pattern) {
        int comma = argument.indexOf(',');
        String index = resolveIndex((comma < 0 ? argument : argument.substring(0, comma)).trim(), pattern);
        String[] parts = argument.split(",", 3);
        String type = parts.length > 1 ? parts[1].trim() : "";
        String style = parts.length > 2 ? parts[2].trim() : null;
        if ("list".equals(type)) {
            return extra(new ListArgument(Integer.parseInt(index), style, locale));
        }
        if ("localdatetime".equals(type)) {
            if (style == null || style.isEmpty()) {
                throw new IllegalArgumentException("localdatetime format requires a skeleton pattern: " + pattern);
            }
            if (style.startsWith(PREDEF)) {
                return extra(new PredefinedDateArgument(Integer.parseInt(index), style.substring(PREDEF.length()), locale));
            }
            return "{" + index + ",date,::" + style + "}";
        }
        return "{" + index + (comma < 0 ? "" : argument.substring(comma)) + "}";
    }

    private String resolveIndex(String index, String pattern) {
        if ("#".equals(index)) {
            if (innerPluralIndex < 0) {
                throw new IllegalArgumentException("Inner-plural notation {#} used in a non-plural message: " + pattern);
            }
            return String.valueOf(innerPluralIndex);
        }
        if (!index.isEmpty() && Character.isDigit(index.charAt(0)) && Integer.parseInt(index) >= parameterCount) {
            throw new IllegalArgumentException("Argument " + index + " beyond range of arguments: " + pattern);
        }
        return index;
    }

    // Extra arguments are appended after the method's own parameters
    private String extra(ExtraArgument argument) {
        extras.add(argument);
        return "{" + (parameterCount + extras.size() - 1) + "}";
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

    static Object escape(Object arg) {
        if (arg instanceof SafeHtml) {
            return ((SafeHtml) arg).asString();
        }
        if (arg instanceof Number || arg instanceof Date) {
            return arg;   // Sub-format patterns need the real value
        }
        return SafeHtmlUtils.htmlEscape(String.valueOf(arg));
    }
}
