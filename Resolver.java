package jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void>{
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>(); //Each element in the stack is a Map representing a single block scope. Keys -> Environment, variable names, Value -> Boolean
    //scope stack is only for local block scopes, not global which is more dynamic. When var is not found while resolving, it is assumed global
    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType{
        NONE,
        FUNCTION,
        METHOD
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt){ //begin new scope, traverses into the statements inside the block and then discards the scope
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt){ //plumb the node through the resolver first
        declare(stmt.name);
        define(stmt.name);

        beginScope();
        scopes.peek().put("this", true); //defining "this" as if it were a variable defined in an implicit scope just outsude the block for method body
        
        for(Stmt.Function method: stmt.methods){
            FunctionType declaration = FunctionType.METHOD;
            resolveFunction(method, declaration);
        }

        endScope();

        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt){
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt){
        declare(stmt.name);
        define(stmt.name); //lets a function recursively refer to itself inside its own body

        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt){
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if(stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt){
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt){
        if(currentFunction == FunctionType.NONE){
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }
        if(stmt.value != null){
            resolve(stmt.value);
        }

        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt){
        declare(stmt.name);
        if(stmt.initializer != null){
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt){
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr){
        resolve(expr.value); //resolve for other vars in the expression
        resolveLocal(expr, expr.name); //resolve the var that's being assigned to
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr){
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr){
        resolve(expr.callee);

        for(Expr argument : expr.arguments){
            resolve(argument);
        }

        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr){
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr){
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr){
        return null; //a literal expn doesn't mentinon any variables and doesn't contain any subexpressions
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr){
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr){
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr){
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr){
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr){
        if(!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE){
            Lox.error(expr.name, "Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    //these resolve methods are similar to evaluate() and execute() methods in Interpreter, they turn around and apply the Visitor pattern to the given syntax tree node
    private void resolve(Stmt stmt){
        stmt.accept(this);
    }

    private void resolve(Expr expr){
        expr.accept(this);
    }

    private void resolveFunction(Stmt.Function function, FunctionType type){ //creates a new scope for the body and then binds variables for each of the function's params
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for(Token param: function.params){
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction; //restores the previous function context
    }

    void resolve(List<Stmt> statements){
        for(Stmt statement : statements){
            resolve(statement);
        }
    }

    private void beginScope(){
        scopes.push(new HashMap<String, Boolean>());
    }

    private void endScope(){ //scopes are stored in explicit stack, easy to exit
        scopes.pop();
    }

    private void declare(Token name){ 
        //adds the variable to the innermost scope so that it shadows any outer one and so that we know the variable exists
        if(scopes.isEmpty()) return;

        Map<String, Boolean> scope = scopes.peek();
        if(scope.containsKey(name.lexeme)){
            Lox.error(name, "Already a variable with this name in this scope.");
        }

        scope.put(name.lexeme, false); //as the variable is not ready yet, the boolean value in the scope map represents resolution completion
    }

    private void define(Token name){
        if(scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true); //it was unavailable in declare, and it is initialized here by defining it, its alive now
    }

    private void resolveLocal(Expr expr, Token name){
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if(scopes.get(i).containsKey(name.lexeme)){
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }


}
