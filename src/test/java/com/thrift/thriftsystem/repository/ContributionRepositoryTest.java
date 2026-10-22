package com.thrift.thriftsystem.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.thrift.thriftsystem.model.Contribution;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Runs against the real MongoDB from MONGO_URI (a service container in CI)
@SpringBootTest
class ContributionRepositoryTest {

    @Autowired private ContributionRepository contributionRepository;

    private Contribution saved;

    @AfterEach
    void cleanUp() {
        if (saved != null) {
            contributionRepository.deleteById(saved.getId());
        }
    }

    @Test
    void save_KeepsPaystackReference() {
        saved =
                contributionRepository.save(
                        Contribution.builder()
                                .userId("user-repo-test")
                                .groupId("group-repo-test")
                                .amount(new BigDecimal("10000"))
                                .paystackReference("THRIFT-repo-test")
                                .build());

        Contribution reloaded = contributionRepository.findById(saved.getId()).orElseThrow();

        assertEquals("THRIFT-repo-test", reloaded.getPaystackReference());
        assertTrue(contributionRepository.findByPaystackReference("THRIFT-repo-test").isPresent());
    }

    @Test
    void findByAnyPaystackReference_MatchesCurrentAndPreviousReferences() {
        Contribution contribution =
                Contribution.builder()
                        .userId("user-repo-test")
                        .groupId("group-repo-test")
                        .amount(new BigDecimal("10000"))
                        .paystackReference("THRIFT-repo-current")
                        .build();
        contribution.getPreviousPaystackReferences().add("THRIFT-repo-old");
        saved = contributionRepository.save(contribution);

        assertEquals(
                saved.getId(),
                contributionRepository
                        .findByAnyPaystackReference("THRIFT-repo-current")
                        .orElseThrow()
                        .getId());
        assertEquals(
                saved.getId(),
                contributionRepository
                        .findByAnyPaystackReference("THRIFT-repo-old")
                        .orElseThrow()
                        .getId());
        assertTrue(contributionRepository.findByAnyPaystackReference("THRIFT-repo-none").isEmpty());
    }

    @Test
    void save_SetsCreatedAndUpdatedDates() {
        saved =
                contributionRepository.save(
                        Contribution.builder()
                                .userId("user-repo-test")
                                .groupId("group-repo-test")
                                .amount(new BigDecimal("10000"))
                                .build());

        Contribution reloaded = contributionRepository.findById(saved.getId()).orElseThrow();

        assertNotNull(reloaded.getCreatedDate());
        assertNotNull(reloaded.getUpdatedDate());
        assertTrue(reloaded.getPreviousPaystackReferences().isEmpty());
    }
}
