package edu.michalvavrik.ptm;

import edu.michalvavrik.ptm.core.TuringMachine;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static edu.michalvavrik.ptm.BinaryNumberCopyTest.splitBySeparator;
import static edu.michalvavrik.ptm.core.TuringMachine.BLANK;

public class CopyBinaryNumToEvenLengthTest {

    private static final Logger LOG = Logger.getLogger(CopyBinaryNumToEvenLengthTest.class);

    @Test
    void testBinaryModulo() throws IOException {
        final var cmd = new StartCommand();
        cmd.setTransitionFunction("src/test/resources/same-length-enforcer-test.txt");

        // copy binary number
        for (int i = 1; i < 10000; i++) {

            // separator BLANK symbol
            // copy the number with leading zeros
            cmd.converseDecimalToBinary = false;
            cmd.setInputData("00000" + Integer.toBinaryString(i) + BLANK + "00" + Integer.toBinaryString(i-1));
            verify(cmd.computeInputData(), i, i-1);
        }

    }

    static void verify(TuringMachine.Configuration configuration, int expectedLeft, int expectedRight) {

        LOG.debugf("Verify - expected left '%d' and expected right '%d'", expectedLeft, expectedRight);
        final var splitBySeparator = splitBySeparator(configuration.tape(), BLANK);
        Assertions.assertEquals(splitBySeparator.length, 2);

        // assert same length
        Assertions.assertEquals(splitBySeparator[0].length(), splitBySeparator[1].length());

        final int actualLeft = Integer.parseInt(splitBySeparator[0], 2);
        final int actualRight = Integer.parseInt(splitBySeparator[1], 2);
        Assertions.assertEquals(expectedLeft, actualLeft);
        Assertions.assertEquals(expectedRight, actualRight);
    }

}
