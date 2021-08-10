package com.redhat.labs.lodestar.hosting.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.hosting.exception.HostingException;

class EncodingUtilisTest {

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
}
