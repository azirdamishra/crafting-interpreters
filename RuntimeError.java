package jlox;

public class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token token, String message) { //tracks the token that identifies where the runtime error came from
        super(message);
        this.token = token;
    }

}
