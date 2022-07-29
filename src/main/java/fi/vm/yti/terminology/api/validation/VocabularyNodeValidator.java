package fi.vm.yti.terminology.api.validation;

import fi.vm.yti.terminology.api.frontend.Status;
import fi.vm.yti.terminology.api.frontend.TerminologyType;
import fi.vm.yti.terminology.api.model.termed.Attribute;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.model.termed.Identifier;
import org.apache.commons.collections4.CollectionUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VocabularyNodeValidator extends BaseValidator implements
        ConstraintValidator<ValidVocabularyNode, GenericNode> {

    @Override
    public boolean isValid(GenericNode genericNode, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);

        final var properties = genericNode.getProperties();
        if (properties.isEmpty()) {
            addConstraintViolation(
                    context,
                    MISSING_VALUE,
                    "properties");
        }

        //
        // language
        //
        final var languages = properties.get("language");
        if (languages == null || languages.isEmpty()) {
            addConstraintViolation(
                    context,
                    MISSING_VALUE,
                    "language");
        }

        //
        // terminologyType
        //
        //
        final var terminologyTypeProperty = "terminologyType";
        final var terminologyType = properties.get(terminologyTypeProperty);
        if (terminologyType == null) {
            addConstraintViolation(
                    context,
                    MISSING_VALUE,
                    terminologyTypeProperty);
        }
        else if (terminologyType.size() != 1) {
            addConstraintViolation(
                    context,
                    "Invalid value",
                    terminologyTypeProperty);
        } else {
            final var validTypes = new String[]{
                    TerminologyType.TERMINOLOGICAL_VOCABULARY.name(),
                    TerminologyType.OTHER_VOCABULARY.name()
            };
            if (!Arrays.asList(validTypes).contains(terminologyType.get(0).getValue())) {
                addConstraintViolation(
                        context,
                        "Invalid value",
                        "terminologyType");
            }
        }

        // type.id should always match TerminologicalVocabulary
        final var vocabularyType = genericNode.getType();
        if (vocabularyType.getId() == null ||
                !vocabularyType.getId().toString().equals("TerminologicalVocabulary")) {
            addConstraintViolation(
                    context,
                    MISSING_VALUE,
                    "type");
        }

        //
        // status
        //
        final var statusProperty = "status";
        final var status = properties.get(statusProperty);
        if (status == null || status.isEmpty()) {
            addConstraintViolation(
                    context,
                    MISSING_VALUE,
                    statusProperty);
        } else {
            // status must be one of Status enum
            if (status.size() != 1 || !List.of(getStatusNames())
                    .contains(status.get(0).getValue())) {
                addConstraintViolation(
                        context,
                        "Invalid value",
                        statusProperty);
            }
        }

        // list of language values, which will be used in later checks
        var langValues = languages == null ? Collections.emptyList() : languages.stream()
                .map(Attribute::getValue)
                .collect(Collectors.toList());

        //
        // prefLabel
        //
        final var prefLabelProperty = "prefLabel";
        final var prefLabel = properties.get(prefLabelProperty);
        if (prefLabel == null || prefLabel.isEmpty()) {
            addConstraintViolation(
                    context,
                    MISSING_VALUE,
                    prefLabelProperty);
        } else {
            // should have one label for each language
            var labelLanguages = prefLabel.stream()
                    .map(Attribute::getLang)
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEqualCollection(langValues, labelLanguages)) {
                addConstraintViolation(
                        context,
                        "Language mismatch",
                        prefLabelProperty);
            }

            // empty strings as values?
            var emptyValues = prefLabel.stream()
                    .filter(label -> label.getValue().trim().isEmpty())
                    .collect(Collectors.toList());
            if (!emptyValues.isEmpty()) {
                addConstraintViolation(
                        context,
                        MISSING_VALUE,
                        prefLabelProperty);
            }
        }

        //
        // references
        //
        final var references = genericNode.getReferences();
        if (references == null || references.size() == 0) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "references");
        }

        // contributor
        if (!references.containsKey("contributor") || references.get("contributor").size() == 0) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "contributor");
        }

        // inGroup
        if (!references.containsKey("inGroup") || references.get("inGroup").size() == 0) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "inGroup");
        }

        return !isConstraintViolationAdded();
    }

    private static String[] getStatusNames() {
        return Arrays.stream(Status.class.getEnumConstants())
                .map(Enum::name)
                .toArray(String[]::new);
    }
}
