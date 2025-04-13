package fr.univreunion.bcterm;

import java.util.ArrayList;
import java.util.List;

import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.jvm.instruction.ConstInstruction;
import fr.univreunion.bcterm.jvm.instruction.DupInstruction;
import fr.univreunion.bcterm.jvm.instruction.GetFieldInstruction;
import fr.univreunion.bcterm.jvm.instruction.IfEqOfTypeInstruction;
import fr.univreunion.bcterm.jvm.instruction.IfNeOfTypeInstruction;
import fr.univreunion.bcterm.jvm.instruction.LoadInstruction;
import fr.univreunion.bcterm.jvm.instruction.NewInstruction;
import fr.univreunion.bcterm.jvm.instruction.PutFieldInstruction;
import fr.univreunion.bcterm.jvm.instruction.StoreInstruction;
import fr.univreunion.bcterm.jvm.state.LocationValue;
import fr.univreunion.bcterm.jvm.state.Value;
import fr.univreunion.bcterm.program.BasicBlock;
import fr.univreunion.bcterm.program.CFG;
import fr.univreunion.bcterm.program.Program;

public class SharingProgramExample {

    public static Program createSharingProgram() {
        // Create a new Program
        Program program = new Program("Sharing");

        // Add the "expand" method
        CFG expandCfg = createExpandCFG();
        program.addMethod("expand", "(Sharing):void", expandCfg);

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
        // block5Instructions.add(new CallInstruction("Sharing", "<init>",
        // "(LSharing;)V", 1));
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

    public static void main(String[] args) {
        Program programme = createSharingProgram();
        System.out.println(programme);

        programme.getMethod("expand").saveToFile();
        programme.saveToFile();
    }
}
