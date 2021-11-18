package com.google.fhir.proxy;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/*")
public class FhirProxyServer extends RestfulServer {

  private static final Logger logger = LoggerFactory.getLogger(FhirProxyServer.class);

  private static final String PROXY_TO_ENV = "PROXY_TO";
  private static final String TOKEN_ISSUER_ENV = "TOKEN_ISSUER";
  private static final String ACCESS_CHECKER_ENV = "ACCESS_CHECKER";
  // TODO add a TEST_MODE config to control whether some permissive settings (like letting
  // all requests to go through) are acceptable or not.

  @Override
  protected void initialize() throws ServletException {
    logger.info("Adding BearerAuthorizationInterceptor ");
    String gcpFhirStore = System.getenv(PROXY_TO_ENV);
    if (gcpFhirStore == null) {
      throw new ServletException(
          String.format("The environment variable %s is not set!", PROXY_TO_ENV));
    }
    String tokenIssuer = System.getenv(TOKEN_ISSUER_ENV);
    if (tokenIssuer == null) {
      throw new ServletException(
          String.format("The environment variable %s is not set!", TOKEN_ISSUER_ENV));
    }

    // Create a context for the appropriate version
    setFhirContext(FhirContext.forR4());

    try {
      PatientAccessCheckerFactory factory = new PermissiveAccessChecker.Factory();
      String accessCheckerType = System.getenv(ACCESS_CHECKER_ENV);
      if (accessCheckerType != null && !accessCheckerType.isEmpty()) {
        logger.info(String.format("Patient access-checker is '%s'", accessCheckerType));
        // Currently this is the only non-trivial checker, hence not caring about the env-var value.
        factory = new PatientListAccessChecker.Factory();
      } else {
        logger.warn(String.format(
            "Environment variable %s is not set; disabling Patient access-checker!",
            ACCESS_CHECKER_ENV));
      }
      registerInterceptor(
          new BearerAuthorizationInterceptor(gcpFhirStore, tokenIssuer, this, factory));
    } catch (IOException e) {
      ExceptionUtil.throwRuntimeExceptionAndLog(logger, "IOException while initializing", e);
    }
  }
}