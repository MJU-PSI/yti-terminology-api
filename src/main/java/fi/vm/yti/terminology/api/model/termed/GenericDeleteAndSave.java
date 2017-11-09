package fi.vm.yti.terminology.api.model.termed;

import java.util.List;

import static java.util.Collections.emptyList;

public final class GenericDeleteAndSave implements DeleteAndSave {

    private final List<Identifier> delete;
    private final List<GenericTermedNode> save;

    // Jackson constructor
    private GenericDeleteAndSave() {
        this(emptyList(), emptyList());
    }

    public GenericDeleteAndSave(List<Identifier> delete, List<GenericTermedNode> save) {
        this.delete = delete;
        this.save = save;
    }

    public List<Identifier> getDelete() {
        return delete;
    }

    public List<GenericTermedNode> getSave() {
        return save;
    }
}
