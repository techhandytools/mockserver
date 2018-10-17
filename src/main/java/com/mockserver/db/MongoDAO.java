package com.mockserver.db;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mockserver.config.MongoDBConfig;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;

public class MongoDAO {

	private static final Logger logger = LoggerFactory.getLogger(MongoDAO.class);

	private String database;
	private String collection;
	private MongoClient client;

	public MongoDAO(final MongoDBConfig mongoDBConfig) {

		logger.info("Initializing the Mongo with the config :: " + mongoDBConfig);
		String hostname = mongoDBConfig.getHost();
		Integer portNumber = mongoDBConfig.getPort();
		String username = mongoDBConfig.getUsername();
		String password = mongoDBConfig.getPassword();
		this.database = mongoDBConfig.getDatabase();
		this.collection = mongoDBConfig.getCollection();

		if (client == null) {
			MongoCredential credential = MongoCredential.createScramSha1Credential(username, database, password.toCharArray());
			client = new MongoClient(new ServerAddress(hostname, portNumber), credential, new MongoClientOptions.Builder().build());
			logger.info("Mongo DB Connection created");
		}
	}

	/**
	 * Creating a document in the mongo collection
	 *
	 * @param document
	 */
	public void create(final Document document) {
		logger.info("Create a document in the mongo :: " + document);
		getCollection().insertOne(document);
	}

	/**
	 * Closing the Mongo client connection
	 */
	public void close() {
		client.close();
	}

	/**
	 * Reading all the document from a collection from Mongo
	 *
	 * @return
	 */
	public List<Document> readAll() {
		final List<Document> result = new ArrayList<>();
		FindIterable<Document> iterable = getCollection().find().projection(fields(excludeId()));
		iterable.forEach((Block<Document>) result::add);
		return result;
	}

	/**
	 * Saving the records in the collection in Mongo
	 *
	 * @param id
	 * @param save
	 * @return
	 */
	public boolean save(final UUID id, final String save) {
		Document saveDoc = Document.parse(save);
		if (!saveDoc.containsKey("id") || !saveDoc.containsKey("uuid")) {
			saveDoc.append("id", id.toString());
			saveDoc.append("uuid", id.toString());
		}
		getCollection().replaceOne((eq("id", id.toString())), saveDoc, new ReplaceOptions().upsert(true));
		return true;
	}


	/**
	 * To update multiple records in collection from Mongo
	 *
	 * @param filter
	 * @param update
	 */
	public void update(final Document filter, final Document update) {
		getCollection().updateMany(filter, update);
	}

	/**
	 * To update a record in collection from mongo
	 *
	 * @param filter
	 * @param update
	 */
	public void updateOne(final Document filter, final Document update) {
		getCollection().updateOne(filter, update);
	}

	/**
	 * To delete multiple records at a time
	 *
	 * @param filter
	 */
	public void delete(final Document filter) {
		getCollection().deleteMany(filter);
	}

	/**
	 * To Delete an record from the collection in Mongo
	 *
	 * @param id
	 */
	public void deleteOne(final UUID id) {
		try {
			getCollection().deleteOne(eq("id", id.toString()));
		} catch (Exception error) {
			logger.error("The exception occurred in deleteOne is :: " + error.getMessage());
			error.printStackTrace();
		}

	}

	/**
	 * Getting the collection from Mongo
	 *
	 * @return
	 */
	private MongoCollection<Document> getCollection() {
		MongoDatabase database = getDatabase();
		return database.getCollection(collection);
	}

	/**
	 * Getting the data from Mongo
	 *
	 * @return
	 */
	public MongoDatabase getDatabase() {
		return client.getDatabase(database);
	}


}

