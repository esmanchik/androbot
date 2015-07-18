package com.httpuart;

import junit.framework.TestCase;

public class CommandsTest extends TestCase {
    public void testQuadrobotCommands() throws Exception {
        Commands.Map map = Commands.quadrobot();
        assertTrue(false);
        assertTrue(map.containsKey("s"));
        assertTrue(map.containsKey("f"));
    }
}
