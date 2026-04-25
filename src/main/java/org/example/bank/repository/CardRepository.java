package org.example.bank.repository;

import org.example.bank.entity.Card;
import org.example.bank.entity.User;
import org.example.bank.entity.enums.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Page<Card> findByOwner(User owner, Pageable pageable);

    Page<Card> findByOwnerAndStatus(User owner, CardStatus status, Pageable pageable);

    @Query("SELECT c FROM Card c WHERE c.owner = :owner AND c.maskedNumber LIKE %:search%")
    Page<Card> searchByOwnerAndMaskedNumber(@Param("owner") User owner,
                                            @Param("search") String search,
                                            Pageable pageable);

    List<Card> findByOwner(User owner);

    Optional<Card> findByIdAndOwner(Long id, User owner);

    boolean existsByIdAndOwner(Long id, User owner);
}