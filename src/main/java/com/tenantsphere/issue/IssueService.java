package com.tenantsphere.issue;

import com.tenantsphere.common.ContentViewRepository;
import com.tenantsphere.issue.dto.IssueResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class IssueService {

    public static final String CONTENT_TYPE = "issue";

    private final ContentViewRepository contentViewRepository;

    public IssueService(ContentViewRepository contentViewRepository) {
        this.contentViewRepository = contentViewRepository;
    }

    public Page<IssueResponse> toResponses(Page<Issue> issues) {
        List<Long> pkids = issues.getContent().stream().map(Issue::getPkid).toList();
        Map<Long, Long> counts = new HashMap<>();
        if (!pkids.isEmpty()) {
            contentViewRepository.countByObjects(CONTENT_TYPE, pkids)
                    .forEach(row -> counts.put(row.getObjectPkid(), row.getTotal()));
        }
        return issues.map(issue -> IssueResponse.of(issue, counts.getOrDefault(issue.getPkid(), 0L)));
    }
}
