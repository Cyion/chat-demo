package de.bredex.chat.repository;

import de.bredex.chat.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

    Optional<Chat> findByParticipantHash(String participantHash);

    @Query("SELECT DISTINCT c FROM Chat c JOIN FETCH c.participants WHERE c.id IN " +
           "(SELECT c2.id FROM Chat c2 JOIN c2.participants p WHERE p.id = :userId) " +
           "ORDER BY c.createdAt DESC")
    List<Chat> findChatsByUserId(@Param("userId") UUID userId);
}
