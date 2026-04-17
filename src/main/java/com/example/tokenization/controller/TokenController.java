package com.example.tokenization.controller;

import com.example.tokenization.service.TokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tokens")
public class TokenController {

   private final TokenService tokenService;

   public TokenController(TokenService tokenService) {
      this.tokenService = tokenService;
   }

   @PostMapping
   public List<String> tokenize(@RequestBody List<String> accountNumbers) {
      return tokenService.tokenizeAccountNumbers( accountNumbers );
   }

   @PostMapping("/resolve")
   public List<String> detokenize(@RequestBody List<String> tokens) {
      return tokenService.detokenizeAccountNumbers( tokens );
   }

}
