package com.example.tokenization.dao;

import com.example.tokenization.model.Token;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Samuel Huang
 * Created: 16-Apr-2026
 */
@RepositoryRestResource(exported = false) // This turns off the /tokens endpoint
public interface TokenRepository extends PagingAndSortingRepository<Token, Long>, CrudRepository<Token, Long> {

   Token findByToken(String token);
}
