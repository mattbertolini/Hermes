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

import com.google.gwt.i18n.client.Messages.Offset;
import com.google.gwt.i18n.client.Messages.PluralCount;
import com.google.gwt.i18n.client.Messages.Select;
import com.google.gwt.i18n.client.PluralRule;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;
import com.mattbertolini.hermes.AbstractI18nProxy;
import com.mattbertolini.hermes.MethodHandler;
import com.mattbertolini.hermes.Resource;
import com.mattbertolini.hermes.ResourceList;
import com.mattbertolini.hermes.internal.locale.GwtLocales;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public class MessagesProxy extends AbstractI18nProxy {
    private static final Set<Class<?>> PLURAL_COUNT_TYPES = new HashSet<>(Arrays.<Class<?>>asList(int.class, short.class));
    private static final Set<Class<?>> SELECT_INTEGRAL_TYPES = new HashSet<>(Arrays.<Class<?>>asList(
            int.class, long.class, short.class, byte.class, char.class));
    private static final String OTHER = GwtPlural.OTHER.getGwtValue();
    private static final String EXACT_PREFIX = "=";
    private static final String SEPARATOR = "|";
    // Localized rule class per base rule class and locale name, so the class
    // search (and its ClassNotFoundExceptions) runs once, not per method
    private static final ClassValue<Map<String, Class<? extends PluralRule>>> LOCALIZED_RULES =
            new ClassValue<Map<String, Class<? extends PluralRule>>>() {
                @Override
                protected Map<String, Class<? extends PluralRule>> computeValue(Class<?> type) {
                    return new ConcurrentHashMap<>();
                }
            };
    
    private final PluralRules pluralRules;
    
    public MessagesProxy(Class<?> clazz, ULocale locale, ResourceList resources) {
        super(clazz, locale, resources);
        pluralRules = PluralRules.forLocale(this.getLocale());
    }

    @Override
    protected MethodHandler createHandler(Method method) {
        return new MessageHandler(method);
    }

    /** Picks one selector's form from those available, or null when none applies. */
    @FunctionalInterface
    private interface AlternateFormSelector {
        String select(Object[] args, Collection<String> available);
    }

    @FunctionalInterface
    private interface ArgumentAdjuster {
        void adjust(Object[] formatArgs);
    }

    private final class MessageHandler implements MethodHandler {
        private final String key;
        private final int parameterCount;
        private final int innerPluralIndex;
        private final boolean safeHtml;
        private final List<AlternateFormSelector> selectors;
        private final List<ArgumentAdjuster> adjusters;
        private final Resource entry;
        private final List<String[]> forms;
        private final Map<String, CompiledMessage> formats = new ConcurrentHashMap<>();

        MessageHandler(Method method) {
            validate(method);
            key = keyFor(method);
            parameterCount = method.getParameterCount();
            innerPluralIndex = lastPluralCountIndex(method);
            safeHtml = method.getReturnType() == SafeHtml.class;
            selectors = buildSelectors(method);
            adjusters = buildAdjusters(method);
            // Like GWT, the message and all of its alternate forms come from the
            // first resource defining the key, never from less specific ones.
            entry = Optional.ofNullable(getResources().getEntry(key))
                    .orElseThrow(() -> new MissingResourceException("No message found for key " + key,
                            getI18nClass().getName(), key));
            forms = selectors.isEmpty() ? Collections.emptyList() : buildForms(method);
        }

        @Override
        public Object handle(Object[] args) {
            String form = selectForm(args);
            String pattern = form == null ? entry.get(key) : entry.getForm(key, form);
            // Parse each pattern once; CompiledMessage clones its formatter per call.
            return formats.computeIfAbsent(pattern, p -> new CompiledMessage(p, getLocale(), parameterCount, innerPluralIndex))
                    .format(args, adjust(args), safeHtml);
        }

        private List<String[]> buildForms(Method method) {
            List<String[]> result = new ArrayList<>();
            for (String form : entry.getForms(key)) {
                String[] parts = form.split("\\|", -1);
                if (parts.length != selectors.size()) {
                    throw new IllegalArgumentException("Incorrect number of selector forms for "
                            + method.getName() + " - '" + form + "'");
                }
                // GWT ignores an all-"other" form; the default message covers it
                if (!Arrays.stream(parts).allMatch(OTHER::equals)) {
                    result.add(parts);
                }
            }
            return result;
        }

        /**
         * Mirrors GWT's generated nested switches: each selector in turn commits
         * to its best available form (exact value, then plural form or value,
         * then "other"), and if a later selector then finds nothing under that
         * choice, the default message is used.
         */
        private String selectForm(Object[] args) {
            List<String[]> remaining = forms;
            List<String> chosen = new ArrayList<>();
            for (int i = 0; i < selectors.size() && !remaining.isEmpty(); i++) {
                int index = i;
                Set<String> available = remaining.stream()
                        .map(parts -> parts[index])
                        .collect(toCollection(LinkedHashSet::new));
                String part = selectors.get(i).select(args, available);
                if (part == null) {
                    return null;
                }
                chosen.add(part);
                remaining = remaining.stream().filter(parts -> parts[index].equals(part)).collect(toList());
            }
            return chosen.size() == selectors.size() && !remaining.isEmpty() ? String.join(SEPARATOR, chosen) : null;
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

    // GWT's {#} refers to the last @PluralCount parameter
    private static int lastPluralCountIndex(Method method) {
        Parameter[] parameters = method.getParameters();
        for (int i = parameters.length - 1; i >= 0; i--) {
            if (parameters[i].isAnnotationPresent(PluralCount.class)) {
                return i;
            }
        }
        return -1;
    }

    /** Rejects the signatures GWT's MessagesMethodCreator refuses to compile. */
    private static void validate(Method method) {
        boolean safeHtmlReturn = method.getReturnType() == SafeHtml.class;
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(PluralCount.class) && parameter.isAnnotationPresent(Select.class)) {
                throw new IllegalArgumentException("Parameter " + i + " of " + method.getName()
                        + " cannot be both @PluralCount and @Select");
            }
            if (!safeHtmlReturn && parameter.getType() == SafeHtml.class) {
                throw new IllegalArgumentException("Message methods with SafeHtml arguments can only have SafeHtml "
                        + "return type: " + method.getName());
            }
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
                            "an integral primitive, boolean, enum, or String"));
            return (args, available) -> {
                for (String candidate : available) {
                    // Like GWT, "=VALUE" is the same as "VALUE" for @Select
                    String value = candidate.startsWith(EXACT_PREFIX) ? candidate.substring(1) : candidate;
                    if (!OTHER.equals(candidate) && kind.matches(args[index], value)) {
                        return candidate;
                    }
                }
                return available.contains(OTHER) ? OTHER : null;
            };
        }
        return null;   // Not a selector parameter
    }

    private AlternateFormSelector pluralSelector(Parameter parameter, PluralCount pluralCount, int index) {
        ToIntFunction<Object> counter = countFor(parameter, index);
        int offset = offsetOf(parameter);
        IntFunction<String> pluralForms = pluralFormsFor(pluralCount.value());
        return (args, available) -> {
            int count = counter.applyAsInt(args[index]);
            // Exact "=n" forms match the raw count; the offset only applies to the plural rule.
            String exact = EXACT_PREFIX + count;
            if (available.contains(exact)) {
                return exact;
            }
            String form = pluralForms.apply(count - offset);
            if (available.contains(form)) {
                return form;
            }
            return available.contains(OTHER) ? OTHER : null;
        };
    }

    private IntFunction<String> pluralFormsFor(Class<? extends PluralRule> ruleClass) {
        if (ruleClass.isInterface()) {
            return count -> GwtPlural.fromNumber(pluralRules, count).getGwtValue();
        }
        PluralRule rule = this.instantiateCustomPluralRuleClass(ruleClass);   // once, not per call
        return count -> CustomPlural.fromNumber(rule, count).getGwtValue();
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
            return arg -> ((Number) arg).intValue();
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
    
    /**
     * Instantiates the plural rule class given inside the PluralCount
     * annotation. Like GWT, the most specific localized subclass along the
     * locale's search list is used (Rule_fr_CA, then Rule_fr, ...), falling
     * back to the given class.
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
        return LOCALIZED_RULES.get(clazz).computeIfAbsent(this.getLocale().getName(),
                localeName -> findLocalizedPluralRuleClass(clazz, localeName));
    }

    private static Class<? extends PluralRule> findLocalizedPluralRuleClass(Class<? extends PluralRule> clazz,
                                                                            String localeName) {
        for (GwtLocale search : GwtLocales.searchList(GwtLocales.fromString(localeName))) {
            if (search.isDefault()) {
                break;
            }
            try {
                return Class.forName(clazz.getName() + "_" + search.getAsString(), true, clazz.getClassLoader())
                        .asSubclass(PluralRule.class);
            } catch (ClassNotFoundException | ClassCastException e) {
                // Not defined for this locale; try the next one
            }
        }
        return clazz;
    }

    private enum SelectKind {
        ENUM(Enum.class::isAssignableFrom) {
            @Override
            boolean matches(Object arg, String value) {
                return arg != null && ((Enum<?>) arg).name().equals(value);
            }
        },
        STRING(String.class::equals) {
            @Override
            boolean matches(Object arg, String value) {
                return value.equals(arg);
            }
        },
        BOOLEAN(boolean.class::equals) {
            @Override
            boolean matches(Object arg, String value) {
                // GWT tests Boolean.parseBoolean(value), so any form other than "true" means false
                return Boolean.parseBoolean(value) == (Boolean) arg;
            }
        },
        INTEGRAL(SELECT_INTEGRAL_TYPES::contains) {
            @Override
            boolean matches(Object arg, String value) {
                long number = arg instanceof Character ? (Character) arg : ((Number) arg).longValue();
                try {
                    return Long.parseLong(value) == number;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("'" + value + "' is not a valid integral value", e);
                }
            }
        };

        private final Predicate<Class<?>> matcher;

        SelectKind(Predicate<Class<?>> matcher) {
            this.matcher = matcher;
        }

        abstract boolean matches(Object arg, String value);

        static Optional<SelectKind> forType(Class<?> type) {
            return Arrays.stream(values()).filter(kind -> kind.matcher.test(type)).findFirst();
        }
    }
}
