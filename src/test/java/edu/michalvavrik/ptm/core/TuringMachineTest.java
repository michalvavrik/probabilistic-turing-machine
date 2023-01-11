package edu.michalvavrik.ptm.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static edu.michalvavrik.ptm.core.TuringMachine.BLANK;
import static edu.michalvavrik.ptm.core.TuringMachine.Configuration.stripBlankSymbols;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TuringMachineTest {

    @Test
    void testStripBlankSymbols() {
        char[] oldTape = new char[] {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};
        char[] expected = Arrays.copyOf(oldTape, oldTape.length);
        assertArrayEquals(oldTape, stripBlankSymbols(oldTape));

        oldTape = new char[] {BLANK, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};
        assertArrayEquals(expected, stripBlankSymbols(oldTape));

        oldTape = new char[] {BLANK, BLANK, BLANK, BLANK, BLANK, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};
        assertArrayEquals(expected, stripBlankSymbols(oldTape));

        oldTape = new char[] {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', BLANK};
        assertArrayEquals(expected, stripBlankSymbols(oldTape));

        oldTape = new char[] {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', BLANK, BLANK, BLANK, BLANK, BLANK};
        assertArrayEquals(expected, stripBlankSymbols(oldTape));

        oldTape = new char[] {BLANK, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', BLANK};
        assertArrayEquals(expected, stripBlankSymbols(oldTape));

        oldTape = new char[] {BLANK, BLANK, BLANK, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', BLANK, BLANK, BLANK};
        assertArrayEquals(expected, stripBlankSymbols(oldTape));

        oldTape = new char[] {BLANK, BLANK, BLANK, 'x', BLANK, BLANK, BLANK};
        expected = new char[] {'x'};
        assertArrayEquals(expected, stripBlankSymbols(oldTape));

        oldTape = new char[] {BLANK, BLANK, BLANK, 'x', 'x', BLANK, BLANK, BLANK};
        expected = new char[] {'x', 'x'};
        assertArrayEquals(expected, stripBlankSymbols(oldTape));

        oldTape = new char[] {BLANK, BLANK, BLANK, BLANK, BLANK, BLANK, BLANK, BLANK};
        expected = new char[] {};
        assertArrayEquals(expected, stripBlankSymbols(oldTape));

        oldTape = new char[] {BLANK};
        assertArrayEquals(expected, stripBlankSymbols(oldTape));

        oldTape = new char[] {};
        assertArrayEquals(expected, stripBlankSymbols(oldTape));
    }

}
