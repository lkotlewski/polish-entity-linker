package pl.edu.pw.elka.polishentitylinker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;
import pl.edu.pw.elka.polishentitylinker.model.json.WikiItem;
import pl.edu.pw.elka.polishentitylinker.repository.WikiItemRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class WikiItemServiceImpl implements WikiItemService {

    private final WikiItemRepository wikiItemRepository;

    private final ModelMapper modelMapper = new ModelMapper();

    @Override
    public WikiItem add(WikiItem wikiItem) {
        WikiItemEntity entity = modelMapper.map(wikiItem, WikiItemEntity.class);
        List<WikiItemEntity> subclassOfList = Optional.ofNullable(wikiItem.getSubclassOf()).orElseGet(Collections::emptyList).stream()
                .map(id -> findById(id).orElseGet(() -> {
                    WikiItemEntity wikiItemEntity = new WikiItemEntity();
                    wikiItemEntity.setId(id);
                    wikiItemRepository.save(wikiItemEntity);
                    return wikiItemEntity;
                }))
                .collect(Collectors.toList());
        List<WikiItemEntity> instanceOfList = Optional.ofNullable(wikiItem.getInstanceOf()).orElseGet(Collections::emptyList).stream()
                .map(id -> findById(id).orElseGet(() -> {
                    WikiItemEntity wikiItemEntity = new WikiItemEntity();
                    wikiItemEntity.setId(id);
                    wikiItemRepository.save(wikiItemEntity);
                    return wikiItemEntity;
                }))
                .collect(Collectors.toList());
        entity.setSubclassOf(subclassOfList);
        entity.setInstanceOf(instanceOfList);
        WikiItemEntity savedEntity = wikiItemRepository.save(entity);

        log.info("Entity with id {} saved", savedEntity.getId());
        return modelMapper.map(savedEntity, WikiItem.class);
    }

    @Override
    public WikiItem findByTitle(String title) {
        WikiItemEntity wikiItemEntity = wikiItemRepository.findByTitlePl(title);
        return wikiItemEntity == null ? null : modelMapper.map(wikiItemEntity, WikiItem.class);
    }

    private Optional<WikiItemEntity> findById(String id) {
        return wikiItemRepository.findById(id);
    }
}
