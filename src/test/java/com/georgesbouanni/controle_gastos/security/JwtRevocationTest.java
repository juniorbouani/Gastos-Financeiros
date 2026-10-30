package com.georgesbouanni.controle_gastos.security;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class JwtRevocationTest {
 @Test void tokensStopWorkingAfterPasswordChangeAndClosure() throws Exception {
  UserRepository repo=mock(UserRepository.class);User user=new User();user.setCpf("12345678900");when(repo.findByCpf(user.getCpf())).thenAnswer(i->Optional.of(user));
  JwtService jwt=new JwtService(repo);ReflectionTestUtils.setField(jwt,"secretKey","012345678901234567890123456789012345678901234567890123456789012345");ReflectionTestUtils.setField(jwt,"expirationMs",60000L);
  String old=jwt.generateToken(user.getCpf());assertTrue(jwt.isTokenValid(old,user.getCpf()));user.setCredentialVersion(1);assertFalse(jwt.isTokenValid(old,user.getCpf()));
  String fresh=jwt.generateToken(user.getCpf());assertTrue(jwt.isTokenValid(fresh,user.getCpf()));user.setAccountClosed(true);assertFalse(jwt.isTokenValid(fresh,user.getCpf()));assertThrows(IllegalArgumentException.class,()->jwt.generateToken(user.getCpf()));
 }
 @Test void malformedTokenReturns401WithoutRunningController() throws Exception {
  UserRepository repo=mock(UserRepository.class);JwtService jwt=new JwtService(repo);ReflectionTestUtils.setField(jwt,"secretKey","012345678901234567890123456789012345678901234567890123456789012345");
  var filter=new JwtAuthenticationFilter(jwt,mock(CustomUserDetailService.class));var request=new MockHttpServletRequest();request.addHeader("Authorization","Bearer inválido");var response=new MockHttpServletResponse();var chain=mock(jakarta.servlet.FilterChain.class);
  try{filter.doFilter(request,response,chain);assertEquals(401,response.getStatus());verifyNoInteractions(chain);}finally{SecurityContextHolder.clearContext();}
 }
 @Test void closedAccountCannotAuthenticate(){UserRepository repo=mock(UserRepository.class);User u=new User();u.setCpf("12345678900");u.setSenhaHash("hash");u.setAccountClosed(true);when(repo.findByCpf(u.getCpf())).thenReturn(Optional.of(u));assertFalse(new CustomUserDetailService(repo).loadUserByUsername(u.getCpf()).isEnabled());}
}
