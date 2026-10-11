package com.tenantsphere.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenantsphere.issue.dto.IssueCreateRequest;
import com.tenantsphere.post.dto.PostCreateRequest;
import com.tenantsphere.post.dto.PostUpdateRequest;
import com.tenantsphere.post.dto.ReplyCreateRequest;
import com.tenantsphere.profile.dto.UpdateProfileRequest;
import com.tenantsphere.rating.dto.RatingCreateRequest;
import com.tenantsphere.report.dto.ReportCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

// Long text is stored as it is sent, so each kind has a most it may be.
class TextLimitsTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> messages(Object request) {
        return validator.validate(request).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    void longTextIsRefusedWithTheMostItMayBe() {
        assertThat(messages(new PostCreateRequest("Title", "x".repeat(10001), List.of())))
                .containsExactly("A post can be at most 10,000 characters.");
        assertThat(messages(new PostUpdateRequest(null, "x".repeat(10001), null)))
                .containsExactly("A post can be at most 10,000 characters.");
        assertThat(messages(new ReplyCreateRequest("x".repeat(5001))))
                .containsExactly("A reply can be at most 5,000 characters.");
        assertThat(messages(new ReportCreateRequest("Title", "x".repeat(5001), "neighbour")))
                .containsExactly("A description can be at most 5,000 characters.");
        assertThat(messages(new IssueCreateRequest("Title", "x".repeat(5001), null)))
                .containsExactly("A description can be at most 5,000 characters.");
        assertThat(messages(new UpdateProfileRequest(null, null, null, null, null, null, "x".repeat(1001), null, null)))
                .containsExactly("A bio can be at most 1,000 characters.");
        assertThat(messages(new RatingCreateRequest("technician", 5, "x".repeat(1001))))
                .containsExactly("A comment can be at most 1,000 characters.");
    }

    @Test
    void textUpToTheMostIsTaken() {
        assertThat(messages(new PostCreateRequest("Title", "x".repeat(10000), List.of()))).isEmpty();
        assertThat(messages(new ReplyCreateRequest("x".repeat(5000)))).isEmpty();
        assertThat(messages(new UpdateProfileRequest(null, null, null, null, null, null, "x".repeat(1000), null, null)))
                .isEmpty();
    }
}
