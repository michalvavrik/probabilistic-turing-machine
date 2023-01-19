package edu.michalvavrik.ptm;

import edu.michalvavrik.ptm.core.TuringMachine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class IncrementTest {

    @Test
    void testBinaryModulo() throws IOException {
        final var cmd = new StartCommand();
        cmd.setTransitionFunction("src/test/resources/increment-test.txt");

        for (int i = 0; i < 10000; i++) {

            // increment exact number (shall start with 1 unless i=0)
            cmd.converseDecimalToBinary = true;
            cmd.setInputData(Integer.toString(i));
            verify(cmd.computeInputData(), i + 1);

            // increment the number with leading zeros
            cmd.converseDecimalToBinary = false;
            cmd.setInputData("00000" + Integer.toBinaryString(i));
            verify(cmd.computeInputData(), i + 1);
        }
    }

    private void verify(TuringMachine.Configuration configuration, int expected) {
        final int actual = Integer.parseInt(new String(configuration.tape()), 2);
        Assertions.assertEquals(expected, actual);
    }

}
