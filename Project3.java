
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

    // Represent the memory system as a binary tree, only saving root node
    private MemoryBlock root;

    // holds current character to assign to a block
    private char curId;


    /** Constructs a memory object given an initial size
     *  @param memSize the initial size of the memory system
     */
    public Memory(int memSize) {
        this.root = new MemoryBlock(memSize);
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

        // find smallest, left-most free memory block still larger than or equal to request
        MemoryBlock freeBlock = findFreeBlock(root, requestSize);

        // no such block found/available
        if (!freeBlock.isValidBlock) {
            return false;
        }
        
        // allocate previously-found block (either mark the block or split it as needed)
        MemoryBlock allocatedBlock = allocateBlock(freeBlock, requestSize);
        
        // add ID to block
        allocatedBlock.id = String.valueOf(curId);
        curId++;

        return true;

    }

    /** Find the left-most, smallest, free block that can hold the memory request. 
     *  Return invalid memory block if nothing can be allocated. 
     *  @param curBlock the current block to process in recursive call
     *  @param requestSize the size of the user request 
     *  @return the best-choice memory block or an invalid block
     */
    private MemoryBlock findFreeBlock(MemoryBlock curBlock, int requestSize) {

        // only check leaf nodes (current memory blocks)
        if (curBlock.leftBlock == null && curBlock.rightBlock == null) {
            
            // if block is free and the size fits the request
            if (curBlock.id.equals(" ") && curBlock.size >= requestSize) {
                return curBlock;
            }

            // if block is invalid (small size or allocated), then return invalid block
            return new MemoryBlock();
        }

        // recursive call to get to leaf nodes and find best-choice block on both sides of tree
        MemoryBlock minLeftBlock = findFreeBlock(curBlock.leftBlock, requestSize);
        MemoryBlock minRightBlock = findFreeBlock(curBlock.rightBlock, requestSize);
        
        // choose the smaller best-choice block and return
        // if blocks are equal, prefer left-most block
        // if one block is invalid, then has MAX_INT value and will not be chosen
        // if both blocks are invalid, then return invalid block to show allocation not possible
        if (minLeftBlock.size <= minRightBlock.size) {
            return minLeftBlock;
        } else {
            return minRightBlock;
        }

    }

    /** Allocates memory to a given free block.
     *  @param curBlock the current block to process in recursive call
     *  @param requestSize the size of the user request
     *  @return the allocated block
     */
    private MemoryBlock allocateBlock(MemoryBlock curBlock, int requestSize) {

        // if request is more than half the current block's size, allocate current block
        if (curBlock.size / 2 < requestSize) {
            return curBlock;
        }

        // otherwise, split current block into equal left/right blocks 
        MemoryBlock leftBlock = new MemoryBlock(curBlock.size / 2);
        MemoryBlock rightBlock = new MemoryBlock(curBlock.size / 2);
        curBlock.leftBlock = leftBlock;
        curBlock.rightBlock = rightBlock;

        // recursively split or allocate left-most block
        return allocateBlock(leftBlock, requestSize);
    }

    /** Tries to release a block of memory given a block ID
     *  @param releaseId the ID of the block to release
     *  @return whether or not the release was successful
     */
    private boolean releaseMemory(String releaseId) {
        
        // find block of memory with given ID
        MemoryBlock block = findBlock(root, releaseId);

        // if no block found with ID, return false
        if (!block.isValidBlock) {
            return false;
        }

        // remove ID from block
        block.id = " ";

        // after freeing block, merge free buddies in tree
        mergeMemory(root);
        return true;

    }

    /** Given an ID, tries to find matching allocated block in tree.
     *  @param curBlock the current block to process in recursive call
     *  @param releaseId the ID of the block to find
     *  @return the matching block or an invalid block if ID not found
     */
    private MemoryBlock findBlock(MemoryBlock curBlock, String releaseId) {

        // only check leaf nodes
        if (curBlock.leftBlock == null & curBlock.rightBlock == null) {
            // if ID matches return block, otherwise return invalid block
            if (curBlock.id.equals(releaseId)) {
                return curBlock;
            } else {
                return new MemoryBlock();
            }
        }

        // scan left and right subtrees
        MemoryBlock leftFind = findBlock(curBlock.leftBlock, releaseId);
        MemoryBlock rightFind = findBlock(curBlock.rightBlock, releaseId);

        // return left or right block if valid, otherwise just return invalid block
        if (leftFind.isValidBlock) {
            return leftFind;
        } else if (rightFind.isValidBlock) {
            return rightFind;
        } else {
            return new MemoryBlock();
        }
        
    }

    /** Try to merge as many free buddies in tree together as possible.
     *  @param curBlock the current block to process in recursive call
     */
    private void mergeMemory(MemoryBlock curBlock) {

        // if leaf node, don't process
        if (curBlock.leftBlock == null && curBlock.rightBlock == null) {
            return;
        }

        // recurse before processing in order to process bottom-up        
        mergeMemory(curBlock.leftBlock);
        mergeMemory(curBlock.rightBlock);

        // check if a node's children are unallocated and have no lower allocated nodes
        // if so, remove node's children
        if (curBlock.leftBlock.id.equals(" ") && !curBlock.leftBlock.hasChildren() && 
            curBlock.rightBlock.id.equals(" ") && !curBlock.rightBlock.hasChildren()) {
            curBlock.leftBlock = null;
            curBlock.rightBlock = null;
        }

    }

    /** Output the entire memory block as formatted in the instructions
     *  @return string representation of the memory
     */
    @Override
    public String toString() {
        
        // get in-order list of memory blocks from memory tree
        ArrayList<MemoryBlock> blockList = generateMemoryList(new ArrayList<MemoryBlock>(), root);
        int n = blockList.size();
        String output = "";

        // top line
        output += "--------------";
        for(int i = 1; i < n; i++) {
            output += "------------";
        }
        output += "\n";

        // middle
        for (MemoryBlock block : blockList) {
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

    /** Generates an in-order list of all memory blocks from the memory tree structure
     *  @param blockList a list that stores the memory blocks through recursive calls
     *  @param curBlock the current block to process in recursive call
     */
    private ArrayList<MemoryBlock> generateMemoryList(ArrayList<MemoryBlock> blockList, MemoryBlock curBlock) {

        // only add leaf nodes to list
        if (curBlock.leftBlock == null & curBlock.rightBlock == null) {
            blockList.add(curBlock);
            return blockList;
        }

        // fill list with leaf nodes from left/right subtrees before returning
        blockList = generateMemoryList(blockList, curBlock.leftBlock);
        blockList = generateMemoryList(blockList, curBlock.rightBlock);
        return blockList;

    }

}


/** Represents a block in the memory */
class MemoryBlock {

    // possible ID and definite size of block
    public String id;
    public int size;

    // block can have left/right node in data tree
    public MemoryBlock leftBlock;
    public MemoryBlock rightBlock;

    // marks if the block is valid or not (used in recursive calls)
    public boolean isValidBlock;


    /** Construct a block with a given size
     *  @param size block size
     */
    public MemoryBlock(int size) {
        this.id = " ";
        this.size = size;
        this.leftBlock = null;
        this.rightBlock = null;
        this.isValidBlock = true;
    }

    /** Construct an invalid block */
    public MemoryBlock() {
        this.size = Integer.MAX_VALUE;
        this.isValidBlock = false;
    }

    /** Check if a memory block has a left or right child
     *  @return whether or not block has children
     */
    public boolean hasChildren() {
        return leftBlock != null || rightBlock != null;
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