package com.yaphet.springtemplate.repositories;

import com.yaphet.springtemplate.models.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    @Query("SELECT a FROM AppUser a WHERE a.email=?1 AND a.deleted=FALSE ")
    Optional<AppUser> findByEmail(String email);

    @Modifying
    @Query("UPDATE AppUser a SET a.enabled=TRUE WHERE a.email=?1")
    void enableAppUser(String email);
}
