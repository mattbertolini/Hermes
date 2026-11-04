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

import java.io.FileNotFoundException;
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

        @DefaultMessage("Date: {0,localdatetime,predef:DATE_SHORT}")
        String predefShortDate(java.util.Date date);

        @DefaultMessage("Date: {0,localdatetime,predef:YEAR_MONTH_ABBR_DAY}")
        String predefSkeletonDate(java.util.Date date);

        @DefaultMessage("{0,localdatetime,predef:ISO_8601}")
        String predefIsoDate(java.util.Date date);

        @DefaultMessage("{0,localdatetime,predef:NO_SUCH_FORMAT}")
        String predefUnknown(java.util.Date date);

        @DefaultMessage("{0,localdatetime}")
        String localDateTimeWithoutSkeleton(java.util.Date date);

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

        @DefaultMessage("Hello {1}")
        String argumentOutOfRange(String name);

        @DefaultMessage("Don't forget {0}")
        String unterminatedQuote(String name);

        @DefaultMessage("{#} items")
        String innerPluralWithoutPluralCount(int count);

        @DefaultMessage("{0}")
        String pluralAndSelect(@PluralCount @Select int count);

        @DefaultMessage("{0}")
        @AlternateMessage({"one|MALE", "One male"})
        String wrongSelectorCount(@PluralCount int count);
    }

    public interface SelectMatchingMessages extends Messages {
        @DefaultMessage("other")
        @AlternateMessage({"true", "yes", "false", "no"})
        String bool(@Select boolean value);

        // GWT compares a char against the form as an integral value
        @DefaultMessage("other")
        @AlternateMessage({"65", "letter A"})
        String character(@Select char value);

        @DefaultMessage("other")
        @AlternateMessage({"=red", "is red", "blue", "is blue"})
        String color(@Select String value);

        @DefaultMessage("other")
        @AlternateMessage({"=MALE", "male"})
        String gender(@Select Gender value);
    }

    @SuppressWarnings("deprecation") // PluralText is deprecated, but GWT still supports it
    public interface PluralTextMessages extends Messages {
        @DefaultMessage("{0} items")
        @PluralText({"one", "One item"})
        String items(@PluralCount int count);
    }

    // Its properties file is named GwtCompatibilityTest_UnderscoreFileMessages
    public interface UnderscoreFileMessages extends Messages {
        String message();
    }

    @GenerateKeys("com.example.NoSuchKeyGenerator")
    public interface UnknownKeyGeneratorMessages extends Messages {
        @DefaultMessage("Hello")
        String hello();
    }

    public interface OddAlternateMessages extends Messages {
        @DefaultMessage("{0}")
        @AlternateMessage({"one"})
        String odd(@PluralCount int count);
    }

    // No properties file and no annotation: its key comes from the child's file
    public interface KeyFromChildParent extends Messages {
        String title();
    }

    public interface KeyFromChildChild extends KeyFromChildParent {
        String subtitle();
    }

    public interface InnerPluralMessages extends Messages {
        @DefaultMessage("{#} items")
        @AlternateMessage({"one", "One item ({#})"})
        String items(@PluralCount int count);

        @DefaultMessage("You and {#} others")
        @AlternateMessage({"=0", "Just you", "one", "You and {#} other"})
        String others(@PluralCount @Offset(1) int count);
    }

    // No properties file, and MD5 can't compute a key without default text
    @GenerateKeys("com.google.gwt.i18n.rebind.keygen.MD5KeyGenerator")
    public interface Md5WithoutFileMessages extends Messages {
        String title();
    }

    public interface OddMapConstants extends Constants {
        @DefaultStringMapValue({"key"})
        Map<String, String> odd();
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

    // The keys in the fr file are uppercase hex MD5 of the text (plus the
    // meaning), matching GWT's MD5KeyGenerator and StringUtils.toHexString.
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

    @Test
    public void testAlternateFormsWithWrongSelectorCountAreRejected() throws IOException {
        InvalidSignatureMessages messages = Hermes.get(InvalidSignatureMessages.class, null);
        Assert.assertThrows(IllegalArgumentException.class, () -> messages.wrongSelectorCount(1));
    }

    @Test
    public void testBooleanSelect() throws IOException {
        SelectMatchingMessages messages = Hermes.get(SelectMatchingMessages.class, null);
        Assert.assertEquals("yes", messages.bool(true));
        Assert.assertEquals("no", messages.bool(false));
    }

    @Test
    public void testCharSelectMatchesIntegralValue() throws IOException {
        SelectMatchingMessages messages = Hermes.get(SelectMatchingMessages.class, null);
        Assert.assertEquals("letter A", messages.character('A'));
        Assert.assertEquals("other", messages.character('B'));
    }

    @Test
    public void testStringSelectTreatsExactPrefixAsValue() throws IOException {
        SelectMatchingMessages messages = Hermes.get(SelectMatchingMessages.class, null);
        Assert.assertEquals("is red", messages.color("red"));
        Assert.assertEquals("is blue", messages.color("blue"));
        Assert.assertEquals("other", messages.color("green"));
        Assert.assertEquals("other", messages.color(null));
    }

    @Test
    public void testEnumSelectTreatsExactPrefixAsValue() throws IOException {
        SelectMatchingMessages messages = Hermes.get(SelectMatchingMessages.class, null);
        Assert.assertEquals("male", messages.gender(Gender.MALE));
        Assert.assertEquals("other", messages.gender(Gender.FEMALE));
        Assert.assertEquals("other", messages.gender(null));
    }

    @Test
    public void testPluralTextAlternateForms() throws IOException {
        PluralTextMessages messages = Hermes.get(PluralTextMessages.class, "en");
        Assert.assertEquals("One item", messages.items(1));
        Assert.assertEquals("2 items", messages.items(2));
    }

    @Test
    public void testNestedInterfaceLoadsUnderscoreNamedPropertiesFile() throws IOException {
        Assert.assertEquals("from underscore file", Hermes.get(UnderscoreFileMessages.class, null).message());
    }

    @Test
    public void testUnknownKeyGeneratorIsRejected() {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class,
                () -> Hermes.get(UnknownKeyGeneratorMessages.class, null));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("com.example.NoSuchKeyGenerator"));
    }

    @Test
    public void testOddLengthAlternateMessageIsRejected() {
        Assert.assertThrows(IllegalArgumentException.class, () -> Hermes.get(OddAlternateMessages.class, null));
    }

    @Test
    public void testOddLengthDefaultStringMapValueIsRejected() {
        Assert.assertThrows(IllegalArgumentException.class, () -> Hermes.get(OddMapConstants.class, null));
    }

    @Test
    public void testParentKeyMayComeFromChildPropertiesFile() throws IOException {
        KeyFromChildChild messages = Hermes.get(KeyFromChildChild.class, null);
        Assert.assertEquals("Hello", messages.title());
        Assert.assertEquals("Sub", messages.subtitle());
    }

    @Test
    public void testInterfaceWhoseKeysResolveNowhereStillFails() {
        Assert.assertThrows(FileNotFoundException.class, () -> Hermes.get(KeyFromChildParent.class, null));
    }

    @Test
    public void testInnerPluralArgument() throws IOException {
        InnerPluralMessages messages = Hermes.get(InnerPluralMessages.class, "en");
        Assert.assertEquals("One item (1)", messages.items(1));
        Assert.assertEquals("3 items", messages.items(3));
    }

    @Test
    public void testInnerPluralArgumentAppliesOffset() throws IOException {
        InnerPluralMessages messages = Hermes.get(InnerPluralMessages.class, "en");
        Assert.assertEquals("Just you", messages.others(0));
        Assert.assertEquals("You and 1 other", messages.others(2));
        Assert.assertEquals("You and 4 others", messages.others(5));
    }

    @Test
    public void testInnerPluralArgumentWithoutPluralCountIsRejected() throws IOException {
        InvalidSignatureMessages messages = Hermes.get(InvalidSignatureMessages.class, null);
        Assert.assertThrows(IllegalArgumentException.class, () -> messages.innerPluralWithoutPluralCount(1));
    }

    @Test
    public void testPredefinedLocalDateTimeFormats() throws IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2026, Calendar.MARCH, 15, 12, 0);
        FormatMessages messages = Hermes.get(FormatMessages.class, "en");
        Assert.assertEquals("Date: 3/15/26", messages.predefShortDate(calendar.getTime()));
        Assert.assertEquals("Date: Mar 15, 2026", messages.predefSkeletonDate(calendar.getTime()));
        String iso = messages.predefIsoDate(calendar.getTime());
        Assert.assertTrue(iso, iso.matches("2026-03-15T12:00:00\\.000([+-]\\d{2}:\\d{2}|Z)"));
    }

    @Test
    public void testUnknownPredefinedFormatIsRejected() throws IOException {
        FormatMessages messages = Hermes.get(FormatMessages.class, "en");
        Assert.assertThrows(IllegalArgumentException.class, () -> messages.predefUnknown(new java.util.Date()));
    }

    @Test
    public void testLocalDateTimeWithoutSkeletonIsRejected() throws IOException {
        FormatMessages messages = Hermes.get(FormatMessages.class, "en");
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class,
                () -> messages.localDateTimeWithoutSkeleton(new java.util.Date()));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("skeleton"));
    }

    @Test
    public void testArgumentBeyondParametersIsRejected() throws IOException {
        InvalidSignatureMessages messages = Hermes.get(InvalidSignatureMessages.class, null);
        Assert.assertThrows(IllegalArgumentException.class, () -> messages.argumentOutOfRange("x"));
    }

    @Test
    public void testUnterminatedQuoteIsRejected() throws IOException {
        InvalidSignatureMessages messages = Hermes.get(InvalidSignatureMessages.class, null);
        Assert.assertThrows(IllegalArgumentException.class, () -> messages.unterminatedQuote("x"));
    }

    @Test
    public void testUncomputableMd5KeyReportsMissingFile() {
        Assert.assertThrows(FileNotFoundException.class, () -> Hermes.get(Md5WithoutFileMessages.class, null));
    }
}
