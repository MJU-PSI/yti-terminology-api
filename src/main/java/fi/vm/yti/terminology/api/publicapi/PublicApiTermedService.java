package fi.vm.yti.terminology.api.publicapi;

import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.Attribute;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.util.Parameters;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static fi.vm.yti.terminology.api.model.termed.VocabularyNodeType.TerminologicalVocabulary;
import static fi.vm.yti.terminology.api.model.termed.VocabularyNodeType.Vocabulary;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpMethod.GET;

@Service
public class PublicApiTermedService {

    private final TermedRequester termedRequester;

    @Autowired
    public PublicApiTermedService(TermedRequester termedRequester) {
        this.termedRequester = termedRequester;
    }

    @SuppressWarnings("Duplicates")
    @NotNull List<PublicApiVocabulary> getVocabularyList() {
        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "properties.prefLabel");

        params.add("where",
                "type.id:" + Vocabulary +
                        " OR type.id:" + TerminologicalVocabulary);

        params.add("max", "-1");

        List<GenericNode> vocabularies = requireNonNull(termedRequester.exchange("/node-trees", GET, params, new ParameterizedTypeReference<List<GenericNode>>() {}));
        return getAsPublicApiVocabularies(vocabularies);
    }

    private List<PublicApiVocabulary> getAsPublicApiVocabularies(List<GenericNode> vocabularies) {
        List<PublicApiVocabulary> result = new ArrayList<>();
        for(GenericNode genericNode : vocabularies) {
            PublicApiVocabulary vocabulary = new PublicApiVocabulary();
            vocabulary.setId(genericNode.getId());
            vocabulary.setPrefLabel(prefLabelAsLocalizable(genericNode));
            result.add(vocabulary);
        }
        return result;
    }

    public  HashMap<String, String> prefLabelAsLocalizable(GenericNode node) {

        HashMap<String, String> result = new HashMap<>();

        for (Attribute prefLabel : node.getProperties().get("prefLabel")) {
            result.put(prefLabel.getLang(), prefLabel.getValue());
        }

        return result;

    }
}
