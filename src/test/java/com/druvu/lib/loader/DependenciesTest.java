package com.druvu.lib.loader;

import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.util.Optional;

import org.testng.Assert;
import org.testng.annotations.Test;

public class DependenciesTest {

	@Test
	public void testDependencyInheritance() {

		Dependencies dependencies = Dependencies.of(TestClassChild.class, new TestClassChild());

		TestClassChild dependency1 = dependencies.getDependency(TestClassChild.class);
		Assert.assertNotNull(dependency1);

		Optional<TestClassParent> dependency2 = dependencies.getOptionalDependency(TestClassParent.class);
		Assert.assertTrue(dependency2.isEmpty());

	}

	@Test
	public void testDependencyInheritanceBad() {

		final TestClassChild instance1 = new TestClassChild();
		final TestClassSecondChild instance2 = new TestClassSecondChild();

		try {
			Dependencies.of(TestClassChild.class, instance1, TestClassSecondChild.class, instance2);
		}
		catch (IllegalStateException e) {
			Assert.assertTrue(e.getMessage().contains("Already registered"));
		}

	}

	@Test
	public void testDependencyInterfaces() {

		final TestInterfaceImpl instance = new TestInterfaceImpl();
		Dependencies dependencies = Dependencies.of(TestInterfaceImpl.class, instance);

		TestInterfaceImpl dependency1 = dependencies.getDependency(TestInterfaceImpl.class);
		Assert.assertEquals(dependency1, instance);

		Optional<TestInterface> dependency2 = dependencies.getOptionalDependency(TestInterface.class);
		Assert.assertTrue(dependency2.isEmpty());
	}

	@Test
	public void testDependencyInterfacesBad() {

		final TestInterfaceImpl instance1 = new TestInterfaceImpl();
		final TestInterfaceSecondImpl instance2 = new TestInterfaceSecondImpl();

		try {
			Dependencies.of(TestInterfaceImpl.class, instance1, TestInterfaceSecondImpl.class, instance2);
		}
		catch (IllegalStateException e) {
			Assert.assertTrue(e.getMessage().contains("Already registered"));
		}
	}

	/*
	 * internal java interfaces should not be included
	 */
	@Test
	public void testDependencyJavaInheritance() {

		Dependencies dependencies = Dependencies.of(String.class, "test");

		String string = dependencies.getDependency(String.class);
		Assert.assertNotNull(string);

		try {
			dependencies.getDependency(Comparable.class);
			Assert.fail("Expected an IllegalStateException to be thrown");
		}
		catch (IllegalStateException e) {
			Assert.assertTrue(e.getMessage().contains("Dependency not found"));
		}

		try {
			dependencies.getDependency(Constable.class);
			Assert.fail("Expected an IllegalStateException to be thrown");
		}
		catch (IllegalStateException e) {
			Assert.assertTrue(e.getMessage().contains("Dependency not found"));
		}

		try {
			dependencies.getDependency(ConstantDesc.class);
			Assert.fail("Expected an IllegalStateException to be thrown");
		}
		catch (IllegalStateException e) {
			Assert.assertTrue(e.getMessage().contains("Dependency not found"));
		}

	}

	@Test
	public void testGetDependency() {
		Dependencies dependencies = Dependencies.of(String.class, "test", Integer.class, 1, Double.class, 2.0);

		String stringDependency = dependencies.getDependency(String.class);
		Assert.assertEquals(stringDependency, "test");

		Integer integerDependency = dependencies.getDependency(Integer.class);
		Assert.assertEquals(integerDependency.intValue(), 1);

		Double doubleDependency = dependencies.getDependency(Double.class);
		Assert.assertEquals(doubleDependency.doubleValue(), 2.0);

		try {
			dependencies.getDependency(Long.class);
			Assert.fail("Expected an IllegalArgumentException to be thrown");
		}
		catch (IllegalStateException e) {
			Assert.assertTrue(e.getMessage().contains("Dependency not found"));
		}
	}

	@Test
	public void testGetOptionalDependency() {
		Dependencies dependencies = Dependencies.of(String.class, "test", Integer.class, 1);

		Optional<String> stringDependency = dependencies.getOptionalDependency(String.class);
		Assert.assertTrue(stringDependency.isPresent());
		Assert.assertEquals(stringDependency.get(), "test");

		Optional<Integer> integerDependency = dependencies.getOptionalDependency(Integer.class);
		Assert.assertTrue(integerDependency.isPresent());
		Assert.assertEquals(integerDependency.get().intValue(), 1);

		Optional<Double> nonExistentDependency = dependencies.getOptionalDependency(Double.class);
		Assert.assertFalse(nonExistentDependency.isPresent());
	}

	public class TestClassParent {
	}

	public class TestClassSecondChild extends TestClassParent {
	}

	public class TestClassChild extends TestClassParent {
	}

	public interface TestInterface {
	}

	public class TestInterfaceImpl implements TestInterface {
	}

	public class TestInterfaceSecondImpl implements TestInterface {
	}
}