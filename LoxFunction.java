package jlox;

import java.util.List;

public class LoxFunction implements LoxCallable{

    private final Stmt.Function declaration;

    LoxFunction( Stmt.Function declaration ) {
        this.declaration = declaration;
    }

    @Override
    public String toString(){ //gives a better looking output if user decides to print a function value
        return "<fn " + declaration.name.lexeme + ">"; 
    }

    @Override
    public int arity(){
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments){ //fundamental pieces of code for our interpreter
        Environment environment = new Environment(interpreter.globals);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        interpreter.executeBlock(declaration.body, environment);
        return null;
    }
    
    
}
