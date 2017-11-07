package fi.vm.yti.terminology.api.model.termed;

public final class TermedLangValue {

    private final String lang;
    private final String value;
    private final String regex;

    // Jackson constructor
    private TermedLangValue() {
        this("", "", "");
    }

    public TermedLangValue(String lang, String value, String regex) {
        this.lang = lang;
        this.value = value;
        this.regex = regex;
    }

    public String getLang() {
        return lang;
    }

    public String getValue() {
        return value;
    }

    public String getRegex() {
        return regex;
    }
}
