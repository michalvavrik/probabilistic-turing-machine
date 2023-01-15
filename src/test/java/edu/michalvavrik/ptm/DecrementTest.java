package edu.michalvavrik.ptm;

import edu.michalvavrik.ptm.core.TuringMachine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class DecrementTest {

    @Test
    void testBinaryModulo() throws IOException {
        final var cmd = new StartCommand();
        cmd.setTransitionFunction("src/test/resources/decrement-test.txt");

        // decrement as result is >= 0
        for (int i = 1; i < 10000; i++) {

            // decrement exact number (shall start with 1 unless i=0)
            cmd.converseDecimalToBinary = true;
            cmd.setInputData(Integer.toString(i));
            verify(cmd.computeInputData(), i - 1);

            // decrement the number with leading zeros
            cmd.converseDecimalToBinary = false;
            cmd.setInputData("00000" + Integer.toBinaryString(i));
            verify(cmd.computeInputData(), i - 1);
        }

        // contract:
        // negative numbers -> undefined
        // zero -> zero

        // verify exact number (shall start with 1 unless i=0)
        cmd.converseDecimalToBinary = true;
        cmd.setInputData(Integer.toString(0));
        verify(cmd.computeInputData(), 0);

        // verify the number with leading zeros
        cmd.converseDecimalToBinary = false;
        cmd.setInputData("00000" + Integer.toBinaryString(0));
        verify(cmd.computeInputData(), 0);
    }

    private void verify(TuringMachine.Configuration[] configurations, int expected) {
        final var configuration = configurations[configurations.length - 1];
        final int actual = Integer.parseInt(new String(configuration.tape()), 2);
        Assertions.assertEquals(expected, actual);
    }

}
