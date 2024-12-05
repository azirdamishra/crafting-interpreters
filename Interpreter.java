package jlox;

import javax.swing.*;
import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{

    private Environment environment = new Environment(); //to keep the variables in memory

    void interpret(List<Stmt> statements){
        try{
            for(Stmt statement : statements){
                execute(statement);
            }
        } catch (RuntimeError error){
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr){
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr){
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr){
        Object right = evaluate(expr.right); //operand expression is evaluated first

        switch (expr.operator.type) {
            case BANG -> {
                return !isTruthy(right);
            }
            case MINUS -> {
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            }
        }
        //unreachable
        return null;
    }

    private void checkNumberOperand(Token operator, Object operand){
        if( operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperand(Token operator, Object left, Object right){
        if(left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object){
        if(object == null ) return false;
        if (object instanceof Boolean){
            return (boolean)object;
        }
        return true;
    }

    private Object evaluate(Expr expr){ //helper method, sends the expression back inot the interpreter's visitor implementation
        return expr.accept(this);
    }

    private void execute(Stmt stmt){ //statement analogue for evaluate() for expressions
        stmt.accept(this);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr){
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type){
            case GREATER -> {
                checkNumberOperand(expr.operator, left, right);
                return (double)left > (double) right;
            }
            case GREATER_EQUAL -> {
                checkNumberOperand(expr.operator, left, right);
                return (double)left >= (double) right;
            }
            case LESS -> {
                checkNumberOperand(expr.operator, left, right);
                return (double)left < (double) right;
            }
            case LESS_EQUAL -> {
                checkNumberOperand(expr.operator, left, right);
                return (double)left <= (double)right;
            }
            case BANG_EQUAL -> {
                checkNumberOperand(expr.operator, left, right);
                //System.out.println("printing != " + left == right);
                return !isEqual(left, right);
            }
            case EQUAL_EQUAL -> {
                checkNumberOperand(expr.operator, left, right);
                //System.out.println("printing == " + left == right);
                return isEqual(left, right);
            }
            case MINUS -> {
                checkNumberOperand(expr.operator, left, right);
                return (double)left - (double)right;
            }
            case PLUS -> { //special case since + ois overloaded for numbers and strings
                if (left instanceof Double && right instanceof Double){
                    return (double)left + (double)right;
                }

                if(left instanceof String && right instanceof String){ //check then case, should there be a try, catch?
                    return (String)left + (String)right;
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            }
            case SLASH -> {
                checkNumberOperand(expr.operator, left, right);
                return (double)left / (double)right;
            }
            case STAR -> {
                checkNumberOperand(expr.operator, left, right);
                return (double)left * (double)right;
            }
        }
        //unreachable
        return null;
    }

    private boolean isEqual(Object a, Object b){ //representing Lox objects in terms of Java, implementing Lox's notion of equality vs Java's notion
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private String stringify(Object object){
        if (object == null) return "nil";

        if (object instanceof Double){
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length()-2);
            }
            return text;
        }

        return object.toString();
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        /*
        var a;
        print a; //"nil"
         */
        Object value = null; //could make not defining a runtime error but chose to keep non-initialized value as null
        if(stmt.initializer != null){
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value); //binding the variable to that value
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }
}
