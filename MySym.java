import java.util.List;
 
enum Kind {
    FUNC, VAR, STRUCT, CLASS
}

public class MySym {
    protected String type;
    protected Kind kind;
    
    public MySym(String type, Kind kind) {
        this.type = type;
        this.kind = kind;
    }
    
    public String getType() {
        return type;
    }

    public Kind getKind() {
        return kind;
    }
    
    public String toString() {
        return type + " " + kind;
    }
}

class MyFuncSym extends MySym{
    private String retType;
    private List<String> formalTypeList;

    public MyFuncSym(String type, Kind kind) {
        super(type, kind);
        retType = type;
    }

    public MyFuncSym(String type){
        super(type, Kind.FUNC);
        retType = type;
    }

    public void setRetType(String rt) {
        this.retType = rt;
    }

    public String getRetType() {
        return this.retType;
    }

    public void setFormalTypeList(List l) {
        this.formalTypeList = l;
    }

    public String getType() {
        String fstr = this.formalTypeList.toString();
        int flen = fstr.length();
        return fstr.substring(1, flen-1) + "->" + this.retType;
    }

    public String toString() {
        return formalTypeList + "->" + retType 
                + " " + kind;
    }
}


class StructDefSym extends MySym {
    public StructDefSym(String type, Kind kind){
        super(type, kind);
        field = new SymTable();
    }

    public StructDefSym(String type) {
        super(type, Kind.STRUCT);
        field = new SymTable();
    }

    public SymTable getField(){
        return field;
    }

    private SymTable field;
}

class StructSym extends MySym {
    public StructSym(String type, Kind kind){
        super(type, kind);
        field = new SymTable();
    }

    public StructSym(String type) {
        super(type, Kind.STRUCT);
        field = new SymTable();
    }
    public void link(SymTable field) {
        this.field = field;
    }

    public SymTable getField() {
        return field;
    }
    private SymTable field;
}