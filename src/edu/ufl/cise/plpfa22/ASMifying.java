package edu.ufl.cise.plpfa22;

import org.objectweb.asm.util.ASMifier;

import java.io.IOException;

public class ASMifying {
    public static void main(String[] args) {
        try {
            ASMifier.main(new String[] {"C:\\" +
                    "Java Projects\\Test.class"});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
