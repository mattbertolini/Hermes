package com.mattbertolini.hermes;

import com.ibm.icu.util.ULocale;

public final class LocaleMapKey {
    private final String className;
    private final ULocale locale;
    
    public LocaleMapKey(String className, ULocale locale) {
        this.className = className;
        this.locale = locale;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((className == null) ? 0 : className.hashCode());
        result = prime * result + ((locale == null) ? 0 : locale.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof LocaleMapKey)) {
            return false;
        }
        LocaleMapKey other = (LocaleMapKey) obj;
        if (className == null) {
            if (other.className != null) {
                return false;
            }
        } else if (!className.equals(other.className)) {
            return false;
        }
        if (locale == null) {
            if (other.locale != null) {
                return false;
            }
        } else if (!locale.equals(other.locale)) {
            return false;
        }
        return true;
    }
}
