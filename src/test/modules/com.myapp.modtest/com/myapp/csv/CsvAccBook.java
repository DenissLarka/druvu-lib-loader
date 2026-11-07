package com.myapp.csv;

import com.myapp.AccBook;

/**
 * Registered under {@code com.myapp.AccBook} in module-info: a direct registration, no library type anywhere in this
 * file.
 */
public final class CsvAccBook implements AccBook {
    @Override
    public String storage() {
        return "accounts.csv";
    }
}
