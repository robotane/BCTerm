package fr.univreunion.bcterm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import fr.univreunion.bcterm.analysis.aliasing.AliasingAnalysisRunner;
import fr.univreunion.bcterm.analysis.sharing.SharingAnalysisRunner;
import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.jvm.instruction.CallInstruction;
import fr.univreunion.bcterm.jvm.instruction.GetFieldInstruction;
import fr.univreunion.bcterm.jvm.instruction.LoadInstruction;
import fr.univreunion.bcterm.jvm.instruction.NewInstruction;
import fr.univreunion.bcterm.jvm.instruction.PutFieldInstruction;
import fr.univreunion.bcterm.jvm.instruction.StoreInstruction;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.Value;
import fr.univreunion.bcterm.program.BasicBlock;
import fr.univreunion.bcterm.program.CFG;
import fr.univreunion.bcterm.program.Program;
import fr.univreunion.bcterm.util.Constants;
import fr.univreunion.bcterm.util.MemoryGraphGenerator;

public class AliasingSharingCallExample {

    public static Program createAliasingCallProgram() {
        Program program = new Program("AliasingSharingCall");

        // Add the "process" method that takes two parameters
        CFG processCfg = createProcessCFG(program);
        program.addMethod("process", "(Object,Object):void", processCfg);

        // Add the "main" method that creates aliased parameters
        CFG mainCfg = createMainCFG(program);
        program.addMethod("main", "(String[]):void", mainCfg);

        program.setMainMethodName("main");
        return program;
    }

    private static CFG createProcessCFG(Program program) {
        CFG cfg = new CFG();

        List<BytecodeInstruction> block1Instructions = new ArrayList<>();
        // load 0
        block1Instructions.add(new LoadInstruction(0));
        // load 1
        block1Instructions.add(new LoadInstruction(1));
        // load 2
        block1Instructions.add(new LoadInstruction(2));
        // getfield next
        block1Instructions.add(new GetFieldInstruction("next"));
        // store 3
        block1Instructions.add(new StoreInstruction(3));

        BasicBlock bb1 = new BasicBlock(1, block1Instructions);
        cfg.addBlock(bb1);

        return cfg;
    }

    private static CFG createMainCFG(Program program) {
        CFG cfg = new CFG();

        List<BytecodeInstruction> block1Instructions = new ArrayList<>();
        // new Object
        block1Instructions.add(new NewInstruction("Object"));
        // store 1
        block1Instructions.add(new StoreInstruction(1));
        // new Object
        block1Instructions.add(new NewInstruction("Object"));
        // store 2
        block1Instructions.add(new StoreInstruction(2));

        // load 2
        block1Instructions.add(new LoadInstruction(2));
        // load 1
        block1Instructions.add(new LoadInstruction(1));
        // putfield next
        block1Instructions.add(new PutFieldInstruction("next"));
        BasicBlock bb1 = new BasicBlock(1, block1Instructions);

        List<BytecodeInstruction> block2Instructions = new ArrayList<>();
        // new Object
        block2Instructions.add(new NewInstruction("Object"));

        // load 1
        block2Instructions.add(new LoadInstruction(1));
        // load 2
        block2Instructions.add(new LoadInstruction(2));
        // getfield next
        block2Instructions.add(new GetFieldInstruction("next"));

        // call Object.process(Object,Object):void
        CallInstruction callProcess = new CallInstruction("Object.process(Object,Object):void");
        callProcess.setProgram(program);
        block2Instructions.add(callProcess);
        BasicBlock bb2 = new BasicBlock(2, block2Instructions);

        cfg.addBlock(bb1);
        cfg.addBlock(bb2);
        cfg.addEdge(bb1, bb2);

        return cfg;
    }

    public static void main(String[] args) {
        Program program = createAliasingCallProgram();
        System.out.println(program);

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
                System.err.println("Error cleaning program directory: " + e.getMessage());
            }
        }

        JVMState initialState = new JVMState();
        initialState.setLocalVariable(0, Value.NULL);

        System.out.println("\n========================================");
        System.out.println("EXECUTING PROGRAM WITH ALIASING ANALYSIS");
        System.out.println("========================================\n");

        AliasingAnalysisRunner aliasingAnalysisRunner = new AliasingAnalysisRunner();
        Set<JVMState> aliasingAnalysisResults = program.execute(initialState, aliasingAnalysisRunner);

        System.out.println("\nFinal state after aliasing analysis:");
        for (JVMState state : aliasingAnalysisResults) {
            System.out.println("Local variables: " + state.getLocalVariablesSize());
            System.out.println("Stack size: " + state.getStackSize());
            System.out.println(state.toDetailedString());
        }

        // Reset initial state for sharing analysis
        initialState = new JVMState();
        initialState.setLocalVariable(0, Value.NULL);

        System.out.println("\n========================================");
        System.out.println("EXECUTING PROGRAM WITH SHARING ANALYSIS");
        System.out.println("========================================\n");

        SharingAnalysisRunner sharingAnalysisRunner = new SharingAnalysisRunner();
        Set<JVMState> sharingAnalysisResults = program.execute(initialState, sharingAnalysisRunner);

        System.out.println("\nFinal state after sharing analysis:");
        for (JVMState state : sharingAnalysisResults) {
            System.out.println("Local variables: " + state.getLocalVariablesSize());
            System.out.println("Stack size: " + state.getStackSize());
            System.out.println(state.toDetailedString());
        }

        if (Constants.ENABLE_FILE_GENERATION) {
            program.getMethod("process").saveToFile();
            program.getMethod("main").saveToFile();
            program.saveToFile("aliasingPairs");

            MemoryGraphGenerator.generateImagesFromDotFiles(programDir);
        }
    }
}