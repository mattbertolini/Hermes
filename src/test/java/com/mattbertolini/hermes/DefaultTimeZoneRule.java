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

import org.jspecify.annotations.Nullable;
import org.junit.rules.ExternalResource;

/**
 * Sets both the JDK and the ICU default time zone for a test, then restores
 * them. ICU caches its own default the first time it is used, so changing
 * only java.util.TimeZone leaves date formatting dependent on test order.
 */
public final class DefaultTimeZoneRule extends ExternalResource {
    private final String zoneId;
    private java.util.@Nullable TimeZone originalJdkZone;
    private com.ibm.icu.util.@Nullable TimeZone originalIcuZone;

    public DefaultTimeZoneRule(String zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    protected void before() {
        originalJdkZone = java.util.TimeZone.getDefault();
        originalIcuZone = com.ibm.icu.util.TimeZone.getDefault();
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zoneId));
        com.ibm.icu.util.TimeZone.setDefault(com.ibm.icu.util.TimeZone.getTimeZone(zoneId));
    }

    @Override
    protected void after() {
        com.ibm.icu.util.TimeZone.setDefault(originalIcuZone);
        java.util.TimeZone.setDefault(originalJdkZone);
    }
}
