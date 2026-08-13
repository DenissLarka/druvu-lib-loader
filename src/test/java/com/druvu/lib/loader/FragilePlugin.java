package com.druvu.lib.loader;

/**
 * A component type whose two implementations do not dispose equally well: {@link Exploding} always fails, {@link Sound}
 * always succeeds. Used to prove that one failing component does not stop the others from being disposed.
 *
 * <p>Implementations and factories are nested to keep the fixture in a single file - the ServiceLoader registration
 * uses their binary names, {@code FragilePlugin$SoundFactory}.
 *
 * @author Deniss Larka on 13 Aug 2026
 */
public interface FragilePlugin {

    String getName();

    class Sound implements FragilePlugin {

        @Override
        public String getName() {
            return "Sound";
        }
    }

    class Exploding implements FragilePlugin {

        @Override
        public String getName() {
            return "Exploding";
        }
    }

    class SoundFactory implements ComponentFactory<FragilePlugin> {

        @Override
        public FragilePlugin createComponent(Dependencies dependencies) {
            return new Sound();
        }

        @Override
        public Class<FragilePlugin> type() {
            return FragilePlugin.class;
        }
    }

    class ExplodingFactory implements ComponentFactory<FragilePlugin> {

        @Override
        public FragilePlugin createComponent(Dependencies dependencies) {
            return new Exploding();
        }

        @Override
        public FragilePlugin disposeComponent(FragilePlugin component) {
            throw new IllegalStateException("dispose fails on purpose: " + component.getName());
        }

        @Override
        public Class<FragilePlugin> type() {
            return FragilePlugin.class;
        }
    }
}
