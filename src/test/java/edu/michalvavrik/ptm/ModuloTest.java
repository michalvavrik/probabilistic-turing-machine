package edu.michalvavrik.ptm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.math.BigInteger;

public class ModuloTest {

    @Test
    void testBinaryModulo() throws IOException {
        moduloAndVerify(9, 5);
        moduloAndVerify(16, 7);
        moduloAndVerify(16, 5);
        moduloAndVerify(8, 3);
        moduloAndVerify(754, 138);
        moduloAndVerify(657, 2);
        moduloAndVerify(5, 1);
        moduloAndVerify(7823, 5756);
        moduloAndVerify(546, 74);
    }

    @EnabledIfSystemProperty(named = "modulo-full", matches = "true")
    @Test
    void bigNumberModulo() throws IOException {
        final var cmd = new StartCommand();
        cmd.setTransitionFunction("src/test/resources/modulo-test.txt");
        cmd.converseDecimalToBinary = true;
        var inputData = String.format("%d#%d", 1350899999L, 20);
        cmd.setInputData(inputData);
        final var result = new String(cmd.computeInputData().tape());

        // test remainder
        final double actual = new BigInteger(result, 2).doubleValue();
        final double expected = 1350899999L % 20;
        Assertions.assertEquals(expected, actual);
    }

    private void moduloAndVerify(int dividend, int divisor) throws IOException {
        final var cmd = new StartCommand();
        cmd.converseDecimalToBinary = true;
        cmd.setTransitionFunction("src/test/resources/modulo-test.txt");
        var inputData = dividend + "#" + divisor;
        cmd.setInputData(inputData);
        final var result = new String(cmd.computeInputData().tape());

        // test remainder
        final long actual = Long.parseLong(result, 2);
        final long expected = dividend % divisor;
        Assertions.assertEquals(expected, actual);
    }

}
