package fr.univreunion.bcterm;

import fr.univreunion.bcterm.analysis.aliasing.AliasingAnalysisEngine;
import fr.univreunion.bcterm.analysis.cyclicity.CyclicityAnalysisEngine;
import fr.univreunion.bcterm.analysis.sharing.SharingAnalysisEngine;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.Value;
import fr.univreunion.bcterm.program.Program;
import fr.univreunion.bcterm.util.Constants;
import fr.univreunion.bcterm.util.Logger;
import fr.univreunion.bcterm.util.MemoryGraphGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Set;

public class ExampleRunner {
    private static final java.util.logging.Logger logger = Logger.getLogger(ExampleRunner.class);

    public static void run(Program program) {
        logger.info(() -> program.toString());

        String programName = program.getName();
        String programDir = Constants.GENERATED_DIR + File.separator + programName;

        if (Constants.ENABLE_FILE_GENERATION) {
            try {
                Path programPath = Paths.get(programDir);
                if (Files.exists(programPath)) {
                    Files.walk(programPath)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
                Files.createDirectories(programPath);
            } catch (IOException e) {
                logger.severe(() -> "Error cleaning program directory: " + e.getMessage());
            }
        }

        runAliasingAnalysis(program);
        runSharingAnalysis(program);
        runCyclicityAnalysis(program);

        if (Constants.ENABLE_FILE_GENERATION) {
            program.getMethods().values().forEach(method -> method.saveToFile());
            program.saveToFile();
            MemoryGraphGenerator.generateImagesFromDotFiles(programDir);
        }
    }

    private static void runAliasingAnalysis(Program program) {
        JVMState initialState = new JVMState();
        initialState.setLocalVariable(0, Value.NULL);

        logger.info(() -> "========================================");
        logger.info(() -> "EXECUTING PROGRAM WITH ALIASING ANALYSIS");
        logger.info(() -> "========================================");

        AliasingAnalysisEngine aliasingAnalysisEngine = new AliasingAnalysisEngine();
        Set<JVMState> aliasingAnalysisResults = program.execute(initialState, aliasingAnalysisEngine);

        logger.info(() -> "Final state after aliasing analysis:");
        for (JVMState state : aliasingAnalysisResults) {
            logger.info(() -> "Local variables: " + state.getLocalVariablesSize());
            logger.info(() -> "Stack size: " + state.getStackSize());
            logger.info(() -> state.toDetailedString());
        }
    }

    private static void runSharingAnalysis(Program program) {
        JVMState initialState = new JVMState();
        initialState.setLocalVariable(0, Value.NULL);

        logger.info(() -> "========================================");
        logger.info(() -> "EXECUTING PROGRAM WITH SHARING ANALYSIS");
        logger.info(() -> "========================================");

        SharingAnalysisEngine sharingAnalysisEngine = new SharingAnalysisEngine();
        Set<JVMState> sharingAnalysisResults = program.execute(initialState, sharingAnalysisEngine);

        logger.info(() -> "Final state after sharing analysis:");
        for (JVMState state : sharingAnalysisResults) {
            logger.info(() -> "Local variables: " + state.getLocalVariablesSize());
            logger.info(() -> "Stack size: " + state.getStackSize());
            logger.info(() -> state.toDetailedString());
        }
    }

    private static void runCyclicityAnalysis(Program program) {
        JVMState initialState = new JVMState();
        initialState.setLocalVariable(0, Value.NULL);

        logger.info(() -> "========================================");
        logger.info(() -> "EXECUTING PROGRAM WITH CYCLICITY ANALYSIS");
        logger.info(() -> "========================================");

        CyclicityAnalysisEngine cyclicityAnalysisEngine = new CyclicityAnalysisEngine();
        Set<JVMState> cyclicityAnalysisResults = program.execute(initialState, cyclicityAnalysisEngine);

        logger.info(() -> "Final state after cyclicity analysis:");
        for (JVMState state : cyclicityAnalysisResults) {
            logger.info(() -> "Local variables: " + state.getLocalVariablesSize());
            logger.info(() -> "Stack size: " + state.getStackSize());
            logger.info(() -> state.toDetailedString());
        }
    }
}
