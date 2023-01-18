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
        final char powerSeparator = '^';
        cmd.setAdditionalInputAlphabetSymbols(powerSeparator + "");

        // copy binary number
        for (int i = 1; i < 10000; i++) {

            // separator BLANK symbol
            // copy exact number (shall start with 1 unless i=0)
            cmd.converseDecimalToBinary = true;
            cmd.setInputData(Integer.toString(i));
            verify(cmd.computeInputData(), i, BLANK);

            // separator ^ symbol
            // copy exact number (shall start with 1 unless i=0)
            cmd.converseDecimalToBinary = true;
            cmd.setInputData(Integer.toString(i) + powerSeparator);
            verify(cmd.computeInputData(), i, powerSeparator);

            // separator BLANK symbol
            // copy the number with leading zeros
            cmd.converseDecimalToBinary = false;
            cmd.setInputData("00000" + Integer.toBinaryString(i));
            verify(cmd.computeInputData(), i, BLANK);
        }

    }

    static void verify(TuringMachine.Configuration configuration, int expected, char separator) {

        // this needs to be done as String split is not reliable with ^
        final var splitBySeparator = splitBySeparator(configuration.tape(),separator);

        final int actualLeft = Integer.parseInt(splitBySeparator[0], 2);
        final int actualRight = Integer.parseInt(splitBySeparator[1], 2);
        Assertions.assertEquals(expected, actualLeft);
        Assertions.assertEquals(actualLeft, actualRight);
        Assertions.assertEquals(splitBySeparator.length, 2);
    }

    static String[] splitBySeparator(char[] tape, char separator) {
        final String[] result = new String[] { "", ""};
        int i = 0;
        for (char c : tape) {
            if (c == separator) {
                i++;
            } else {
                result[i] += c;
            }
        }
        return result;
    }

}
