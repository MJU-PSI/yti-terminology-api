package fi.vm.yti.terminology.api.validation;

import fi.vm.yti.terminology.api.model.termed.GenericNode;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.annotation.*;

public class VocabularyNodeValidator implements
        ConstraintValidator<ValidVocabularyNode, GenericNode>, Annotation {

    private boolean constraintViolationAdded;

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }

    @Override
    public boolean isValid(GenericNode genericNode, ConstraintValidatorContext context) {
        this.constraintViolationAdded = false;

        final var properties = genericNode.getProperties();
        if (properties.size() == 0) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "properties");
        }

        final var prefLabel = properties.get("prefLabel");
        if (prefLabel == null || prefLabel.size() == 0 || prefLabel.get(0).getValue().isEmpty()) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "prefLabel");
        }

        final var references = genericNode.getReferences();
        if (references == null || references.size() == 0) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "references");
        }

        if (!references.containsKey("contributor") || references.get("contributor").size() == 0) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "contributor");
        }

        if (!references.containsKey("inGroup") || references.get("inGroup").size() == 0) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "inGroup");
        }

        return !this.constraintViolationAdded;
    }

    private void addConstraintViolation(
            ConstraintValidatorContext context,
            String message,
            String property) {

        if (!this.constraintViolationAdded) {
            context.disableDefaultConstraintViolation();
            this.constraintViolationAdded = true;
        }

        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(property)
                .addConstraintViolation();
    }
}
