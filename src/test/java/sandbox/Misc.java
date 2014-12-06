package sandbox;

import org.junit.Assert;
import org.junit.Test;


public class Misc {

	@Test
	public void testModulo(){
		Assert.assertEquals(5f, 125f % 120f, 0f);
		Assert.assertEquals(5f, 125f % ((float)120), 0f);
		Assert.assertEquals(0.5f, 120.5f % ((float)120), 0f);
	}
}
