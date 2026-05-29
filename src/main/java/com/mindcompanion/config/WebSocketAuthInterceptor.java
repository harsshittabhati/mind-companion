package com.mindcompanion.config;

import com.mindcompanion.security.UserDetailsServiceImpl;
import com.mindcompanion.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor
                .getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null
                && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract JWT from STOMP connect headers
            String authHeader = accessor
                    .getFirstNativeHeader("Authorization");

            if (StringUtils.hasText(authHeader)
                    && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);

                if (jwtUtils.validateJwtToken(jwt)) {
                    String username = jwtUtils
                            .getUsernameFromJwtToken(jwt);
                    UserDetails userDetails = userDetailsService
                            .loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    // Set authenticated user in WebSocket session
                    accessor.setUser(authentication);
                    log.debug("WebSocket authenticated: {}", username);
                }
            }
        }
        return message;
    }
}