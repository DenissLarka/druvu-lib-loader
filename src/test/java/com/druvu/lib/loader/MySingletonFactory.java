package com.druvu.lib.loader;

/** @author Deniss Larka on 23 May 2025 */
public class MySingletonFactory implements ComponentFactory<MySingleton> {

    @Override
    public MySingleton get() {
        return new MySingleton();
    }

    @Override
    public MySingleton createComponent(Dependencies dependencies) {
        return get();
    }

    @Override
    public MySingleton disposeComponent(MySingleton component) {
        return ComponentFactory.super.disposeComponent(component);
    }

    @Override
    public Class<MySingleton> type() {
        return MySingleton.class;
    }
}
