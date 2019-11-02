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
        return this.formalTypeList.toString() + "->" + this.retType;
    }

    public String toString() {
        return formalTypeList + "->" + retType 
                + " " + kind;
    }
}
