package pl.edu.pw.elka.polishentitylinker.core;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TaggedTextIteratorTest {

    private final TaggedTextIterator taggedTextProcessor = new TaggedTextIterator();
    private final String path = "src/main/resources/final-filtered.tsv";

    @Before
    public void setUp() {
        taggedTextProcessor.processFile(path);
    }

    @Test
    public void shouldFindFirstEntity() {
        assertNotNull(taggedTextProcessor.getNamedEntities());
    }

    @Test
    public void shouldExtractNamedEntities() {
        assertEquals(3195, taggedTextProcessor.getNamedEntities().size());
    }


}
