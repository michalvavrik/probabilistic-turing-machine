package edu.michalvavrik.ptm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static edu.michalvavrik.ptm.BinaryNumberCopyTest.splitBySeparator;
import static edu.michalvavrik.ptm.core.TuringMachine.BLANK;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BinaryNumberComparatorTest {

    private static final char EQUAL_STATE = 'Ś';
    private static final char NOT_EQUAL_STATE = 'Š';

    @Test
    void compareNumbers() throws IOException {

        final var cmd = new StartCommand();
        cmd.setTransitionFunction("src/test/resources/binary-number-comparator-test.txt");
        cmd.converseDecimalToBinary = true;

        compareAndVerify(0, 1, cmd);
        compareAndVerify(10, 10, cmd);
        compareAndVerify(523, 523, cmd);
        compareAndVerify(341, 522, cmd);
        compareAndVerify(600, 291, cmd);
        compareAndVerify(2, 99999, cmd);
        compareAndVerify(2, 90000, cmd);

        cmd.converseDecimalToBinary = false;
        cmd.setInputData(Integer.toBinaryString(2) + BLANK + Integer.toBinaryString(4));
        final var configuration = cmd.computeInputData();
        assertEquals(NOT_EQUAL_STATE, configuration.state());
        final var tape = splitBySeparator(configuration.tape(), BLANK);
        final int leftActual = Integer.parseInt(tape[0], 2);
        final int rightActual = Integer.parseInt(tape[1], 2);
        assertEquals(2, leftActual);
        assertEquals(4, rightActual);

        // assure when both numbers have multiple leading zeros
        cmd.setInputData("00000" + Integer.toBinaryString(1) + BLANK + "00000" + Integer.toBinaryString(0));
        assertEquals(NOT_EQUAL_STATE, cmd.computeInputData().state());
        cmd.setInputData("00000" + Integer.toBinaryString(1) + BLANK + "00000" + Integer.toBinaryString(1));
        assertEquals(EQUAL_STATE, cmd.computeInputData().state());
        cmd.setInputData("00000" + Integer.toBinaryString(20) + BLANK + "00000" + Integer.toBinaryString(9));
        assertEquals(NOT_EQUAL_STATE, cmd.computeInputData().state());

        // this rather shows that leading zeros confuse comparator
        // it's incorrect per se, but we don't really care
        // so here we just establish contract

        // assure when the left number has leading zeros, numbers are never equal
        cmd.setInputData("00000" + Integer.toBinaryString(20) + BLANK + Integer.toBinaryString(9));
        assertEquals(NOT_EQUAL_STATE, cmd.computeInputData().state());
        cmd.setInputData("00000" + Integer.toBinaryString(10) + BLANK + Integer.toBinaryString(10));
        assertEquals(NOT_EQUAL_STATE, cmd.computeInputData().state());

        // assure when the right number has leading zeros, numbers are never equal
        cmd.setInputData("00000" + Integer.toBinaryString(20) + BLANK + Integer.toBinaryString(9));
        assertEquals(NOT_EQUAL_STATE, cmd.computeInputData().state());
        cmd.setInputData("00000" + Integer.toBinaryString(10) + BLANK + Integer.toBinaryString(10));
        assertEquals(NOT_EQUAL_STATE, cmd.computeInputData().state());
    }

    private void compareAndVerify(int leftExpected, int rightExpected, StartCommand cmd) {
        cmd.setInputData(Integer.toString(leftExpected) + BLANK + rightExpected);
        final var result = cmd.computeInputData();
        assertEquals(leftExpected == rightExpected ? EQUAL_STATE : NOT_EQUAL_STATE, result.state());
        final var tape = splitBySeparator(result.tape(), BLANK);
        final int leftActual = Integer.parseInt(tape[0], 2);
        Assertions.assertEquals(leftExpected, leftActual);
        final int rightActual = Integer.parseInt(tape[1], 2);
        Assertions.assertEquals(rightExpected, rightActual);
    }

}
