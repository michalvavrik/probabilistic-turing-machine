package edu.michalvavrik.ptm.core;

import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


final class TuringMachineImpl implements TuringMachine {

    private static final Logger LOG = Logger.getLogger(TuringMachineImpl.class);

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
        Set<Character> inputAlphabetList = new HashSet<>(inputAlphabet);
        inputAlphabetList.add(BLANK);
        this.inputAlphabet = Set.copyOf(inputAlphabetList);
        Set<Character> tapeAlphabetList = new HashSet<>(inputAlphabetList);
        tapeAlphabetList.addAll(specialSymbols);
        tapeAlphabetList.add(ANY);
        this.tapeAlphabet = Set.copyOf(tapeAlphabetList);
    }

    @Override
    public Configuration compute(char[] inputData) {
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
        int configurationIndex = 0;
        int writeOperationNum = 0;
        var configuration = new Configuration(tape, currentState, configurationIndex, writeOperationNum);
        printOutConfiguration(configuration, configurationIndex);

        // process input
        while (!finalStates.contains(currentState)) {

            // take this action, e.g. move the tape head, rewrite current symbol, change state
            final var action = transitionFunction.project(currentState, tape[tapeHead]);

            // validate resulting action
            if (action == null) {
                throw new IllegalStateException(String.format("Transition function returned empty action for state '%s' " +
                        "and symbol '%s'", currentState, tape[tapeHead]));
            }
            if (action.move() == null) {
                throw new IllegalStateException("'Move' must not be null");
            }
            if (!states.contains(action.state())) {
                throw new IllegalStateException(String.format("State '%s' is not a valid state", action.state()));
            }
            if (!tapeAlphabet.contains(action.symbol())) {
                throw new IllegalStateException(String.format("Symbol '%s' is not a valid tape alphabet symbol",
                        action.symbol()));
            }

            currentState = action.state();
            // write the symbol to the tape
            if (tape[tapeHead] != action.symbol()) {
                tape[tapeHead] = action.symbol();
                writeOperationNum++;
            }

            // move the tape head
            switch (action.move()) {
                case LEFT -> --tapeHead;
                case RIGHT -> ++tapeHead;
            }

            // ensure we can write to the tape next time
            if (tape.length == tapeHead) {
                tape = growTapeOnRight(tape);
            } else if (tapeHead < 0) {
                final int preLength = tape.length;
                tape = growTapeOnLeft(tape);
                final int numberOfAddedElements = tape.length - preLength;
                // keep head position on the exactly same symbol
                tapeHead += numberOfAddedElements;
            }

            // record configuration
            configuration = new Configuration(Arrays.copyOf(tape, tape.length), currentState,
                    configurationIndex, writeOperationNum);
            configurationIndex++;
            printOutConfiguration(configuration, configurationIndex);
        }

        return configuration;
    }

    private static void printOutConfiguration(Configuration configuration, int configurationIndex) {
        LOG.debugf("Configuration #%d: state '%s', tape: %s", (Object) configurationIndex, configuration.state(),
                new String(configuration.tape()));
    }

    static char[] growTapeOnRight(char[] oldTape) {
        final char[] newTape;
        if (oldTape.length == 1) {
            // special handling for single item array as load factor * 1 will result in 1 (int casting == round down)
            newTape = Arrays.copyOf(oldTape, 5);
        } else {
            // reallocate new space on the right so that we don't have to create new array with every new item
            newTape = Arrays.copyOf(oldTape, nextTapeLength(oldTape.length));
        }
        // empty tape consist of blank symbols
        Arrays.fill(newTape, oldTape.length, newTape.length, BLANK);
        return newTape;
    }

    static char[] growTapeOnLeft(char[] oldTape) {
        // add blank symbols to the left
        final char[] newTape = new char[nextTapeLength(oldTape.length)];
        System.arraycopy(oldTape, 0, newTape, newTape.length - oldTape.length, oldTape.length);
        Arrays.fill(newTape, 0, newTape.length - oldTape.length, BLANK);
        return newTape;
    }

    private static int nextTapeLength(int oldTapeLength) {
        // some Java collections use 1.75f load factor, but that leads faster to Java heap space error
        // therefore use array and enlarge it slightly (25 items)
        // practically it means slower execution but better memory efficiency
        return oldTapeLength + 25;
    }

}
