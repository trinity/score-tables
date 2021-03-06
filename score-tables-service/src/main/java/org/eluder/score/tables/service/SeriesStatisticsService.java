package org.eluder.score.tables.service;

import static org.springframework.data.mongodb.core.mapreduce.MapReduceOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.util.Collections;
import java.util.List;

import org.eluder.score.tables.api.MatchTypeConfiguration;
import org.eluder.score.tables.api.MatchType;
import org.eluder.score.tables.api.PlayerStats;
import org.eluder.score.tables.api.Tournament;
import org.eluder.score.tables.service.comparator.PlayerStatsComparator;
import org.eluder.score.tables.service.exception.NotFoundException;
import org.eluder.score.tables.service.repository.PlayerRepository;
import org.eluder.score.tables.service.repository.TournamentRepository;
import org.eluder.score.tables.service.utils.PlayerStatsPointsTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapreduce.MapReduceResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@Service
public class SeriesStatisticsService {
    
    private static final MatchType SERIES = MatchType.SERIES;
    
    @Autowired
    private MongoOperations mongoTemplate;
    
    @Autowired
    private PlayerRepository playerRepository;
    
    @Autowired
    private TournamentRepository tournamentRepository;
    
    @Autowired
    private PlayerStatsComparator playerStatsComparator;
    
    public List<PlayerStats> getTournamentStatistics(final String tournamentId) {
        Tournament tournament = getTournament(tournamentId);
        Query query = query(where("tournamentId").is(tournamentId).and("type").is(SERIES.toString()));
        MapReduceResults<PlayerStatsValue> results = mongoTemplate.mapReduce(query, "matches", "classpath:/mapreduce/player_stats_map.js", "classpath:/mapreduce/player_stats_reduce.js", options().javaScriptMode(true).outputTypeInline(), PlayerStatsValue.class);
        List<PlayerStats> playerStats = Lists.newArrayList(transformResults(results, tournament.getConfigurations().get(SERIES)));
        Collections.sort(playerStats, playerStatsComparator);
        return playerStats;
    }
    
    private Iterable<PlayerStats> transformResults(final Iterable<PlayerStatsValue> results, final MatchTypeConfiguration configuration) {
        Function<PlayerStatsValue, PlayerStats> transformer = Functions.compose(
                new PlayerStatsPointsTransformer(configuration), new FlattenPlayerStats()
        );
        return Iterables.transform(results, transformer);
    }
    
    private Tournament getTournament(final String tournamentId) {
        Tournament tournament = tournamentRepository.findOne(tournamentId);
        if (tournament == null) {
            throw new NotFoundException(Tournament.class, tournamentId);
        }
        return tournament;
    }
    
    private class PlayerStatsValue {
        public String id;
        public PlayerStats value;
    }
    
    private class FlattenPlayerStats implements Function<PlayerStatsValue, PlayerStats> {
        @Override
        public PlayerStats apply(final PlayerStatsValue input) {
            PlayerStats playerStats = input.value;
            playerStats.setPlayerId(input.id);
            playerStats.setPlayerName(playerRepository.findOne(input.id).getName());
            return playerStats;
        }
    }
}
