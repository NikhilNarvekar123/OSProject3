
import java.util.Scanner;
import java.util.ArrayList;
import java.io.File;
import java.util.Queue;
import java.util.LinkedList;

public class Project3 {

    private static final int MEM_SIZE = 1024; 

    public static void main(String[] args) throws Exception {

        // file read
        ArrayList<Command> commands = new ArrayList<>();
        try {            
            Scanner sc = new Scanner(new File("input.txt"));
            while (sc.hasNextLine()) {
                commands.add(new Command(sc.nextLine()));
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        // make memory object
        Memory mem = new Memory(MEM_SIZE);

        // process commands
        for(Command cmd : commands) {
            System.out.println(cmd);

            // do mem operation
            if (!mem.processCommand(cmd)) {
                throw new Exception("Could not satisfy request!");
            }

            System.out.println(mem);
        }

    }

}


/** TODO: */
class Memory {

    // TODO:
    private int memSize;
    private ArrayList<MemoryEntry> mem;
    private char curId;

    /** TODO: */
    public Memory(int memSize) {
        this.memSize = memSize;
        this.mem = new ArrayList<MemoryEntry>();
        this.mem.add(new MemoryEntry(memSize));
        this.curId = 'A';
    }

    /** TODO: */
    public boolean processCommand(Command cmd) {
        if (cmd.getType() == CommandType.REQUEST) {
            return requestMemory(cmd.getRequestSize());
        } else {
            return releaseMemory(cmd.getReleaseId());
        }
    }

    /** TODO: */
    private boolean requestMemory(int requestSize) {

        // algo 
        //  - do entire pass to find min size section (where size is still > reqSize)
        //      - left-right pass
        //  - recursively partition min until 2n-1 < rs < 2n
        //    - each size is added to queue
        //  - finally, queue FIFO is the new memory partition


        if (requestSize < 64) {
            return false;
        }

        Queue<Integer> sizes = new LinkedList<>();


        int minSize = Integer.MAX_VALUE;
        int minIdx = -1;
        for (int i = 0; i < mem.size(); i++) {
            MemoryEntry memEntry = mem.get(i);
            if (memEntry.id.equals(" ") && memEntry.size >= requestSize) {
                if (memEntry.size < minSize) {
                    minSize = memEntry.size;
                    minIdx = i;
                }
            }
        }

        // op failed
        if (minIdx == -1) {
            return false;
        }
        
        // recursively break size at mem index into fitting partitions
        boolean hasSplit = false;
        while (requestSize <= mem.get(minIdx).size / 2) {
            int size = mem.get(minIdx).size;
            mem.get(minIdx).size = size / 2;
            mem.add(minIdx + 1, new MemoryEntry(size / 2, -1));
            hasSplit = true;
        }

        if (hasSplit) {
            mem.buddyDirection = 1;
        }
        
        // set current section to be allocated section
        mem.get(minIdx).id = String.valueOf(curId);
        curId++;

        return true;
    }

    /** TODO: */
    private boolean releaseMemory(String releaseId) {
        
        int memIdx = -1;
        int size;
        for(int i = 0; i < mem.size(); i++) {
            if (mem.get(i).id == releaseId) {
                memIdx = i;
                size = mem.get(i).size;
                break;
            }
        }

        if (memIdx == -1) {
            return false;
        }


        // only want to merge with buddy blocks
        // i.e.
        // given sequence ... 64k 64k(A) 64k 64k(B) ...
        // if we release A, we only want to merge with the leftmost mem for
        // result ... 128k 64k 64k(B) ...
        // if we merge the A mem block right first then this issue happens
        // result ... 64k 128k 64k (B)
        // where both 64k mems are left without buddies and cannot be merged back in


        MemoryEntry left;
        MemoryEntry right;

        while (true) {

            if (mem.get(memIdx).buddyDirection == 1) {

                mem.remove(memIdx + 1);
                mem.get(memIdx).size *= 2;

            } 
  


        }


        return true;
    }

    /** TODO: */
    public int getInitialSize() {
        return memSize;
    }

    /** TODO: */
    @Override
    public String toString() {
        
        int n = mem.size();
        String output = "";

        // top line
        output += "--------------";
        for(int i = 1; i < n; i++) {
            output += "-------------";
        }
        output += "\n";

        // middle
        for (MemoryEntry memEntry : mem) {
            String sizeLabel = String.format("%4d", memEntry.size);
            String box = String.format(" " + memEntry.id + "    " + sizeLabel + "K ");
            output += "|" + box;
        }
        output += "|\n";

        // bottom line
        output += "--------------";
        for(int i = 1; i < n; i++) {
            output += "-------------";
        }
        output += "\n";

        return output;
    } 

}

/** TODO: */
class MemoryEntry {

    // TODO:
    public String id;
    public int size;
    public int buddyDirection;

    /** TODO: */
    public MemoryEntry(int size) {
        this.id = " ";
        this.size = size;
    }

    /** TODO: */
    public MemoryEntry(String id, int size) {
        this.id = id;
        this.size = size;
    }

    /** TODO: */
    public MemoryEntry(int size, int buddyDirection) {
        this.id = id;
        this.size = size;
        this.buddyDirection = buddyDirection;
    }

}




/** TODO: */
enum CommandType {
    REQUEST, RELEASE;
}

/** TODO: */
class Command {

    // TODO:
    private CommandType commandType;
    private int requestSize;
    private String releaseId;

    /** TODO: */
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

    /** TODO: */
    public CommandType getType() {
        return commandType;
    }

    /** TODO: */
    public int getRequestSize() {
        return requestSize;
    }

    /** TODO: */
    public String getReleaseId() {
        return releaseId;
    }

    /** TODO: */
    @Override
    public String toString() {
        if (commandType == CommandType.REQUEST) {
            return String.format("Request %dK", requestSize);
        } else {
            return "Release " + releaseId;
        }
    }

}