package com.example.mcp.sqlite.config;

import java.util.Objects;

public final class CipherProfile {
    private final String name;
    private final int pageSize;
    private final int kdfIterations;
    private final String hmacAlgorithm;
    private final String kdfAlgorithm;

    private CipherProfile(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "name");
        this.pageSize = builder.pageSize;
        this.kdfIterations = builder.kdfIterations;
        this.hmacAlgorithm = Objects.requireNonNull(builder.hmacAlgorithm, "hmacAlgorithm");
        this.kdfAlgorithm = Objects.requireNonNull(builder.kdfAlgorithm, "kdfAlgorithm");
    }

    public static CipherProfile sqlCipher4Defaults() {
        return builder()
                .name("SQLCipher 4 defaults")
                .pageSize(4096)
                .kdfIterations(256_000)
                .hmacAlgorithm("HMAC_SHA512")
                .kdfAlgorithm("PBKDF2_HMAC_SHA512")
                .build();
    }

    public String name() {
        return name;
    }

    public int pageSize() {
        return pageSize;
    }

    public int kdfIterations() {
        return kdfIterations;
    }

    public String hmacAlgorithm() {
        return hmacAlgorithm;
    }

    public String kdfAlgorithm() {
        return kdfAlgorithm;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .name(this.name)
                .pageSize(this.pageSize)
                .kdfIterations(this.kdfIterations)
                .hmacAlgorithm(this.hmacAlgorithm)
                .kdfAlgorithm(this.kdfAlgorithm);
    }

    public static final class Builder {
        private String name;
        private int pageSize;
        private int kdfIterations;
        private String hmacAlgorithm;
        private String kdfAlgorithm;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder kdfIterations(int kdfIterations) {
            this.kdfIterations = kdfIterations;
            return this;
        }

        public Builder hmacAlgorithm(String hmacAlgorithm) {
            this.hmacAlgorithm = hmacAlgorithm;
            return this;
        }

        public Builder kdfAlgorithm(String kdfAlgorithm) {
            this.kdfAlgorithm = kdfAlgorithm;
            return this;
        }

        public CipherProfile build() {
            return new CipherProfile(this);
        }
    }
}
