package fi.vm.yti.terminology.api.integration;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.elasticsearch.common.inject.Singleton;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Singleton
@Service
public class LuceneQueryFactory {

    private static final String plainQueryPatternString = "^(?:(?!(?:\\s++|^)(?:AND|OR|TO)(?:\\s|$))(?:\\w++|\\s++|(?<=\\w)-++))+$";
    private static final String asteriskQueryPatternString = "^(?:(?!(?:\\s++|^)(?:AND|OR|TO)(?:\\s|$))(?:\\w++|\\s++|(?<=[\\w*])-++|(?<!\\*)\\*(?=[\\w-])|(?<=[\\w-])\\*(?!\\*)))+$";
    private static final Logger LOG = LoggerFactory.getLogger(LuceneQueryFactory.class);
    private final Pattern plainQueryPattern = Pattern.compile(plainQueryPatternString, Pattern.UNICODE_CHARACTER_CLASS);
    private final Pattern plainSplitter = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private final Pattern givenQueryPattern;

    @Autowired
    public LuceneQueryFactory() {
        givenQueryPattern = Pattern.compile(asteriskQueryPatternString, Pattern.UNICODE_CHARACTER_CLASS);
    }

    QueryStringQueryBuilder buildPrefixSuffixQuery(final String searchTerm) {
        if (searchTerm != null) {
            final String trimmed = searchTerm.trim().toLowerCase();
            if (!searchTerm.isEmpty()) {
                String parsedQuery = null;
                if (plainQueryPattern.matcher(trimmed).matches()) {
                    final String[] splitQuery = plainSplitter.split(trimmed);
                    if (splitQuery.length == 1) {
                        parsedQuery = trimmed + " OR " + trimmed + "* OR *" + trimmed;
                    } else if (splitQuery.length > 1) {
                        parsedQuery = Arrays.stream(splitQuery).map(q -> "(" + q + " OR " + q + "* OR *" + q + ")").collect(Collectors.joining(" AND "));
                    }
                } else if (givenQueryPattern.matcher(trimmed).matches()) {
                    parsedQuery = trimmed;
                }
                if (parsedQuery != null) {
                    final StandardQueryParser parser = new StandardQueryParser();
                    try {
                        parser.setAllowLeadingWildcard(true);
                        LOG.info(" BuildLuceneQuery"+parsedQuery.toString());
                        return QueryBuilders.queryStringQuery(parser.parse(parsedQuery, "").toString());
                    } catch (final QueryNodeException e) {
                        // nop
                    }
                }
            }
        }
        LOG.error("Search term string disqualified: '" + searchTerm + "'");
        return null;
    }
}
