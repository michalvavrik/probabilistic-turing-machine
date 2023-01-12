package edu.michalvavrik.ptm.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class TuringMachineImpl implements TuringMachine {

    private static final float LOAD_FACTOR = 1.75f;

    private final TransitionFunction transitionFunction;

    private final Set<Character> tapeAlphabet;

    private final Set<Character> inputAlphabet;

    /**
     * Set of final states or accepting states.
     */
    private final Set<Character> finalStates;

    private final Set<Character> states;

    private final char initialState;

    TuringMachineImpl(TransitionFunction transitionFunction, Set<Character> inputAlphabet, char initialState,
                      Set<Character> finalStates, Set<Character> specialSymbols, Set<Character> states) {
        // validate Turing machine definition
        if (inputAlphabet == null || inputAlphabet.isEmpty()) {
            throw new IllegalArgumentException("Input alphabet must have at least one letter");
        }
        if (inputAlphabet.contains(BLANK)) {
            throw new IllegalArgumentException(String.format("Input alphabet must not contain blank symbol '%s'",
                    BLANK));
        }
        if (inputAlphabet.contains(ANY)) {
            throw new IllegalArgumentException(String.format("Input alphabet must not contain blank symbol '%s'",
                    ANY));
        }
        if (specialSymbols != null && !specialSymbols.isEmpty()) {
            if (specialSymbols.contains(BLANK)) {
                throw new IllegalArgumentException(String.format("Special symbols must not contain blank symbol '%s'",
                        BLANK));
            }
            if (specialSymbols.contains(ANY)) {
                throw new IllegalArgumentException(String.format("Special symbols must not contain any symbol '%s'",
                        ANY));
            }
            if (specialSymbols.stream().anyMatch(inputAlphabet::contains)) {
                throw new IllegalArgumentException("Special symbols and input alphabet must not overlay");
            }
            for (Character specialSymbol : specialSymbols) {
                if (inputAlphabet.stream().anyMatch(specialSymbol::equals)) {
                    throw new IllegalArgumentException(String.format("Special symbol '%s' must not be part of input " +
                            "alphabet", specialSymbol));
                }
            }
        } else {
            specialSymbols = Set.of();
        }
        if (states == null || states.isEmpty()) {
            throw new IllegalArgumentException("States must not be empty");
        }
        if (!states.contains(initialState)) {
            throw new IllegalArgumentException("Initial state is unknown");
        }
        if (finalStates == null || finalStates.isEmpty()) {
            throw new IllegalArgumentException("At least one final state must be specified");
        }
        for (Character finalState : finalStates) {
            if (states.stream().noneMatch(finalState::equals)) {
                throw new IllegalArgumentException(String.format("Final state '%s' us unknown", finalState));
            }
        }

        this.states = states;
        this.initialState = initialState;
        this.transitionFunction = Objects.requireNonNull(transitionFunction);
        this.finalStates = Set.copyOf(finalStates);
        Set<Character> tapeAlphabetList = new HashSet<>(inputAlphabet);
        tapeAlphabetList.addAll(specialSymbols);
        tapeAlphabetList.add(BLANK);
        this.tapeAlphabet = Set.copyOf(tapeAlphabetList);
        this.inputAlphabet = Set.copyOf(inputAlphabet);
    }

    @Override
    public Configuration[] compute(char[] inputData) {
        // validate input data
        if (inputData == null || inputData.length == 0) {
            throw new IllegalArgumentException("Input data were not provided");
        }
        for (char symbol : inputData) {
            if (!inputAlphabet.contains(symbol)) {
                throw new IllegalArgumentException(String.format("Symbol '%s' does not belong to the input alphabet",
                        symbol));
            }
        }

        // tape is used as a memory
        char[] tape = Arrays.copyOf(inputData, inputData.length);
        int tapeHead = 0;
        char currentState = initialState;
        final List<Configuration> configurations = new ArrayList<>();
        configurations.add(new Configuration(tape, currentState));

        // process input
        while (!finalStates.contains(currentState)) {
            // take this action, e.g. move the tape head, rewrite current symbol, change state
            var action = transitionFunction.project(currentState, tape[tapeHead]);

            // validate resulting action
            if (action == null) {
                throw new IllegalStateException("Transition function returned empty action");
            }
            if (action.move() == null) {
                throw new IllegalStateException("'Move' must not be null");
            }
            if (!states.contains(action.state())) {
                throw new IllegalStateException(String.format("State '%s' is not a valid state", action.state()));
            }
            if (!tapeAlphabet.contains(action.symbol())) {
                throw new IllegalStateException(String.format("Symbol '%s' is not a valid tape alphabet symbol",
                        action.state()));
            }

            currentState = action.state();
            // write the symbol to the tape
            tape[tapeHead] = action.symbol();

            // move the tape head
            switch (action.move()) {
                case LEFT -> --tapeHead;
                case RIGHT -> ++tapeHead;
            }

            // ensure we can write to the tape next time
            if (tape.length == tapeHead) {
                tape = growTapeOnRight(tape);
            } else if (tapeHead < 0) {
                tape = growTapeOnLeft(tape);
            }

            // record configuration
            configurations.add(new Configuration(tape, currentState));
        }

        return configurations.toArray(new Configuration[0]);
    }

    static char[] growTapeOnRight(char[] oldTape) {
        // reallocate new space on the right so that we don't have to create new array with every new item
        final char[] newTape = Arrays.copyOf(oldTape, (int)(oldTape.length * LOAD_FACTOR));
        // empty tape consist of blank symbols
        Arrays.fill(newTape, oldTape.length, newTape.length, BLANK);
        return newTape;
    }

    static char[] growTapeOnLeft(char[] oldTape) {
        // add blank symbols to the left
        final char[] newTape = new char[(int)(oldTape.length * LOAD_FACTOR)];
        System.arraycopy(oldTape, 0, newTape, newTape.length - oldTape.length, oldTape.length);
        Arrays.fill(newTape, 0, newTape.length - oldTape.length, BLANK);
        return newTape;
    }

}
