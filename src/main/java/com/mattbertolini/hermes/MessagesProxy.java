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

package com.mattbertolini.hermes;

import com.google.gwt.i18n.client.Messages.AlternateMessage;
import com.google.gwt.i18n.client.Messages.DefaultMessage;
import com.google.gwt.i18n.client.Messages.Offset;
import com.google.gwt.i18n.client.Messages.PluralCount;
import com.google.gwt.i18n.client.Messages.PluralText;
import com.google.gwt.i18n.client.Messages.Select;
import com.google.gwt.i18n.client.PluralRule;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class MessagesProxy extends AbstractI18nProxy {
    private static final Set<Class<?>> PLURAL_COUNT_TYPES = new HashSet<>(Arrays.<Class<?>>asList(
            int.class, Integer.class, short.class, Short.class));
    // GWT allows integral primitives and boolean. float and double are a Hermes extension.
    private static final Set<Class<?>> SELECT_PRIMITIVE_TYPES = new HashSet<>(Arrays.<Class<?>>asList(
            int.class, long.class, short.class, byte.class, boolean.class, float.class, double.class));
    private static final String OPEN_BRACKET = "[";
    private static final String CLOSE_BRACKET = "]";
    private static final String SEPARATOR = "|";
    
    private final PluralRules pluralRules;
    
    public MessagesProxy(Class<?> clazz, ULocale locale, Properties properties) {
        super(clazz, locale, properties);
        pluralRules = PluralRules.forLocale(this.getLocale());
    }

    @Override
    protected MethodHandler createHandler(Method method) {
        return new MessageHandler(method);
    }

    @FunctionalInterface
    private interface AlternateFormSelector {
        List<String> select(Object[] args);
    }

    @FunctionalInterface
    private interface ArgumentAdjuster {
        void adjust(Object[] formatArgs);
    }

    @FunctionalInterface
    private interface MessageRenderer {
        Object render(MessageFormat formatter, Object[] args);
    }

    private final class MessageHandler implements MethodHandler {
        private final String key;
        private final List<AlternateFormSelector> selectors;
        private final List<ArgumentAdjuster> adjusters;
        private final Map<String, String> alternateMessages;
        private final String defaultMessage;
        private final MessageRenderer renderer;

        public MessageHandler(Method method) {
            key = keyFor(method);
            selectors = buildSelectors(method);
            adjusters = buildAdjusters(method);
            alternateMessages = buildAlternateMessages(key, method);
            defaultMessage = Optional.ofNullable(method.getAnnotation(DefaultMessage.class))
                    .map(DefaultMessage::value)
                    .orElse(null);
            renderer = method.getReturnType() == SafeHtml.class ? MessagesProxy::formatSafeHtml : MessagesProxy::formatPlain;
        }

        @Override
        public Object handle(Object[] args) {
            List<List<String>> candidates = selectors.stream()
                    .map(alternateFormSelector -> alternateFormSelector.select(args))
                    .collect(toList());
            List<String> patternNames = combinations(candidates).stream()
                    .map(forms -> buildPatternName(key, forms))
                    .distinct()
                    .collect(toList());
            MessageFormat formatter = new MessageFormat(findPattern(patternNames), getLocale());
            return renderer.render(formatter, adjust(args));
        }

        private String findPattern(List<String> patternNames) {
            // For each candidate, the properties file overrides annotations.
            Stream<Supplier<String>> specific = patternNames.stream()
                    .flatMap(name -> Stream.of(
                            () -> getProperties().getProperty(name),
                            () -> alternateMessages.get(name)));
            Stream<Supplier<String>> fallback = Stream.of(
                    () -> getProperties().getProperty(key),
                    () -> defaultMessage);
            return Stream.concat(specific, fallback)
                    .map(Supplier::get)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow(() -> new MissingResourceException("No message found for key " + key, getI18nClass().getName(), key));
        }

        private Object[] adjust(Object[] args) {
            if (adjusters.isEmpty()) {
                return args;
            }
            Object[] formatArgs = args.clone();   // Selectors already used the raw values
            adjusters.forEach(adjuster -> adjuster.adjust(formatArgs));
            return formatArgs;
        }
    }

    private List<AlternateFormSelector> buildSelectors(Method method) {
        Parameter[] parameters = method.getParameters();
        return IntStream.range(0, parameters.length)
                .mapToObj(index -> selectorFor(parameters[index], index))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private AlternateFormSelector selectorFor(Parameter parameter, int index) {
        PluralCount pluralCount = parameter.getAnnotation(PluralCount.class);
        if (pluralCount != null) {
            return pluralSelector(parameter, pluralCount, index);
        }
        if (parameter.isAnnotationPresent(Select.class)) {
            SelectKind kind = SelectKind.forType(parameter.getType())
                    .orElseThrow(() -> unsupported(Select.class, parameter, index,
                            "an integral primitive, boolean, float, double, enum, or String"));
            return args -> Collections.singletonList(kind.select(args[index]));
        }
        return null;   // Not a selector parameter
    }

    private AlternateFormSelector pluralSelector(Parameter parameter, PluralCount pluralCount, int index) {
        ToIntFunction<Object> counter = countFor(parameter, index);
        int offset = offsetOf(parameter);
        IntFunction<String> pluralForms = pluralFormsFor(pluralCount.value());
        return args -> {
            int count = counter.applyAsInt(args[index]);
            // Exact "=n" forms match the raw count; the offset only applies to the plural rule.
            return Arrays.asList("=" + count, pluralForms.apply(count - offset));
        };
    }

    private IntFunction<String> pluralFormsFor(Class<? extends PluralRule> ruleClass) {
        if (ruleClass.isInterface()) {
            return count -> GwtPlural.fromNumber(pluralRules, count).getGwtValue();
        }
        PluralRule rule = this.instantiateCustomPluralRuleClass(ruleClass);   // once, not per call
        return count -> CustomPlural.fromNumber(rule, count).getGwtValue();
    }

    /** Every combination of one candidate per selector, in priority order. */
    private static List<List<String>> combinations(List<List<String>> candidates) {
        List<List<String>> result = Collections.singletonList(Collections.emptyList());
        for (List<String> options : candidates) {
            result = result.stream()
                    .flatMap(prefix -> options.stream().map(option -> {
                        List<String> next = new ArrayList<>(prefix);
                        next.add(option);
                        return next;
                    }))
                    .collect(toList());
        }
        return result;
    }

    /**
     * Formatting a @PluralCount argument uses the same count the plural rule
     * saw: the size of an array or List, minus any @Offset.
     */
    private static List<ArgumentAdjuster> buildAdjusters(Method method) {
        Parameter[] parameters = method.getParameters();
        List<ArgumentAdjuster> adjusters = new ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            int offset = offsetOf(parameter);
            if (parameter.isAnnotationPresent(PluralCount.class) && (offset != 0 || isCollection(parameter.getType()))) {
                ToIntFunction<Object> counter = countFor(parameter, i);
                int index = i;
                adjusters.add(formatArgs -> formatArgs[index] = counter.applyAsInt(formatArgs[index]) - offset);
            }
        }
        return adjusters;
    }

    private static ToIntFunction<Object> countFor(Parameter parameter, int index) {
        Class<?> type = parameter.getType();
        if (PLURAL_COUNT_TYPES.contains(type)) {
            return arg -> arg == null ? 0 : ((Number) arg).intValue();
        }
        if (type.isArray()) {
            return arg -> arg == null ? 0 : Array.getLength(arg);
        }
        if (type == List.class) {
            return arg -> arg == null ? 0 : ((List<?>) arg).size();
        }
        throw unsupported(PluralCount.class, parameter, index, "int, short, an array, or List");
    }

    private static boolean isCollection(Class<?> type) {
        return type.isArray() || type == List.class;
    }

    private static int offsetOf(Parameter parameter) {
        return Optional.ofNullable(parameter.getAnnotation(Offset.class)).map(Offset::value).orElse(0);
    }

    private static IllegalArgumentException unsupported(Class<? extends Annotation> annotation, Parameter parameter,
                                                        int index, String allowed) {
        return new IllegalArgumentException("@" + annotation.getSimpleName() + " parameter " + index + " of "
                + parameter.getDeclaringExecutable().getName() + " must be " + allowed
                + " but was " + parameter.getType().getName());
    }

    private static String formatPlain(MessageFormat formatter, Object[] args) {
        return formatter.format(args, new StringBuffer(), null).toString();
    }

    private static SafeHtml formatSafeHtml(MessageFormat formatter, Object[] args) {
        Object[] safeArgs = args == null ? null : Arrays.stream(args).map(MessagesProxy::escape).toArray();
        // Would rather use fromSafeConstant() but doesn't work on server.
        return SafeHtmlUtils.fromTrustedString(formatPlain(formatter, safeArgs));
    }

    private static Object escape(Object arg) {
        if (arg instanceof SafeHtml) {
            return ((SafeHtml) arg).asString();
        }
        if (arg instanceof Number || arg instanceof Date) {
            return arg;   // Sub-format patterns need the real value
        }
        return SafeHtmlUtils.htmlEscape(String.valueOf(arg));
    }
    
    /**
     * Instantiates the plural rule class given inside the PluralCount 
     * annotation. The pluralRule class that is instantiated is based on the 
     * language originally given to the library. If that language is not found, 
     * defaults to the given class.
     * 
     * @param clazz The PluralRule class
     * @return An instantiated PluralRule class
     */
    private PluralRule instantiateCustomPluralRuleClass(Class<? extends PluralRule> clazz) {
        Class<? extends PluralRule> ruleClass = localizedPluralRuleClass(clazz);
        try {
            return ruleClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not instantiate custom PluralRule class " + ruleClass.getName() + ". "
                    + "Make sure the class is not an abstract class or interface and has a public default constructor.", e);
        }
    }

    private Class<? extends PluralRule> localizedPluralRuleClass(Class<? extends PluralRule> clazz) {
        String localizedClassName = clazz.getName() + "_" + this.getLocale().getLanguage();
        try {
            return Class.forName(localizedClassName, true, clazz.getClassLoader()).asSubclass(PluralRule.class);
        } catch (ClassNotFoundException e) {
            return clazz;
        }
    }
    
    private String buildPatternName(String baseName, List<String> pluralNames) {
        boolean allOther = pluralNames.stream().allMatch(GwtPlural.OTHER.getGwtValue()::equals);
        return allOther ? baseName : pluralNames.stream().collect(joining(SEPARATOR, baseName + OPEN_BRACKET, CLOSE_BRACKET));
    }

    @SuppressWarnings("deprecation")
    private Map<String, String> buildAlternateMessages(String baseName, Method method) {
        String[] values = Optional.ofNullable(method.getAnnotation(AlternateMessage.class))
                .map(AlternateMessage::value)
                .orElseGet(() -> Optional.ofNullable(method.getAnnotation(PluralText.class))
                        .map(PluralText::value)
                        .orElse(new String[0]));
        Map<String, String> alternateMessages = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            alternateMessages.put(baseName + OPEN_BRACKET + values[i] + CLOSE_BRACKET, values[i + 1]);
        }
        return alternateMessages;
    }

    private enum SelectKind {
        ENUM(Enum.class::isAssignableFrom) {
            @Override
            String select(Object arg) {
                return arg == null ? OTHER : ((Enum<?>) arg).name();
            }
        },
        STRING(String.class::equals) {
            @Override
            String select(Object arg) {
                return arg == null ? OTHER : (String) arg;
            }
        },
        PRIMITIVE(SELECT_PRIMITIVE_TYPES::contains) {
            @Override
            String select(Object arg) {
                return arg.toString();
            }
        };

        private static final String OTHER = GwtPlural.OTHER.getGwtValue();

        private final Predicate<Class<?>> matcher;

        SelectKind(Predicate<Class<?>> matcher) {
            this.matcher = matcher;
        }

        abstract String select(Object arg);

        static Optional<SelectKind> forType(Class<?> type) {
            return Arrays.stream(values()).filter(kind -> kind.matcher.test(type)).findFirst();
        }
    }
}
