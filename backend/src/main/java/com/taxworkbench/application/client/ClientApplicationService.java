package com.taxworkbench.application.client;

import com.taxworkbench.application.error.ConflictException;
import com.taxworkbench.application.error.InvalidRequestException;
import com.taxworkbench.application.error.NotFoundException;
import com.taxworkbench.application.shared.CursorCodec;
import com.taxworkbench.application.shared.CursorPage;
import com.taxworkbench.infrastructure.persistence.ClientEntity;
import com.taxworkbench.infrastructure.persistence.ClientJpaRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class ClientApplicationService implements ClientQueryUseCase, ClientCommandUseCase {

    private final ClientJpaRepository clientJpaRepository;

    public ClientApplicationService(ClientJpaRepository clientJpaRepository) {
        this.clientJpaRepository = clientJpaRepository;
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public CursorPage<ClientView> listClients(ClientListQuery query) {
        int pageSize = normalizePageSize(query.pageSize());
        var page = clientJpaRepository.findAll(
                specification(query).and(afterCursor(query.cursor())),
                PageRequest.of(0, pageSize + 1, Sort.by("name").ascending().and(Sort.by("id").ascending()))
        );
        boolean hasNext = page.getContent().size() > pageSize;
        List<ClientEntity> content = hasNext ? page.getContent().subList(0, pageSize) : page.getContent();
        String nextCursor = hasNext && !content.isEmpty()
                ? CursorCodec.encode(List.of(content.get(content.size() - 1).getName(), String.valueOf(content.get(content.size() - 1).getId())))
                : null;
        return new CursorPage<>(content.stream().map(this::toView).toList(), new CursorPage.Page(nextCursor, pageSize, hasNext));
    }

    @Override
    public ClientView createClient(CreateClientCommand command) {
        clientJpaRepository.findByBizNo(command.bizNo()).ifPresent(existing -> {
            throw new InvalidRequestException("A client with the same bizNo already exists.", List.of());
        });
        ClientEntity entity = new ClientEntity();
        entity.setName(command.name());
        entity.setBizNo(command.bizNo());
        entity.setType(command.type());
        entity.setStatus(command.status());
        entity.setTier(command.tier());
        return toView(clientJpaRepository.save(entity));
    }

    @Override
    public ClientView updateClient(UpdateClientCommand command) {
        ClientEntity entity = clientJpaRepository.findById(command.clientId())
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client %s does not exist.".formatted(command.clientId())));
        if (entity.getVersion() != command.version()) {
            throw new ConflictException("CLIENT_CONFLICT", "The client was modified by another user.", entity.getId(), entity.getVersion(), "system", entity.getUpdatedAt(), List.of());
        }
        if (command.status() == null && command.tier() == null) {
            throw new InvalidRequestException("At least one mutable client field must be provided.", List.of());
        }
        if (command.status() != null) {
            entity.setStatus(command.status());
        }
        if (command.tier() != null) {
            entity.setTier(command.tier());
        }
        return toView(clientJpaRepository.save(entity));
    }

    private Specification<ClientEntity> specification(ClientListQuery query) {
        return Specification.where(nameContains(query.name()))
                .and(equalsStatus(query.status()))
                .and(equalsType(query.type()))
                .and(equalsTier(query.tier()));
    }

    private Specification<ClientEntity> afterCursor(String cursor) {
        List<String> parts = CursorCodec.decode(cursor);
        if (parts.isEmpty()) {
            return (root, ignoredQuery, criteriaBuilder) -> criteriaBuilder.conjunction();
        }
        String name = parts.get(0);
        Long id = Long.parseLong(parts.get(1));
        return (root, ignoredQuery, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.greaterThan(root.get("name"), name),
                criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("name"), name),
                        criteriaBuilder.greaterThan(root.get("id"), id)
                )
        );
    }

    private Specification<ClientEntity> nameContains(String name) {
        return (root, ignoredQuery, criteriaBuilder) ->
                name == null || name.isBlank()
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    private Specification<ClientEntity> equalsStatus(Object status) {
        return (root, ignoredQuery, criteriaBuilder) ->
                status == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("status"), status);
    }

    private Specification<ClientEntity> equalsType(Object type) {
        return (root, ignoredQuery, criteriaBuilder) ->
                type == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("type"), type);
    }

    private Specification<ClientEntity> equalsTier(Object tier) {
        return (root, ignoredQuery, criteriaBuilder) ->
                tier == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("tier"), tier);
    }

    private ClientView toView(ClientEntity entity) {
        return new ClientView(
                entity.getId(),
                entity.getName(),
                entity.getBizNo(),
                entity.getType(),
                entity.getStatus(),
                entity.getTier(),
                entity.getVersion(),
                entity.getUpdatedAt()
        );
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return 50;
        }
        return Math.min(pageSize, 200);
    }
}
