package xyz.tbvns.Exeptions;

public class NoEmptyConstructor extends RuntimeException {
    public NoEmptyConstructor(String message) {
        super(message);
    }
}
