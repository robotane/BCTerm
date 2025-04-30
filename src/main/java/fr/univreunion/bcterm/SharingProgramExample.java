package fr.univreunion.bcterm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

public class SharingProgramExample {

    public static Program createSharingProgram() {
        // Create a new Program
        Program program = new Program("Sharing");

        // Add the "expand" method
        CFG expandCfg = createExpandCFG();
        program.addMethod("expand", "(Sharing):void", expandCfg);

        // Add the "init" method
        CFG initCfg = createInitCFG();
        program.addMethod("<init>", "(Sharing):void", initCfg);

        // Add the "main_term" method
        CFG mainTermCfg = createMainTermCFG();
        program.addMethod("main_term", "(String[]):void", mainTermCfg);

        // Set the main method
        program.setMainMethodName("main_term");

        return program;
    }

    private static CFG createExpandCFG() {
        // Create the CFG of "expand"
        List<BytecodeInstruction> block1Instructions = new ArrayList<>();
        List<BytecodeInstruction> block2Instructions = new ArrayList<>();
        List<BytecodeInstruction> block3Instructions = new ArrayList<>();
        List<BytecodeInstruction> block4Instructions = new ArrayList<>();
        List<BytecodeInstruction> block5Instructions = new ArrayList<>();

        // Block 1: load 0, store 2
        block1Instructions.add(new LoadInstruction(0));
        block1Instructions.add(new StoreInstruction(2));

        // Block 2: load 2
        block2Instructions.add(new LoadInstruction(2));

        // Block 3: ifeq of type Sharing
        Value sharingType = new LocationValue(0, "Sharing");
        block3Instructions.add(new IfEqOfTypeInstruction(sharingType));

        // Block 4: ifne of type Sharing, load 1, new Sharing, dup, const null
        block4Instructions.add(new IfNeOfTypeInstruction(sharingType));
        block4Instructions.add(new LoadInstruction(1));
        block4Instructions.add(new NewInstruction("Sharing"));
        block4Instructions.add(new DupInstruction());
        block4Instructions.add(new ConstInstruction());

        // Block 5: call Sharing.<init>(Sharing):void, putfield next, load 1, getfield
        // next, store 1, load 2, getfield next, store 2
        block5Instructions.add(new CallInstruction("Sharing.<init>(Sharing):void"));
        block5Instructions.add(new PutFieldInstruction("next"));
        block5Instructions.add(new LoadInstruction(1));
        block5Instructions.add(new GetFieldInstruction("next"));
        block5Instructions.add(new StoreInstruction(1));
        block5Instructions.add(new LoadInstruction(2));
        block5Instructions.add(new GetFieldInstruction("next"));
        block5Instructions.add(new StoreInstruction(2));

        // Create the CFG
        CFG cfg = new CFG();

        // Create basic blocks
        BasicBlock bb1 = new BasicBlock(1, block1Instructions);
        BasicBlock bb2 = new BasicBlock(2, block2Instructions);
        BasicBlock bb3 = new BasicBlock(3, block3Instructions);
        BasicBlock bb4 = new BasicBlock(4, block4Instructions);
        BasicBlock bb5 = new BasicBlock(5, block5Instructions);

        // Add blocks to CFG
        cfg.addBlock(bb1);
        cfg.addBlock(bb2);
        cfg.addBlock(bb3);
        cfg.addBlock(bb4);
        cfg.addBlock(bb5);

        // Add edges to CFG
        cfg.addEdge(bb1, bb2);
        cfg.addEdge(bb2, bb3);
        cfg.addEdge(bb2, bb4);
        cfg.addEdge(bb4, bb5);
        cfg.addEdge(bb5, bb2);

        return cfg;
    }

    private static CFG createInitCFG() {
        // Create the CFG for "init"
        List<BytecodeInstruction> block1Instructions = new ArrayList<>();

        // Block 1: load 0, load 1, putfield next
        block1Instructions.add(new LoadInstruction(0));
        block1Instructions.add(new LoadInstruction(1));
        block1Instructions.add(new PutFieldInstruction("next"));

        // Create the CFG
        CFG cfg = new CFG();

        // Create the basic block
        BasicBlock bb1 = new BasicBlock(1, block1Instructions);

        // Add block to CFG
        cfg.addBlock(bb1);

        return cfg;
    }

    private static CFG createMainTermCFG() {
        // Create the CFG for "main_term"
        CFG cfg = new CFG();

        // Block 1: new Sharing, dup, new Sharing, dup, new Sharing, dup, const null
        List<BytecodeInstruction> block1Instructions = new ArrayList<>();
        block1Instructions.add(new NewInstruction("Sharing"));
        block1Instructions.add(new DupInstruction());
        block1Instructions.add(new NewInstruction("Sharing"));
        block1Instructions.add(new DupInstruction());
        block1Instructions.add(new NewInstruction("Sharing"));
        block1Instructions.add(new DupInstruction());
        block1Instructions.add(new ConstInstruction()); // null
        BasicBlock bb1 = new BasicBlock(1, block1Instructions);

        // Block 2: call Sharing.<init>(Sharing):void
        List<BytecodeInstruction> block2Instructions = new ArrayList<>();
        block2Instructions.add(new CallInstruction("Sharing.<init>(Sharing):void"));
        BasicBlock bb2 = new BasicBlock(2, block2Instructions);

        // Block 3: call Sharing.<init>(Sharing):void
        List<BytecodeInstruction> block3Instructions = new ArrayList<>();
        block3Instructions.add(new CallInstruction("Sharing.<init>(Sharing):void"));
        BasicBlock bb3 = new BasicBlock(3, block3Instructions);

        // Block 4: call Sharing.<init>(Sharing):void, store 1, new Sharing, dup, new
        // Sharing, dup, const null
        List<BytecodeInstruction> block4Instructions = new ArrayList<>();
        block4Instructions.add(new CallInstruction("Sharing.<init>(Sharing):void"));
        block4Instructions.add(new StoreInstruction(1));
        block4Instructions.add(new NewInstruction("Sharing"));
        block4Instructions.add(new DupInstruction());
        block4Instructions.add(new NewInstruction("Sharing"));
        block4Instructions.add(new DupInstruction());
        block4Instructions.add(new ConstInstruction()); // null
        BasicBlock bb4 = new BasicBlock(4, block4Instructions);

        // Block 5: call Sharing.<init>(Sharing):void
        List<BytecodeInstruction> block5Instructions = new ArrayList<>();
        block5Instructions.add(new CallInstruction("Sharing.<init>(Sharing):void"));
        BasicBlock bb5 = new BasicBlock(5, block5Instructions);

        // Block 6: call Sharing.<init>(Sharing):void, store 2, load 1, load 2
        List<BytecodeInstruction> block6Instructions = new ArrayList<>();
        block6Instructions.add(new CallInstruction("Sharing.<init>(Sharing):void"));
        block6Instructions.add(new StoreInstruction(2));
        block6Instructions.add(new LoadInstruction(1));
        block6Instructions.add(new LoadInstruction(2));
        BasicBlock bb6 = new BasicBlock(6, block6Instructions);

        // Block 7: call Sharing.expand(Sharing):void
        List<BytecodeInstruction> block7Instructions = new ArrayList<>();
        block7Instructions.add(new CallInstruction("Sharing.expand(Sharing):void"));
        BasicBlock bb7 = new BasicBlock(7, block7Instructions);

        // Add blocks to CFG
        cfg.addBlock(bb1);
        cfg.addBlock(bb2);
        cfg.addBlock(bb3);
        cfg.addBlock(bb4);
        cfg.addBlock(bb5);
        cfg.addBlock(bb6);
        cfg.addBlock(bb7);

        // Add edges to CFG
        cfg.addEdge(bb1, bb2);
        cfg.addEdge(bb2, bb3);
        cfg.addEdge(bb3, bb4);
        cfg.addEdge(bb4, bb5);
        cfg.addEdge(bb5, bb6);
        cfg.addEdge(bb6, bb7);

        return cfg;
    }

    public static void main(String[] args) {
        Program program = createSharingProgram();
        System.out.println(program);

        if (Constants.ENABLE_FILE_GENERATION) {
            // Delete all existing memoryGraph_*.dot files
            File dir = new File(Constants.GENERATED_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File[] dotFiles = dir.listFiles((d, name) -> name.startsWith(Constants.MEMORY_GRAPH_PREFIX)
                    && name.endsWith(Constants.DOT_FILE_EXTENSION));
            if (dotFiles != null) {
                for (File file : dotFiles) {
                    file.delete();
                }
            }
        }

        JVMState initialState = new JVMState();
        // Initialize local variable 0 with null to represent args
        initialState.setLocalVariable(0, Value.NULL);

        Set<JVMState> finalStates = program.execute(initialState);

        System.out.println("\nFinal state of the main method:");
        for (JVMState state : finalStates) {
            System.out.println("Local variables: " + state.getLocalVariablesSize());
            System.out.println("Stack size: " + state.getStackSize());
            System.out.println(state.toDetailedString());
        }

        if (Constants.ENABLE_FILE_GENERATION) {
            program.getMethod("expand").saveToFile();
            program.saveToFile("sharingPairs");

            // Generate graphics for all memoryGraph_*.dot files
            File dir = new File(Constants.GENERATED_DIR);
            File[] dotFiles = dir.listFiles((d, name) -> name.startsWith(Constants.MEMORY_GRAPH_PREFIX)
                    && name.endsWith(Constants.DOT_FILE_EXTENSION));
            if (dotFiles != null) {
                for (File dotFile : dotFiles) {
                    String baseName = dotFile.getName().substring(0,
                            dotFile.getName().length() - Constants.DOT_FILE_EXTENSION.length());
                    try {
                        ProcessBuilder pbDefault = new ProcessBuilder(
                                Constants.DOT_COMMAND,
                                Constants.DOT_TYPE_FLAG_PREFIX + Constants.DEFAULT_GRAPH_EXTENSION,
                                dotFile.getAbsolutePath(),
                                Constants.DOT_OUTPUT_FLAG,
                                new File(Constants.GENERATED_DIR, baseName + "." + Constants.DEFAULT_GRAPH_EXTENSION)
                                        .getAbsolutePath());
                        Process pDefault = pbDefault.start();
                        pDefault.waitFor();
                        System.out.println(
                                "Generated " + dotFile + "." + Constants.DEFAULT_GRAPH_EXTENSION
                                        + " successfully.");
                    } catch (IOException | InterruptedException e) {
                        System.err.println("Error generating files from " + dotFile.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
    }
}