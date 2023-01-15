package edu.michalvavrik.ptm;

import edu.michalvavrik.ptm.core.TuringMachine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static edu.michalvavrik.ptm.core.TuringMachine.BLANK;

public class BinaryNumberCopyTest {

    @Test
    void testBinaryModulo() throws IOException {
        final var cmd = new StartCommand();
        cmd.setTransitionFunction("src/test/resources/copy-binary-number-test.txt");

        // copy binary number
        for (int i = 1; i < 10000; i++) {

            // copy exact number (shall start with 1 unless i=0)
            cmd.converseDecimalToBinary = true;
            cmd.setInputData(Integer.toString(i));
            verify(cmd.computeInputData(), i, BLANK);

            // copy the number with leading zeros
            cmd.converseDecimalToBinary = false;
            cmd.setInputData("00000" + Integer.toBinaryString(i));
            verify(cmd.computeInputData(), i, BLANK);
        }

    }

    private void verify(TuringMachine.Configuration[] configurations, int expected, char separator) {
        final var configuration = configurations[configurations.length - 1];
        final var splitBySeparator = new String(configuration.tape()).split("" + separator);
        final int actualLeft = Integer.parseInt(splitBySeparator[0], 2);
        final int actualRight = Integer.parseInt(splitBySeparator[0], 2);
        Assertions.assertEquals(expected, actualLeft);
        Assertions.assertEquals(actualLeft, actualRight);
        Assertions.assertEquals(splitBySeparator.length, 2);
    }

}
