package com.tenantsphere;

import com.tenantsphere.common.Countries;
import com.tenantsphere.common.PageResponse;
import com.tenantsphere.issue.*;
import com.tenantsphere.issue.dto.IssueStatusUpdateRequest;
import com.tenantsphere.apartment.ApartmentRepository;
import com.tenantsphere.mail.MailService;
import com.tenantsphere.user.User;
import com.tenantsphere.common.AfterCommit;
import com.tenantsphere.post.dto.PostCreateRequest;
import jakarta.validation.Validation;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class BugReportUnitTest {
    @Test void countriesAcceptIsoCodesButRejectMalformedAndUnknownCodes() {
        assertThat(Countries.nameOf("in")).isEqualTo("India");
        for (String value : List.of("zz", "ZZ", "abc!", "123456", "")) {
            assertThat(Countries.nameOf(value)).isEmpty(); assertThat(Countries.codeOf(value)).isEmpty();
        }
    }
    @Test void pagingUsesTheActualServerPageSize() {
        var response = PageResponse.from(new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 7), "https://example/posts/?search=x");
        assertThat(response.pageSize()).isEqualTo(2); assertThat(response.totalPages()).isEqualTo(4);
        assertThat(response.next()).endsWith("&page=3"); assertThat(response.previous()).isEqualTo("https://example/posts/?search=x");
    }
    @Test void repeatedResolutionDoesNotSendAnotherEmail() {
        var issues = mock(IssueRepository.class); var mail = mock(MailService.class);
        var issue = new Issue(); issue.setId(UUID.randomUUID()); issue.setTitle("Tap"); issue.setStatus(IssueStatus.REPORTED);
        User reporter = new User(); reporter.setEmail("resident@example.com"); issue.setReportedBy(reporter);
        User actor = new User(); actor.setStaff(true);
        when(issues.findByExternalIdForUpdate(issue.getId())).thenReturn(Optional.of(issue));
        var service = new IssueWriteService(issues, mock(ApartmentRepository.class), mail);
        var update = new IssueStatusUpdateRequest(null, null, IssueStatus.RESOLVED);
        service.updateStatus(issue.getId(), update, actor); service.updateStatus(issue.getId(), update, actor);
        verify(mail, times(1)).send(eq("resident@example.com"), eq("Issue Resolved"), anyString());
        assertThat(issue.getResolvedOn()).isNotNull();
    }

    @Test void postsAcceptAtMostTenTags() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var tags = IntStream.range(0, 11).mapToObj(i -> "tag" + i).toList();
        assertThat(validator.validate(new PostCreateRequest("Title", "Body", tags)))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("tags");
        assertThat(validator.validate(new PostCreateRequest("Title", "Body", tags.subList(0, 10)))).isEmpty();
    }

    @Test void failingPostCommitSideEffectsDoNotFailTheCommittedRequest() {
        Runnable failingMail = () -> { throw new IllegalStateException("SMTP unavailable"); };
        assertThatCode(() -> AfterCommit.run(failingMail)).doesNotThrowAnyException();
        TransactionSynchronizationManager.initSynchronization();
        try {
            AfterCommit.run(failingMail);
            assertThatCode(TransactionSynchronizationUtils::triggerAfterCommit).doesNotThrowAnyException();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
