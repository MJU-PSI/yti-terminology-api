package fi.vm.yti.terminology.api.model.termed;

import java.util.List;

public final class DeleteAndSave {

    private final List<TermedIdentifier> delete;
    private final List<? extends Object> save;

    public DeleteAndSave(List<TermedIdentifier> delete, List<? extends Object> save) {
        this.delete = delete;
        this.save = save;
    }

    public List<TermedIdentifier> getDelete() {
        return delete;
    }

    public List<? extends Object> getSave() {
        return save;
    }
}
