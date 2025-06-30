package fr.univreunion.bcterm;

import java.util.ArrayList;
import java.util.List;
import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.jvm.instruction.CallInstruction;
import fr.univreunion.bcterm.jvm.instruction.GetFieldInstruction;
import fr.univreunion.bcterm.jvm.instruction.LoadInstruction;
import fr.univreunion.bcterm.jvm.instruction.NewInstruction;
import fr.univreunion.bcterm.jvm.instruction.PutFieldInstruction;
import fr.univreunion.bcterm.jvm.instruction.StoreInstruction;
import fr.univreunion.bcterm.program.BasicBlock;
import fr.univreunion.bcterm.program.CFG;
import fr.univreunion.bcterm.program.Program;
import fr.univreunion.bcterm.util.Logger;

public class AliasingSharingCallExample {
    private static final java.util.logging.Logger logger = Logger.getLogger(AliasingSharingCallExample.class);

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
        ExampleRunner.run(program);
    }
}