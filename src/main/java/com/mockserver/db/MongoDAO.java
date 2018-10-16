package com.mockserver.db;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.Document;

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

	private String database;
	private String collection;

	private MongoClient client;

	public MongoDAO(MongoDBConfig db) {

		String hostname = db.getHost();
		Integer portNumber = db.getPort();
		String username = db.getUsername();
		String password = db.getPassword();
		this.database = db.getDatabase();
		this.collection = db.getCollection();

		if (client == null) {
			MongoCredential credential = MongoCredential.createScramSha1Credential(username, database, password.toCharArray());
			client = new MongoClient(new ServerAddress(hostname, portNumber), credential, new MongoClientOptions.Builder().build());
		}
	}

	public void create(Document document) {
		getCollection().insertOne(document);
	}

	public void close() {
		client.close();
	}

	public List<Document> readAll() {
		final List<Document> result = new ArrayList<>();
		FindIterable<Document> iterable = getCollection().find().projection(fields(excludeId()));
		iterable.forEach((Block<Document>) result::add);
		return result;
	}

	public boolean save(UUID id, String save) {
		Document saveDoc = Document.parse(save);
		if (!saveDoc.containsKey("id") || !saveDoc.containsKey("uuid")) {
			saveDoc.append("id", id.toString());
			saveDoc.append("uuid", id.toString());
		}
		getCollection().replaceOne((eq("id", id.toString())), saveDoc, new ReplaceOptions().upsert(true));
		return true;
	}

	public void update(Document filter, Document update) {
		getCollection().updateMany(filter, update);
	}

	public void updateOne(Document filter, Document update) {
		getCollection().updateOne(filter, update);
	}

	public void delete(Document filter) {
		getCollection().deleteMany(filter);
	}

	public void deleteOne(UUID id) {
		try {
			getCollection().deleteOne(eq("id", id.toString()));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private MongoCollection<Document> getCollection() {
		MongoDatabase database = getDatabase();
		return database.getCollection(collection);
	}

	public MongoDatabase getDatabase() {
		return client.getDatabase(database);
	}


}

