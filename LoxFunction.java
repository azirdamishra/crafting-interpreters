package jlox;

import java.util.List;

public class LoxFunction implements LoxCallable{

    private final Stmt.Function declaration;
    private final Environment closure;

    private final boolean isInitializer;

    LoxFunction( Stmt.Function declaration, Environment closure, boolean isInitializer ) {
        this.isInitializer = isInitializer;
        this.declaration = declaration;
        this.closure = closure;
    }

    LoxFunction bind(LoxInstance instance){
        /*
         * We create a new env nestled inside the method's original closure "closure-within-a-closure"
         * When the method is called, that will become the parent of the method body's env
         */
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment, isInitializer);
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
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            if(isInitializer) return closure.getAt(0, "this");
            return returnValue.value;
        }

        if(isInitializer) return closure.getAt(0, "this");

        return null;
    }
    
    
}
