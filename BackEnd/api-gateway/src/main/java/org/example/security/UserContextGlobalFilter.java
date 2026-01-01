package org.example.security;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class UserContextGlobalFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {

                    var jwt = auth.getToken();

                    String userId = jwt.getSubject(); // sub
                    String username = jwt.getClaimAsString("preferred_username");

                    List<String> roles = List.of();
                    Object realmAccess = jwt.getClaim("realm_access");
                    if (realmAccess instanceof Map<?, ?> map) {
                        Object r = map.get("roles");
                        if (r instanceof List<?>) {
                            roles = ((List<?>) r).stream().map(Object::toString).toList();
                        }
                    }

                    var mutatedRequest = exchange.getRequest()
                            .mutate()
                            .header("X-User-Id", userId)
                            .header("X-Username", username)
                            .header("X-User-Roles", String.join(",", roles))
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }
}
