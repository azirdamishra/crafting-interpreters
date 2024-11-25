package jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jlox.TokenType.*; //static imports

class Scanner{
    private final String source; //this is the item/object that we are scanning for tokens
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0; //pointer for token finding
    private int current = 0; //overall position of the file getting read on a line
    private int line = 1; //line of the file

    private static final Map<String, TokenType> keywords;

    static{
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);

    }

    Scanner(String source){ //jlox scanner not java scanner
        this.source = source;
    }

    List<Token> scanTokens(){
        while(!isAtEnd()){
            //we are at the beginning of the next lexeme
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken(){
        char c = advance();
        switch (c){
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
            case '/':
                if(match('/')){
                    //A comment goes on until the end of the line
                    while(peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                //ignore whitespace
                break;
            case '\n':
                line++;
                break;
            case '"': string(); break;    //strings should always start with double quotes


            default:
                if (isDigit(c)){
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character '" + c + "'");
                }
                break;
        }
    }

    private void  identifier(){ //checking for reserved characters
        while(isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        System.out.println("print text: " + text);
        TokenType type = keywords.get(text); //checking if the identifier matches anything in the map
        //if it does then we assign and use that keyword's tokentype
        if(type == null){
            type = IDENTIFIER; //otherwise we use the regular user-defined identifier (variable)
        }
        addToken(type);
    }

    private void number(){
        while (isDigit(peek())) advance();

        //look for a fractional part
        if (peek() == '.' && isDigit(peekNext())){
            //consume the "."
            advance();

            while (isDigit(peek())) advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void string(){
        while(peek() != '"' && !isAtEnd()){ //checking for new line whitespaces before the end of the double quote
            if(peek() == '\n') line++;
            advance();
        }

        if(isAtEnd()){
            Lox.error(line, "Unterminated string");
            return;
        }

        advance();

        //Trim the surrounding quotes
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);

    }

    private boolean match(char expected){
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek(){ //sort of like advance() but doesn't consume the character [lookahead]
        //mostly specifically created for using in comments
        if(isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext(){ //this function is required to check if there is a digit after '.' making it a valid decimal
        if(current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c){
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c == '_');
    }

    private boolean isAlphaNumeric(char c){
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c){
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd(){
        return current >= source.length();
    }

    private char advance(){
        return source.charAt(current++);
    }

    private void addToken(TokenType type){
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal){
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}