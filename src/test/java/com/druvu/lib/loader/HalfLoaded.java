package com.druvu.lib.loader;

import java.util.ArrayList;
import java.util.List;

/**
 * A plugin type whose set cannot be loaded whole: {@link SoundFactory} creates a component, {@link NullFactory} creates
 * a null. Proves that a refused set releases what was created before the failure; the services file registers the sound
 * factory first.
 */
public interface HalfLoaded {

    class Sound implements HalfLoaded {}

    class SoundFactory implements ComponentFactory<HalfLoaded> {

        /** Every component this factory was asked to dispose, for the test to read. */
        static final List<HalfLoaded> DISPOSED = new ArrayList<>();

        @Override
        public HalfLoaded createComponent(Dependencies dependencies) {
            return new Sound();
        }

        @Override
        public HalfLoaded disposeComponent(HalfLoaded component) {
            DISPOSED.add(component);
            return component;
        }

        @Override
        public Class<HalfLoaded> type() {
            return HalfLoaded.class;
        }
    }

    class NullFactory implements ComponentFactory<HalfLoaded> {

        @Override
        public HalfLoaded createComponent(Dependencies dependencies) {
            return null;
        }

        @Override
        public Class<HalfLoaded> type() {
            return HalfLoaded.class;
        }
    }
}
