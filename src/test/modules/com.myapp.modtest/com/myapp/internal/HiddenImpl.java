package com.myapp.internal;

public final class HiddenImpl implements Hidden {
    @Override
    public String secret() {
        return "never loaded";
    }
}
