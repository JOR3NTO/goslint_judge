package co.uceva.auth.domain.model;

public class AuthToken {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public Long getExpiresIn() { return expiresIn; }

    public static AuthTokenBuilder builder() {
        return new AuthTokenBuilder();
    }

    public static class AuthTokenBuilder {
        private String accessToken;
        private String refreshToken;
        private Long expiresIn;

        public AuthTokenBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public AuthTokenBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public AuthTokenBuilder expiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public AuthToken build() {
            AuthToken authToken = new AuthToken();
            authToken.accessToken = this.accessToken;
            authToken.refreshToken = this.refreshToken;
            authToken.expiresIn = this.expiresIn;
            return authToken;
        }
    }
}
