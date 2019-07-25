package com.emc.mongoose.storage.driver.coop.netty;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public interface SslUtil {

	static SslContext sslContext(final String[] protocols, final SslProvider provider) {
		try {
			return SslContextBuilder
							.forClient()
							.trustManager(InsecureTrustManagerFactory.INSTANCE)
							.sslProvider(provider)
							.protocols(protocols)
							.ciphers(Arrays.asList(SSLContext.getDefault().getServerSocketFactory().getSupportedCipherSuites()))
							.build();
		} catch (final NoSuchAlgorithmException | SSLException e) {
			throw new AssertionError(e);
		}
	}
}
