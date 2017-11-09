package fi.vm.yti.terminology.api.model.termed;

import java.util.List;

import static java.util.Collections.emptyList;

public class UpdateOrganizations implements DeleteAndSave {

    private final List<Identifier> delete = emptyList();
    private final List<OrganizationNode> save;

    public UpdateOrganizations(List<OrganizationNode> save) {
        this.save = save;
    }

    public List<Identifier> getDelete() {
        return delete;
    }

    public List<OrganizationNode> getSave() {
        return save;
    }
}
