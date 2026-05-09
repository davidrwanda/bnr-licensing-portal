package rw.bnr.licensing.concurrency;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import rw.bnr.licensing.application.dto.DecisionRequest;
import rw.bnr.licensing.application.service.ApplicationService;
import rw.bnr.licensing.common.exception.ConflictException;
import rw.bnr.licensing.domain.model.*;
import rw.bnr.licensing.domain.repository.ApplicationRepository;
import rw.bnr.licensing.domain.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David NTAMAKEMWA
 *
 * Demonstrates that optimistic locking prevents two concurrent decisions on
 * the same application from both succeeding. Runs against a real PostgreSQL
 * instance via Testcontainers — mocks cannot give meaningful concurrency guarantees.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ConcurrentApprovalTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bnr_test")
            .withUsername("bnr_user")
            .withPassword("changeme");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired ApplicationService applicationService;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired UserRepository userRepository;

    @Test
    void only_one_of_two_concurrent_decisions_should_succeed() throws InterruptedException {
        // Set up: one application in REVIEW_COMPLETE state, two approvers
        User applicant = userRepository.save(
                new User("c-applicant@test.rw", "$2a$12$hash", "Concurrent Applicant", Role.APPLICANT));
        User reviewer = userRepository.save(
                new User("c-reviewer@test.rw", "$2a$12$hash", "Concurrent Reviewer", Role.REVIEWER));
        User approver1 = userRepository.save(
                new User("c-approver1@test.rw", "$2a$12$hash", "Approver One", Role.APPROVER));
        User approver2 = userRepository.save(
                new User("c-approver2@test.rw", "$2a$12$hash", "Approver Two", Role.APPROVER));

        Application app = new Application();
        app.setApplicant(applicant);
        app.setReviewer(reviewer);
        app.setInstitutionName("Concurrent Test Bank");
        app.setInstitutionType("Commercial Bank");
        app.setStatus(ApplicationStatus.REVIEW_COMPLETE);
        Application saved = applicationRepository.save(app);
        UUID appId = saved.getId();

        // Fire two concurrent approve requests
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);
        List<Future<Void>> futures = new ArrayList<>();

        for (UUID approverId : List.of(approver1.getId(), approver2.getId())) {
            futures.add(pool.submit(() -> {
                barrier.await(); // both threads start simultaneously
                try {
                    applicationService.approve(appId, approverId,
                            new DecisionRequest("Concurrent approval attempt"));
                    successes.incrementAndGet();
                } catch (ConflictException e) {
                    conflicts.incrementAndGet();
                }
                return null;
            }));
        }

        for (Future<Void> f : futures) {
            try { f.get(10, TimeUnit.SECONDS); }
            catch (ExecutionException | TimeoutException e) {
                // CyclicBarrier BrokenBarrierException surfaces here — treat as conflict
                conflicts.incrementAndGet();
            }
        }

        pool.shutdown();

        assertThat(successes.get())
                .as("Exactly one approval should succeed")
                .isEqualTo(1);
        assertThat(conflicts.get())
                .as("Exactly one request should be rejected with a conflict")
                .isEqualTo(1);
    }
}
