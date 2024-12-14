package jlox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment(){ //this constructor is for the global scope's env, which ends the chain
        enclosing = null;
    }

    Environment(Environment enclosing){ //creates new local scope nested outside the outer one
        this.enclosing = enclosing;
    }

    Object get(Token name){ //follows the chain recursively
        if(values.containsKey(name.lexeme)){
            return values.get(name.lexeme);
        }

        if(enclosing != null){
            return enclosing.get(name);
        }

        throw new RuntimeError(name,
                "Undefined variable '"+name.lexeme+"'.");
    }

    void assign(Token name, Object value){
        /*
        key difference between assignment and defn is that assignment is that
        assignment is anot allowed to create a new variable -> its a runtime error
        if the key doesn't already exist in the env's var map
         */

        if(values.containsKey(name.lexeme)){
            values.put(name.lexeme, value);
            return;
        }

        if(enclosing != null){
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name,
                "Undefined variable '"+name.lexeme+"'.");
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

}
