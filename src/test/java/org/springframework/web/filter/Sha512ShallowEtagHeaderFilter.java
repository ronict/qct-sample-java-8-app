package org.springframework.web.filter;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * A filter that generates SHA-512 based ETag headers.
 * Reimplemented for Spring Boot 3.x / Jakarta Servlet compatibility.
 */
public class Sha512ShallowEtagHeaderFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {

		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
		filterChain.doFilter(request, responseWrapper);

		byte[] body = responseWrapper.getContentAsByteArray();
		if (body.length > 0 && isEligibleForEtag(request, responseWrapper)) {
			String eTag = generateETagHeaderValue(body);
			response.setHeader("ETag", eTag);

			String requestETag = request.getHeader("If-None-Match");
			if (eTag.equals(requestETag)) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				response.setContentLength(0);
				return;
			}
		}
		responseWrapper.copyBodyToResponse();
	}

	protected String generateETagHeaderValue(byte[] bytes) {
		final HashCode hash = Hashing.sha512().hashBytes(bytes);
		return "\"" + hash + "\"";
	}

	private boolean isEligibleForEtag(HttpServletRequest request, ContentCachingResponseWrapper response) {
		int status = response.getStatus();
		return status >= 200 && status < 300 && "GET".equals(request.getMethod());
	}
}
