package com.sample.api.server.test;

public class TestAspectUseCase {

	static {
		System.out.println("TestAspectUseCase.static");
	}

	public void testAspect() {
		testAspectAnnotated();
		if (!TestAspect.aspectWorked) {
			throw new IllegalStateException("Aspect did not work!");
		} else {
			System.out.println("Aspect worked!");
		}
	}

	@TestAspectAnnotation
	private void testAspectAnnotated() {
		System.out.println("TestAspectUseCase.testAspectAnnotated");
	}

}
