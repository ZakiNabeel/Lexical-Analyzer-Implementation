import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTable {

    public static class Entry {
        public final String name;
        public final String type; // Keeping as string for flexibility (later phases)
        public final int firstLine;
        public final int firstCol;
        public int frequency;

        public Entry(String name, String type, int firstLine, int firstCol) {
            this.name = name;
            this.type = type;
            this.firstLine = firstLine;
            this.firstCol = firstCol;
            this.frequency = 1;
        }
    }

    private final Map<String, Entry> table = new LinkedHashMap<>();

    public void recordIdentifier(String name, int line, int col) {
        Entry e = table.get(name);
        if (e == null) {
            table.put(name, new Entry(name, "IDENTIFIER", line, col));
        } else {
            e.frequency++;
        }
    }

    public void print() {
        System.out.println("\n--- SYMBOL TABLE ---");
        System.out.printf("%-20s %-12s %-10s %-10s %-10s%n",
                "Name", "Type", "FirstLine", "FirstCol", "Freq");
        for (Entry e : table.values()) {
            System.out.printf("%-20s %-12s %-10d %-10d %-10d%n",
                    e.name, e.type, e.firstLine, e.firstCol, e.frequency);
        }
    }
}