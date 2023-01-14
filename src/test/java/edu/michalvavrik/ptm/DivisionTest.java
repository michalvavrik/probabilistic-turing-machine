package edu.michalvavrik.ptm;

import edu.michalvavrik.ptm.core.TuringMachine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class DivisionTest {

    @Test
    void testBinaryDivision() throws IOException {
        divideAndVerify(5, 2);
        divideAndVerify(22, 10);
        divideAndVerify(5, 1);
        divideAndVerify(500, 250);
        divideAndVerify(589, 6);
        divideAndVerify(12678, 63);
        divideAndVerify(12678, 6);
        divideAndVerify(12678, 999);
    }

    private void divideAndVerify(int dividend, int divisor) throws IOException {
        final var cmd = new StartCommand();
        cmd.converseDecimalToBinary = true;
        cmd.setTransitionFunction("src/test/resources/division-test.txt");
        var inputData = dividend + "#" + divisor;
        cmd.setInputData(inputData);
        final var configurations = cmd.computeInputData();
        final var result = new String(configurations[configurations.length - 1].tape())
                .split(TuringMachine.BLANK + "" + TuringMachine.BLANK);

        // test whole-number (integer) result
        long actual = Long.parseLong(result[0], 2);
        long expected = dividend / divisor;
        Assertions.assertEquals(expected, actual);

        // test remainder
        actual = Long.parseLong(result[1], 2);
        expected = dividend % divisor;
        Assertions.assertEquals(expected, actual);
    }

}
