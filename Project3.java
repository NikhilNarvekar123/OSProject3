
import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.LinkedList;


public class Project3 {

    // holds initial memory size (bytes) used in memory system
    private static final int MEM_SIZE = 1024; 

    /** Makes memory object and reads input files, running all commands
     *  on memory object.
     */
    public static void main(String[] args) throws Exception {

        // read file
        ArrayList<Command> commands = new ArrayList<>();
        try {            
            Scanner sc = new Scanner(new File("input.txt"));
            while (sc.hasNextLine()) {
                commands.add(new Command(sc.nextLine()));
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        // make memory object with initial size
        Memory mem = new Memory(MEM_SIZE);

        // process commands from file
        System.out.println(mem);
        for(Command cmd : commands) {
            System.out.println(cmd);
            if (!mem.processCommand(cmd)) {
                // throw exception if command was invalid
                throw new Exception("Could not satisfy request!");
            }
            System.out.println(mem);
        }
        
    }

}


/** Represents the system memory using a buddy allocation system */
class Memory {

    // list holds all memory blocks
    private ArrayList<MemoryBlock> mem;

    // holds current character to assign to a block
    private char curId;


    /** Constructs a memory object given an initial size
     *  @param memSize the initial size of the memory system
     */
    public Memory(int memSize) {
        this.mem = new ArrayList<MemoryBlock>();
        this.mem.add(new MemoryBlock(memSize));
        this.curId = 'A';
    }

    /** Processes an input file command, if invalid returns false
     *  @param cmd the command to process (req or rel)
     *  @return whether or not command could be processed
     */
    public boolean processCommand(Command cmd) {
        if (cmd.commandType == CommandType.REQUEST) {
            return requestMemory(cmd.requestSize);
        } else {
            return releaseMemory(cmd.releaseId);
        }
    }

    /** Tries to allocate a block of memory given a request
     *  @param requestSize the size of the memory request
     *  @return whether or not the allocation was possible
     */
    private boolean requestMemory(int requestSize) {

        // cannot request less than 64 bytes
        if (requestSize < 64) {
            return false;
        }

        // find smallest memory block still larger than or equal to request
        // that is also free
        int minSize = Integer.MAX_VALUE;
        int minIdx = -1;
        for (int i = 0; i < mem.size(); i++) {
            MemoryBlock block = mem.get(i);
            if (block.id.equals(" ") && block.size >= requestSize) {
                if (block.size < minSize) {
                    minSize = block.size;
                    minIdx = i;
                }
            }
        }

        // no such block found/available
        if (minIdx == -1) {
            return false;
        }
        
        // iteratively break chosen block into smallest size possible
        // to still be able to hold request
        while (requestSize <= mem.get(minIdx).size / 2) {
            mem.add(minIdx + 1, new MemoryBlock(mem.get(minIdx).size / 2));
            mem.get(minIdx).size /= 2;
        }
        
        // attach ID to newly allocated block
        mem.get(minIdx).id = String.valueOf(curId);
        curId++;

        return true;

    }

    /** Tries to release a block of memory given a block ID
     *  @param releaseId the ID of the block to release
     *  @return whether or not the release was successful
     */
    private boolean releaseMemory(String releaseId) {
        
        // find index of block matching given ID
        int memIdx = -1;
        for(int i = 0; i < mem.size(); i++) {
            if (mem.get(i).id.equals(releaseId)) {
                memIdx = i;
                break;
            }
        }

        // given ID not found in current memory
        if (memIdx == -1) {
            return false;
        }

        // remove ID from block
        mem.get(memIdx).id = " ";

        // merge back memory as much as possible
        MemoryBlock left;
        MemoryBlock right;
        int curSize = mem.get(memIdx).size;

        while (true) {
            
            // get right memory block (if it exists)
            if (memIdx + 1 < mem.size()) {
                right = mem.get(memIdx + 1);
            } else {
                right = null;
            }    
            
            // get left memory block (if it exists)
            if (memIdx - 1 >= 0) {
                left = mem.get(memIdx - 1);
            } else {
                left = null;
            }

            curSize = mem.get(memIdx).size;

            // if right block exists, is same size, and is free, then merge
            if (right != null && right.size == curSize && right.id.equals(" ")) {
                mem.get(memIdx).size *= 2;
                mem.remove(memIdx + 1);
            }
            // if left block exists, is same size, and is free, then merge 
            // (lower priority than right merge)
            else if (left != null && left.size == curSize && left.id.equals(" ")) {
                mem.get(memIdx).size *= 2;
                mem.remove(memIdx - 1);
                memIdx--;
            // if neither left or right match conditions then merging finished
            } else {
                break;
            }

        }

        return true;

    }

    /** Output the entire memory block as formatted in the instructions
     *  @return string representation of the memory
     */
    @Override
    public String toString() {
        
        int n = mem.size();
        String output = "";

        // top line
        output += "--------------";
        for(int i = 1; i < n; i++) {
            output += "------------";
        }
        output += "\n";

        // middle
        for (MemoryBlock block : mem) {
            String sizeLabel = String.format("%5s", block.size + "K");
            String box = String.format(" " + block.id + "   " + sizeLabel + " ");
            output += "|" + box;
        }
        output += " |\n";

        // bottom line
        output += "--------------";
        for(int i = 1; i < n; i++) {
            output += "------------";
        }
        output += "\n";

        return output;

    } 

}


/** Represents a block in the memory */
class MemoryBlock {

    // possible ID and definite size of block
    public String id;
    public int size;

    /** Construct a block with a given size
     *  @param size block size
     */
    public MemoryBlock(int size) {
        this.id = " ";
        this.size = size;
    }

}


/** Possible types of input commands on memory system */
enum CommandType {
    REQUEST, RELEASE;
}

/** Represents a input file command to be run on the memory system */
class Command {

    // variables used in command
    public CommandType commandType;
    public int requestSize;
    public String releaseId;

    /** Construct a command object if valid input given
     *  @param commandStr string to parse into command
     */
    public Command(String commandStr) throws Exception {

        String[] commandArr = commandStr.split("\\s+");

        if (commandArr[0].equals("Request")) {
            this.commandType = CommandType.REQUEST;
            int n = commandArr[1].length();
            this.requestSize = Integer.parseInt(commandArr[1].substring(0, n - 1));
            this.releaseId = "";
        } else if (commandArr[0].equals("Release")) {
            this.commandType = CommandType.RELEASE;
            this.releaseId = commandArr[1];
            this.requestSize = -1;
        } else {
            throw new Exception("Invalid command found in input!");
        }

    }

    /** Provide string representation of command to display
     *  @return string representing command
     */
    @Override
    public String toString() {
        if (commandType == CommandType.REQUEST) {
            return String.format("Request %dK", requestSize);
        } else {
            return "Release " + releaseId;
        }
    }

}