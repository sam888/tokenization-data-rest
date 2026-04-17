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
    * Propagation.REQUIRES_NEW not used in TokenService to avoid the "Self-Invocation" Problem (The Silent Killer).
    * If Propagation.REQUIRES_NEW is used in TokenService, Spring applies @Transactional by creating a "Proxy" around
    * TokenService so if TokenService.save(newToken) having Propagation.REQUIRES_NEW is called within TokenService, the
    * method save(newToken) will be called on this (the local object), not the Proxy => @Transactional settings on the
    * save(newToken) method are ignored entirely. Calling this method within TokenService will fix this issue.
    *
    * @param token
    * @return
    */
   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public Token save(Token token) {
      return tokenRepository.save( token );
   }
}
