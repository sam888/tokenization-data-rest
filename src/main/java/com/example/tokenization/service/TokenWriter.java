package com.example.tokenization.service;

import com.example.tokenization.model.Token;
import com.example.tokenization.dao.TokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Samuel Huang
 * Created: 16-Apr-2026
 */
@Service
public class TokenWriter {

   private final TokenRepository tokenRepository;

   public TokenWriter(TokenRepository tokenRepository) {
      this.tokenRepository = tokenRepository;
   }

   /**
    * Saves a token in its own independent transaction (REQUIRES_NEW), so a failure
    * here does not affect other in-progress saves. Called from TokenService to avoid
    * the self-invocation proxy problem.
    */
   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public Token save(Token token) {
      return tokenRepository.save( token );
   }
}
