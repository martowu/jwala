package com.siemens.cto.aem.domain.model.exec;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RuntimeCommandTest {

    @Test
    public void testExecute() {
        RuntimeCommand rtCommand = new RuntimeCommand("C:\\WINDOWS\\system32\\cmd.exe /c echo Hello World");
        ExecData result = rtCommand.execute();
        assertTrue(result.getReturnCode().wasSuccessful());
    }
}
