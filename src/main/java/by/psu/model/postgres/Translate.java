package by.psu.model.postgres;

import lombok.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;


@Entity(
        name = "translate"
)
@Table(
        name = "translate"
)
@Getter @Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "values")
@ToString(exclude = "values", callSuper = true)
public class Translate extends BasicEntity {

    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "translate_id")
    private List<StringValue> values;

    public Optional<StringValue> setValue(StringValue stringValue) {
        Optional<StringValue> optionalValue = Optional.ofNullable(stringValue);

        if ( optionalValue.isPresent() ) {
            Optional.of(stringValue.getLanguage()).orElseThrow(() -> new RuntimeException("Language is null"));

            Arrays.stream(Language.values())
                    .filter(language -> language.getUuid().equals(optionalValue.get().getLanguage()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Language with id" + optionalValue.get().getLanguage() + " not supported"));

            Optional.of(stringValue.getValue()).orElseThrow(() -> new RuntimeException("Value is null or empty"));
            values = Optional.ofNullable(values).orElseGet(ArrayList::new);

            Optional<StringValue> stringValueFounded = values.stream()
                    .filter(value -> value.getLanguage().equals(optionalValue.get().getLanguage()))
                    .findFirst();

            if ( values.isEmpty() || !stringValueFounded.isPresent() ) {
                values.add(optionalValue.get());
                return optionalValue;
            } else {
                stringValueFounded.get().setValue(optionalValue.get().getValue());
                return stringValueFounded;
            }
        }

        return Optional.empty();
    }

    public Optional<List<StringValue>> setListValues(List<StringValue> values) {
        Optional<List<StringValue>> optionalStringValueList = Optional.ofNullable(values);
        return optionalStringValueList
                .map(values1 -> values1.stream()
                        .map(value -> setValue(value).orElse(null))
                        .filter(Objects::isNull)
                        .collect(Collectors.toList())
                );
    }
}

