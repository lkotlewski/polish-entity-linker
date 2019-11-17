package pl.edu.pw.elka.polishentitylinker.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.NamedEntity;
import pl.edu.pw.elka.polishentitylinker.repository.RedirectPageRepository;
import pl.edu.pw.elka.polishentitylinker.service.Disambiguator;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NaiveDisambiguatorImpl implements Disambiguator {

    private final RedirectPageRepository redirectPageRepository;

    @Override
    public WikiItemEntity choose(NamedEntity namedEntity, List<WikiItemEntity> candidates) {
        return candidates
                .stream()
                .reduce((a, b) -> getRedirectCount(a) > getRedirectCount(b) ? a : b)
                .orElse(null);
    }

    private int getRedirectCount(WikiItemEntity a) {
        return redirectPageRepository.findAllByTarget(a).size();
    }
}
