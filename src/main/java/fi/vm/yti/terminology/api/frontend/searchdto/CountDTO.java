package fi.vm.yti.terminology.api.frontend.searchdto;

import java.util.Collections;
import java.util.Map;

public class CountDTO {

    public enum Category {

        TERMINOLOGICAL_VOCABULARY("TerminologicalVocabulary"),
        OTHER_VOCABULARY("OtherVocabulary"),
        CONCEPT("Concept"),
        COLLECTION("Collection");

        private final String name;

        Category(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /*
    class CategoriesCountDTO {
        private long terminologicalVocabulary;
        private long otherVocabulary;
        private long concept;
        private long collection;

        public CategoriesCountDTO() {
            this.terminologicalVocabulary = 0;
            this.otherVocabulary = 0;
            this.concept = 0;
            this.collection = 0;
        }

        public CategoriesCountDTO(Map<String, Long> categories) {
            this.terminologicalVocabulary = categories.getOrDefault(
                    "TerminologicalVocabulary", 0L);
            this.otherVocabulary = categories.getOrDefault(
                    "OtherVocabulary", 0L);
            this.concept = categories.getOrDefault(
                    "Concept", 0L);
            this.collection = categories.getOrDefault("Collection", 0L);
        }

        public long getTerminologicalVocabulary() {
            return terminologicalVocabulary;
        }

        public void setTerminologicalVocabulary(int terminologicalVocabulary) {
            this.terminologicalVocabulary = terminologicalVocabulary;
        }

        public long getOtherVocabulary() {
            return otherVocabulary;
        }

        public void setOtherVocabulary(int otherVocabulary) {
            this.otherVocabulary = otherVocabulary;
        }

        public long getConcept() {
            return concept;
        }

        public void setConcept(int concept) {
            this.concept = concept;
        }

        public long getCollection() {
            return collection;
        }

        public void setCollection(long collection) {
            this.collection = collection;
        }
    }
    */

    private Map<String, Long> categories;
    private Map<String, Long> statuses;
    private Map<String, Long> groups;

    public CountDTO() {
        this.categories = Collections.emptyMap();
        this.statuses = Collections.emptyMap();
        this.groups = Collections.emptyMap();
    }

    public CountDTO(
            final Map<String, Long> categories,
            final Map<String, Long> statuses,
            final Map<String, Long> groups) {
        this.categories = categories;
        this.statuses = statuses;
        this.groups = groups;
    }

    public Map<String, Long> getCategories() {
        return categories;
    }

    public void setCategories(Map<String, Long> categories) {
        this.categories = categories;
    }

    public Map<String, Long> getStatuses() {
        return statuses;
    }

    public void setStatuses(Map<String, Long> statuses) {
        this.statuses = statuses;
    }

    public Map<String, Long> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Long> groups) {
        this.groups = groups;
    }
}
