package org.eluder.score.tables.service;

import java.util.List;

import org.eluder.score.tables.api.Player;
import org.eluder.score.tables.service.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;
    
    @Autowired
    private MongoOperations mongoTemplate;
    
    public Player findOne(final String id) {
        return playerRepository.findOne(id);
    }
    
    public List<Player> findAll() {
        return ImmutableList.copyOf(playerRepository.findAll());
    }
    
    public Player save(final Player player) {
        return playerRepository.save(player);
    }
    
    public List<Player> findByNameParts(final String term) {
        return playerRepository.findBySearchNameKeywords(term);
    }
}
