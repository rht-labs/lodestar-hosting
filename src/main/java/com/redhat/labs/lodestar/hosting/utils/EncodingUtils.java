package com.redhat.labs.lodestar.hosting.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.redhat.labs.lodestar.hosting.exception.EncodingException;
import com.redhat.labs.lodestar.hosting.exception.HostingException;

public class EncodingUtils {
    
    private EncodingUtils() {}

    public static byte[] base64Encode(byte[] src) {
        return Base64.getEncoder().encode(src);
    }

    public static byte[] base64Decode(String src) {
        return Base64.getDecoder().decode(src);
    }

    public static String urlEncode(String src) {
        if(src == null) {
            throw new HostingException("URL encoding error. Input is null");
        }
        
        try {
            return URLEncoder.encode(src, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new EncodingException(String.format("failed to encode src %s", src));
        }
    }

    public static String urlDecode(String src) {
        if(src == null) {
            throw new HostingException("URL decoding error. Input is null");
        }
        
        try {
            return URLDecoder.decode(src, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new EncodingException(String.format("failed to decode src %s", src));
        }
    }

}
