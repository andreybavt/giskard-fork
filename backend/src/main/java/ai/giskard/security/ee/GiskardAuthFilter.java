package ai.giskard.security.ee;

import ai.giskard.security.ee.jwt.JWTFilter;
import ai.giskard.security.ee.jwt.TokenProvider;
import ai.giskard.service.ee.FeatureFlag;
import ai.giskard.service.ee.LicenseService;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * This filter is applied for every request and will check for authentication.
 * It will check if authentication is enabled on the instance and pick between the admin-by-default filter or the JWT filter
 */
public class GiskardAuthFilter extends GenericFilterBean {

    private final LicenseService licenseService;

    private final JWTFilter jwtFilter;
    private final NoAuthFilter noAuthFilter;
    private final NoLicenseAuthFilter noLicenseAuthFilter;

    public GiskardAuthFilter(LicenseService licenseService, TokenProvider tokenProvider) {
        this.licenseService = licenseService;

        this.jwtFilter = new JWTFilter(tokenProvider);
        this.noAuthFilter = new NoAuthFilter();
        this.noLicenseAuthFilter = new NoLicenseAuthFilter();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!this.licenseService.getCurrentLicense().isActive()) {
            this.noLicenseAuthFilter.doFilter(request, response, chain);
        } else if (this.licenseService.hasFeature(FeatureFlag.AUTH)) {
            this.jwtFilter.doFilter(request, response, chain);
        } else {
            this.noAuthFilter.doFilter(request, response, chain);
        }
    }
}
