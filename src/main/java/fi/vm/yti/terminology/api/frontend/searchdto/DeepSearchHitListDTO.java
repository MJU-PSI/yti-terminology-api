package fi.vm.yti.terminology.api.frontend.searchdto;

import java.util.List;

public abstract class DeepSearchHitListDTO<T> {
    public enum Type {CONCEPT};

    private final Type type;
    private int totalHitCount;
    private List<T> topHits;

    protected DeepSearchHitListDTO(Type type) {
        this.type = type;
    }

    protected DeepSearchHitListDTO(Type type, int totalHitCount, List<T> topHits) {
        this.type = type;
        this.totalHitCount = totalHitCount;
        this.topHits = topHits;
    }

    public Type getType() {
        return type;
    }

    public int getTotalHitCount() {
        return totalHitCount;
    }

    public void setTotalHitCount(final int totalHitCount) {
        this.totalHitCount = totalHitCount;
    }

    public List<T> getTopHits() {
        return topHits;
    }

    public void setTopHits(final List<T> topHits) {
        this.topHits = topHits;
    }
}
