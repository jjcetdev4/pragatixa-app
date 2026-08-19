package jjcet.PragatiX.infrastructure.interceptor;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class HibernateFilterInterceptor implements HandlerInterceptor {

    @Autowired
    private EntityManager entityManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // Do not enable the filter for the Recycle Bin endpoints because we need to query deleted records there
        if (!path.startsWith("/api/v1/recycle-bin")) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("deletedFilter");
        }

        return true;
    }
}
