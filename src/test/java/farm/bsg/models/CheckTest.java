package farm.bsg.models;

import org.junit.Assert;
import org.junit.Test;

public class CheckTest {

    @Test
    public void Coverage() {
        final Check check = new Check();
        Assert.assertNotNull(check.toJson());
    }

}
