package com.example.auth.repositories;

import com.example.auth.entities.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID> {
    @Query
    Optional<Person> findByUsername(String username);

    @Query("SELECT p from Person p where p.username = :username and p.admin = true")
    Optional<Person> findPersonByUsernameAndAdmin(@Param("username")String username);

}

