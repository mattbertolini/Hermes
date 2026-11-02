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

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.i18n.client.LocalizableResource.GenerateKeys;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.client.PluralRule;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Behavior that must match GWT's own i18n code generation.
 */
public class GwtCompatibilityTest {

    public enum Gender { MALE, FEMALE }

    // Annotations supply English plural forms; the fr file has only the base message.
    public interface AnnotationFormsMessages extends Messages {
        @DefaultMessage("{0} files")
        @AlternateMessage({"one", "One file"})
        String files(@PluralCount int count);
    }

    public interface NestedSelectMessages extends Messages {
        @DefaultMessage("default")
        @AlternateMessage({"one|MALE", "one male", "other|FEMALE", "other female"})
        String committed(@PluralCount int count, @Select Gender gender);

        @DefaultMessage("default")
        @AlternateMessage({"one|other", "one other", "other|FEMALE", "other female"})
        String innerOther(@PluralCount int count, @Select Gender gender);

        @DefaultMessage("default")
        @AlternateMessage({"=1|MALE", "exactly one male", "one|FEMALE", "one female"})
        String exactCommits(@PluralCount int count, @Select Gender gender);
    }

    public interface CollectionConstants extends Constants {
        String[] escapedArray();
        Map<String, String> formMap();
        Map<String, String> orderedMap();

        @DefaultStringArrayValue({"x,y", "z"})
        String[] defaultArray();

        @DefaultStringMapValue({"k2", "v2", "k1", "v1"})
        Map<String, String> defaultMap();
    }

    public interface FormatMessages extends Messages {
        @DefaultMessage("Attendees: {0,list}")
        String attendees(List<String> names);

        @DefaultMessage("Totals: {0,list,number,0.0}")
        String totals(double[] values);

        @DefaultMessage("Date: {0,localdatetime,yMMMd}")
        String date(java.util.Date date);

        @DefaultMessage("<b>{0,list}</b>")
        SafeHtml safeAttendees(List<String> names);
    }

    @GenerateKeys("com.google.gwt.i18n.rebind.keygen.MD5KeyGenerator")
    public interface Md5Messages extends Messages {
        @DefaultMessage("Hello")
        String hello();

        @DefaultMessage("Hello")
        @Meaning("greeting")
        String helloWithMeaning();
    }

    @GenerateKeys("com.google.gwt.i18n.server.keygen.FullyQualifiedMethodNameKeyGenerator")
    public interface QualifiedKeyMessages extends Messages {
        @DefaultMessage("Hi")
        String hi();
    }

    public interface LocaleSearchMessages extends Messages {
        @DefaultMessage("default")
        String message();
    }

    public interface RegionalRuleMessages extends Messages {
        @DefaultMessage("other form")
        @AlternateMessage({"base", "base form", "pt", "pt form", "ptpt", "pt_PT form"})
        String rule(@PluralCount(RegionalPluralRule.class) int count);
    }

    public interface PrecedenceParent extends Messages {
        String title();
    }

    public interface PrecedenceChild extends PrecedenceParent {
        @Override
        @DefaultMessage("Child")
        String title();
    }

    @DefaultLocale("fr")
    public interface FrenchDefaultMessages extends Messages {
        @DefaultMessage("Bonjour")
        String hello();
    }

    public interface InvalidSignatureMessages extends Messages {
        @DefaultMessage("{0}")
        String safeHtmlArgument(SafeHtml html);

        @DefaultMessage("{0}")
        String pluralAndSelect(@PluralCount @Select int count);
    }

    public static class RegionalPluralRule implements PluralRule {
        private final String name;

        public RegionalPluralRule() {
            this("base");
        }

        RegionalPluralRule(String name) {
            this.name = name;
        }

        @Override
        public PluralForm[] pluralForms() {
            return new PluralForm[]{new PluralForm("other", "Other"), new PluralForm(name, "One")};
        }

        @Override
        public int select(int n) {
            return n == 1 ? 1 : 0;
        }
    }

    public static class RegionalPluralRule_pt extends RegionalPluralRule {
        public RegionalPluralRule_pt() {
            super("pt");
        }
    }

    public static class RegionalPluralRule_pt_PT extends RegionalPluralRule {
        public RegionalPluralRule_pt_PT() {
            super("ptpt");
        }
    }

    @Test
    public void testAnnotationFormsDoNotLeakIntoTranslation() throws IOException {
        Assert.assertEquals("1 fichiers", Hermes.get(AnnotationFormsMessages.class, "fr").files(1));
        Assert.assertEquals("One file", Hermes.get(AnnotationFormsMessages.class, "en").files(1));
    }

    @Test
    public void testOuterSelectorCommitsBeforeInnerMatch() throws IOException {
        NestedSelectMessages messages = Hermes.get(NestedSelectMessages.class, "en");
        Assert.assertEquals("default", messages.committed(1, Gender.FEMALE));
        Assert.assertEquals("one male", messages.committed(1, Gender.MALE));
        Assert.assertEquals("other female", messages.committed(5, Gender.FEMALE));
    }

    @Test
    public void testInnerSelectorFallsBackToOther() throws IOException {
        NestedSelectMessages messages = Hermes.get(NestedSelectMessages.class, "en");
        Assert.assertEquals("one other", messages.innerOther(1, Gender.FEMALE));
        Assert.assertEquals("other female", messages.innerOther(3, Gender.FEMALE));
        Assert.assertEquals("default", messages.innerOther(3, Gender.MALE));
    }

    @Test
    public void testExactMatchCommitsBeforePluralForm() throws IOException {
        NestedSelectMessages messages = Hermes.get(NestedSelectMessages.class, "en");
        Assert.assertEquals("exactly one male", messages.exactCommits(1, Gender.MALE));
        Assert.assertEquals("default", messages.exactCommits(1, Gender.FEMALE));
    }

    @Test
    public void testStringArraySplitsLikeGwt() throws IOException {
        CollectionConstants constants = Hermes.get(CollectionConstants.class, null);
        Assert.assertArrayEquals(new String[]{"a,b", "c", "d", ""}, constants.escapedArray());
        Assert.assertArrayEquals(new String[]{"x,y", "z"}, constants.defaultArray());
    }

    @Test
    public void testMapUsesBracketFormsInDeclarationOrder() throws IOException {
        CollectionConstants constants = Hermes.get(CollectionConstants.class, null);
        Map<String, String> formMap = constants.formMap();
        Assert.assertEquals(Arrays.asList("red", "blue"), new ArrayList<>(formMap.keySet()));
        Assert.assertEquals("Rouge", formMap.get("red"));
        Assert.assertEquals("Bleu", formMap.get("blue"));
        Assert.assertEquals(Arrays.asList("z", "a", "m"), new ArrayList<>(constants.orderedMap().keySet()));
        Assert.assertEquals(Arrays.asList("k2", "k1"), new ArrayList<>(constants.defaultMap().keySet()));
        Assert.assertEquals("v1", constants.defaultMap().get("k1"));
    }

    @Test
    public void testListFormat() throws IOException {
        FormatMessages messages = Hermes.get(FormatMessages.class, "en");
        Assert.assertEquals("Attendees: a, b, and c", messages.attendees(Arrays.asList("a", "b", "c")));
        Assert.assertEquals("Attendees: a and b", messages.attendees(Arrays.asList("a", "b")));
        Assert.assertEquals("Attendees: ", messages.attendees(Collections.emptyList()));
        Assert.assertEquals("Totals: 1.5 and 2.0", messages.totals(new double[]{1.5, 2}));
    }

    @Test
    public void testListFormatEscapesSafeHtmlElements() throws IOException {
        FormatMessages messages = Hermes.get(FormatMessages.class, "en");
        SafeHtml expected = SafeHtmlUtils.fromTrustedString("<b>&lt;i&gt; and b</b>");
        Assert.assertEquals(expected, messages.safeAttendees(Arrays.asList("<i>", "b")));
    }

    @Test
    public void testLocalDateTimeFormat() throws IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2026, Calendar.MARCH, 15, 12, 0);
        FormatMessages messages = Hermes.get(FormatMessages.class, "en");
        Assert.assertEquals("Date: Mar 15, 2026", messages.date(calendar.getTime()));
    }

    @Test
    public void testMd5GeneratedKeys() throws IOException {
        Md5Messages messages = Hermes.get(Md5Messages.class, "fr");
        Assert.assertEquals("Bonjour", messages.hello());
        Assert.assertEquals("Salut", messages.helloWithMeaning());
        Assert.assertEquals("Hello", Hermes.get(Md5Messages.class, "en").hello());
    }

    @Test
    public void testFullyQualifiedGeneratedKeys() throws IOException {
        Assert.assertEquals("Salut", Hermes.get(QualifiedKeyMessages.class, "fr").hi());
    }

    @Test
    public void testLocaleSearchUsesGwtInheritance() throws IOException {
        Assert.assertEquals("traditional", Hermes.get(LocaleSearchMessages.class, "zh_TW").message());
        Assert.assertEquals("latam", Hermes.get(LocaleSearchMessages.class, "es_MX").message());
        Assert.assertEquals("hebrew", Hermes.get(LocaleSearchMessages.class, "iw").message());
        Assert.assertEquals("default", Hermes.get(LocaleSearchMessages.class, "de").message());
    }

    @Test
    public void testCustomPluralRuleUsesMostSpecificLocalizedClass() throws IOException {
        Assert.assertEquals("pt_PT form", Hermes.get(RegionalRuleMessages.class, "pt_PT").rule(1));
        Assert.assertEquals("pt form", Hermes.get(RegionalRuleMessages.class, "pt_BR").rule(1));
        Assert.assertEquals("base form", Hermes.get(RegionalRuleMessages.class, "en").rule(1));
    }

    @Test
    public void testChildAnnotationBeatsParentDefaultFile() throws IOException {
        Assert.assertEquals("Child", Hermes.get(PrecedenceChild.class, null).title());
        Assert.assertEquals("Parent", Hermes.get(PrecedenceParent.class, null).title());
    }

    @Test
    public void testDefaultLocaleAnnotationsBeatDefaultFile() throws IOException {
        Assert.assertEquals("Bonjour", Hermes.get(FrenchDefaultMessages.class, "fr_CA").hello());
        Assert.assertEquals("Hello", Hermes.get(FrenchDefaultMessages.class, "en").hello());
    }

    @Test
    public void testSafeHtmlArgumentRequiresSafeHtmlReturn() throws IOException {
        InvalidSignatureMessages messages = Hermes.get(InvalidSignatureMessages.class, null);
        Assert.assertThrows(IllegalArgumentException.class,
                () -> messages.safeHtmlArgument(SafeHtmlUtils.fromString("x")));
    }

    @Test
    public void testPluralCountAndSelectOnSameParameterIsRejected() throws IOException {
        InvalidSignatureMessages messages = Hermes.get(InvalidSignatureMessages.class, null);
        Assert.assertThrows(IllegalArgumentException.class, () -> messages.pluralAndSelect(1));
    }
}
