package by.psu.service.api2;

import by.psu.model.postgres.Language;
import by.psu.model.postgres.Nsi;
import by.psu.model.postgres.StringValue;
import by.psu.model.postgres.Translate;
import by.psu.model.postgres.repository.RepositoryNsi;
import javassist.tools.web.BadHttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import javax.persistence.criteria.*;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

abstract public class ServiceNsi<T extends Nsi> {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    protected RepositoryNsi<T> repositoryNsi;

    @PersistenceContext
    private EntityManager entityManager;

    private Class<T> type;


    public ServiceNsi() {
        this.type = (Class<T>) ((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }


    public List<T> getAll() {
        return repositoryNsi.findAll();
    }

    public T getOne(UUID uuid) {
        return repositoryNsi.getOne(uuid);
    }

    protected Optional<T> isExists(T nsi) {
        if (nsi == null) {
            return Optional.empty();
        }

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(type);
        Root<T> root = criteriaQuery.from(type);

        Subquery<StringValue> stringValueSubquery = criteriaQuery.subquery(StringValue.class);
        Root<StringValue> stringValueRoot = stringValueSubquery.from(StringValue.class);

        Optional<StringValue> stringValueEn = LanguageUtil.getValueByLanguage(nsi.getTitle(), Language.EN);
        Optional<StringValue> stringValueRu = LanguageUtil.getValueByLanguage(nsi.getTitle(), Language.RU);

        List<Predicate> expressions = new ArrayList<>();

        stringValueEn.ifPresent(stringValue -> expressions.add(
                criteriaBuilder.like(criteriaBuilder.lower(stringValueRoot.get("value")), stringValue.getValue().toLowerCase())
        ));

        stringValueRu.ifPresent(stringValue -> expressions.add(
                criteriaBuilder.like(criteriaBuilder.lower(stringValueRoot.get("value")), stringValue.getValue().toLowerCase())
        ));

        if (expressions.isEmpty()) {
            throw new RuntimeException("Nsi doesn't have a valid value");
        }

        Predicate predicate = expressions.size() == 1 ? expressions.get(0) :
                    criteriaBuilder.or(expressions.toArray(new Predicate[0]));

        stringValueSubquery.select(stringValueRoot)
                .where(
                        criteriaBuilder.and(
                                predicate,
                                criteriaBuilder.equal(root.get("title"), stringValueRoot.get("translate"))
                        )
                );

        criteriaQuery.select(root).where(criteriaBuilder.exists(stringValueSubquery));
        TypedQuery<T> tTypedQuery = entityManager.createQuery(criteriaQuery);
        //tTypedQuery.unwrap(org.hibernate.Query.class).getQueryString();
        List<T> objList = tTypedQuery.getResultList();
        return objList.stream().findFirst();
    }

    @Transactional
    public T update(T nsi) {
        Optional<T> optionalNsi = Optional.of(nsi);
        optionalNsi.orElseThrow(() -> new RuntimeException("Nsi is null", new BadHttpRequest()));
        optionalNsi.map(Nsi::getId).orElseThrow(() -> new RuntimeException("Id is null"));

        Optional<T> findNsi = Optional.of(repositoryNsi.getOne(nsi.getId()));
        findNsi.orElseThrow(() -> new RuntimeException(new EntityNotFoundException("Nsi not found")));

        Translate translate = optionalNsi.map(Nsi::getTitle).orElseThrow(() -> new RuntimeException(new EntityNotFoundException("Nsi title is null")));
        Translate translateFind = findNsi.map(Nsi::getTitle).orElseThrow(() -> new RuntimeException(new EntityNotFoundException("Find nsi (BD) title is null")));

        if ( !translate.getValues().isEmpty() && !translateFind.getValues().isEmpty() ) {
            findNsi.get().getTitle().setListValues(nsi.getTitle().getValues());
        }

        return repositoryNsi.save(findNsi.get());
    }

    @Transactional
    public T save(T nsi) {
        Optional<T> optionalNsi = Optional.ofNullable(nsi);
        optionalNsi.orElseThrow(() -> new RuntimeException("Nsi is null", new BadHttpRequest()));

        if ( optionalNsi.map(Nsi::getId).isPresent() ) {
             throw new RuntimeException(new EntityExistsException("Id is not null"));
        }

        optionalNsi.map(Nsi::getTitle).orElseThrow(() -> new RuntimeException("Nsi title is null", new BadHttpRequest()));

        optionalNsi.get().getTitle().setListValues(optionalNsi.get().getTitle().getValues());

        return isExists(optionalNsi.get()).orElseGet(() -> repositoryNsi.save(optionalNsi.get()));
    }

    public List<T> place(List<T> nsiCollection) {

        if (isNull(nsiCollection)) {
            return new ArrayList<>();
        }

        return nsiCollection.stream()
                .map(nsiItem -> isExists(nsiItem).orElseGet(() -> repositoryNsi.save(nsiItem)))
                .collect(Collectors.toList());
    }
}
