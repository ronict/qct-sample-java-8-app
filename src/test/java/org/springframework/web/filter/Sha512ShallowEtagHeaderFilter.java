package org.springframework.web.filter;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Custom implementation of a shallow ETag filter that uses SHA-512 for ETag generation.
 * This filter generates an ETag header based on the response content, using SHA-512 hashing
 * algorithm instead of the default MD5 used by Spring's ShallowEtagHeaderFilter.
 */
public class Sha512ShallowEtagHeaderFilter implements Filter {

    private static final String HEADER_ETAG = "ETag";
    private static final String HEADER_IF_NONE_MATCH = "If-None-Match";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

	@Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestETag = httpRequest.getHeader(HEADER_IF_NONE_MATCH);
        
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);
        
        chain.doFilter(request, responseWrapper);
        
        byte[] responseBody = responseWrapper.getContentAsByteArray();
        String responseETag = generateETagHeaderValue(responseBody);
        
        if (responseETag.equals(requestETag)) {
            httpResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        } else {
            httpResponse.setHeader(HEADER_ETAG, responseETag);
            responseWrapper.copyBodyToResponse();
        }
    }

    @Override
    public void destroy() {
        // No resources to release
    }

    /**
     * Generates an ETag header value from the given response body byte array.
     * This implementation uses SHA-512 instead of the default MD5 algorithm.
     *
     * @param responseBody the response body byte array
     * @return the ETag header value
     */
    protected String generateETagHeaderValue(byte[] responseBody) {
        final HashCode hash = Hashing.sha512().hashBytes(responseBody);
		return "\"" + hash + "\"";
	}
}
