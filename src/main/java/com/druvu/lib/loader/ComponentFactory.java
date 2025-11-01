package com.druvu.lib.loader;

/**
 * @author Deniss Larka
 * on 19 Aug 2025
 */
public interface ComponentFactory<T> {

	T createComponent(Dependencies dependencies);

	default T disposeComponent(T component) {
		//free resources
		return component;  //no-op by default. override if needed.
	}

	Class getComponentType();

}
