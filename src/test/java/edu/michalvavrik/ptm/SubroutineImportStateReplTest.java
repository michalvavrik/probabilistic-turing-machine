package edu.michalvavrik.ptm;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static edu.michalvavrik.ptm.core.TuringMachine.BLANK;

public class SubroutineImportStateReplTest {

    @Test
    void testStateReplacement() throws IOException {
        final var cmd = new StartCommand();
        cmd.setTransitionFunction("src/test/resources/subroutine-state-replacement.txt");
        cmd.converseDecimalToBinary = true;
        cmd.setInputData(Integer.toString(5));
        BinaryNumberCopyTest.verify(cmd.computeInputData(), 5, BLANK);
    }

}
