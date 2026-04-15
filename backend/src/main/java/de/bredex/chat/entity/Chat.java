package de.bredex.chat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "participant_hash", nullable = false, unique = true, length = 73)
    private String participantHash;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "chat_participants",
        joinColumns = @JoinColumn(name = "chat_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participants = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Chat() {}

    public Chat(String participantHash, Set<User> participants) {
        this.participantHash = participantHash;
        this.participants = participants;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getParticipantHash() { return participantHash; }
    public Set<User> getParticipants() { return participants; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "Chat{id=" + id + ", participantHash='" + participantHash + "'}";
    }
}
