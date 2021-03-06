package by.psu.merger;

import by.psu.model.postgres.Translate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;

@Component
public class TranslateObjectMerger implements BaseMerger<Translate> {

    private final StringValueMerger stringValueMerger;

    @Autowired
    public TranslateObjectMerger(StringValueMerger stringValueMerger) {
        this.stringValueMerger = stringValueMerger;
    }

    @Override
    public Translate merge(Translate first, Translate second) {
        if ( isNotValidTranslate(first) ) {
            if ( nonNull(first.getId()) ) {
                second.setId(first.getId());
            }
            return second;
        }

        if ( isNotValidTranslate(second) ) {
            return first;
        }

        first.setListValues( stringValueMerger.merge( first.getValues(), second.getValues() ) );

        return first;
    }

    private boolean isNotValidTranslate (Translate obj) {
        return obj == null || obj.getValues() == null || obj.getValues().isEmpty();
    }
}
