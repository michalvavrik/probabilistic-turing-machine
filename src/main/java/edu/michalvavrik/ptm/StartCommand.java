package edu.michalvavrik.ptm;

import edu.michalvavrik.ptm.core.TransitionFunction;
import edu.michalvavrik.ptm.core.TuringMachine;
import edu.michalvavrik.ptm.core.TuringMachine.TuringMachineBuilder;
import org.jboss.logging.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.michalvavrik.ptm.core.TuringMachine.ANY;
import static edu.michalvavrik.ptm.core.TuringMachine.BLANK;
import static java.lang.String.format;

@Command(name = "turing-machine", mixinStandardHelpOptions = true)
public class StartCommand implements Runnable {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d");
    private static final Logger LOG = Logger.getLogger(StartCommand.class);
    String inputData;
    final TuringMachineBuilder turingMachineBuilder = new TuringMachineBuilder();

    @CommandLine.Option(names = {"--binary-mode"}, description = "Detect all decimal numbers and converse them to binary",
            defaultValue = "yes")
    boolean converseDecimalToBinary;

    @CommandLine.Option(names = {"--additional-input-alphabet-symbols"}, description = """
            Comma separated list of additional input alphabet symbols.
            
            Input alphabet symbols are auto-detected as symbols used by transition function not explicitly marked as
            special symbols. This option allows you to define additional symbols that can't be detected from the rules.
            That is useful mainly when you have the rule to match ANY character.
            """)
    public void setAdditionalInputAlphabetSymbols(String symbols) throws IOException {
        for (String symbol : symbols.split(",")) {
            symbol = symbol.trim();
            if (symbol.length() != 1) {
                throw new IllegalArgumentException("Input alphabet symbol must be exactly one character");
            }
            final char symbolChar = symbol.charAt(0);
            if (symbolChar == ANY) {
                throw new IllegalArgumentException(String.format("Input alphabet must not contain '%s' symbol", ANY));
            }
            if (symbolChar == BLANK) {
                throw new IllegalArgumentException(String.format("Input alphabet must not contain '%s' symbol", BLANK));
            }
            turingMachineBuilder.addInputAlphabetSymbol(symbolChar);
        }
    }

    @CommandLine.Option(names = {"--input-file-path", "-ip"})
    public void setInputDataPath(String path) throws IOException {
        inputData = Files.readString(Path.of(path));
    }

    @CommandLine.Option(names = {"--input-data", "-in"})
    public void setInputData(String inputData) {
        this.inputData = inputData;
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
            
            Please bear in mind that '×' (multiplication sign) and '→' (arrow U+2192) must not be part of the tape 
            alphabet. The symbol '#' is used to mark the blank symbol.
            
            Be default Q, F, Γ elements state are auto-detected from transition function rules, that is final states are
            are states only found on the right side of the '→' and so on. The initial state can't be auto-detected, so
            that you need to define it like this:
            
            initial-state A
            
            The tape alphabet consist of input alphabet and special symbols, you can explicitly declare special symbols
            like that (just add the line anywhere):

            special-symbols , - ; @ .
            
            Parsed special symbols will be ',' ,'-', ';', '@' and '.'.
            
            Divide and conquer - many very complicated tasks can be reduced to subroutines of a Turing machine.
            The subroutine is a small set of states in the Turing machine that performs a small computation. You can
            like this:
            
            import-subroutine src/main/resources/plus-subroutine.txt
            
            Imported subroutines are parsed identically as are lines in the main (this) file. It's simply an option to
            re-use transition rules at multiple places.
            
            Sometimes you need to match any character, in such case you can use special symbol '−'. For example when you
            want to reach the right-most non-blank symbols, you can do:

            δ : A × − → A × − × R
            # more concrete match has priority
            δ : A × # → B × # × L
            
            """)
    public void setTransitionFunction(String path) throws IOException {
        final List<String> transitionRules = Files.readAllLines(Path.of(path));
        List<List<String>> subroutines = importSubroutines(transitionRules);
        if (subroutines.isEmpty()) {
            parseTransitionRules(transitionRules);
        } else {
            // prepend subroutines prior to transition rules
            parseTransitionRules(
                    Stream.concat(subroutines.stream().flatMap(List::stream), transitionRules.stream()).toList()
            );
        }
    }

    private List<List<String>> importSubroutines(List<String> transitionRules) {
        return transitionRules
                .stream()
                .filter(line -> line.startsWith("import-subroutine "))
                .map(line -> line.replace("import-subroutine ", "").trim())
                .map(path -> {
                    try {
                        return Files.readAllLines(Path.of(path));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(subroutineLines -> {
                    var nestedSubroutines = importSubroutines(subroutineLines);
                    if (nestedSubroutines.isEmpty()) {
                        return subroutineLines;
                    } else {
                        // append nested subroutines to the end of the file
                        return Stream.concat(
                                subroutineLines.stream(), nestedSubroutines.stream().flatMap(List::stream)).toList();
                    }
                })
                .toList();
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
                                    format("'%s' is not a valid move, accepted values are L, R, N", move));
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
                var action = transitionMap.get(new From(state, symbol));
                if (action == null) {
                    action = transitionMap.get(new From(state, ANY));
                    if (action != null && action.symbol() == ANY) {
                        // if we are transiting to ANY, we want to keep previous symbol
                        action = new Action(action.state(), symbol, action.move());
                    }
                }
                return action;
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
                        throw new RuntimeException(format("Initial state must be exactly one character, got '%s'",
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
                                format("Special symbol must be exactly one character, but was '%s'", symbol));
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
            final char fromSymbol = entry.getKey().fromSymbol();
            final char toSymbol = entry.getValue().symbol();
            if (hasSpecialSymbols) {
                if (!specialSymbols.contains(fromSymbol)) {
                    consumer.accept(fromSymbol);
                }
                if (!specialSymbols.contains(toSymbol)) {
                    consumer.accept(toSymbol);
                }
            } else {
                consumer.accept(fromSymbol);
                consumer.accept(toSymbol);
            }
        })
                .filter(symbol -> symbol != BLANK)
                .filter(symbol -> symbol != ANY)
                .forEach(turingMachineBuilder::addInputAlphabetSymbol);
    }

    @Override
    public void run() {
        final TuringMachine.Configuration[] configurations = computeInputData();

        // print out all configuration
        LOG.info("Configurations:");
        for (int i = 0; i < configurations.length; i++) {
            var configuration = configurations[i];
            LOG.info(format("#%d. state '%s', tape: %s", i, configuration.state(), new String(configuration.tape())));
        }
        // FIXME count complexity as we know exact number of steps
    }

    TuringMachine.Configuration[] computeInputData() {
        Objects.requireNonNull(inputData);
        inputData = inputData.trim();
        final var turingMachine = turingMachineBuilder.build();
        final TuringMachine.Configuration[] configurations;

        // if string contains digits and conversion is enabled, converse them to binary numbers
        if (converseDecimalToBinary && NUMBER_PATTERN.matcher(inputData).find()) {

            configurations = turingMachine.compute(decimalToBinary(inputData.toCharArray()).toCharArray());
        } else {

            configurations = turingMachine.compute(inputData.toCharArray());
        }

        return configurations;
    }

    static String decimalToBinary(char[] charArr) {
        StringBuilder conversed = new StringBuilder();
        int digitStart = -1;
        for (int i = 0; i < charArr.length; i++) {
            if (Character.isDigit(charArr[i])) {
                if (digitStart == -1) {
                    digitStart = i;
                }
            } else if (digitStart != -1) {
                digitStart = addBinaryNumber(charArr, conversed, digitStart, i);
            } else {
                conversed.append(charArr[i]);
            }
        }

        // handle remainder
        if (digitStart != -1) {
            addBinaryNumber(charArr, conversed, digitStart, charArr.length);
        }

        return conversed.toString();
    }

    private static int addBinaryNumber(char[] charArr, StringBuilder conversed, int digitStart, int i) {
        // converse decimal to binary number
        StringBuilder digitAsStr = new StringBuilder();
        for (int j = digitStart; j < i; j++) {
            digitAsStr.append(charArr[j]);
        }
        String binaryNumber = Long.toBinaryString(Long.parseLong(digitAsStr.toString()));
        // add decimal number as binary
        conversed.append(binaryNumber);

        // reset
        digitStart = -1;

        // append current char if there is any
        // the condition is false when this is the last char of the string
        if (i < charArr.length) {
            conversed.append(charArr[i]);
        }

        return digitStart;
    }
}
