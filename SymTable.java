import java.util.*;

public class SymTable {
    private List<HashMap<String, MySym>> list;
    
    public SymTable() {
        list = new LinkedList<HashMap<String, MySym>>();
        list.add(new HashMap<String, MySym>());
    }
    
    public void addDecl(String name, MySym sym) 
	throws DuplicateSymException, EmptySymTableException, WrongArgumentException {
	if (name == null && sym == null) {
	    throw new WrongArgumentException("Arguments name and sym are null.");
	}
	else if (name == null) {
	    throw new WrongArgumentException("Argument name is null.");
	}
	else if (sym == null) {
	    throw new WrongArgumentException("Argument sym is null.");
	}
               
        if (list.isEmpty()) {
            throw new EmptySymTableException();
        }
	
        HashMap<String, MySym> symTab = list.get(0);
        if (symTab.containsKey(name))
            throw new DuplicateSymException();
        
        symTab.put(name, sym);
    }
    
    public void addScope() {
        list.add(0, new HashMap<String, MySym>());
    }
    
    public MySym lookupLocal(String name) {
        if (list.isEmpty())
            return null;
        
        HashMap<String, MySym> symTab = list.get(0); 
        return symTab.get(name);
    }
    
    public MySym lookupGlobal(String name) {
        if (list.isEmpty())
            return null;
        
        for (HashMap<String, MySym> symTab : list) {
            MySym sym = symTab.get(name);
            if (sym != null)
                return sym;
        }
        return null;
    }
    
    public void removeScope() throws EmptySymTableException {
        if (list.isEmpty())
            throw new EmptySymTableException();
        list.remove(0);
    }
    
    public void print() {
        System.out.print("\n=== MySym Table ===\n");
        for (HashMap<String, MySym> symTab : list) {
            System.out.println(symTab.toString());
        }
        System.out.println();
    }
}
