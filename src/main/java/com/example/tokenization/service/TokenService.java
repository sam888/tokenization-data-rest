package com.example.tokenization.service;

import com.example.tokenization.model.Token;
import com.example.tokenization.dao.TokenRepository;
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
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
@Service
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

   public List<String> tokenizeAccountNumbers(List<String> accountNumbers) {
      return accountNumbers.stream().map(accountNumber -> {
         try {
            // This call leaves Propagation.SUPPORTS context and starts a Propagation.REQUIRES_NEW context
            // to avoid rolling back all transactions if a single transaction fails.
            return tokenWriter.save( getNewToken( accountNumber ) ).getToken();
         } catch (DataIntegrityViolationException ex) {
            String errorMessage = "Failed to tokenize account " + accountNumber +
                                  ". Account number exists already.";
            log.error( errorMessage, ex );
            return errorMessage + " Please contact support for help.";
         }
      }).toList();

   }

   public List<String> detokenizeAccountNumbers(List<String> tokenList) {
      List<String> accountNumberList = new ArrayList<>();
      for (String token : tokenList) {
         Token resolvedToken = null;
         String accountNumber = null;
         try {
            resolvedToken = getAccountTokenByToken( token );
            if ( resolvedToken != null ) {
               accountNumber = resolvedToken.getAccount();
            }
            accountNumberList.add( accountNumber );
         } catch (Exception ex) {
            log.error( "Failed to detokenize account token " + token, ex );
            accountNumberList.add( "Failed to detokenize token " + token + ". Error: " + ex.getLocalizedMessage()
                    + " Please contact customer support for help." );
         }
      }
      return accountNumberList;
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
