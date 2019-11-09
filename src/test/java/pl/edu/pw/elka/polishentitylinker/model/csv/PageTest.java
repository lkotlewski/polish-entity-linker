package pl.edu.pw.elka.polishentitylinker.model.csv;

import org.junit.Test;

import static org.junit.Assert.*;

public class PageTest {

    @Test
    public void shouldParseStandardLine() {
        Page page = new Page("572,Bozony,0,0,3605");
        assertEquals(572, page.getPageId());
        assertEquals("Bozony", page.getTitle());
        assertEquals(PageType.REGULAR_ARTICLE, page.getType());
    }

    @Test
    public void shouldParseLineWithQuots() {
        Page page = new Page("638,\"Boże, coś Polskę\",0,0,7375");
        assertEquals(638, page.getPageId());
        assertEquals("Boże, coś Polskę", page.getTitle());
        assertEquals(PageType.REGULAR_ARTICLE, page.getType());
    }
}
