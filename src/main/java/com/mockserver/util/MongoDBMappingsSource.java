package com.mockserver.util;

import java.util.List;

import org.apache.curator.framework.recipes.shared.SharedCount;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.standalone.MappingsSource;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.stubbing.StubMappings;
import com.mockserver.db.MongoDAO;

public class MongoDBMappingsSource implements MappingsSource {

	private static final Logger logger = LoggerFactory.getLogger(MongoDBMappingsSource.class);
	private MongoDAO mongoDAO;
	private SharedCount sharedCount;

	public MongoDBMappingsSource(final MongoDAO mongoDAO, final SharedCount sharedCount) {
		this.mongoDAO = mongoDAO;
		this.sharedCount = sharedCount;
	}

	@Override
	public void save(final List<StubMapping> stubMappings) {
		logger.info("Saving the list of stub mappings");
		logger.info("Stub mappings list :: " + stubMappings);
		for (StubMapping mapping : stubMappings) {
			if (mapping != null && mapping.isDirty()) {
				save(mapping);
			}
		}
	}

	@Override
	public void save(final StubMapping stubMapping) {
		logger.info("Saving the stub mappings");
		if (mongoDAO.save(stubMapping.getId(), StubMapping.buildJsonStringFor(stubMapping))) {
			stubMapping.setDirty(false);
			try {
				if (sharedCount.getCount() == 0) {
					sharedCount.setCount(1);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void remove(final StubMapping stubMapping) {
		logger.info("Removing the stub mappings");
		mongoDAO.deleteOne(stubMapping.getId());
	}

	@Override
	public void removeAll() {
	}

	@Override
	public void loadMappingsInto(final StubMappings stubMappings) {
		logger.info("Loading maps into stubs");
		List<Document> readDoc = mongoDAO.readAll();

		for (Document doc : readDoc) {
			StubMapping mapping = StubMapping.buildFrom(doc.toJson());
			mapping.setDirty(false);
			stubMappings.addMapping(mapping);
		}

	}
}
