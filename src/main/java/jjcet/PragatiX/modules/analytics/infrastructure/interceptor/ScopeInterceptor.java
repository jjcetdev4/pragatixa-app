package jjcet.PragatiX.modules.analytics.infrastructure.interceptor;

import jjcet.PragatiX.modules.analytics.api.dto.request.AnalyticsFilter;
import jjcet.PragatiX.modules.analytics.api.dto.request.AttendanceFilter;
import jjcet.PragatiX.modules.analytics.api.dto.request.FunnelFilter;
import jjcet.PragatiX.modules.analytics.api.dto.request.XpFilter;
import jjcet.PragatiX.modules.analytics.domain.model.Scope;
import jjcet.PragatiX.modules.analytics.domain.scope.ScopeAware;
import jjcet.PragatiX.modules.analytics.domain.scope.ScopeResolver;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
public class ScopeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ScopeInterceptor.class);

    private final ScopeResolver scopeResolver;
    private final AuthUtils authUtils;

    public ScopeInterceptor(ScopeResolver scopeResolver, AuthUtils authUtils) {
        this.scopeResolver = scopeResolver;
        this.authUtils = authUtils;
    }

    @Around("within(jjcet.PragatiX.modules.analytics..*) " +
            "&& (@annotation(jjcet.PragatiX.modules.analytics.domain.scope.ScopeAware) " +
            "|| @target(jjcet.PragatiX.modules.analytics.domain.scope.ScopeAware))")
    public Object applyScope(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        if (method.getAnnotation(ScopeAware.class) == null &&
                method.getDeclaringClass().getAnnotation(ScopeAware.class) == null) {
            return joinPoint.proceed();
        }

        User user = authUtils.getCurrentUser();
        Scope scope = scopeResolver.resolveScope(user);

        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Object arg = args[i];
            if (arg == null) continue;
            if (arg instanceof AnalyticsFilter filter) {
                args[i] = constrainFilter(filter, scope);
            } else if (arg instanceof AttendanceFilter filter) {
                args[i] = constrainAttendanceFilter(filter, scope);
            } else if (arg instanceof XpFilter filter) {
                args[i] = constrainXpFilter(filter, scope);
            } else if (arg instanceof FunnelFilter filter) {
                args[i] = constrainFunnelFilter(filter, scope);
            } else if (arg instanceof Long departmentId && "departmentId".equals(parameters[i].getName())) {
                scopeResolver.validateDepartmentAccess(user, departmentId);
            } else if (arg instanceof Long sectionId && "sectionId".equals(parameters[i].getName())) {
                scopeResolver.validateSectionAccess(user, sectionId);
            }
        }

        log.debug("Applied scope {} for user {}", scope, user != null ? user.getUsername() : "anonymous");
        return joinPoint.proceed(args);
    }

    private AnalyticsFilter constrainFilter(AnalyticsFilter filter, Scope scope) {
        if (scope.level() == Scope.ScopeLevel.INSTITUTION) {
            return filter;
        }
        if (scope.departmentId() != null && filter.departmentId() != null
                && !filter.departmentId().equals(scope.departmentId())) {
            throw new AccessDeniedException("Department filter exceeds scope");
        }
        return AnalyticsFilter.builder()
                .yearNo(filter.yearNo())
                .semester(filter.semester())
                .departmentId(scope.departmentId())
                .stage(filter.stage())
                .sectionId(filter.sectionId())
                .startDate(filter.startDate())
                .endDate(filter.endDate())
                .build();
    }

    private AttendanceFilter constrainAttendanceFilter(AttendanceFilter filter, Scope scope) {
        if (scope.level() == Scope.ScopeLevel.INSTITUTION) {
            return filter;
        }
        return AttendanceFilter.builder()
                .academicYear(filter.academicYear())
                .departmentId(scope.departmentId())
                .stageId(filter.stageId())
                .sectionId(scope.level() == Scope.ScopeLevel.SECTION ? scope.sectionId() : filter.sectionId())
                .date(filter.date())
                .startDate(filter.startDate())
                .endDate(filter.endDate())
                .period(filter.period())
                .threshold(filter.threshold())
                .build();
    }

    private XpFilter constrainXpFilter(XpFilter filter, Scope scope) {
        if (scope.level() == Scope.ScopeLevel.INSTITUTION) {
            return filter;
        }
        return XpFilter.builder()
                .academicYear(filter.academicYear())
                .departmentId(scope.departmentId())
                .stageId(filter.stageId())
                .sectionId(scope.level() == Scope.ScopeLevel.SECTION ? scope.sectionId() : filter.sectionId())
                .startDate(filter.startDate())
                .endDate(filter.endDate())
                .category(filter.category())
                .type(filter.type())
                .threshold(filter.threshold())
                .page(filter.page())
                .size(filter.size())
                .build();
    }

    private FunnelFilter constrainFunnelFilter(FunnelFilter filter, Scope scope) {
        if (scope.level() == Scope.ScopeLevel.INSTITUTION) {
            return filter;
        }
        return FunnelFilter.builder()
                .academicYear(filter.academicYear())
                .departmentId(scope.departmentId())
                .stageId(filter.stageId())
                .sectionId(filter.sectionId())
                .startDate(filter.startDate())
                .endDate(filter.endDate())
                .build();
    }
}