package edu.michalvavrik.ptm;

import edu.michalvavrik.ptm.core.TransitionFunction;
import edu.michalvavrik.ptm.core.TransitionFunction.Move;
import edu.michalvavrik.ptm.core.TuringMachine;
import edu.michalvavrik.ptm.core.TuringMachine.TuringMachineBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.michalvavrik.ptm.core.TuringMachine.BLANK;

@Command(name = "turing-machine", mixinStandardHelpOptions = true)
public class StartCommand implements Runnable {

    String inputData;
    final TuringMachineBuilder turingMachineBuilder = new TuringMachineBuilder();

    @CommandLine.Option(names = {"--input-file-path", "-in"})
    public void setInputData(String path) throws IOException {
        inputData = Files.readString(Path.of(path));
    }

    @CommandLine.Option(names = {"--transition-function-file-path", "-tf"}, description = """
            Path to the file with transition function.
            
            Transition function must be defined as `δ : (Q \\ F ) × Γ → Q × Γ × {L, R, N }`, that is a projection
            from current state and symbol to next symbol, next state and a direction in which head should be moved
            (that determines which symbol is going to be read next).
            
            Q - character set of all states; Q element must be a single character
            F - set of all finite states or accepting states; F element must be a single character
            Γ - a symbol from the tape alphabet; Γ element must be a single character
            {L, R, N } - is direction (left, right, neutral [AKA do not move]}

            There must be maximum number of one transition rule per line, white spaces and lines that does not start 
            with a character 'δ' are ignored. See an example below:
            
            # Rewrite 'aaaaa' to 'bbbbb;'
            δ : A × a → A × b × R
            δ : A × # → B × ; × N
            
            # Rewrite 'bbbbb;' to 'ccccc;'
            δ : B × ; → B × ; × L
            δ : B × b → B × c × L
            δ : B × # → C × c × N
            
            Please bear in mind that '×' and '→' must not be part of the tape alphabet.
            The symbol '#' is used to mark the blank symbol.
            
            Be default Q, F, Γ elements state are auto-detected from transition function rules, that is final states are
            are states only found on the right side of the '→' and so on. The initial state can't be auto-detected, so
            that you need to define it like this:
            
            initial-state A
            
            The tape alphabet consist of input alphabet and special symbols, you can explicitly declare special symbols
            like that (just add the line anywhere):

            special-symbols , - ; @ .
            
            Parsed special symbols will be ',' ,'-', ';', '@' and '.'.
            """)
    public void setTransitionFunction(String path) throws IOException {
        final List<String> transitionRules = Files.readAllLines(Path.of(path));
        parseTransitionRules(transitionRules);
    }

    private void parseTransitionRules(List<String> transitionRules) {
        var fun = new TransitionFunction() {

            record From(char fromState, char fromSymbol) { }

            private final Map<From, TransitionFunction.Action> transitionMap = parseMap(transitionRules);

            private static Map<From, Action> parseMap(List<String> transitionRules) {
                record Parser(From from, TransitionFunction.Action action) {
                    private static final String SPLIT_BY = "×";

                    Parser(String[] arr) {
                        this(arr[0].split(Parser.SPLIT_BY), arr[1].split(Parser.SPLIT_BY));
                    }

                    Parser(String[] leftSideSplit, String[] rightSideSplit) {
                        this(new From(checkChar(leftSideSplit, 0), checkChar(leftSideSplit, 1)),
                                new TransitionFunction.Action(checkChar(rightSideSplit, 0), checkChar(rightSideSplit, 1),
                                        parseMove(rightSideSplit[2])));
                    }

                    private static Move parseMove(String move) {
                        return switch (move.trim()) {
                            case "N" -> Move.NEUTRAL;
                            case "L" -> Move.LEFT;
                            case "R" -> Move.RIGHT;
                            default -> throw new RuntimeException(
                                    String.format("'%s' is not a valid move, accepted values are L, R, N", move));
                        };
                    }

                    private static char checkChar(String[] split, int x) {
                        var character = split[x].trim();
                        if (character.length() != 1) {
                            throw new IllegalStateException(split[x] + " is not valid character");
                        }
                        return character.charAt(0);
                    }
                }

                return transitionRules
                        .stream()
                        .filter(line -> line != null && line.startsWith("δ"))
                        // δ : B × b → B × c × L    =>    B × b → B × c × L
                        .map(line -> line.substring(line.indexOf(":") + 1).trim())
                        // B × b → B × c × L    =>    new String[] {"B × b", "B × c × L");
                        .map(line -> line.split("→"))
                        .peek(arr -> {
                            // validate
                            if (arr.length != 2 || arr[0].split(Parser.SPLIT_BY).length != 2 ||
                                    arr[1].split(Parser.SPLIT_BY).length != 3) {
                                throw new RuntimeException("Detected incorrect transition function format: " +
                                        String.join("→", arr));
                            }
                        })
                        .map(Parser::new)
                        .collect(Collectors.toUnmodifiableMap(Parser::from, Parser::action));
            }

            @Override
            public Action project(char state, char symbol) {
                return transitionMap.get(new From(state, symbol));
            }
        };
        turingMachineBuilder.transitionFunction(fun);

        // extract initial state
        transitionRules
                .stream()
                .filter(line -> line != null && line.startsWith("initial-state "))
                .map(line -> line.replace("initial-state ", "").trim())
                .peek(state -> {
                    if (state.length() != 1) {
                        throw new RuntimeException(String.format("Initial state must be exactly one character, got '%s'",
                                state));
                    }
                })
                .map(state -> state.charAt(0))
                .forEach(turingMachineBuilder::initialState);

        // extract final states
        fun.transitionMap.values()
                .stream()
                .map(TransitionFunction.Action::state)
                .filter(state -> fun.transitionMap.keySet().stream().noneMatch(from -> from.fromState() == state))
                .forEach(turingMachineBuilder::addFinalState);

        // extract all states
        fun.transitionMap.entrySet().stream().<Character>mapMulti((entry, consumer) -> {
            consumer.accept(entry.getKey().fromState());
            consumer.accept(entry.getValue().state());
        }).forEach(turingMachineBuilder::addState);

        // extract special symbols
        final var specialSymbols = transitionRules
                .stream()
                .filter(line -> line != null && line.startsWith("special-symbols "))
                .map(line -> line.replace("special-symbols ", "").trim())
                .map(line -> line.split(" "))
                .flatMap(Stream::of)
                .map(String::trim)
                .filter(symbol -> !symbol.isEmpty())
                .peek(symbol -> {
                    if (symbol.length() != 1) {
                        throw new RuntimeException(
                                String.format("Special symbol must be exactly one character, but was '%s'", symbol));
                    }
                })
                .map(symbol -> symbol.charAt(0))
                .collect(Collectors.toUnmodifiableSet());
        final boolean hasSpecialSymbols;
        if (!specialSymbols.isEmpty()) {
            hasSpecialSymbols = true;
            turingMachineBuilder.specialSymbols(specialSymbols);
        } else {
            hasSpecialSymbols = false;
        }

        // extract input alphabet
        fun.transitionMap.entrySet().stream().<Character>mapMulti((entry, consumer) -> {
            if (hasSpecialSymbols) {
                if (!specialSymbols.contains(entry.getKey().fromSymbol())) {
                    consumer.accept(entry.getKey().fromSymbol());
                }
                if (!specialSymbols.contains(entry.getValue().symbol())) {
                    consumer.accept(entry.getValue().symbol());
                }
            } else {
                consumer.accept(entry.getKey().fromSymbol());
                consumer.accept(entry.getValue().symbol());
            }
        })
                .filter(symbol -> symbol != BLANK)
                .forEach(turingMachineBuilder::addInputAlphabetSymbol);
    }

    @Override
    public void run() {
        Objects.requireNonNull(inputData);
        final var turingMachine = turingMachineBuilder.build();
        final var result = turingMachine.compute(inputData.trim().toCharArray());
    }
}
