package fi.csc.termed.search;

import fi.csc.termed.search.service.ElasticSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReindexTask {

	private final ElasticSearchService esService;

	@Autowired
	public ReindexTask(ElasticSearchService esService) {
		this.esService = esService;
	}

	@Scheduled(cron = "0 0 3 * * *")
	public void reindex() {
		this.esService.reindex();
	}
}
