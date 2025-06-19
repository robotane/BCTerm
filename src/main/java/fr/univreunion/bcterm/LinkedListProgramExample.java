package fr.univreunion.bcterm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.analysis.aliasing.AliasingAnalysisRunner;
import fr.univreunion.bcterm.analysis.cyclicity.CyclicityAnalysisRunner;
import fr.univreunion.bcterm.analysis.cyclicity.CyclicityState;
import fr.univreunion.bcterm.analysis.sharing.SharingAnalysisRunner;
import fr.univreunion.bcterm.jvm.instruction.AddInstruction;
import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.jvm.instruction.CallInstruction;
import fr.univreunion.bcterm.jvm.instruction.ConstInstruction;
import fr.univreunion.bcterm.jvm.instruction.DupInstruction;
import fr.univreunion.bcterm.jvm.instruction.GetFieldInstruction;
import fr.univreunion.bcterm.jvm.instruction.IfEqOfTypeInstruction;
import fr.univreunion.bcterm.jvm.instruction.IfNeOfTypeInstruction;
import fr.univreunion.bcterm.jvm.instruction.LoadInstruction;
import fr.univreunion.bcterm.jvm.instruction.NewInstruction;
import fr.univreunion.bcterm.jvm.instruction.PutFieldInstruction;
import fr.univreunion.bcterm.jvm.instruction.StoreInstruction;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;
import fr.univreunion.bcterm.jvm.state.Value;
import fr.univreunion.bcterm.program.BasicBlock;
import fr.univreunion.bcterm.program.CFG;
import fr.univreunion.bcterm.program.Program;
import fr.univreunion.bcterm.util.Constants;
import fr.univreunion.bcterm.util.MemoryGraphGenerator;

public class LinkedListProgramExample {

    public static Program createLinkedListProgram() {
        Program program = new Program("LinkedList");

        CFG initCfg = createInitCFG(program);
        program.addMethod("<init>", "(int,LinkedList):void", initCfg);

        CFG sizeCfg = createSizeCFG(program);
        program.addMethod("size", "():int", sizeCfg);

        CFG appendCfg = createAppendCFG(program);
        program.addMethod("append", "(int):void", appendCfg);

        CFG getValueCfg = createGetValueCFG(program);
        program.addMethod("getValue", "():int", getValueCfg);

        CFG getNextCfg = createGetNextCFG(program);
        program.addMethod("getNext", "():LinkedList", getNextCfg);

        CFG mainCfg = createMainCFG(program);
        program.addMethod("main", "(String[]):void", mainCfg);

        program.setMainMethodName("main");

        return program;
    }

    private static CFG createInitCFG(Program program) {
        List<BytecodeInstruction> block1Instructions = new ArrayList<>();

        block1Instructions.add(new LoadInstruction(0));
        block1Instructions.add(new LoadInstruction(1));
        block1Instructions.add(new PutFieldInstruction("value"));
        block1Instructions.add(new LoadInstruction(0));
        block1Instructions.add(new LoadInstruction(2));
        block1Instructions.add(new PutFieldInstruction("next"));

        CFG cfg = new CFG();
        BasicBlock bb1 = new BasicBlock(1, block1Instructions);
        cfg.addBlock(bb1);

        return cfg;
    }

    private static CFG createSizeCFG(Program program) {
        List<BytecodeInstruction> block1Instructions = new ArrayList<>();
        List<BytecodeInstruction> block2Instructions = new ArrayList<>();
        List<BytecodeInstruction> block3Instructions = new ArrayList<>();
        List<BytecodeInstruction> block4Instructions = new ArrayList<>();

        Value linkedListType = new LocationValue(0, "LinkedList");

        block1Instructions.add(new LoadInstruction(0));
        block1Instructions.add(new GetFieldInstruction("next"));

        block2Instructions.add(new IfEqOfTypeInstruction(linkedListType));
        block2Instructions.add(new ConstInstruction(1));

        block3Instructions.add(new IfNeOfTypeInstruction(linkedListType));
        block3Instructions.add(new ConstInstruction(1));
        block3Instructions.add(new LoadInstruction(0));
        block3Instructions.add(new GetFieldInstruction("next"));

        CallInstruction callSize = new CallInstruction("LinkedList.size():int");
        callSize.setProgram(program);
        block4Instructions.add(callSize);
        block4Instructions.add(new AddInstruction());

        CFG cfg = new CFG();
        BasicBlock bb1 = new BasicBlock(1, block1Instructions);
        BasicBlock bb2 = new BasicBlock(2, block2Instructions);
        BasicBlock bb3 = new BasicBlock(3, block3Instructions);
        BasicBlock bb4 = new BasicBlock(4, block4Instructions);

        cfg.addBlock(bb1);
        cfg.addBlock(bb2);
        cfg.addBlock(bb3);
        cfg.addBlock(bb4);

        cfg.addEdge(bb1, bb2);
        cfg.addEdge(bb1, bb3);
        cfg.addEdge(bb3, bb4);

        return cfg;
    }

    private static CFG createAppendCFG(Program program) {
        List<BytecodeInstruction> block1Instructions = new ArrayList<>();
        List<BytecodeInstruction> block2Instructions = new ArrayList<>();
        List<BytecodeInstruction> block3Instructions = new ArrayList<>();
        List<BytecodeInstruction> block4Instructions = new ArrayList<>();
        List<BytecodeInstruction> block5Instructions = new ArrayList<>();

        Value linkedListType = new LocationValue(0, "LinkedList");

        // Block 1: load 0, store 2
        block1Instructions.add(new LoadInstruction(0));
        block1Instructions.add(new StoreInstruction(2));

        // Block 2: load 2, getfield next
        block2Instructions.add(new LoadInstruction(2));
        block2Instructions.add(new GetFieldInstruction("next"));

        // Block 3: ifne of type LinkedList, load 2, getfield next, store 2
        block3Instructions.add(new IfNeOfTypeInstruction(linkedListType));
        block3Instructions.add(new LoadInstruction(2));
        block3Instructions.add(new GetFieldInstruction("next"));
        block3Instructions.add(new StoreInstruction(2));

        // Block 4: ifeq of type LinkedList, load 2, new LinkedList, dup, load 1, const
        // null
        block4Instructions.add(new IfEqOfTypeInstruction(linkedListType));
        block4Instructions.add(new LoadInstruction(2));
        block4Instructions.add(new NewInstruction("LinkedList"));
        block4Instructions.add(new DupInstruction());
        block4Instructions.add(new LoadInstruction(1));
        block4Instructions.add(new ConstInstruction());

        // Block 5: call LinkedList.<init>(int,LinkedList):void, putfield next
        CallInstruction callInit = new CallInstruction("LinkedList.<init>(int,LinkedList):void");
        callInit.setProgram(program);
        block5Instructions.add(callInit);
        block5Instructions.add(new PutFieldInstruction("next"));

        CFG cfg = new CFG();
        BasicBlock bb1 = new BasicBlock(1, block1Instructions);
        BasicBlock bb2 = new BasicBlock(2, block2Instructions);
        BasicBlock bb3 = new BasicBlock(3, block3Instructions);
        BasicBlock bb4 = new BasicBlock(4, block4Instructions);
        BasicBlock bb5 = new BasicBlock(5, block5Instructions);

        cfg.addBlock(bb1);
        cfg.addBlock(bb2);
        cfg.addBlock(bb3);
        cfg.addBlock(bb4);
        cfg.addBlock(bb5);

        cfg.addEdge(bb1, bb2);
        cfg.addEdge(bb2, bb3);
        cfg.addEdge(bb2, bb4);
        cfg.addEdge(bb3, bb2);
        cfg.addEdge(bb4, bb5);

        return cfg;
    }

    private static CFG createGetValueCFG(Program program) {
        List<BytecodeInstruction> block1Instructions = new ArrayList<>();

        block1Instructions.add(new LoadInstruction(0));
        block1Instructions.add(new GetFieldInstruction("value"));

        CFG cfg = new CFG();
        BasicBlock bb1 = new BasicBlock(1, block1Instructions);
        cfg.addBlock(bb1);

        return cfg;
    }

    private static CFG createGetNextCFG(Program program) {
        List<BytecodeInstruction> block1Instructions = new ArrayList<>();

        block1Instructions.add(new LoadInstruction(0));
        block1Instructions.add(new GetFieldInstruction("next"));

        CFG cfg = new CFG();
        BasicBlock bb1 = new BasicBlock(1, block1Instructions);
        cfg.addBlock(bb1);

        return cfg;
    }

    private static CFG createMainCFG(Program program) {
        List<BytecodeInstruction> block1Instructions = new ArrayList<>();
        List<BytecodeInstruction> block2Instructions = new ArrayList<>();
        List<BytecodeInstruction> block3Instructions = new ArrayList<>();
        List<BytecodeInstruction> block4Instructions = new ArrayList<>();
        List<BytecodeInstruction> block5Instructions = new ArrayList<>();

        block1Instructions.add(new NewInstruction("LinkedList"));
        block1Instructions.add(new DupInstruction());
        block1Instructions.add(new ConstInstruction(1));
        block1Instructions.add(new ConstInstruction());

        CallInstruction callInit = new CallInstruction("LinkedList.<init>(int,LinkedList):void");
        callInit.setProgram(program);
        block2Instructions.add(callInit);
        block2Instructions.add(new StoreInstruction(1));
        block2Instructions.add(new LoadInstruction(1));
        block2Instructions.add(new ConstInstruction(2));

        CallInstruction callAppend1 = new CallInstruction("LinkedList.append(int):void");
        callAppend1.setProgram(program);
        block3Instructions.add(callAppend1);
        block3Instructions.add(new LoadInstruction(1));
        block3Instructions.add(new ConstInstruction(3));

        CallInstruction callAppend2 = new CallInstruction("LinkedList.append(int):void");
        callAppend2.setProgram(program);
        block4Instructions.add(callAppend2);
        block4Instructions.add(new LoadInstruction(1));

        CallInstruction callSize = new CallInstruction("LinkedList.size():int");
        callSize.setProgram(program);
        block5Instructions.add(callSize);
        block5Instructions.add(new StoreInstruction(2));

        CFG cfg = new CFG();
        BasicBlock bb1 = new BasicBlock(1, block1Instructions);
        BasicBlock bb2 = new BasicBlock(2, block2Instructions);
        BasicBlock bb3 = new BasicBlock(3, block3Instructions);
        BasicBlock bb4 = new BasicBlock(4, block4Instructions);
        BasicBlock bb5 = new BasicBlock(5, block5Instructions);

        cfg.addBlock(bb1);
        cfg.addBlock(bb2);
        cfg.addBlock(bb3);
        cfg.addBlock(bb4);
        cfg.addBlock(bb5);

        cfg.addEdge(bb1, bb2);
        cfg.addEdge(bb2, bb3);
        cfg.addEdge(bb3, bb4);
        cfg.addEdge(bb4, bb5);

        return cfg;
    }

    public static void main(String[] args) {
        Program program = createLinkedListProgram();
        System.out.println(program);

        String programName = program.getName();
        String programDir = Constants.GENERATED_DIR + File.separator + programName;

        if (Constants.ENABLE_FILE_GENERATION) {
            try {
                Path programPath = Paths.get(programDir);
                if (Files.exists(programPath)) {
                    Files.walk(programPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
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
            program.getMethod("append").saveToFile();
            program.getMethod("size").saveToFile();
            program.saveToFile("linkedListAnalysis");

            // Generate images from all DOT files in the program directory
            MemoryGraphGenerator.generateImagesFromDotFiles(programDir);
        }

        // Reset initial state for cyclicity analysis
        initialState = new JVMState();
        initialState.setLocalVariable(0, Value.NULL);

        System.out.println("\n========================================");
        System.out.println("EXECUTING PROGRAM WITH CYCLICITY ANALYSIS");
        System.out.println("========================================\n");

        CyclicityAnalysisRunner cyclicityAnalysisRunner = new CyclicityAnalysisRunner();
        Set<JVMState> cyclicityAnalysisResults = program.execute(initialState, cyclicityAnalysisRunner);

        System.out.println("\nFinal state after cyclicity analysis:");
        for (JVMState state : cyclicityAnalysisResults) {
            System.out.println("Local variables: " + state.getLocalVariablesSize());
            System.out.println("Stack size: " + state.getStackSize());
            System.out.println(state.toDetailedString());
        }

        if (Constants.ENABLE_FILE_GENERATION) {
            program.getMethod("append").saveToFile();
            program.getMethod("size").saveToFile();
            program.saveToFile("linkedListAnalysis");

            // Generate images from all DOT files in the program directory
            MemoryGraphGenerator.generateImagesFromDotFiles(programDir);
        }
    }
}