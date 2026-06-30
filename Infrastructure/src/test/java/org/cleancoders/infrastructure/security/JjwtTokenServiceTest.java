package org.cleancoders.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JjwtTokenServiceTest {

    private final JjwtTokenService service = new JjwtTokenService();

    @Test
    void shouldGenerateNonEmptyToken() {
        String token = service.generate("u1", "alice", "STUDENT");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldGenerateTokenWithThreeDotSegments() {
        String token = service.generate("u1", "alice", "STUDENT");
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT 应包含 header.payload.signature 三段");
    }

    @Test
    void shouldGenerateDifferentTokensForDifferentUsers() {
        String t1 = service.generate("u1", "alice", "STUDENT");
        String t2 = service.generate("u2", "bob", "ADMIN");
        assertNotEquals(t1, t2);
    }

    @Test
    void shouldGenerateDeterministicTokensForSameInput() {
        String t1 = service.generate("u1", "alice", "STUDENT");
        String t2 = service.generate("u1", "alice", "STUDENT");
        assertEquals(t1, t2, "相同输入在同一时刻应产生相同 token（确定性）");
    }
}
