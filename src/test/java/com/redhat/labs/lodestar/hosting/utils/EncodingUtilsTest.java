package com.redhat.labs.lodestar.hosting.utils;

import com.redhat.labs.lodestar.hosting.exception.EncodingException;
import com.redhat.labs.lodestar.hosting.model.GitlabFile;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.hosting.exception.HostingException;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class EncodingUtilsTest {

    @Test
    void testEmptyEncodeSource() {
        assertThrows(HostingException.class, () -> {
           EncodingUtils.urlEncode(null); 
        });
    }
    
    @Test
    void testEmptyDecodeSource() {
        assertThrows(HostingException.class, () -> {
           EncodingUtils.urlEncode(null); 
        });
    }

    @Test
    void gitlabFileEncoding() {
        GitlabFile f = GitlabFile.builder().filePath("a.json").build();
        f.encodeFileAttributes();
        assertNull(f.getContent());

        f.decodeFileAttributes();
        assertNull(f.getContent());
    }
    
    @Test
    void testEncodingException() {
        EncodingException ex = new EncodingException("test");
        
        assertEquals("test", ex.getMessage());
    }
}
