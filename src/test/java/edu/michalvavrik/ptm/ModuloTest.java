package edu.michalvavrik.ptm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ModuloTest {

    @Test
    void testBinaryModulo() throws IOException {
        moduloAndVerify(5, 2);
        moduloAndVerify(22, 10);
        moduloAndVerify(5, 1);
        moduloAndVerify(500, 250);
        moduloAndVerify(589, 6);
        moduloAndVerify(12678, 63);
        moduloAndVerify(12678, 6);
        moduloAndVerify(12678, 999);
    }

    private void moduloAndVerify(int dividend, int divisor) throws IOException {
        final var cmd = new StartCommand();
        cmd.converseDecimalToBinary = true;
        cmd.setTransitionFunction("src/test/resources/modulo-test.txt");
        var inputData = dividend + "#" + divisor;
        cmd.setInputData(inputData);
        final var configurations = cmd.computeInputData();
        final var result = new String(configurations[configurations.length - 1].tape());

        // test remainder
        final long actual = Long.parseLong(result, 2);
        final long expected = dividend % divisor;
        Assertions.assertEquals(expected, actual);
    }

}
