package com.game.service;

import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.exceptions.*;
import com.game.entity.Player;
import com.game.repository.PlayerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.util.Calendar;
import java.util.Date;


@Service
public class PlayerService {
    private final PlayerRepository playerRepository;
    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }
    public Integer calculateLevel(Integer exp) {
        return (((int) Math.sqrt(2500 + (200 * exp))) - 50) / 100;
    }
    private Integer calculateExpToLevel(Integer exp, Integer level) {
        return (50 * (level + 1) * (level + 2))- exp;
    }
    public Page<Player> findAllPlayers(Specification<Player> spec, Pageable pageable) {
        return playerRepository.findAll(spec, pageable);
    }

    public Long getCountPlayers(Specification<Player> specification) {
        return playerRepository.count(specification);
    }

    public Player createPlayer(Player player) {
        if(player.getName() == null || player.getName().isEmpty() || player.getName().length() > 12) {
            throw new BadRequestException("Name not valid");
        }
        if(player.getTitle().length() > 30 || player.getTitle() == null || player.getTitle().isEmpty()) {
            throw new BadRequestException("Title not valid");
        }
        if(player.getRace() == null) {
            throw new BadRequestException("Race not valid");
        }
        if(player.getProfession() == null) {
            throw new BadRequestException("Profession not valid");
        }
        if(player.getExperience() < 0
                || player.getExperience() > 10_000_000
                || player.getExperience() == null) {
            throw new BadRequestException("Experience not valid");
        }
        checkDate(player.getBirthday());
        if(player.getBanned() == null) player.setBanned(false);

        player.setLevel(calculateLevel(player.getExperience()));
        player.setUntilNextLevel(calculateExpToLevel(player.getExperience(), player.getLevel()));
        return playerRepository.saveAndFlush(player);
    }

    public Player getPlayerById(Long id) {
        if(id <= 0) {
            throw new BadRequestException("The ID must be greater than zero");
        }
        return playerRepository.findById(id).orElseThrow(() ->
                new NotFoundException("Player not found"));
    }

    public Player updatePlayer(Long id, Player player) {
        Player bufferPlayer = getPlayerById(id);
        int a = 0;
        System.out.println(++a);//1
        if(player.getName() == null
                && player.getTitle() == null
                && player.getRace() == null
                && player.getProfession() == null
                && player.getExperience() == null
                && player.getBirthday() == null
                && player.getBanned() == null) {
            return bufferPlayer;
        }
        System.out.println(++a);//2
        if(player.getName() != null) {
            if (player.getName().length() > 12) {
                throw new BadRequestException("Name not valid");
            } else {
                bufferPlayer.setName(player.getName());
            }
        }
        System.out.println(++a);//3
        if(player.getTitle() != null) {
            if (player.getTitle().length() > 30) {
                throw new BadRequestException("Title not valid");
            } else {
                bufferPlayer.setTitle(player.getTitle());
            }
        }
        System.out.println(++a);//4
        if(player.getRace() != null) {
            bufferPlayer.setRace(player.getRace());
        }
        System.out.println(++a);//5
        if(player.getProfession() != null) {
            bufferPlayer.setProfession(player.getProfession());
        }
        System.out.println(++a);//6
        if(player.getBirthday() != null) {
            checkDate(player.getBirthday());
            bufferPlayer.setBirthday(player.getBirthday());
        }
        System.out.println(++a);//7
        if(player.getBanned() != null) {
            bufferPlayer.setBanned(player.getBanned());
        }
        System.out.println(++a);//8
        if(player.getExperience() == null) {
            bufferPlayer.setExperience(bufferPlayer.getExperience());
        } else {
            if (player.getExperience() < 0 || player.getExperience() > 10_000_000) {
                throw new BadRequestException("Experience not valid");
            } else {
                bufferPlayer.setExperience(player.getExperience());
            }
        }
        System.out.println(++a);//9
        bufferPlayer.setLevel(calculateLevel(bufferPlayer.getExperience()));
        System.out.println(++a);//10
        bufferPlayer.setUntilNextLevel(calculateExpToLevel(bufferPlayer.getExperience(),
                bufferPlayer.getLevel()));
        System.out.println(++a);//11
        return playerRepository.save(bufferPlayer);
    }

    public Player deletePlayer(Long id) {
        Player player = getPlayerById(id);
        playerRepository.delete(player);
        return player;
    }

    public void checkDate(Date birthday) {
        if(birthday == null) {
            throw new BadRequestException("Birthday not valid");
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(birthday.getTime());
        if(calendar.get(Calendar.YEAR) < 2000 || calendar.get(Calendar.YEAR) > 3000) {
            throw new BadRequestException("Birthday out of range");
        }
    }
    public Specification<Player> filterByName(String name) {
        return (root, query, criteriaBuilder) -> name == null ?
                null : criteriaBuilder.like(root.get("name"), "%" + name + "%");
    }
    public Specification<Player> filterByTitle(String title) {
        return (root, query, criteriaBuilder) -> title == null ?
                null : criteriaBuilder.like(root.get("title"), "%" + title + "%");
    }

    public Specification<Player> filterByBirthday(Long after, Long before) {
        return (root, query, criteriaBuilder) -> {
            if(after == null && before == null) return null;
            if(after == null) return criteriaBuilder.lessThanOrEqualTo(root.get("birthday"),
                    new Date(before));
            if(before == null) return criteriaBuilder.greaterThanOrEqualTo(root.get("birthday"),
                    new Date(after));
            return criteriaBuilder.between(root.get("birthday"), new Date(after), new Date(before));
        };
    }

    public Specification<Player> filterByExperience(Integer min, Integer max) {
        return (root, query, criteriaBuilder) -> {
            if(min == null && max == null) return null;
            if(min == null) return criteriaBuilder.lessThanOrEqualTo(root.get("experience"), max);
            if(max == null) return criteriaBuilder.greaterThanOrEqualTo(root.get("experience"), min);
            return criteriaBuilder.between(root.get("experience"), min, max);
        };
    }

    public Specification<Player> filterByLevel(Integer min,Integer max) {
        return (root, query, criteriaBuilder) -> {
            if(min == null && max == null) return null;
            if(min == null) return criteriaBuilder.lessThanOrEqualTo(root.get("level"), max);
            if(max == null) return criteriaBuilder.greaterThanOrEqualTo(root.get("level"), min);
            return criteriaBuilder.between(root.get("level"), min, max);
        };
    }

    public Specification<Player> filterByRace(Race race) {
        return (root, query, criteriaBuilder) -> race == null ?
                null : criteriaBuilder.equal(root.get("race"), race);
    }

    public Specification<Player> filterByProfession(Profession profession) {
        return (root, query, criteriaBuilder) -> profession == null ?
                null : criteriaBuilder.equal(root.get("profession"), profession);
    }

    public Specification<Player> filterByBanned(Boolean isBanned) {
        return (root, query, criteriaBuilder) -> {
            if(isBanned == null) return null;
            if(isBanned) return criteriaBuilder.isTrue(root.get("banned"));
            return criteriaBuilder.isFalse(root.get("banned"));
        };
    }
}






























