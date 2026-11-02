package com.mattbertolini.hermes.messages;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.MessagePattern;
import com.ibm.icu.util.ULocale;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

final class ListArgument {
    private final int index;
    private final MessageFormat elementFormat;
    private final ListFormatter listFormatter;   // Only built for patterns that have a list

    ListArgument(int index, String elementStyle, ULocale locale) {
        this.index = index;
        this.listFormatter = ListFormatter.getInstance(locale);
        this.elementFormat = new MessageFormat("", locale);
        this.elementFormat.applyPattern(elementStyle == null ? "{0}" : "{0," + elementStyle + "}",
                MessagePattern.ApostropheMode.DOUBLE_REQUIRED);
    }

    Object format(Object[] args, boolean safeHtml) {
        List<String> items = new ArrayList<>();
        for (Object element : elements(args[index])) {
            MessageFormat formatter = (MessageFormat) elementFormat.clone();
            items.add(formatter.format(new Object[]{safeHtml ? CompiledMessage.escape(element) : element}));
        }
        String list = listFormatter.format(items);
        return safeHtml ? SafeHtmlUtils.fromTrustedString(list) : list;
    }

    private List<?> elements(Object arg) {
        if (arg == null) {
            return Collections.emptyList();
        }
        if (arg instanceof List) {
            return (List<?>) arg;
        }
        if (arg.getClass().isArray()) {
            return IntStream.range(0, Array.getLength(arg)).mapToObj(i -> Array.get(arg, i)).collect(toList());
        }
        throw new IllegalArgumentException("Parameters formatted as lists must be declared as java.util.List "
                + "or arrays but argument " + index + " was " + arg.getClass().getName());
    }
}
