package com.tenantsphere.common;

import com.tenantsphere.user.User;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentViewRecorder {

    private final ContentViewRepository contentViewRepository;

    public ContentViewRecorder(ContentViewRepository contentViewRepository) {
        this.contentViewRepository = contentViewRepository;
    }

    @Transactional
    public void record(String contentType, Long objectPkid, User user, HttpServletRequest request) {
        String ip = clientIp(request);
        Long userPkid = user == null ? null : user.getPkid();

        ContentView view = contentViewRepository
                .findByContentTypeAndObjectPkidAndUserPkidAndViewerIp(
                        contentType, objectPkid, userPkid, ip)
                .orElseGet(ContentView::new);

        view.setContentType(contentType);
        view.setObjectPkid(objectPkid);
        view.setUser(user);
        view.setViewerIp(ip);
        view.setLastViewed(Instant.now());
        contentViewRepository.save(view);
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
