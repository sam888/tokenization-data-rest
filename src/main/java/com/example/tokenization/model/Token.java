package com.example.tokenization.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * @author Samuel Huang
 * Created: 16-Apr-2026
 */
@Data
@Entity
public class Token {

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private Long id;

   @Column(unique = true, nullable = false)
   private String account;

   @Column(unique = true, nullable = false)
   private String token;
}
