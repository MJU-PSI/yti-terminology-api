package fi.vm.yti.terminology.api.model.termed;

import java.util.List;

public interface DeleteAndSave {

    List<Identifier> getDelete();
    List<? extends Node> getSave();
}
