package fi.vm.yti.terminology.api;

import fi.vm.yti.terminology.api.index.ElasticSearchService;
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
