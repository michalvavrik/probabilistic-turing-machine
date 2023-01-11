package edu.michalvavrik.ptm.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface TuringMachine {

    /**
     * Special symbol used to swipe alphabet. We consider untouched tape parts to be blank symbols.
     */
    char BLANK = '#';

    record Configuration(char[] tape, char state) {

        public Configuration(char[] tape, char state) {
            this.tape = stripBlankSymbols(tape);
            this.state = state;
        }

        /**
         * Strips blank symbols from left and right and only leaves actual computation result.
         */
        static char[] stripBlankSymbols(char[] tape) {
            // go from left until we detect content symbols and remember where content starts
            boolean stripFromLeft = false;
            int contentStart = 0;
            for (int i = 0; i < tape.length; i++) {
                if (tape[i] == BLANK) {
                    if (!stripFromLeft) {
                        stripFromLeft = true;
                    }
                    contentStart = i + 1;
                } else {
                    break;
                }
            }

            // go from right until we detect content symbols and remember where content ends
            boolean stripFromRight = false;
            int contentEnd = tape.length;
            for (int i = contentEnd - 1; (i >= 0) && (i >= contentStart); i--) {
                if (tape[i] == BLANK) {
                    if (!stripFromRight) {
                        stripFromRight = true;
                    }
                    contentEnd = i;
                } else {
                    break;
                }
            }

            if (stripFromLeft || stripFromRight) {
                return Arrays.copyOfRange(tape, contentStart, contentEnd);
            } else {
                return tape;
            }
        }

    }

    /**
     * @param inputData sequence of input alphabet symbols
     * @return ordered configurations; last configuration is simply the state of tape when Turing machine reached one of
     * the final states (plus the final state)
     */
    Configuration[] compute(char[] inputData);

    final class TuringMachineBuilder {

        private final Set<Character> inputAlphabet = new HashSet<>();
        private final Set<Character> finalStates = new HashSet<>();
        private final Set<Character> specialSymbols = new HashSet<>();
        private final Set<Character> states = new HashSet<>();
        private TransitionFunction transitionFunction = null;
        private char initialState;

        public TuringMachine build() {
            return new TuringMachineImpl(transitionFunction, inputAlphabet, initialState, finalStates, specialSymbols,
                    states);
        }
    }
}
