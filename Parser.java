package jlox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static jlox.TokenType.*;

/*
Lox grammar rules:

expression -> equality;
equality -> comparison ( ( "!=" | "==" ) comparison )* ;
comparison -> term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term -> factor ( ( "-" | "+" ) unary )* ;
factor -> unary ( ( "/" | "*" ) unary )* ;
unary -> ( "!" | "-" ) unary | primary ;
primary -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER ;

Rules for kinds of statements that declare names (Variable declarations and assignments)

program -> declaration* EOF ;
declaration -> varDecl | statement ;
statement -> exprStmt | printStmt ;

    Rule for declaring a variable

varDecl -> "var" IDENTIFIER ( "=" expression )? ";" ;

Rules for assignment '='

expression -> assignment;
assignment -> IDENTIFIER "=" assignment | equality;

Adding blocks to the environment:

statement -> exprStmt | printStmt | block;
block -> "{" declaration* "}" ;

Adding conditional operators:

statement -> exprStmt | ifStmt | printStmt | block;
ifStmt -> "if" "(" expression ")" statement
            ( "else" statement )? ;

Adding logical operators:

expression -> assignment;
assignment -> IDENTIFIER "=" assignment | logic_or ;
logic_or -> logic_and ( "or" logic_and )* ;
logic_and -> equality ( "and" equality )* ;

Adding while loop:
statement -> exprStmt | ifStmt | printStmt | whileStmt | block ;
whileStmt -> "while" "(" expression ")" statement ;

Adding for loops:
statement -> exprStmt | forStmt | ifStmt | printStmt | whileStmt | block ;
forStmt -> "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement ;

 */

public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

//    Expr parse(){
//        try{
//            return expression();
//        }catch (ParseError error){
//            return null;
//        }
//    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()){
            //statements.add(statement());
            statements.add(declarations()); //entry point method to the parser
        }

        return statements;
    }

    private Expr expression(){
        return assignment();
    }

    private Stmt declarations() {
        try{
            if (match(VAR)) return  varDeclaration();

            return statement();
        } catch (ParseError error){
            synchronize();
            return null;
        }
    }

    private Stmt statement(){
        if(match(FOR)) return forStatement();
        if(match(IF)) return ifStatement();
        if(match(PRINT)) return printStatement();
        if(match(WHILE)) return whileStatement();
        if(match(LEFT_BRACE)) return new Stmt.Block((block()));
        return expressionStatement();
    }

    private Stmt forStatement(){
        consume(LEFT_PAREN, "Expect '(' after 'for'.)");

        Stmt initializer;
        if(match(SEMICOLON)){
            initializer = null;
        } else if (match(VAR)){
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement(); //so that initializer is always a type of statement 
        }

        Expr condition = null;
        if(!check(SEMICOLON)){
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition."); //to check if the clause has been omitted 

        Expr increment = null;
        if(!check(RIGHT_PAREN)){
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        Stmt body = statement();

        //synthesize syntax tree nodes that express the semantics of the for loop

        //increment 
        if(increment != null){
            body = new Stmt.Block(
                Arrays.asList(
                    body,
                    new Stmt.Expression(increment)
                )
            );
        }

        //building the loop
        if(condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        //initializer to run before the entire loop and wrap the entire process in a Block
        if(initializer != null){
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;

    }

    private Stmt ifStatement(){
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if(match(ELSE)){
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement(){
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt varDeclaration(){
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if(match(EQUAL)){ //post matching the var token as variable initializer
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement(){
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Experct ')' after condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement(){
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block(){
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()){
            statements.add(declarations());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr assignment(){
        //Expr expr = equality(); /* to weave the new expressions into parser, we first change the parsing code for assignment to call or() */
        Expr expr = or();

        if(match(EQUAL)){
            Token equals = previous();
            Expr value = assignment();

            if(expr instanceof Expr.Variable){
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
            /*
             we report an error if the left hand side isn't a valid assignment target
             we don't throw it because the parser is not in a confused state where we need to de-panic and synchronize
             */
        }
        return expr;
    }

    private Expr or(){ //parse a series of or expressions 
        Expr expr = and();

        while(match(OR)){
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and(){
        Expr expr = equality();

        while(match(AND)){
            Token operator = previous();
            Expr right = equality(); //calls equality for its operands
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr equality(){
        Expr expr = comparison();

        while(match(EQUAL_EQUAL, BANG_EQUAL)){
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private boolean match(TokenType... types){
        for(TokenType type : types){
            if(check(type)){
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type){
        if( isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance(){
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd(){
        return peek().type == EOF;
    }

    private Token peek(){
        return tokens.get(current);
    }

    private Token previous(){
        return tokens.get(current - 1);
    }

    private Expr comparison(){
        Expr expr = term();

        while( match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)){
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term(){
        Expr expr = factor();

        while ( match(MINUS, PLUS)){
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor(){
        Expr expr = unary();

        while (match(SLASH, STAR)){
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary(){
        if(match(BANG, MINUS)){
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    private Expr primary(){
        if(match(FALSE)) return new Expr.Literal(false);
        if(match(TRUE)) return new Expr.Literal(true);
        if(match(NIL)) return new Expr.Literal(null);

        if(match(NUMBER, STRING)){
            return new Expr.Literal(previous().literal);
        }

        if(match(IDENTIFIER)){
            return new Expr.Variable(previous());
        }

        if(match(LEFT_PAREN)){
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message){
        if(check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message){
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize(){
        advance();

        while (!isAtEnd()){
            if(previous().type == SEMICOLON) return;

            switch (peek().type){
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }

    }

}

