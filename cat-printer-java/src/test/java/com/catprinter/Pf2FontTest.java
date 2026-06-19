package com.catprinter;

import com.catprinter.text.Pf2Font;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Pf2FontTest {

    @Test
    void rejectsTooShortInput() throws IOException {
        Pf2Font f = Pf2Font.fromBytes(new byte[] {1, 2, 3}, 1, '?');
        assertTrue(f.isBroken());
    }

    @Test
    void rejectsBadMagic() throws IOException {
        byte[] bogus = new byte[] {
            'X', 'X', 'X', 'X', 0, 0, 0, 4, 'P', 'F', 'F', '2'
        };
        Pf2Font f = Pf2Font.fromBytes(bogus, 1, '?');
        assertTrue(f.isBroken());
    }
}
