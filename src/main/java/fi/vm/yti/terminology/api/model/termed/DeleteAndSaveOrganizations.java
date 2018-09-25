package fi.vm.yti.terminology.api.model.termed;

import java.util.List;

public class DeleteAndSaveOrganizations implements DeleteAndSave {

    private final List<Identifier> delete;
    private final List<OrganizationNode> save;

    public DeleteAndSaveOrganizations(List<Identifier> delete, List<OrganizationNode> save) {
        this.delete = delete;
        this.save = save;
    }

    public List<Identifier> getDelete() {
        return delete;
    }

    public List<OrganizationNode> getSave() {
        return save;
    }
}
