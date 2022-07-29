package fi.vm.yti.terminology.api.validation;

import fi.vm.yti.terminology.api.frontend.Status;
import fi.vm.yti.terminology.api.model.termed.GenericDeleteAndSave;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.model.termed.NodeType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenericDeleteAndSaveValidator extends BaseValidator implements
        ConstraintValidator<ValidGenericDeleteAndSave, GenericDeleteAndSave> {

    @Override
    public boolean isValid(GenericDeleteAndSave deleteAndSave, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);
        Stream.concat(deleteAndSave.getSave().stream(), deleteAndSave.getPatch().stream()).forEach(node -> {
            //These checks are split into smaller functions to improve readability and documentation
            checkPropertiesAndPrefLabel(node, context);
            checkNodeType(node, context);
        });


        return !isConstraintViolationAdded();
    }


    /**
     * Checks if generic node has properties node and if the properties node contains prefLabel
     * Adds constraint violation if check doesn't pass
     * @param node Node to check
     * @param context Constraint validator context
     */
    private void checkPropertiesAndPrefLabel(GenericNode node, ConstraintValidatorContext context){
        final var prefLabel = "prefLabel";
        final var properties = node.getProperties();
        if (properties.isEmpty() ) {
            this.addConstraintViolation(context, MISSING_VALUE,"properties");
        }else if(!node.getType().getId().equals(NodeType.Concept)
                && (properties.get(prefLabel) == null || properties.get(prefLabel).isEmpty())){
            this.addConstraintViolation(context, MISSING_VALUE, prefLabel);
        }
    }

    /**
     * Checks if node has a node type,
     * if node type is Concept or Term check status
     * If node type is Concept check if it has a single prefLabelXl
     * @param node Node to check
     * @param context Constraint validator context
     */
    private void checkNodeType(GenericNode node, ConstraintValidatorContext context){
        //validate like vocabulary node if concept or term
        final var nodeType = node.getType().getId();
        if (nodeType == null) {
            this.addConstraintViolation(context, MISSING_VALUE, "type");
        } else if (nodeType.equals(NodeType.Concept) || nodeType.equals(NodeType.Term)) {
            checkStatus(node, context);
            if (nodeType.equals(NodeType.Concept)) {
                final var prefLabelXl = node.getReferences().get("prefLabelXl");
                if (prefLabelXl == null || prefLabelXl.size() != 1) {
                    addConstraintViolation(context, MISSING_VALUE, "prefLabelXl");
                }
            }
        }
    }

    /**
     * Check that status exists and not empty,
     * Status must also be one of {@link Status}
     * @param node Node to check
     * @param context Constraint validator context
     */
    private void checkStatus(GenericNode node, ConstraintValidatorContext context){
        final var statusProperty = "status";
        final var status = node.getProperties().get(statusProperty);
        if (status == null || status.isEmpty()) {
            addConstraintViolation(
                    context,
                    MISSING_VALUE,
                    statusProperty);
        } else {
            // status must be one of Status enum
            if (status.size() != 1 || !getStatusNames().contains(status.get(0).getValue())) {
                addConstraintViolation(context, "Invalid value", statusProperty);
            }
        }
    }
    private static List<String> getStatusNames() {
        return Arrays.stream(Status.class.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}
