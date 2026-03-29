package io.github.sentinel.system;

import org.junit.Test;

import io.github.sentinel.exceptions.MalformedURLException;

public class JavaURLTests {

	@Test(expected = MalformedURLException.class)
	public void MalFormedURLTest() {
		JavaURL.create(null);
	}

}
