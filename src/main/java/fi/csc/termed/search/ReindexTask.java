package fi.csc.termed.search;

import fi.csc.termed.search.service.ElasticSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Created by jmlehtin on 28/4/2017.
 */

@Component
public class ReindexTask {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private ElasticSearchService esService;

	@Autowired
	public ReindexTask(ElasticSearchService esService) {
		this.esService = esService;
	}

	@Scheduled(cron = "0 0 3 * * *")
	public void reindex() {
		log.info("Starting nightly reindexing task..");
		esService.deleteAllDocumentsFromIndex();
		esService.doFullIndexing();
		log.info("Finished nightly reindexing!");
	}
}
