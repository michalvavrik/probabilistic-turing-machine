package edu.michalvavrik.ptm.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface TuringMachine {

    /**
     * Special symbol used to swipe symbols. We consider untouched tape parts to be blank symbols.
     */
    char BLANK = '#';

    /**
     * Special symbol used to match any symbol.
     */
    char ANY = '−';

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

        public void addState(char state) {
            states.add(state);
        }

        public void addFinalState(char state) {
            finalStates.add(state);
        }

        public void transitionFunction(TransitionFunction transitionFunction) {
            this.transitionFunction = transitionFunction;
        }

        public void initialState(char initialState) {
            this.initialState = initialState;
        }

        public void addInputAlphabetSymbol(char symbol) {
            inputAlphabet.add(symbol);
        }

        public void specialSymbols(Set<Character> specialSymbols) {
            this.specialSymbols.addAll(specialSymbols);
        }

        public TuringMachine build() {
            return new TuringMachineImpl(transitionFunction, inputAlphabet, initialState, finalStates, specialSymbols,
                    states);
        }

        public Set<Character> getInputAlphabet() {
            return Set.copyOf(inputAlphabet);
        }

        public Set<Character> getFinalStates() {
            return Set.copyOf(finalStates);
        }

        public Set<Character> getSpecialSymbols() {
            return Set.copyOf(specialSymbols);
        }

        public Set<Character> getStates() {
            return Set.copyOf(states);
        }

        public char getInitialState() {
            return initialState;
        }

        public TransitionFunction getTransitionFunction() {
            return transitionFunction;
        }
    }
}
