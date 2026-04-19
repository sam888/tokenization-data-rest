package com.example.tokenization.service;

import com.example.tokenization.model.Token;
import com.example.tokenization.dao.TokenRepository;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Samuel Huang
 * Created: 16-Apr-2026
 */
@Slf4j
@Service
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class TokenService {

   private final TokenRepository tokenRepository;
   private final TokenWriter tokenWriter;

   private final RandomStringGenerator randomStringGenerator;


   public TokenService(TokenRepository tokenRepository, TokenWriter tokenWriter) {
      this.tokenRepository = tokenRepository;
      this.tokenWriter = tokenWriter;

      // Configured to produce alphanumeric strings (0-9, a-z, A-Z)
      this.randomStringGenerator = new RandomStringGenerator.Builder()
              .withinRange('0', 'z')
              .filteredBy(Character::isLetterOrDigit)
              .get();
   }

   /**
    * Uncomment @Transactional annotation and replace tokenRepository.save(..) with tokenWriter.save(..) if all
    * transactions happened within tokenizeAccountNumbers(..) need to be rolled back in database if any single transaction fails.
    *
    * How does TokenWriter.save(..) work?
    * Propagation.REQUIRES_NEW is placed in save(..) of TokenWriter (a separate bean) rather than here
    * to avoid the Spring "Self-Invocation" problem. If a REQUIRES_NEW method is called from
    * within the same class (e.g. this.save()), Spring's proxy is bypassed and the
    * @Transactional annotation is silently ignored. Delegating to TokenWriter ensures
    * the call goes through Spring's proxy, so REQUIRES_NEW is honoured — each save runs
    * in its own independent transaction, isolating failures per account.
    *
    * @param accountNumbers
    * @return
    */
   // @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
   public List<String> tokenizeAccountNumbers(List<String> accountNumbers) {
      return accountNumbers.stream()
              .map(accountNumber -> Try
                      .of( () -> tokenRepository.save( getNewToken( accountNumber ) ).getToken())
                      .recover(DataIntegrityViolationException.class, ex -> {
                         String errorMessage = "Failed to tokenize account " + accountNumber +
                                 ". Account number exists already.";
                         log.error(errorMessage, ex);
                         return errorMessage + " Please contact support for help.";
                      })
                      .get())
              .toList();
   }

   public List<String> detokenizeAccountNumbers(List<String> tokenList) {
      return tokenList.stream()
              .map(token -> Try
                      .of( () -> getAccountTokenByToken( token ) )
                      .map(resolvedToken -> resolvedToken != null ? resolvedToken.getAccount() : null)
                      .recover( Exception.class, ex -> {
                         log.error( "Failed to detokenize account token " + token, ex);
                         return "Failed to detokenize token " + token + ". Error: " + ex.getLocalizedMessage()
                                 + ". Please contact customer support for help.";
                      })
                      .get())
              .toList();
   }

   public Token getAccountTokenByToken(String token) {
      return tokenRepository.findByToken( token );
   }

   private Token getNewToken(String accountNumber) {
      Token newToken = new Token();
      newToken.setAccount( accountNumber );
      newToken.setToken( randomAlphanumeric() );
      return newToken;
   }

   public String randomAlphanumeric() {
      return randomStringGenerator.generate(32);
   }

}
