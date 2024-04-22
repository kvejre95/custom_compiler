package edu.ufl.cise.plpfa22;


import java.util.ArrayList;

public class Token implements IToken{

    Kind kind;
    SourceLocation s;
    int column_number, line_number;
    String value;

    ArrayList<Character> ActualText = new ArrayList<Character>();


//    public Token(Kind kind, int column_number, int line_number, String value){
//        this.kind = kind;
//        this.column_number = column_number;
//        this.line_number = line_number;
//        this.value = value;
//    }

    public void setKind(Kind kind){
        this.kind = kind;
    }

    public void setLocation(int column_number, int line_number){
        this.column_number = column_number;
        this.line_number = line_number;
        s = new SourceLocation(line_number,column_number);
    }

    public void setValue(String value){
        this.value = value;
    }

    @Override
    public Kind getKind() {
        return this.kind;
    }

    @Override
    public char[] getText() {
        char[] myCharArray = new char[ActualText.size()];
        for(int i = 0; i < ActualText.size(); i++) {
            myCharArray[i] = ActualText.get(i);
        }
        return  myCharArray;
        //return value.toCharArray();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return s;
    }

    @Override
    public int getIntValue() {
        return Integer.parseInt(value);
    }

    @Override
    public boolean getBooleanValue() {
        return Boolean.parseBoolean(value);
    }

    @Override
    public String getStringValue() {
        return value;
    }
}
