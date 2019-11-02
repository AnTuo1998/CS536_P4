import java.io.*;
import java.io.ObjectInputStream.GetField;
import java.util.*;
import java.util.stream.Collectors;

// **********************************************************************
// The ASTnode class defines the nodes of the abstract-syntax tree that
// represents a cflat program.
//
// Internal nodes of the tree contain pointers to children, organized
// either in a list (for nodes that may have a variable number of 
// children) or as a fixed set of fields.
//
// The nodes for literals and ids contain line and character number
// information; for string literals and identifiers, they also contain a
// string; for integer literals, they also contain an integer value.
//
// Here are all the different kinds of AST nodes and what kinds of children
// they have.  All of these kinds of AST nodes are subclasses of "ASTnode".
// Indentation indicates further subclassing:
//
//     Subclass            Kids
//     --------            ----
//     ProgramNode         DeclListNode
//     DeclListNode        linked list of DeclNode
//     DeclNode:
//       VarDeclNode       TypeNode, IdNode, int
//       FnDeclNode        TypeNode, IdNode, FormalsListNode, FnBodyNode
//       FormalDeclNode    TypeNode, IdNode
//       StructDeclNode    IdNode, DeclListNode
//
//     FormalsListNode     linked list of FormalDeclNode
//     FnBodyNode          DeclListNode, StmtListNode
//     StmtListNode        linked list of StmtNode
//     ExpListNode         linked list of ExpNode
//
//     TypeNode:
//       IntNode           -- none --
//       BoolNode          -- none --
//       VoidNode          -- none --
//       StructNode        IdNode
//
//     StmtNode:
//       AssignStmtNode      AssignNode
//       PostIncStmtNode     ExpNode
//       PostDecStmtNode     ExpNode
//       ReadStmtNode        ExpNode
//       WriteStmtNode       ExpNode
//       IfStmtNode          ExpNode, DeclListNode, StmtListNode
//       IfElseStmtNode      ExpNode, DeclListNode, StmtListNode,
//                                    DeclListNode, StmtListNode
//       WhileStmtNode       ExpNode, DeclListNode, StmtListNode
//       RepeatStmtNode      ExpNode, DeclListNode, StmtListNode
//       CallStmtNode        CallExpNode
//       ReturnStmtNode      ExpNode
//
//     ExpNode:
//       IntLitNode          -- none --
//       StrLitNode          -- none --
//       TrueNode            -- none --
//       FalseNode           -- none --
//       IdNode              -- none --
//       DotAccessNode       ExpNode, IdNode
//       AssignNode          ExpNode, ExpNode
//       CallExpNode         IdNode, ExpListNode
//       UnaryExpNode        ExpNode
//         UnaryMinusNode
//         NotNode
//       BinaryExpNode       ExpNode ExpNode
//         PlusNode     
//         MinusNode
//         TimesNode
//         DivideNode
//         AndNode
//         OrNode
//         EqualsNode
//         NotEqualsNode
//         LessNode
//         GreaterNode
//         LessEqNode
//         GreaterEqNode
//
// Here are the different kinds of AST nodes again, organized according to
// whether they are leaves, internal nodes with linked lists of kids, or
// internal nodes with a fixed number of kids:
//
// (1) Leaf nodes:
//        IntNode,   BoolNode,  VoidNode,  IntLitNode,  StrLitNode,
//        TrueNode,  FalseNode, IdNode
//
// (2) Internal nodes with (possibly empty) linked lists of children:
//        DeclListNode, FormalsListNode, StmtListNode, ExpListNode
//
// (3) Internal nodes with fixed numbers of kids:
//        ProgramNode,     VarDeclNode,     FnDeclNode,     FormalDeclNode,
//        StructDeclNode,  FnBodyNode,      StructNode,     AssignStmtNode,
//        PostIncStmtNode, PostDecStmtNode, ReadStmtNode,   WriteStmtNode   
//        IfStmtNode,      IfElseStmtNode,  WhileStmtNode,  RepeatStmtNode,
//        CallStmtNode
//        ReturnStmtNode,  DotAccessNode,   AssignExpNode,  CallExpNode,
//        UnaryExpNode,    BinaryExpNode,   UnaryMinusNode, NotNode,
//        PlusNode,        MinusNode,       TimesNode,      DivideNode,
//        AndNode,         OrNode,          EqualsNode,     NotEqualsNode,
//        LessNode,        GreaterNode,     LessEqNode,     GreaterEqNode
//
// **********************************************************************

// **********************************************************************
// <<<ASTnode class (base class for all other kinds of nodes)>>>
// **********************************************************************

abstract class ASTnode { 
    // every subclass must provide an unparse operation
    abstract public void unparse(PrintWriter p, int indent, boolean isDecl);

    // this method can be used by the unparse methods to do indenting
    protected void addIndent(PrintWriter p, int indent) {
        for (int k = 0; k < indent; k++) p.print(" ");
    }

    abstract public void nameAnalysis(SymTable st);
}

// **********************************************************************
// <<<ProgramNode,  DeclListNode, FormalsListNode, FnBodyNode,
// StmtListNode, ExpListNode>>>
// **********************************************************************

class ProgramNode extends ASTnode {
    public ProgramNode(DeclListNode L) {
        myDeclList = L;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        myDeclList.unparse(p, indent, isDecl);
    }

    public void nameAnalysis(SymTable st) {
        System.out.println("name ana begin");
        SymTable mySt = new SymTable();
        myDeclList.nameAnalysis(mySt);
        System.out.println("name ana end");
    }

    // 1 kid
    private DeclListNode myDeclList;
}

class DeclListNode extends ASTnode {
    public DeclListNode(List<DeclNode> S) {
        myDecls = S;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        Iterator it = myDecls.iterator();
        try {
            while (it.hasNext()) {
                ((DeclNode)it.next()).unparse(p, indent, isDecl);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    }

    public void nameAnalysis(SymTable st) {
        System.out.println("Decl List nam");
        Iterator it = myDecls.iterator();
        try {
            while (it.hasNext()) {
                ((DeclNode) it.next()).nameAnalysis(st);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    }

    // list of kids (DeclNodes)
    private List<DeclNode> myDecls;
}

class FormalsListNode extends ASTnode {
    public FormalsListNode(List<FormalDeclNode> S) {
        myFormals = S;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        Iterator<FormalDeclNode> it = myFormals.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent, isDecl);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent, isDecl);
            }
        } 
    }

    public void nameAnalysis(SymTable st) {
        Iterator it = myFormals.iterator();
        try {
            while (it.hasNext()) {
                ((FormalDeclNode) it.next()).nameAnalysis(st);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in FormalsListNode.print");
            System.exit(-1);
        }
    }
    
    List<String> getFormalTypeList() {
        return myFormals.stream().map(n -> n.getType())
                        .collect(Collectors.toList());
    }

    // list of kids (FormalDeclNodes)
    private List<FormalDeclNode> myFormals;
}

class FnBodyNode extends ASTnode {
    public FnBodyNode(DeclListNode declList, StmtListNode stmtList) {
        myDeclList = declList;
        myStmtList = stmtList;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        myDeclList.unparse(p, indent, isDecl);
        myStmtList.unparse(p, indent, false);
    }

    public void nameAnalysis(SymTable st) {
        System.out.println("Fnbody nam");
        myDeclList.nameAnalysis(st);
        st.print();
        myStmtList.nameAnalysis(st);
    }

    // 2 kids
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class StmtListNode extends ASTnode {
    public StmtListNode(List<StmtNode> S) {
        myStmts = S;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        Iterator<StmtNode> it = myStmts.iterator();
        while (it.hasNext()) {
            it.next().unparse(p, indent, isDecl);
        }
    }

    public void nameAnalysis(SymTable st) {
        Iterator it = myStmts.iterator();
        try {
            while (it.hasNext()) {
                ((StmtNode) it.next()).nameAnalysis(st);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    }

    // list of kids (StmtNodes)
    private List<StmtNode> myStmts;
}

class ExpListNode extends ASTnode {
    public ExpListNode(List<ExpNode> S) {
        myExps = S;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        Iterator<ExpNode> it = myExps.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent, isDecl);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent, isDecl);
            }
        } 
    }
    
    public void nameAnalysis(SymTable st) {
        Iterator it = myExps.iterator();
        try {
            while (it.hasNext()) {
                ((ExpNode) it.next()).nameAnalysis(st);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    }

    // list of kids (ExpNodes)
    private List<ExpNode> myExps;
}

// **********************************************************************
// <<<DeclNode and its subclasses>>>
// **********************************************************************

abstract class DeclNode extends ASTnode {
}

class VarDeclNode extends DeclNode {
    public VarDeclNode(TypeNode type, IdNode id, int size) {
        myType = type;
        myId = id;
        mySize = size;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        addIndent(p, indent);
        myType.unparse(p, 0, isDecl);
        p.print(" ");
        myId.unparse(p, 0, isDecl);
        p.println(";");
    }

    public void nameAnalysis(SymTable st){
        MySym mySym = new MySym(myType.getType(), Kind.VAR);

        try {
            System.out.print("add new var ");
            System.out.println(myId.getMyStrVal());
            st.addDecl(myId.getMyStrVal(), mySym);
            myId.link(mySym);
        } catch(EmptySymTableException e) {
            ErrMsg.fatal(myId.getLineNum(), 
                         myId.getCharNum(), 
                         "SymTable empty");
        } catch(DuplicateSymException e) {
            ErrMsg.fatal(myId.getLineNum(), 
                         myId.getCharNum(), 
                         "Multiply declared identifier");
        } catch(WrongArgumentException e){
            ErrMsg.fatal(myId.getLineNum(), 
                         myId.getCharNum(), 
                         e.getMessage());
        }


    }

    // 3 kids
    private TypeNode myType;
    private IdNode myId;
    private int mySize;  // use value NOT_STRUCT if this is not a struct type

    public static int NOT_STRUCT = -1;
}

class FnDeclNode extends DeclNode {
    public FnDeclNode(TypeNode type,
                      IdNode id,
                      FormalsListNode formalList,
                      FnBodyNode body) {
        myType = type;
        myId = id;
        myFormalsList = formalList;
        myBody = body;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        addIndent(p, indent);
        myType.unparse(p, 0, isDecl);
        p.print(" ");
        myId.unparse(p, 0, isDecl);
        p.print("(");
        myFormalsList.unparse(p, 0, isDecl);
        p.println(") {");
        myBody.unparse(p, indent+4, isDecl);
        p.println("}\n");
    }

    public void nameAnalysis(SymTable st) {
        MyFuncSym mySym = new MyFuncSym(myType.getType(), Kind.FUNC);

        try {
            System.out.print("add new func ");
            System.out.println(myId.getMyStrVal());
            st.addDecl(myId.getMyStrVal(), mySym);
            myId.link(mySym);
        } catch(EmptySymTableException e) {
            ErrMsg.fatal(myId.getLineNum(), 
                         myId.getCharNum(), 
                         "SymTable empty");
        } catch(DuplicateSymException e) {
            ErrMsg.fatal(myId.getLineNum(), 
                         myId.getCharNum(), 
                         "Multiply declared identifier");
        } catch(WrongArgumentException e){
            ErrMsg.fatal(myId.getLineNum(), 
                         myId.getCharNum(), 
                         e.getMessage());
        }

        st.addScope();
        myFormalsList.nameAnalysis(st);
        mySym.setFormalTypeList(myFormalsList.getFormalTypeList());
        myBody.nameAnalysis(st);
        st.print();
        try {
            st.removeScope();
        } catch(EmptySymTableException e) {
            ErrMsg.fatal(myId.getLineNum(), 
                         myId.getCharNum(), 
                         "SymTable empty");
        }
        st.print();
    }

    // 4 kids
    private TypeNode myType;
    private IdNode myId;
    private FormalsListNode myFormalsList;
    private FnBodyNode myBody;
}

class FormalDeclNode extends DeclNode {
    public FormalDeclNode(TypeNode type, IdNode id) {
        myType = type;
        myId = id;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        myType.unparse(p, 0, isDecl);
        p.print(" ");
        myId.unparse(p, 0, isDecl);
    }

    public void nameAnalysis(SymTable st) {
        MySym mySym = new MySym(myType.getType(), Kind.VAR);

        try {
            System.out.print("add new var ");
            System.out.println(myId.getMyStrVal());
            st.addDecl(myId.getMyStrVal(), mySym);
            myId.link(mySym);
        } catch(EmptySymTableException e) {
            ErrMsg.fatal(myId.getLineNum(), 
                         myId.getCharNum(), 
                         "SymTable empty");
        } catch(DuplicateSymException e) {
            ErrMsg.fatal(myId.getLineNum(), 
                         myId.getCharNum(), 
                         "Multiply declared identifier");
        } catch(WrongArgumentException e){
            ErrMsg.fatal(myId.getLineNum(), 
                         myId.getCharNum(), 
                         e.getMessage());
        }
    }

    String getType() {
        return myType.getType();
    }

    // 2 kids
    private TypeNode myType;
    private IdNode myId;
}

class StructDeclNode extends DeclNode {
    public StructDeclNode(IdNode id, DeclListNode declList) {
        myId = id;
        myDeclList = declList;
        mySymTable = new SymTable();
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        addIndent(p, indent);
        p.print("struct ");
        myId.unparse(p, 0, isDecl);
        p.println(" {");
        myDeclList.unparse(p, indent+4, isDecl);
        addIndent(p, indent);
        p.println("};");

    }

    public void nameAnalysis(SymTable st) {
        MySym mySym = new MySym("struct", Kind.STRUCT);
        try {
            System.out.print("add new var ");
            System.out.println(myId.getMyStrVal());
            st.addDecl(myId.getMyStrVal(), mySym);
            myId.link(mySym);
        } catch (EmptySymTableException e) {
            ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(), "SymTable empty");
        } catch (DuplicateSymException e) {
            ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(), "Multiply declared identifier");
        } catch (WrongArgumentException e) {
            ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(), e.getMessage());
        }

        myDeclList.nameAnalysis(mySymTable);
        st.print();
        mySymTable.print();
    }

    // 2 kids
    private IdNode myId;
    private DeclListNode myDeclList;
    private SymTable mySymTable;
}

// **********************************************************************
// <<<TypeNode and its Subclasses>>>
// **********************************************************************

abstract class TypeNode extends ASTnode {
    abstract public String getType();
}

class IntNode extends TypeNode {
    public IntNode() {
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        p.print("int");
    }

    public void nameAnalysis(SymTable st) {

    }

    public String getType() {
        return "int";
    }
}

class BoolNode extends TypeNode {
    public BoolNode() {
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        p.print("bool");
    }

    public void nameAnalysis(SymTable st) {

    }

    public String getType() {
        return "bool";
    }
}

class VoidNode extends TypeNode {
    public VoidNode() {
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        p.print("void");
    }
    
    public void nameAnalysis(SymTable st) {

    }
    
    public String getType() {
        return "void";
    }
}

class StructNode extends TypeNode {
    public StructNode(IdNode id) {
        myId = id;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        p.print("struct ");
        myId.unparse(p, 0, isDecl);
    }
    
    public void nameAnalysis(SymTable st) {

    }
    
    public String getType() {
        return myId.getMyStrVal();
    }
    
    // 1 kid
    private IdNode myId;
}

// **********************************************************************
// <<<StmtNode and its subclasses>>>
// **********************************************************************

abstract class StmtNode extends ASTnode {
}

class AssignStmtNode extends StmtNode {
    public AssignStmtNode(AssignNode assign) {
        myAssign = assign;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        addIndent(p, indent);
        myAssign.unparse(p, -1, isDecl); // no parentheses
        p.println(";");
    }

    public void nameAnalysis(SymTable st) {
        System.out.println("ASS nam");
        myAssign.nameAnalysis(st);
    }

    // 1 kid
    private AssignNode myAssign;
}

class PostIncStmtNode extends StmtNode {
    public PostIncStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        addIndent(p, indent);
        myExp.unparse(p, 0, isDecl);
        p.println("++;");
    }

    public void nameAnalysis(SymTable st) {
        System.out.println("INC nam");
        myExp.nameAnalysis(st);
    }

    // 1 kid
    private ExpNode myExp;
}

class PostDecStmtNode extends StmtNode {
    public PostDecStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        addIndent(p, indent);
        myExp.unparse(p, 0, isDecl);
        p.println("--;");
    }

    public void nameAnalysis(SymTable st) {
        myExp.nameAnalysis(st);
    }
    // 1 kid
    private ExpNode myExp;
}

class ReadStmtNode extends StmtNode {
    public ReadStmtNode(ExpNode e) {
        myExp = e;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        addIndent(p, indent);
        p.print("cin >> ");
        myExp.unparse(p, 0, isDecl);
        p.println(";");
    }

    public void nameAnalysis(SymTable st) {
        myExp.nameAnalysis(st);
    }

    // 1 kid (actually can only be an IdNode or an ArrayExpNode)
    private ExpNode myExp;
}

class WriteStmtNode extends StmtNode {
    public WriteStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        addIndent(p, indent);
        p.print("cout << ");
        myExp.unparse(p, 0, isDecl);
        p.println(";");
    }

    public void nameAnalysis(SymTable st) {
        myExp.nameAnalysis(st);
    }

    // 1 kid
    private ExpNode myExp;
}

class IfStmtNode extends StmtNode {
    public IfStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myDeclList = dlist;
        myExp = exp;
        myStmtList = slist;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        addIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, 0, isDecl);
        p.println(") {");
        myDeclList.unparse(p, indent+4, isDecl);
        myStmtList.unparse(p, indent+4, isDecl);
        addIndent(p, indent);
        p.println("}");
    }

    public void nameAnalysis(SymTable st) {
        myExp.nameAnalysis(st);
        myDeclList.nameAnalysis(st);
        myStmtList.nameAnalysis(st);
    }

    // e kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class IfElseStmtNode extends StmtNode {
    public IfElseStmtNode(ExpNode exp, DeclListNode dlist1,
                          StmtListNode slist1, DeclListNode dlist2,
                          StmtListNode slist2) {
        myExp = exp;
        myThenDeclList = dlist1;
        myThenStmtList = slist1;
        myElseDeclList = dlist2;
        myElseStmtList = slist2;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        addIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, 0, isDecl);
        p.println(") {");
        myThenDeclList.unparse(p, indent+4, isDecl);
        myThenStmtList.unparse(p, indent+4, isDecl);
        addIndent(p, indent);
        p.println("}");
        addIndent(p, indent);
        p.println("else {");
        myElseDeclList.unparse(p, indent+4, isDecl);
        myElseStmtList.unparse(p, indent+4, isDecl);
        addIndent(p, indent);
        p.println("}");        
    }

    public void nameAnalysis(SymTable st) {
        myExp.nameAnalysis(st);
        myThenDeclList.nameAnalysis(st);
        myThenStmtList.nameAnalysis(st);
        myElseDeclList.nameAnalysis(st);
        myElseStmtList.nameAnalysis(st);
    }

    // 5 kids
    private ExpNode myExp;
    private DeclListNode myThenDeclList;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
    private DeclListNode myElseDeclList;
}

class WhileStmtNode extends StmtNode {
    public WhileStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }
    
    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        addIndent(p, indent);
        p.print("while (");
        myExp.unparse(p, 0, isDecl);
        p.println(") {");
        myDeclList.unparse(p, indent+4, isDecl);
        myStmtList.unparse(p, indent+4, isDecl);
        addIndent(p, indent);
        p.println("}");
    }

    public void nameAnalysis(SymTable st) {
        myExp.nameAnalysis(st);
        myDeclList.nameAnalysis(st);
        myStmtList.nameAnalysis(st);
    }

    // 3 kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class RepeatStmtNode extends StmtNode {
    public RepeatStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }
	
    public void unparse(PrintWriter p, int indent, boolean isDecl) {
	addIndent(p, indent);
        p.print("repeat (");
        myExp.unparse(p, 0, isDecl);
        p.println(") {");
        myDeclList.unparse(p, indent+4, isDecl);
        myStmtList.unparse(p, indent+4, isDecl);
        addIndent(p, indent);
        p.println("}");
    }

    public void nameAnalysis(SymTable st) {
        myExp.nameAnalysis(st);
        myDeclList.nameAnalysis(st);
        myStmtList.nameAnalysis(st);
    }
    // 3 kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class CallStmtNode extends StmtNode {
    public CallStmtNode(CallExpNode call) {
        myCall = call;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        addIndent(p, indent);
        myCall.unparse(p, indent, isDecl);
        p.println(";");
    }

    public void nameAnalysis(SymTable st) {
        myCall.nameAnalysis(st);
    }

    // 1 kid
    private CallExpNode myCall;
}

class ReturnStmtNode extends StmtNode {
    public ReturnStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        addIndent(p, indent);
        p.print("return");
        if (myExp != null) {
            p.print(" ");
            myExp.unparse(p, 0, isDecl);
        }
        p.println(";");
    }

    public void nameAnalysis(SymTable st) {
        myExp.nameAnalysis(st);
    }

    // 1 kid
    private ExpNode myExp; // possibly null
}

// **********************************************************************
// <<<ExpNode and its subclasses>>>
// **********************************************************************

abstract class ExpNode extends ASTnode {
}

class IntLitNode extends ExpNode {
    public IntLitNode(int lineNum, int charNum, int intVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myIntVal = intVal;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        p.print(myIntVal);
    }

    public void nameAnalysis(SymTable st) {

    }

    private int myLineNum;
    private int myCharNum;
    private int myIntVal;
}

class StringLitNode extends ExpNode {
    public StringLitNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        p.print(myStrVal);
    }

    public void nameAnalysis(SymTable st) {

    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
}

class TrueNode extends ExpNode {
    public TrueNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        p.print("true");
    }

    public void nameAnalysis(SymTable st) {

    }

    private int myLineNum;
    private int myCharNum;
}

class FalseNode extends ExpNode {
    public FalseNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        p.print("false");
    }

    public void nameAnalysis(SymTable st) {

    }

    private int myLineNum;
    private int myCharNum;
}

class IdNode extends ExpNode {
    public IdNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        p.print(myStrVal);
        System.out.println("ID "+getMyStrVal());
        if (!isDecl) {
            if (mySym == null){
                System.out.println("ID " + getMyStrVal());
            }
            p.print("(");
            p.print(mySym.getType());
            p.print(")");
        }

    }

    public void nameAnalysis(SymTable st) {
        System.out.println("ID nam");
        MySym sym = st.lookupGlobal(myStrVal);
        if (sym == null){
            ErrMsg.fatal(myLineNum, myCharNum, 
                         "Undeclared identifier");
        }
        else {
            this.link(sym);
        }
    }

    public String getMyStrVal() {
        return myStrVal;
    }

    public int getLineNum(){
        return myLineNum;
    }

    public int getCharNum(){
        return myCharNum;
    }

    public void link(MySym sym){
        System.out.print(getMyStrVal() + " linked ");
        mySym = sym;
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
    private MySym mySym;
}

class DotAccessExpNode extends ExpNode {
    public DotAccessExpNode(ExpNode loc, IdNode id) {
        myLoc = loc;    
        myId = id;
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        myLoc.unparse(p, 0, isDecl);
        p.print(".");
        myId.unparse(p, 0, isDecl);
    }

    public void nameAnalysis(SymTable st) {
        myLoc.nameAnalysis(st);
        SymTable field = getStructField(myLoc);
        myId.nameAnalysis(field);
    }

    public getStructField(ExpNode loc){
        
    }

    // 2 kids
    private ExpNode myLoc;    
    private IdNode myId;
}

class AssignNode extends ExpNode {
    public AssignNode(ExpNode lhs, ExpNode exp) {
        myLhs = lhs;
        myExp = exp;
    }
    
    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        System.out.println("ASSIGN");
        if (indent != -1)  p.print("(");
        myLhs.unparse(p, 0, isDecl);

        p.print(" = ");
        myExp.unparse(p, 0, isDecl);
        if (indent != -1)  p.print(")");
    }

    public void nameAnalysis(SymTable st) {
        myLhs.nameAnalysis(st);
        myExp.nameAnalysis(st);
    }

    // 2 kids
    private ExpNode myLhs;
    private ExpNode myExp;
}

class CallExpNode extends ExpNode {
    public CallExpNode(IdNode name, ExpListNode elist) {
        myId = name;
        myExpList = elist;
    }

    public CallExpNode(IdNode name) {
        myId = name;
        myExpList = new ExpListNode(new LinkedList<ExpNode>());
    }

    // ** unparse **
    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        myId.unparse(p, 0, isDecl);
        p.print("(");
        if (myExpList != null) {
            myExpList.unparse(p, 0, isDecl);
        }
        p.print(")");
    }

    public void nameAnalysis(SymTable st) {
        System.out.println("Call name");
        myId.nameAnalysis(st);
        myExpList.nameAnalysis(st);
    }

    // 2 kids
    private IdNode myId;
    private ExpListNode myExpList;  // possibly null
}

abstract class UnaryExpNode extends ExpNode {
    public UnaryExpNode(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalysis(SymTable st) {

    }

    // one child
    protected ExpNode myExp;
}

abstract class BinaryExpNode extends ExpNode {
    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
        myExp1 = exp1;
        myExp2 = exp2;
    }

    // two kids
    protected ExpNode myExp1;
    protected ExpNode myExp2;
    protected String binaryOp;
    
    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        p.print("(");
        myExp1.unparse(p, 0, isDecl);
        p.print(" " + binaryOp + " ");
        myExp2.unparse(p, 0, isDecl);
        p.print(")");
    }

    public void nameAnalysis(SymTable st) {
        myExp1.nameAnalysis(st);
        myExp2.nameAnalysis(st);
        System.out.println(binaryOp);
    }

}

// **********************************************************************
// <<<Subclasses of UnaryExpNode>>>
// **********************************************************************

class UnaryMinusNode extends UnaryExpNode {
    public UnaryMinusNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        p.print("(-");
        myExp.unparse(p, 0, isDecl);
        p.print(")");
    }

    public void nameAnalysis(SymTable st) {
        myExp.nameAnalysis(st);
    }
}

class NotNode extends UnaryExpNode {
    public NotNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent, boolean isDecl) {
        p.print("(!");
        myExp.unparse(p, 0, isDecl);
        p.print(")");
    }

    public void nameAnalysis(SymTable st) {
        myExp.nameAnalysis(st);
    }
}

// **********************************************************************
// <<<Subclasses of BinaryExpNode>>>
// **********************************************************************

class PlusNode extends BinaryExpNode {
    public PlusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
        binaryOp = "+";
    }
}

class MinusNode extends BinaryExpNode {
    public MinusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
        binaryOp = "-";
    }
}

class TimesNode extends BinaryExpNode {
    public TimesNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
        binaryOp = "*";
    }
}

class DivideNode extends BinaryExpNode {
    public DivideNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
        binaryOp = "/";
    }
}

class AndNode extends BinaryExpNode {
    public AndNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
        binaryOp = "&&";
    }
}

class OrNode extends BinaryExpNode {
    public OrNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
        binaryOp = "||";
    }
}

class EqualsNode extends BinaryExpNode {
    public EqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
        binaryOp = "==";
    }
}

class NotEqualsNode extends BinaryExpNode {
    public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
        binaryOp = "!=";
    }
}

class LessNode extends BinaryExpNode {
    public LessNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
        binaryOp = "<";
    }
}

class GreaterNode extends BinaryExpNode {
    public GreaterNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
        binaryOp = ">";
    }
}

class LessEqNode extends BinaryExpNode {
    public LessEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
        binaryOp = "<=";
    }
}

class GreaterEqNode extends BinaryExpNode {
    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
        binaryOp = ">=";
    }
}
